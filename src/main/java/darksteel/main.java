package darksteel;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import darksteel.content.DPlanets;
import darksteel.content.Blocks;
import dev.jojofr.multicrafter.MultiCrafterMod;
import darksteel.storyengine.StorySystem;
import darksteel.storyengine.StoryData;
import darksteel.storyengine.StoryDialog;

public class main extends Mod {

    public static Mods.LoadedMod mod;

    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
        Blocks.load();
        DPlanets.load();

        // ✅ 加载剧情数据
        StorySystem.load();
    }

    @Override
    public void init() {
        new MultiCrafterMod();
        // ❌ 删掉 new StoryEngineMod(); —— 没用
    }

    // ✅ 关键：在主类里注册 story 命令
    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("story", "测试剧情引擎", args -> {
            StorySystem.load();
            StoryData data = StorySystem.stories.get("test_dialog");
            if (data != null) {
                new StoryDialog(data, data.start).open();
            } else {
                Log.err("找不到 test_dialog 剧情！");
            }
        });
    }
}