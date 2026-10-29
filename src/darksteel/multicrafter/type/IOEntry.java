package darksteel.multicrafter.type;

import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import darksteel.multicrafter.meta.SimpleStatValues;
import mindustry.type.*;
import mindustry.world.blocks.payloads.Payload;

/**
 * 表示一条 {@link Recipe} 的输入或输出（玄钢重工版）。
 * <p>
 * 支持物品、液体、电力、热量与载荷。
 */
@SuppressWarnings("DeprecatedIsStillUsed")
public class IOEntry {
    /**
     * 消耗/产出的物品。没有物品时可为 {@code null}。
     * <p>
     * 建议通过 {@link #getItems()} 与 {@link #withItems(Object...)} 读写，不建议直接访问。
     */
    public @Nullable ItemStack[] items;
    /**
     * 消耗/产出的液体（每 tick）。没有液体时可为 {@code null}。
     * <p>
     * 建议通过 {@link #getLiquids()} 与 {@link #withLiquids(Object...)} 读写，不建议直接访问。
     */
    public @Nullable LiquidStack[] liquids;
    /** 消耗/产出的电力（每 tick）。 */
    public float power = 0;
    /** 消耗/产出的热量。 */
    public float heat = 0;
    /**
     * 消耗/产出的载荷。没有载荷时可为 {@code null}。
     * <p>
     * 建议通过 {@link #getPayloads()} 与 {@link #withPayloads(Object...)} 读写，不建议直接访问。
     */
    public @Nullable Seq<PayloadStack> payloads;

    /** 创建一个空的 {@link IOEntry}。 */
    public IOEntry() {}
    /** 用给定物品创建一个 {@link IOEntry}。 */
    public IOEntry(ItemStack[] items) { this.items = items; }
    /** 用给定液体创建一个 {@link IOEntry}。 */
    public IOEntry(LiquidStack[] liquids) { this.liquids = liquids; }
    /** 用给定物品与液体创建一个 {@link IOEntry}。 */
    public IOEntry(ItemStack[] items, LiquidStack[] liquids) { this.items = items; this.liquids = liquids; }

    /**
     * 浅拷贝 {@code other}。
     * @param other 要拷贝的 {@link IOEntry}。
     */
    public void copy(IOEntry other) {
        this.items = other.items;
        this.liquids = other.liquids;
        this.power = other.power;
        this.heat = other.heat;
        this.payloads = other.payloads;
    }

    /**
     * 直接设置 {@link #items}。
     *
     * @deprecated  请改用 {@link #withItems(Object...)}。
     * @param items 要设置的 {@link ItemStack} 数组。
     * @return      该条目，用于链式调用。
     */
    @Deprecated(since = "1.5.0")
    public IOEntry withItems(ItemStack... items) {
        this.items = items;
        return this;
    }
    /**
     * 用 {@code Item, 数量} 交替的参数设置 {@link #items}。
     *
     * @param items 交替的 {@code Item, amount} 参数，传给 {@link ItemStack#with(Object...)}。
     * @return      该条目，用于链式调用。
     */
    public IOEntry withItems(Object... items) {
        this.items = ItemStack.with(items);
        return this;
    }

    /**
     * 直接设置 {@link #liquids}。
     *
     * @deprecated    请改用 {@link #withLiquids(Object...)}。
     * @param liquids 要设置的 {@link LiquidStack} 数组。
     * @return        该条目，用于链式调用。
     */
    @Deprecated(since = "1.5.0")
    public IOEntry withLiquids(LiquidStack... liquids) {
        this.liquids = liquids;
        return this;
    }
    /**
     * 用 {@code Liquid, 数量} 交替的参数设置 {@link #liquids}。
     *
     * @param liquids 交替的 {@code Liquid, amount} 参数，传给 {@link LiquidStack#with(Object...)}。
     * @return        该条目，用于链式调用。
     */
    public IOEntry withLiquids(Object... liquids) {
        this.liquids = LiquidStack.with(liquids);
        return this;
    }

