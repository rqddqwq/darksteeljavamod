package darksteel.exhaust;

import arc.Core;
import arc.Events;
import arc.struct.IntMap;
import arc.struct.IntSet;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import darksteel.world.blocks.exhaust.ExhaustBlock;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.io.SaveFileReader;
import mindustry.io.SaveVersion;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.StatUnit;

/**
 * 废气场 —— 整个机制的数据层。
 * <p>
 * 和电力、热量一样，废气是「挂在建筑身上的一层隐形数值」，不进背包、不走电网。
 * <p>
 * <b>本模组的废气方块会自己产生废气</b>（{@link ExhaustBlock#exhaustRate}，默认 0 = 不自产）：
 * {@link #init()} 挂了一个全局 update 钩子，逐帧扫描 {@link Groups#build}，
 * 按方块 id 从缓存数组里 O(1) 取产率加气。原版方块不主动产气
 * （{@link #defaultExhaustRate} 默认 0），但被爆炸波及或手动灌气时照样会积累、会泄漏。
 * 产率 / 容量优先级：
 * <ol>
 *     <li>建筑实现了 {@link ExhaustTolerant} → 用它的 {@code totalExhaust()}；</li>
 *     <li>方块是 {@link ExhaustBlock} → 用它公开的 {@code exhaustRate} / {@code totalExhaust}
 *         字段（<b>JSON / hjson 内容可直接配置</b>）；</li>
 *     <li>其它（原版方块）→ 用 {@link #defaultExhaustRate} / {@link #defaultTotalExhaust}，
 *         也可以用 {@link #setExhaustRate} / {@link #setTotalExhaust} 单独覆盖。</li>
 * </ol>
 * 攒到上限即触发泄漏（或按 {@link ExhaustExplosive} 的参数爆炸）。数值按 <b>地块坐标</b>
 * 存储，用 {@code buildingId + generation + blockId} 三重校验，建筑被拆/重建后旧数据自动失效；
 * 存档写独立 chunk，联机用批量二进制包同步。
 */
public class ExhaustStates {

    /**
     * 没有声明产率的方块（原版方块、别的模组方块）每秒自产多少废气。
     * <p>
     * <b>默认 0：只有本模组的废气方块会自己产气。</b>原版建筑不会平白无故攒废气，
     * 但一旦被爆炸波及、或被泵/逻辑手动灌气，它们照样会积累、照样会泄漏
     * （见 {@link #defaultTotalExhaust}）—— 威慑力保留，只是不再"全图通杀"。
     * <p>
     * 想让某个原版方块也自产，用 {@link #setExhaustRate} 单独开；想全图都产，把这里改成 &gt; 0。
     */
    public static float defaultExhaustRate = 0f;
    /** 没有声明的方块能容纳多少废气（原版方块被爆炸灌气时的上限）。 */
    public static float defaultTotalExhaust = 40f;

    /** 信息面板：「废气上限」。 */
    public static final Stat exhaustTotal = new Stat("exhaust-total", StatCat.general);
    /** 信息面板：「废气自产」。 */
    public static final Stat exhaustRateStat = new Stat("exhaust-rate", StatCat.function);
    /** 信息面板：「废气抽取」（泵类方块的最大抽取速率）。 */
    public static final Stat exhaustPumpRate = new Stat("exhaust-pump-rate", StatCat.function);

    private static final String chunkName = "darksteel-exhaust-states";
    private static final String packetName = "darksteel-exhaust-state";

    private static final byte formatVersion = 1;
    private static final byte opUpsert = 1;
    private static final byte opClear = 2;
    private static final byte opBatch = 3;

    /** 批量同步间隔（刻）。逐帧同步会刷爆网络，所以攒一批再发。 */
    private static final float syncInterval = 20f;

    private static final IntMap<State> states = new IntMap<>();
    /** 每格的"重建代数"，地块上的建筑一换就 +1，让旧状态自动失效。 */
    private static final IntMap<Integer> generations = new IntMap<>();
    /** 客户端比建筑先到的包，先存着，等地块建好再套用。 */
    private static final IntMap<PacketState> pendingPackets = new IntMap<>();
    /** 这一批需要下发给客户端的格子。 */
    private static final IntSet changed = new IntSet();

