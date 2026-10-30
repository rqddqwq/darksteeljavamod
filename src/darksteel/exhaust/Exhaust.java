package darksteel.exhaust;

import arc.util.Log;
import darksteel.world.blocks.exhaust.ExhaustBlock;
import darksteel.world.blocks.exhaust.ExhaustBomb;
import darksteel.world.blocks.exhaust.ExhaustConduit;
import darksteel.world.blocks.exhaust.ExhaustExtractor;
import darksteel.world.blocks.exhaust.ExhaustPump;
import mindustry.mod.ClassMap;

/**
 * 玄钢重工 · 废气机制入口。
 * <p>
 * 只负责把废气相关的方块类登记进 {@link ClassMap}，让 <b>.json / .hjson 内容文件</b>
 * 可以直接写这些 {@code type}，并用字段配置废气行为：
 * <pre>{@code
 * type: ExhaustPump
 * name: my-vent
 * exhaustRate: 0f      // 每秒自产废气量
 * totalExhaust: 120f   // 废气总容量，攒满即泄漏/爆炸
 * pumpRate: 12f        // 泵专属：每秒最大抽取量
 * }</pre>
 * 纯 Java 定义的方块不登记也能直接 new；登记是 JSON 内容的 {@code type:} 能解析的前提。
 * 必须在内容解析开始前调用，因此在 {@code loadContent()} 里、建方块之前。
 */
public final class Exhaust {

    private Exhaust() {}

    /** 登记废气方块类，供 {@link ClassMap} 解析 JSON 的 {@code type} 字段。 */
    public static void register() {
        ClassMap.classes.put("ExhaustBlock", ExhaustBlock.class);
        ClassMap.classes.put("ExhaustExtractor", ExhaustExtractor.class);
        ClassMap.classes.put("ExhaustConduit", ExhaustConduit.class);
        ClassMap.classes.put("ExhaustBomb", ExhaustBomb.class);
        ClassMap.classes.put("ExhaustPump", ExhaustPump.class);

    }
}