    /**
     * 直接设置 {@link #power}。
     *
     * @param power 每 tick 的电力，正数为消耗/产出。
     * @return      该条目，用于链式调用。
     */
    public IOEntry withPower(float power) {
        this.power = power;
        return this;
    }

    /**
     * 直接设置 {@link #heat}。
     *
     * @param heat 热量数值。
     * @return     该条目，用于链式调用。
     */
    public IOEntry withHeat(float heat) {
        this.heat = heat;
        return this;
    }

    /**
     * 直接设置 {@link #payloads}。
     *
     * @deprecated     请改用 {@link #withPayloads(Object...)}。
     * @param payloads 要设置的 {@link PayloadStack} 数组。
     * @return         该条目，用于链式调用。
     */
    @Deprecated(since = "1.5.0")
    public IOEntry withPayloads(PayloadStack... payloads) {
        this.payloads = new Seq<>(payloads);
        return this;
    }
    /**
     * 用 {@code 载荷, 数量} 交替的参数设置 {@link #payloads}。
     *
     * @param payloads 交替的 {@code Payload, amount} 参数，传给 {@link PayloadStack#with(Object...)}。
     * @return         该条目，用于链式调用。
     */
    public IOEntry withPayloads(Object... payloads) {
        PayloadStack[] stacks = PayloadStack.with(payloads);
        this.payloads = new Seq<>(stacks);
        return this;
    }

    public boolean isEmpty() { return (items == null || items.length == 0) && (liquids == null || liquids.length == 0) && power <= 0 && heat <= 0 && (payloads == null || payloads.size == 0); }

    public boolean hasItems() { return items != null && items.length > 0; }
    public boolean acceptItem(Item item) {
        if (items == null) return false;

        for (ItemStack stack : items) if (item == stack.item) return true;
        return false;
    }

    public boolean hasLiquids() { return liquids != null && liquids.length > 0; }
    public boolean acceptLiquid(Liquid liquid) {
        if (liquids == null) return false;

        for (LiquidStack stack : liquids) if (liquid == stack.liquid) return true;
        return false;
    }

    public boolean hasPower() { return power > 0; }
    public boolean hasHeat() { return heat > 0; }

    public boolean hasPayloads() { return payloads != null && payloads.size > 0; }
    public boolean hasPayloadsUnit() { return hasPayloads() && payloads.contains(stack -> stack.item instanceof UnitType); }
    public boolean acceptPayload(Payload payload) {
        if (payloads == null) return false;

        for (PayloadStack stack : payloads) if (payload.content() == stack.item) return true;
        return false;
    }

    /**
     * 返回该条目对指定载荷的需求数量。
     *
     * @param payload 要检查的载荷。
     * @return        该载荷的需求数量；若条目中没有该载荷则返回 {@code 0}。
     */
    public int getPayloadRequirements(Payload payload) {
        if (payloads == null) return 0;

        for (PayloadStack stack : payloads) if (payload.content() == stack.item) return stack.amount;
        return 0;
    }

    /**
     * 返回该条目中的物品。
     *
     * @return 物品数组；没有则为空数组。
     */
    public ItemStack[] getItems() { return items == null ? ItemStack.empty : items; }
    /**
     * 返回该条目中的液体。
     *
     * @return 液体数组；没有则为空数组。
     */
    public LiquidStack[] getLiquids() { return liquids == null ? LiquidStack.empty : liquids; }
    public float getPower() { return power; }
    public float getHeat() { return heat; }
    /**
     * 返回该条目中的载荷。
     *
     * @return 载荷序列；没有则为空序列。
     */
    public Seq<PayloadStack> getPayloads() { return payloads == null ? new Seq<>(0) : payloads; }