    /** 给非 {@link ExhaustBlock} 方块单独指定产率 / 容量（JSON 写不进去的原版方块用）。 */
    private static final ObjectMap<Block, Float> rateOverrides = new ObjectMap<>();
    private static final ObjectMap<Block, Float> capacityOverrides = new ObjectMap<>();

    /** 按方块 id 索引的产率 / 容量缓存，避免每帧做一堆 instanceof。 */
    private static float[] rateCache = new float[0];
    private static float[] capacityCache = new float[0];

    private static boolean initialized;
    private static float syncTimer;

    private ExhaustStates() {
    }

    // region 生命周期

    /** 必须在 {@code loadContent()} 里调用：注册存档 chunk、联机包、地块事件和自产废气钩子。 */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SaveVersion.addCustomChunk(chunkName, new StateChunk());

        // 纯服务器（headless）没有 netClient
        if (Vars.netClient != null) {
            Vars.netClient.addBinaryPacketHandler(packetName, ExhaustStates::handlePacket);
        }

        Events.on(EventType.ContentInitEvent.class, event -> installContentInfo());
        Events.on(EventType.WorldLoadBeginEvent.class, event -> reset());
        Events.on(EventType.ResetEvent.class, event -> reset());
        Events.on(EventType.TilePreChangeEvent.class, event -> invalidate(event.tile));
        Events.on(EventType.TileChangeEvent.class, event -> applyPending(event.tile));

