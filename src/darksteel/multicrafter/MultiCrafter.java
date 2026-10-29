package darksteel.multicrafter;

import arc.util.Log;
import darksteel.multicrafter.type.DrawRecipe;
import darksteel.multicrafter.world.AttributeMultiCrafterBlock;
import mindustry.mod.ClassMap;

/**
 * 玄钢重工 · 多配方体系入口。
 * <p>
 * 该类只负责把多配方相关的类登记进 {@link ClassMap}，使得 .json / .hjson 内容文件里
 * 可以写 {@code type: MultiCrafter}、{@code type: AttributeMultiCrafter} 或
 * {@code type: DrawRecipe}。
 * <p>
 * 纯 Java 定义的方块不需要登记也能直接 new，登记只是为了让 JSON 内容也能用。
 * 注意：登记必须在内容(json/hjson)解析之前完成，因此要在 {@code loadContent()} 里调用，
 * 不能放到 {@code init()} 里。
 */
public final class MultiCrafter {

    private MultiCrafter(){}

    /** 登记多配方方块类，供 ClassMap 使用。应在 loadContent() 中、加载方块之前调用。 */
    public static void register(){
        ClassMap.classes.put("MultiCrafter", MultiCrafterBlock.class);
        ClassMap.classes.put("AttributeMultiCrafter", AttributeMultiCrafterBlock.class);
        ClassMap.classes.put("DrawRecipe", DrawRecipe.class);

        Log.info("[darksteel] 多配方体系已加载 (darksteel.multicrafter)");
    }
}
