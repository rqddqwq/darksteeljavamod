package darksteel.storyengine;

import arc.util.CommandHandler;
import mindustry.mod.Mod;

public class StoryEngineMod extends Mod {

    @Override
    public void init() {
        // 在这里初始化，但不注册指令
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        // 注册一个叫 "story" 的客户端指令
        handler.<mindustry.gen.Player>register("story", "测试剧情引擎", args -> {
            // 游戏内输入 /story 时执行
            StorySystem.load(); // 确保数据加载
            StoryData data = StorySystem.stories.get("test_dialog");
            
            if (data != null) {
                // 用之前教你的 .open() 方法（绕过 AIDE 的报错）
                 new StoryDialog(data, data.start).open();
            } else {
                arc.util.Log.err("找不到 test_dialog 剧情！");
            }
        });
    }

    @Override
    public void loadContent() {
        StorySystem.load(); // 游戏启动时加载 JSON
    }
}
