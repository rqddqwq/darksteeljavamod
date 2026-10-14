package dev.jojofr.multicrafter;

import arc.util.Log;
import dev.jojofr.multicrafter.type.DrawRecipe;
import dev.jojofr.multicrafter.world.AttributeMultiCrafterBlock;
import mindustry.mod.ClassMap;

public class MultiCrafterMod {

    // ✅ 静态方法，可以在 Main 的 static 块里调用
    public static void registerTypes() {
        ClassMap.classes.put("MultiCrafter", MultiCrafterBlock.class);
        ClassMap.classes.put("AttributeMultiCrafter", AttributeMultiCrafterBlock.class);
        ClassMap.classes.put("DrawRecipe", DrawRecipe.class);

        Log.info("[MultiCrafter] 类型已注册");
    }

    public MultiCrafterMod() {
        registerTypes();
    }
}