        // 机制层的核心：产率大于 0 的方块逐帧自产废气（原版方块产率为 0，不受影响）
        Events.run(EventType.Trigger.update, ExhaustStates::updateAll);
    }

    /** 世界重置 / 载入新地图时清空所有数值。 */
    private static void reset() {
        states.clear();
        generations.clear();
        pendingPackets.clear();
        changed.clear();
        syncTimer = 0f;
        rebuildCaches();
    }

    // endregion
    // region 自产废气（本模组方块）

    /** 逐帧给本模组会产气的方块加废气。产率 0 的方块（原版方块）直接跳过，不做任何事。 */
    private static void updateAll() {
        if (Vars.net.client() || !Vars.state.isGame() || Vars.world.isGenerating()) {
            return;
        }
        if (rateCache.length == 0) {
            rebuildCaches();
            if (rateCache.length == 0) {
                return;
            }
        }

        for (Building building : Groups.build) {
            int id = building.block.id;
            if (id < 0 || id >= rateCache.length) {
                continue;
            }
            float rate = rateCache[id];
            if (rate <= 0f || !eligible(building)) {
                continue;
            }
            accumulate(building, rate / 60f * building.delta(), capacityCache[id]);
        }

        flushSync();
    }

    /** 加气到上限；到顶就交给反应层决定是爆炸还是泄漏。 */
    private static void accumulate(Building building, float amount, float capacity) {
        if (amount <= 0f) {
            return;
        }
        State state = getOrCreate(building, capacity);
        state.exhaust = Math.min(state.total, state.exhaust + amount);
        changed.add(state.pos);

        if (state.exhaust >= state.total) {
            if (building instanceof ExhaustExplosive) {
                ExhaustReactions.request(building);
            } else {
                ExhaustReactions.requestConversion(building);
            }
        }
    }

    private static void rebuildCaches() {
        int count = Vars.content.blocks().size;
        rateCache = new float[count];
        capacityCache = new float[count];
        for (Block block : Vars.content.blocks()) {
            if (block.id < 0 || block.id >= count) {
                continue;
            }
            rateCache[block.id] = rateOf(block);
            capacityCache[block.id] = capacityOf(block);
        }
    }

    // endregion
    // region 对外读写

    /** 该建筑当前积累的废气量。 */
    public static float exhaust(Building building) {
        State state = state(building);
        return state == null ? 0f : state.exhaust;
    }

    /** 该建筑的废气上限。 */
    public static float totalExhaust(Building building) {
        State state = state(building);
        return state == null ? capacityOf(building.block) : state.total;
    }

    /** 废气占上限的比例，给进度条用。 */
    public static float fillFraction(Building building) {
        State state = state(building);
        return state == null || state.total <= 0f ? 0f : state.exhaust / state.total;
    }

    /** 方块每秒自产多少废气。 */
    public static float rateOf(Block block) {
        if (block instanceof ExhaustBlock exhaustBlock) {
            return exhaustBlock.exhaustRate;
        }
        Float override = rateOverrides.get(block);
        return override != null ? override : defaultExhaustRate;
    }

    /** 方块能容纳多少废气。 */
    public static float capacityOf(Block block) {
        if (block instanceof ExhaustBlock exhaustBlock) {
            return exhaustBlock.totalExhaust;
        }
        Float override = capacityOverrides.get(block);
        return override != null ? override : defaultTotalExhaust;
    }

    /** 给原版方块单独指定产率。注意：信息面板的统计项只在内容初始化时装一次，不会跟着刷新。 */
    public static void setExhaustRate(Block block, float rate) {
        rateOverrides.put(block, rate);
        rebuildCaches();
    }

    /** 给原版方块单独指定容量。 */
    public static void setTotalExhaust(Block block, float total) {
        capacityOverrides.put(block, total);
        rebuildCaches();
    }

    /** 手动改写上限。 */
    public static void setCapacity(Building building, float total) {
        if (!authoritative() || !eligible(building)) {
            return;
        }
        State state = getOrCreate(building, Math.max(total, 0f));
        state.total = Math.max(total, 0f);
        state.exhaust = Math.min(state.exhaust, state.total);
        sync(state, opUpsert);
    }

    /** 注入废气。返回注入后的值。 */
    public static float addExhaust(Building building, float amount) {
        if (!authoritative() || !eligible(building) || amount <= 0f) {
            return exhaust(building);
        }
        State state = getOrCreate(building, capacityOf(building.block));
        state.exhaust = Math.min(state.total, state.exhaust + amount);
        sync(state, opUpsert);
        return state.exhaust;
    }

    /** 抽走废气。返回实际抽走的量（可能不足 amount）。 */
    public static float removeExhaust(Building building, float amount) {
        if (!authoritative() || amount <= 0f) {
            return 0f;
        }
        State state = state(building);
        if (state == null) {
            return 0f;
        }
        float removed = Math.min(state.exhaust, amount);
        if (removed <= 0f) {
            return 0f;
        }
        state.exhaust -= removed;
        sync(state, opUpsert);
        return removed;
    }

    /** 把废气从 source 搬到 destination，会被 destination 的剩余容量卡住。返回实际搬运量。 */
    public static float transferExhaust(Building source, Building destination, float amount) {
        if (!authoritative() || source == destination || amount <= 0f
            || !eligible(source) || !eligible(destination)) {
            return 0f;
        }
        State sourceState = state(source);
        if (sourceState == null || sourceState.exhaust <= 0f) {
            return 0f;
        }
        State destinationState = getOrCreate(destination, capacityOf(destination.block));

        float transferred = Math.min(amount,
            Math.min(sourceState.exhaust, destinationState.total - destinationState.exhaust));
        if (transferred <= 0f) {
            return 0f;
        }
        sourceState.exhaust -= transferred;
        destinationState.exhaust += transferred;
        sync(sourceState, opUpsert);
        sync(destinationState, opUpsert);
        return transferred;
    }

    /** 是否已经顶到上限（下一刻就会泄漏）。 */
    public static boolean atCapacity(Building building) {
        State state = state(building);
        return state != null && state.exhaust >= state.total;
    }

    // endregion
    // region 信息面板

    /** 给所有参与废气场的方块加「废气上限 / 自产速率」统计项和废气进度条（只在内容初始化时执行一次）。 */
    private static void installContentInfo() {
        rebuildCaches();
        for (Block block : Vars.content.blocks()) {
            if (eligible(block)) {
                installInfo(block);
            }
        }
    }

    /** 给单个方块装统计项 + 进度条。进度条读实时值，统计项是初始化时的快照。 */
    private static void installInfo(Block block) {
        try {
            // 会产气的方块才值得在信息面板上写产率；原版方块（产率 0）只挂进度条，
            // 用来显示"被爆炸波及后沾了多少废气"，没气时条是隐藏的。
            if (rateOf(block) > 0f) {
                block.stats.add(exhaustTotal, capacityOf(block), StatUnit.none);
                block.stats.add(exhaustRateStat, rateOf(block), StatUnit.perSecond);
            }

            block.addBar("exhaust", building -> {
                Bar bar = new Bar(
                    () -> Core.bundle.get("bar.exhaust"),
                    () -> ExhaustFx.exhaustColor,
                    () -> fillFraction(building));
                bar.visible(() -> exhaust(building) > 0f);
                return bar;
            });
        } catch (Throwable error) {
            Log.err("Failed to install exhaust info for block " + block.name, error);
        }
    }

    // endregion
    // region 内部：状态存取

    private static boolean authoritative() {
        return !Vars.net.client();
    }

    /** 方块层面能否承载废气：只认玩家可建造的方块（核心不积累，植被/残骸不算建筑）。 */
    private static boolean eligible(Block block) {
        return block.hasBuilding() && block.placeablePlayer
            && !(block instanceof CoreBlock)
            && !(block instanceof Prop);
    }

    private static boolean eligible(Building building) {
        return building != null && building.tile != null && building.tile.build == building
            && eligible(building.block);
    }

    private static int generation(int pos) {
        return generations.get(pos, 0);
    }

    private static State state(Building building) {
        if (!eligible(building)) {
            return null;
        }
        State state = states.get(building.pos());
        return state != null && state.matches(building, generation(building.pos())) ? state : null;
    }

    private static State getOrCreate(Building building, float capacity) {
        int pos = building.pos();
        int generation = generation(pos);
        State state = states.get(pos);
        if (state == null || !state.matches(building, generation)) {
            state = new State(building, generation, capacity);
            states.put(pos, state);
        }
        return state;
    }

    /** 建筑被拆 / 被替换：丢掉旧废气，代数 +1。 */
    private static void invalidate(Tile tile) {
        Building building = tile.build;
        if (building == null || building.tile != tile) {
            return;
        }
        int pos = building.pos();
        int generation = generation(pos);
        State state = states.get(pos);
        if (state != null && state.matches(building, generation)) {
            if (authoritative()) {
                sync(state, opClear);
            }
            states.remove(pos);
        }
        pendingPackets.remove(pos);
        changed.remove(pos);
        generations.put(pos, generation + 1);
    }

    // endregion
    // region 内部：联机同步

    /** 立即下发单个状态（离散事件用：搬运、清空）。 */
    private static void sync(State state, byte operation) {
        if (!Vars.net.server() || Vars.world.isGenerating()) {
            return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeByte(formatVersion);
            output.writeByte(operation);
            output.writeInt(state.pos);
            output.writeShort(state.blockId);
            output.writeFloat(state.exhaust);
            output.writeFloat(state.total);
            Call.clientBinaryPacketReliable(packetName, bytes.toByteArray());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** 把这一批变化打包成一个包下发，避免逐帧刷爆网络。 */
    private static void flushSync() {
        if (changed.isEmpty()) {
            return;
        }
        syncTimer -= Time.delta;
        if (syncTimer > 0f) {
            return;
        }
        syncTimer = syncInterval;

        if (!Vars.net.server() || Vars.world.isGenerating()) {
            changed.clear();
            return;
        }

        try {
            ByteArrayOutputStream entries = new ByteArrayOutputStream();
            DataOutputStream entryStream = new DataOutputStream(entries);
            int count = 0;
            for (IntMap.Entry<State> entry : states) {
                if (!changed.contains(entry.key)) {
                    continue;
                }
                State state = entry.value;
                // 条上根本看不出来的微量变化不值得发包
                if (state.total <= 0f || state.exhaust < state.total * 0.01f) {
                    continue;
                }
                entryStream.writeInt(state.pos);
                entryStream.writeShort(state.blockId);
                entryStream.writeFloat(state.exhaust);
                entryStream.writeFloat(state.total);
                count++;
            }
            changed.clear();

            if (count == 0) {
                return;
            }

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            output.writeByte(formatVersion);
            output.writeByte(opBatch);
            output.writeInt(count);
            output.write(entries.toByteArray());
            Call.clientBinaryPacketReliable(packetName, bytes.toByteArray());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void handlePacket(byte[] bytes) {
        if (!Vars.net.client()) {
            return;
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (input.readUnsignedByte() != formatVersion) {
                return;
            }
            byte operation = input.readByte();

            if (operation == opBatch) {
                int count = input.readInt();
                for (int i = 0; i < count; i++) {
                    int pos = input.readInt();
                    int blockId = input.readUnsignedShort();
                    float exhaust = input.readFloat();
                    float total = input.readFloat();
                    applyPacket(new PacketState(opUpsert, pos, blockId, exhaust, total));
                }
                return;
            }

            PacketState packet = new PacketState(operation, input.readInt(),
                input.readUnsignedShort(), input.readFloat(), input.readFloat());
            if (!Float.isFinite(packet.exhaust()) || !Float.isFinite(packet.total())) {
                return;
            }
            applyPacket(packet);
        } catch (IOException exception) {
            Log.err("Failed to read exhaust state packet.", exception);
        }
    }

    private static void applyPacket(PacketState packet) {
        if (packet.operation() == opClear) {
            states.remove(packet.pos());
            pendingPackets.remove(packet.pos());
            return;
        }
        if (packet.operation() != opUpsert) {
            return;
        }
        Building building = build(packet.pos());
        if (building == null || !packet.matches(building)) {
            // 建筑还没建出来，先存着，等地块事件再套用
            pendingPackets.put(packet.pos(), packet);
            return;
        }
        int generation = generation(packet.pos());
        State state = states.get(packet.pos());
        if (state == null || !state.matches(building, generation)) {
            state = new State(building, generation, packet.total());
            states.put(packet.pos(), state);
        }
        state.total = Math.max(0f, packet.total());
        state.exhaust = Math.max(0f, Math.min(packet.exhaust(), state.total));
        pendingPackets.remove(packet.pos());
    }

    private static void applyPending(Tile tile) {
        if (tile == null) {
            return;
        }
        PacketState direct = pendingPackets.get(tile.pos());
        if (direct != null) {
            applyPacket(direct);
        }
        if (tile.build != null) {
            PacketState root = pendingPackets.get(tile.build.pos());
            if (root != null) {
                applyPacket(root);
            }
        }
    }

    private static Building build(int pos) {
        Tile tile = Vars.world.tile(pos);
        return tile == null ? null : tile.build;
    }

    // endregion

    /** 单个建筑的废气状态。 */
    private static class State {
        final int pos;
        final int buildingId;
        final int generation;
        final int blockId;
        float exhaust;
        float total;

        State(Building building, int generation, float total) {
            this.pos = building.pos();
            this.buildingId = building.id();
            this.generation = generation;
            this.blockId = building.block.id;
            this.exhaust = 0f;
            this.total = total;
        }

        /** 三重校验：同一格、同一个建筑实例、同一个方块。 */
        boolean matches(Building building, int generation) {
            return building.id() == buildingId && this.generation == generation && blockId == building.block.id;
        }
    }

    /** 网络包里带的状态。 */
    private record PacketState(byte operation, int pos, int blockId, float exhaust, float total) {

        boolean matches(Building building) {
            return building.pos() == pos && building.block.id == blockId && eligible(building);
        }
    }

    /** 存档 chunk：{@code [版本][数量]{ pos, exhaust, total }...} */
    private static class StateChunk implements SaveFileReader.CustomChunk {

        @Override
        public void write(DataOutput output) throws IOException {
            Seq<State> valid = new Seq<>();
            for (IntMap.Entry<State> entry : states) {
                Building building = build(entry.key);
                if (building == null || !entry.value.matches(building, generation(entry.key))) {
                    continue;
                }
                valid.add(entry.value);
            }

            output.writeByte(formatVersion);
            output.writeInt(valid.size);
            for (State state : valid) {
                output.writeInt(state.pos);
                output.writeFloat(state.exhaust);
                output.writeFloat(state.total);
            }
        }

        @Override
        public void read(DataInput input) throws IOException {
            int version = input.readUnsignedByte();
            if (version != formatVersion) {
                throw new IOException("Unsupported exhaust state version: " + version);
            }
            int amount = input.readInt();
            if (amount < 0) {
                throw new IOException("Invalid exhaust state count: " + amount);
            }
            for (int i = 0; i < amount; i++) {
                int pos = input.readInt();
                float exhaust = input.readFloat();
                float total = input.readFloat();

                Building building = build(pos);
                if (!eligible(building) || !Float.isFinite(exhaust) || !Float.isFinite(total) || total < 0f) {
                    continue;
                }
                State state = new State(building, generation(pos), total);
                state.exhaust = Math.max(0f, Math.min(exhaust, total));
                states.put(pos, state);
            }
        }
    }
}
