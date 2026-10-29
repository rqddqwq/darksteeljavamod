package darksteel.multicrafter.type;

import arc.Core;
import arc.func.Cons;
import arc.math.Interp;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import darksteel.multicrafter.MultiCrafterBlock;
import darksteel.multicrafter.world.AttributeMultiCrafterBlock;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValue;
import mindustry.world.meta.StatValues;

/**
 * 定义一条被 {@link MultiCrafterBlock} 使用的配方（玄钢重工版）。
 * <p>
 * 一条配方指定输入与输出、加工时间、特效等运行时使用的属性。
 * 输入/输出支持物品、液体、电力、热量与载荷。
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public class Recipe extends UnlockableContent {
    public final IOEntry input, output;
    /** 自动选配方时的相对权重，越高越优先。 */
    public float weight = 1f;

    /** 完成一条配方所需的时间（tick）。 */
    public float craftTime = 80f;
    /** 配方完成时播放的特效。 */
    public Effect craftEffect = Fx.none;
    /** 配方加工过程中播放的特效。 */
    public Effect updateEffect = Fx.none;
    /** 每次更新播放 update 特效的概率。 */
    public float updateEffectChance = 0.04f;
    /** update 特效的扩散范围。 */
    public float updateEffectSpread = 4f;
    /** 配方预热的速度。 */
    public float warmupSpeed = 0.019f;

    /** [热量] 热量升温/降温的速度。 */
    public float warmupRate = 0.15f;
    /** [产热] 对超出需求热量部分的倍率。 */
    public float overheatScale = 1f;
    /** [产热] 通过加热能到达的最大效率。 */
    public float maxEfficiency = 4f;

    /** [属性] 该配方使用的属性，为 {@code null} 时使用方块默认值。 */
    public Attribute attribute = null;
    /** [属性] 基础效率覆盖，{@link Float#NaN} 表示使用方块默认值。 */
    public float baseEfficiency = Float.NaN;
    /** [属性] 增幅系数覆盖，{@link Float#NaN} 表示使用方块默认值。 */
    public float boostScale = Float.NaN;
    /** [属性] 最大增幅覆盖，{@link Float#NaN} 表示使用方块默认值。 */
    public float maxBoost = Float.NaN;
    /** [属性] 最小效率覆盖，{@link Float#NaN} 表示使用方块默认值。 */
    public float minEfficiency = Float.NaN;

    /**
     * 物品输出是否按数量随机抽取。
     * <p>
     * 仅在配方只有物品输出（无液体、电力、热量、载荷输出）时生效。
     */
    public boolean randomOutput = false;

    /** 该配方渲染方块时使用的 drawer。 */
    public DrawBlock drawer = new DrawDefault();


    private static final Cons<IOEntry> EMPTY_CONS = e -> {};

    /** 创建一条空配方。 */
    public Recipe(String name) { this(name, EMPTY_CONS, EMPTY_CONS, 80f); }

    /** @deprecated 请改用 {@link #Recipe(String, Cons)}。 */
    @Deprecated(since = "1.5.0") public Recipe(String name, IOEntry input) { this(name, input, new IOEntry(), 80f); }
    /** 创建一条只有输入的配方。 */
    public Recipe(String name, Cons<IOEntry> input) { this(name, input, EMPTY_CONS, 80f); }

    /** @deprecated 请改用 {@link #Recipe(String, Cons, Cons)}。 */
    @Deprecated(since = "1.5.0") public Recipe(String name, IOEntry input, IOEntry output) { this(name, input, output, 80f); }
    /** 创建一条有输入与输出的配方。 */
    public Recipe(String name, Cons<IOEntry> input, Cons<IOEntry> output) { this(name, input, output, 80f); }

    /** @deprecated 请改用 {@link #Recipe(String, Cons, Cons, float)}。 */
    @Deprecated(since = "1.5.0") public Recipe(String name, IOEntry input, IOEntry output, float craftTime) { this(name, in -> in.copy(input), out -> out.copy(output), craftTime); }
    /** 创建一条有输入、输出与加工时间的配方。 */
    public Recipe(String name, Cons<IOEntry> input, Cons<IOEntry> output, float craftTime) {
        super(name);

        this.localizedName = Core.bundle.get(getContentTypeName() + "." + this.name + ".name", this.name);
        this.description = Core.bundle.getOrNull(getContentTypeName() + "." + this.name + ".description");
        this.details = Core.bundle.getOrNull(getContentTypeName() + "." + this.name + ".details");
        this.credit = Core.bundle.getOrNull(getContentTypeName() + "." + this.name + ".credit");

        this.input = configure(input).removeDuplicate(name);
        this.output = configure(output).removeDuplicate(name);
        this.craftTime = craftTime;
    }

    private static IOEntry configure(Cons<IOEntry> configurator) {
        IOEntry entry = new IOEntry();
        if (configurator != null) configurator.get(entry);
        return entry;
    }

    @Override
    public void postInit() {
        if(databaseCategory == null || databaseCategory.isEmpty()) databaseCategory = getContentTypeName();
        if(databaseTag == null || databaseTag.isEmpty()) databaseTag = "default";

        databaseTabs.addAll(shownPlanets);
    }

    @Override
    public void loadIcon() {
        fullIcon =
            Core.atlas.find(fullOverride == null ? "" : fullOverride,
                Core.atlas.find(getContentTypeName() + "-" + name + "-full",
                    Core.atlas.find(name + "-full",
                        Core.atlas.find(name,
                            Core.atlas.find(getContentTypeName() + "-" + name,
                                Core.atlas.find(name + "1"))))));

        uiIcon = Core.atlas.find(getContentTypeName() + "-" + name + "-ui", fullIcon);
    }

    @Override
    public void setStats() {
        stats.add(Stat.output, table -> {
            // 与方块信息面板保持一致：不显示勾选框，按每次加工显示
            boolean perSecond = Core.settings.getBool("multicrafter.show-per-second");

            table.add(buildTable(null, false, perSecond)).pad(4f).grow();
            table.defaults().grow();
        });
    }

    /**
     * 构建用于显示该配方的 UI 表格。
     *
     * @param block         拥有该配方的方块，用于显示属性加成。可为 null。
     * @param showAttribute 是否显示属性加成信息
     * @param perSecond     是否按每秒显示资源数量（否则按每次加工显示）
     * @return              构建好的表格
     */
    public Table buildTable(MultiCrafterBlock block, boolean showAttribute, boolean perSecond) {
        Table table =  new Table();
        table.setBackground(Tex.whiteui);
        table.setColor(Pal.darkerGray);

        Table recipeTable = new Table();
        if (!unlockedNow()) {
            recipeTable.setColor(Pal.darkestGray);
            recipeTable.image(Icon.lock).size(100f, 50f).pad(12f).fill();

            return recipeTable;
        }

        Cell<Table> inputTable = recipeTable.add(this.input.buildTable(perSecond, craftTime)).minWidth(80f).pad(12f).fill();
        inputTable.left();

        Table time = new Table();
        Bar timeBar = new Bar(String.format("%.1f", this.craftTime / 60f) + "s",
            Pal.accent, () -> Interp.smooth.apply((Time.time % this.craftTime) / this.craftTime));
        time.add(timeBar).height(50f).width(250f);
        recipeTable.add(time).pad(12f);

        Cell<Table> outputCell = recipeTable.add(this.output.buildTable(perSecond, craftTime, this.randomOutput)).minWidth(80f).pad(12f).fill();
        outputCell.right();

        table.add(recipeTable).growX();

        if (showAttribute && attribute != null && block instanceof AttributeMultiCrafterBlock attributeBlock) {
            Table attributeTable = new Table();

            float baseEfficiency = !Float.isNaN(this.baseEfficiency) ? this.baseEfficiency : attributeBlock.baseEfficiency;
            attributeTable.add("[lightgray] " + (baseEfficiency <= 0.0001f ? Stat.tiles : Stat.affinities).localized() + ": []");

            float boostScale = !Float.isNaN(this.boostScale) ? this.boostScale : attributeBlock.boostScale;
            StatValue statValue = StatValues.blocks(attribute, block.floating, boostScale * block.size * block.size, !attributeBlock.displayEfficiency);
            statValue.display(attributeTable);

            table.row();
            table.add(attributeTable).pad(4f).growX();
        }

        return table;
    }

    public Recipe withLocalizedName(String localizedName) {
        this.localizedName = localizedName;
        return this;
    }

    public Recipe withWeight(float weight) {
        this.weight = weight;
        return this;
    }

    public Recipe withCraftTime(float craftTime) {
        this.craftTime = craftTime;
        return this;
    }

    public Recipe withCraftEffect(Effect craftEffect) {
        this.craftEffect = craftEffect;
        return this;
    }

    public Recipe withUpdateEffect(Effect updateEffect) { return withUpdateEffect(updateEffect, 0.04f, 4f); }
    public Recipe withUpdateEffect(Effect updateEffect, float chance) { return withUpdateEffect(updateEffect, chance, 4f); }
    public Recipe withUpdateEffect(Effect updateEffect, float chance, float spread) {
        this.updateEffect = updateEffect;
        this.updateEffectChance = chance;
        this.updateEffectSpread = spread;
        return this;
    }

    public Recipe withWarmupSpeed(float speed) {
        this.warmupSpeed = speed;
        return this;
    }

    public Recipe withOverheatScale(float scale) {
        this.overheatScale = scale;
        return this;
    }

    public Recipe withMaxEfficiency(float maxEfficiency) {
        this.maxEfficiency = maxEfficiency;
        return this;
    }

    public Recipe withWarmupRate(float warmupRate) {
        this.warmupRate = warmupRate;
        return this;
    }

    /** 只对 {@link AttributeMultiCrafterBlock} 有效。 */
    public Recipe withAttribute(Attribute attribute) {
        this.attribute = attribute;
        return this;
    }
    /** 只对 {@link AttributeMultiCrafterBlock} 有效。 */
    public Recipe withBaseEfficiency(float baseEfficiency) {
        this.baseEfficiency = baseEfficiency;
        return this;
    }
    /** 只对 {@link AttributeMultiCrafterBlock} 有效。 */
    public Recipe withBoostScale(float boostScale) {
        this.boostScale = boostScale;
        return this;
    }
    /** 只对 {@link AttributeMultiCrafterBlock} 有效。 */
    public Recipe withMaxBoost(float maxBoost) {
        this.maxBoost = maxBoost;
        return this;
    }
    /** 只对 {@link AttributeMultiCrafterBlock} 有效。 */
    public Recipe withMinEfficiency(float minEfficiency) {
        this.minEfficiency = minEfficiency;
        return this;
    }

    public Recipe isRandomOutput() { return isRandomOutput(true); }
    public Recipe isNotRandomOutput() { return isRandomOutput(false); }
    /**
     * 设置该配方是否随机输出物品。
     *
     * @param randomOutput 是否启用随机输出
     * @return             该配方，用于链式调用
     * @throws IllegalArgumentException 配方存在非物品输出时抛出
     */
    public Recipe isRandomOutput(boolean randomOutput) {
        if (output.hasLiquids() || output.hasPower() || output.hasHeat() || output.hasPayloads())
            throw new IllegalArgumentException("Recipe '" + this.name + "' is set to random output, but has non-item outputs. Random output only works with items.");

        this.randomOutput = randomOutput;
        return this;
    }

    public Recipe isUnlocked() { return isUnlocked(true); }
    public Recipe isLocked() { return isUnlocked(false); }
    public Recipe isUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
        return this;
    }

    public Recipe isAlwaysUnlocked() { return isAlwaysUnlocked(true); }
    public Recipe isNotAlwaysUnlocked() { return isAlwaysUnlocked(false); }
    public Recipe isAlwaysUnlocked(boolean alwaysUnlocked) {
        this.alwaysUnlocked = alwaysUnlocked;
        return this;
    }

    public Recipe withDrawer(DrawBlock drawer) {
        this.drawer = drawer;
        return this;
    }

    public boolean hasItems() { return input != null && input.hasItems() || output != null && output.hasItems(); }

    public boolean hasLiquids() { return input != null && input.hasLiquids() || output != null && output.hasLiquids(); }

    public boolean hasPower() { return input != null && input.hasPower() || output != null && output.hasPower(); }

    public boolean hasHeat() { return input != null && input.hasHeat() || output != null && output.hasHeat(); }

    public boolean hasPayloads() { return input != null && input.hasPayloads() || output != null && output.hasPayloads(); }

    @Override
    public ContentType getContentType() {
        return ContentType.typeid_UNUSED;
    }

    protected String getContentTypeName() {
        return "recipe";
    }
}
