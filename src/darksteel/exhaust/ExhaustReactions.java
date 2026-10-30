package darksteel.exhaust;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

/**
 * 废气泄漏 —— 机制的「爆炸与连锁」部分。
 * <p>
 * 一次反应的完整流程：
 * <ol>
 *     <li>爆心消失，半径 {@code parameters.radius} 内的建筑全部登记；</li>
 *     <li>半径内的单位吃 {@code unitDamage}；</li>
 *     <li>半径内的建筑各灌 {@code propagatedEnergy} 点废气：<br>
 *         核心只是掉血（{@code coreDamage}），因为核心不参与废气积累；</li>
 *     <li>被灌满的建筑：是 {@link ExhaustExplosive} 就 <b>排进队列连锁引爆</b>，
 *         否则原地 <b>泄漏</b>（拆掉 + 按足迹铺残骸）；</li>
 *     <li>铺残骸、放特效。</li>
 * </ol>
 * <b>不改地形</b>：爆炸只拆建筑、不替换地板/矿石/墙，也不吹飞装饰物。
 * <p>
 * 所有连锁都走 {@link #queue} + {@link Core#app#post}，绝不在一次递归里做完，
 * 因此几百个建筑一起炸也不会爆栈；同时用 {@link #queued} / {@link #processed}
 * 保证同一格在一次连锁里只处理一次。
 */
public class ExhaustReactions {

    private static final Seq<Reaction> queue = new Seq<>();
    /** 已排队的地块，防重复入队。 */
    private static final IntSet queued = new IntSet();
    /** 本轮已处理过的地块。 */
    private static final IntSet processed = new IntSet();
    /** 正在等待延迟执行的"废气"，防止同一建筑被排两次。 */
    private static final IntSet pendingConversions = new IntSet();
    /** pos -> buildingId，标记这次废气是"强制"的（不看当前能量）。 */
    private static final IntMap<Integer> forcedConversions = new IntMap<>();

    private static boolean initialized;
    private static boolean scheduled;
    private static int worldGeneration;

    /**
     * 普通建筑（非 {@link ExhaustExplosive}）被废气时留下的残骸：
     * {@code [0]}=1x1，{@code [1]}=2x2，{@code [2]}=3x3；null = 不留残骸。
     */
    public static Block[] conversionRemnants;

    private ExhaustReactions() {
    }