    /**
     * 以网格形式构建显示该条目内容的表格，每行最多 5 个。
     *
     * @param perSecond 是否按每秒显示（否则按每次加工显示）。
     * @param craftTime 配方加工时间（tick）。
     * @return          构建好的表格。
     */
    public Table buildTable(boolean perSecond, float craftTime) { return buildTable(true, perSecond, craftTime, false); }
    /**
     * 以网格形式构建显示该条目内容的表格，每行最多 5 个。
     * <p>
     * 若 {@code random} 为 true，则只显示物品，且数量显示为占总数的百分比。
     *
     * @param perSecond 是否按每秒显示（否则按每次加工显示）。
     * @param craftTime 配方加工时间（tick）。
     * @param random    是否按百分比显示物品（随机输出模式）。
     * @return          构建好的表格。
     */
    public Table buildTable(boolean perSecond, float craftTime, boolean random) { return buildTable(true, perSecond, craftTime, random); }
    /**
     * 以网格形式构建显示该条目内容的表格，每行最多 5 个。
     *
     * @param tooltip   是否为物品、液体、载荷显示悬浮提示。
     * @param perSecond 是否按每秒显示（否则按每次加工显示）。
     * @param craftTime 配方加工时间（tick）。
     * @return          构建好的表格。
     */
    public Table buildTable(boolean tooltip, boolean perSecond, float craftTime) { return buildTable(tooltip, perSecond, craftTime, false); }
    /**
     * 以网格形式构建显示该条目内容的表格，每行最多 5 个。
     * <p>
     * 若 {@code random} 为 true，则只显示物品，且数量显示为占总数的百分比。
     *
     * @param tooltip   是否为物品、液体、载荷显示悬浮提示。
     * @param perSecond 是否按每秒显示（否则按每次加工显示）。
     * @param craftTime 配方加工时间（tick）。
     * @param random    是否按百分比显示物品（随机输出模式）。
     * @return          构建好的表格。
     */
    public Table buildTable(boolean tooltip, boolean perSecond, float craftTime, boolean random) {
        Table table = new Table();
        if (random && !hasItems()) return table;
        Table materialTable = new Table();

        SimpleStatValues.count = 0;
        SimpleStatValues.perSecond = perSecond;
        SimpleStatValues.craftTime = craftTime;

        if (random) {
            int sum = 0;
            for (ItemStack stack : items) sum += stack.amount;

            SimpleStatValues.itemsPercent(false, tooltip, sum, items).display(materialTable);
            table.add(materialTable);
            return table;
        }

        if (hasItems()) SimpleStatValues.items(false, tooltip, items).display(materialTable);
        if (hasLiquids()) SimpleStatValues.liquids(false, tooltip, liquids).display(materialTable);
        if (hasPayloads()) SimpleStatValues.payloads(false, tooltip, payloads).display(materialTable);

        Table smallIndictor = new Table();
        if (hasPower()) SimpleStatValues.power(power).display(smallIndictor);
        if (hasHeat()) SimpleStatValues.heat(heat).display(smallIndictor);

        table.add(materialTable);
        table.row();
        table.add(smallIndictor);

        return table;
    }

    /**
     * 移除该条目中的重复资源。
     * <p>
     * 发现重复时保留第一个，并输出一条警告日志。
     *
     * @param name 配方名，用于日志。
     * @return     该条目，用于链式调用。
     */
    public IOEntry removeDuplicate(String name) {
        if (hasItems()) items = removeDuplicates(items).toArray(ItemStack.class);
        if (hasLiquids()) liquids = removeDuplicates(liquids).toArray(LiquidStack.class);
        if (hasPayloads()) payloads = removeDuplicates(payloads.toArray(PayloadStack.class));

        return this;
    }

    private <T> Seq<T> removeDuplicates(T[] array) {
        Seq<T> unique = new Seq<>(array.length);

        for (T element : array) {
            if (unique.contains(other -> other == element)) {
                Log.warn("Duplicate element '@' found in IOEntry, ignoring.", element.toString());
                continue;
            }
            unique.add(element);
        }

        return unique;
    }
}
