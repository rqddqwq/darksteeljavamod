package darksteel;

import arc.util.Log;
import mindustry.mod.Mod;
import darksteel.content.Blocks;
import dev.jojofr.multicrafter.MultiCrafterMod;

public class Main extends Mod {

    // ✅ 静态块：类加载时立即注册，比内容解析早
    static {
        MultiCrafterMod.registerTypes();
    }

    @Override
    public void loadContent() {
        Blocks.load();
    }

    @Override
    public void init() {
        // 不再需要 new MultiCrafterMod()
    }
}