    /** 必须在 {@code loadContent()} 里调用。 */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Events.on(EventType.WorldLoadBeginEvent.class, event -> clearAll());
        Events.on(EventType.ResetEvent.class, event -> clearAll());
    }

    // region 对外入口

    /**
     * 请求一次「废气」（不是爆炸）。只有已经顶到耐受度、且不是爆炸物的建筑才会被处理。
     * 建筑更新循环里调用它是标准做法：能量每涨一点就请求一次，满了就会废气。
     */
    public static void requestConversion(Building building) {
        if (Vars.net.client() || building == null || building.block instanceof CoreBlock
            || building instanceof ExhaustExplosive) {
            return;
        }
        if (!ExhaustStates.atCapacity(building)) {
            return;
        }
        scheduleConversion(building, false);
    }

    /** 强制废气：不看能量，直接干掉该建筑并铺残骸（逻辑处理器/调试用）。 */
    public static void forceConversion(Building building) {
        if (Vars.net.client() || building == null || building.tile == null || building.block instanceof CoreBlock) {
            return;
        }
        scheduleConversion(building, true);
    }

    /** 该建筑是否正在等待被废气（能量满的建筑应该停止工作）。 */
    public static boolean conversionPending(Building building) {
        if (building == null) {
            return false;
        }
        Integer buildingId = forcedConversions.get(building.pos());
        return buildingId != null && buildingId.intValue() == building.id();
    }

    /**
     * 请求引爆一个 {@link ExhaustExplosive}。
     * 排队执行，因此可以在 {@code updateTile} 里安全调用。
     */
    public static void request(Building source) {
        if (Vars.net.client() || source == null || source.tile == null
            || !(source instanceof ExhaustExplosive explosive)) {
            return;
        }
        ExhaustReactionParameters parameters = explosive.exhaustParameters();
        if (parameters == null) {
            // JSON/hjson 建的方块填不了 parameters，退化成普通泄漏（拆掉铺残骸），
            // 绝不能让它带着 null 参数进反应层炸穿队列。
            requestConversion(source);
            return;
        }
        Reaction reaction = capture(source, parameters);
        if (!queued.add(reaction.pos)) {
            return;
        }
        queue.add(reaction);

        if (!scheduled) {
            scheduled = true;
            Core.app.post(ExhaustReactions::processQueue);
        }
    }

    // endregion
    // region 队列

    private static void processQueue() {
        scheduled = false;
        if (Vars.net.client()) {
            clearQueue();
            return;
        }
        while (queue.any()) {
            Reaction reaction = queue.remove(0);
            if (!processed.add(reaction.pos)) {
                continue;   // 这一格本轮已经炸过了
            }
            process(reaction);
        }
        clearQueue();
    }

    private static Reaction capture(Building source, ExhaustReactionParameters parameters) {
        return new Reaction(source.pos(), source.id(), source.tile.x, source.tile.y,
            source.x, source.y, footprint(source).toArray(), parameters);
    }

    /** 建筑废气时会一起被清空的地块。异形建筑可以自己实现 {@link ExhaustFootprint}。 */
    private static IntSeq footprint(Building building) {
        IntSeq footprint = new IntSeq();
        if (building instanceof ExhaustFootprint custom) {
            custom.eachExhaustTile(tile -> addUnique(footprint, tile.pos()));
        } else {
            building.tile.getLinkedTilesAs(building.block, tile -> addUnique(footprint, tile.pos()));
        }
        return footprint;
    }

    // endregion
    // region 反应本体

    private static void process(Reaction reaction) {
        ExhaustReactionParameters parameters = reaction.parameters;
        int radius = Math.max(parameters.radius, 0);

        // 1. 收集半径内地块与建筑（圆形范围，与爆炸的视觉范围一致）
        Seq<Building> buildings = new Seq<>();
        IntSet buildingPositions = new IntSet();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy > radius * radius) {
                    continue;
                }
                Tile tile = Vars.world.tile(reaction.tileX + dx, reaction.tileY + dy);
                if (tile == null || tile.build == null || !buildingPositions.add(tile.build.pos())) {
                    continue;
                }
                buildings.add(tile.build);
            }
        }

        // 2. 单位伤害
        Seq<Unit> units = new Seq<>();
        Units.nearby((Team) null, reaction.worldX, reaction.worldY, radius * 8f, units::add);
        for (Unit unit : units) {
            if (unit.isValid()) {
                unit.damage(parameters.unitDamage);
            }
        }

        // 3. 残骸候选 = 爆心的足迹
        IntSeq remnants = new IntSeq();
        for (int position : reaction.footprint) {
            addUnique(remnants, position);
        }

        // 4. 半径内建筑：注能 → 连锁 / 废气
        for (Building building : buildings) {
            if (!building.isValid()) {
                continue;
            }

            if (building.block instanceof CoreBlock) {
                if (building instanceof ExhaustTolerant) {
                    ExhaustStates.addExhaust(building, parameters.propagatedEnergy);
                } else {
                    building.damage(parameters.coreDamage);
                }
                continue;
            }

            // 爆心自己：直接抹掉
            if (building.pos() == reaction.pos && building.id() == reaction.buildingId) {
                building.tile.removeNet();
                continue;
            }

            ExhaustStates.addExhaust(building, parameters.propagatedEnergy);
            if (!ExhaustStates.atCapacity(building)) {
                continue;
            }

            if (building instanceof ExhaustExplosive explosive) {
                // 连锁引爆：先拆掉自己再入队，避免它在下一轮又被处理
                Reaction chained = capture(building, explosive.exhaustParameters());
                building.tile.removeNet();
                if (queued.add(chained.pos)) {
                    queue.add(chained);
                }
            } else {
                scheduleConversion(building, true);
            }
        }

        // 5. 残骸（不改地形：爆炸只清建筑、不换地块）
        placeRemnants(remnants, parameters.remnants);

        // 6. 特效
        playEffects(reaction, radius);
    }

    /**
     * 延迟执行一次废气：延迟到下一帧是为了安全地改地块（可能正在被别的逻辑遍历）。
     * 执行前会重新校验"这一格还是不是当初那个建筑"。
     */
    private static void scheduleConversion(Building building, boolean forced) {
        int pos = building.pos();
        int buildingId = building.id();

        if (forced) {
            forcedConversions.put(pos, buildingId);
        }
        if (!pendingConversions.add(pos)) {
            return;
        }

        int scheduledGeneration = worldGeneration;
        int[] positions = footprint(building).toArray();
        Block[] remnantSet = conversionRemnants;

        Core.app.post(() -> {
            pendingConversions.remove(pos);
            Integer forcedId = forcedConversions.remove(pos);
            boolean force = forcedId != null && forcedId.intValue() == buildingId;

            // 换图了 / 客户端没有权威数据
            if (Vars.net.client() || scheduledGeneration != worldGeneration) {
                return;
            }

            Tile tile = Vars.world.tile(pos);
            Building current = tile == null ? null : tile.build;
            if (current == null || current.id() != buildingId) {
                return;
            }
            if (!force && !ExhaustStates.atCapacity(current)) {
                return;
            }

            current.tile.removeNet();

            IntSeq remnants = new IntSeq();
            for (int position : positions) {
                addUnique(remnants, position);
            }
            placeRemnants(remnants, remnantSet);
        });
    }

    /**
     * 在候选地块上贪心地铺残骸：优先 3x3，其次 2x2，剩下 1x1。
     * 这样废气过的基地才会看起来像一片真正的废墟，而不是一坨马赛克。
     */
    private static void placeRemnants(IntSeq remnants, Block[] set) {
        if (set == null || set.length == 0) {
            return;
        }

        IntSet available = new IntSet();
        for (int i = 0; i < remnants.size; i++) {
            Tile tile = Vars.world.tile(remnants.get(i));
            if (tile == null || tile.build != null || tile.block() instanceof CoreBlock) {
                continue;
            }
            available.add(tile.pos());
        }

        while (available.size > 0) {
            int position = first(available);
            int x = Point2.x(position);
            int y = Point2.y(position);

            int size = 1;
            if (set.length > 2 && set[2] != null && fits(available, x, y, 3) && Mathf.random(6) < 3) {
                size = 3;
            } else if (set.length > 1 && set[1] != null && fits(available, x, y, 2) && Mathf.random(6) < 2) {
                size = 2;
            }

            Block remnant = size == 3 ? set[2] : size == 2 ? set[1] : set[0];
            if (remnant != null) {
                // 残骸以左下角为锚点，需要按 sizeOffset 回退
                Tile anchor = Vars.world.tile(x - remnant.sizeOffset, y - remnant.sizeOffset);
                if (anchor != null) {
                    anchor.setNet(remnant, Team.derelict, 0);
                }
            }

            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    available.remove(Point2.pack(x + dx, y + dy));
                }
            }
        }
    }

    /** 取"最下、再最左"的候选格，保证残骸从边缘开始铺，看起来更自然。 */
    private static int first(IntSet positions) {
        int first = -1;
        IntSet.IntSetIterator iterator = positions.iterator();
        while (iterator.hasNext) {
            int position = iterator.next();
            if (first == -1
                || Point2.y(position) < Point2.y(first)
                || (Point2.y(position) == Point2.y(first) && Point2.x(position) < Point2.x(first))) {
                first = position;
            }
        }
        return first;
    }

    private static boolean fits(IntSet available, int x, int y, int size) {
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                if (!available.contains(Point2.pack(x + dx, y + dy))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void playEffects(Reaction reaction, int radius) {
        float effectRadius = radius * 8f;
        Effect effect = reaction.parameters.effect != null
            ? reaction.parameters.effect
            : ExhaustFx.exhaustExplosion;
        Color color = reaction.parameters.color != null
            ? reaction.parameters.color
            : ExhaustFx.exhaustColor;

        if (Vars.net.server()) {
            Call.effect(effect, reaction.worldX, reaction.worldY, effectRadius, color);
            Call.soundAt(Sounds.explosionReactor, reaction.worldX, reaction.worldY, 1f, 1f);
        }
        if (!Vars.headless) {
            effect.at(reaction.worldX, reaction.worldY, effectRadius, color);
            Sounds.explosionReactor.at(reaction.worldX, reaction.worldY, 1f, 1f);
        }
    }

    private static void addUnique(IntSeq values, int value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private static void clearQueue() {
        queue.clear();
        queued.clear();
        processed.clear();
    }

    private static void clearAll() {
        worldGeneration++;
        scheduled = false;
        clearQueue();
        pendingConversions.clear();
        forcedConversions.clear();
    }

    /** 一次排队中的反应快照。建筑可能在这一帧被拆掉，所以坐标、id、足迹都要先存下来。 */
    private record Reaction(int pos, int buildingId, int tileX, int tileY, float worldX, float worldY,
                            int[] footprint, ExhaustReactionParameters parameters) {
    }
}
