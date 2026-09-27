package darksteel.content;

import arc.Events;
import arc.util.Log;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import darksteel.DialogueSystem;

public class dia {

    @Override
    public void loadContent() {
        // 加载对话数据
        DialogueSystem.load();
        Log.info("[DarkSteel] 对话数据加载完成");
    }

    @Override
    public void init() {

        Events.on(EventType.ClientLoadEvent.class, e -> {
            
            Timer.schedule(() -> {
                Log.info("[DarkSteel] 尝试触发对话");

                if (Vars.player != null) {
                    DialogueSystem.start("start", Vars.player);
                } else {
                    Log.warn("[DarkSteel] player 为 null，无法触发对话");
                }
            }, 0.5f);
        });
    }
}