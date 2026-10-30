package darksteel;

import arc.util.CommandHandler;
import arc.util.Log;
import arc.Events;
import arc.func.Cons;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.game.EventType.ContentInitEvent;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.content.TechTree.TechNode;
import mindustry.type.ItemStack;
import mindustry.type.Planet;
import mindustry.ctype.*;
import darksteel.exhaust.Exhaust;
import darksteel.exhaust.ExhaustReactions;
import darksteel.exhaust.ExhaustStates;
import darksteel.content.DarksteelBlocks;
import darksteel.multicrafter.MultiCrafter;

public class main extends Mod {

    public static Mods.LoadedMod mod;
    
    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
        // 多配方体系（darksteel 自己的实现，取代 dev.jojofr.multicrafter）
        MultiCrafter.register();

        // 废气机制：先装数据层/反应层（注册存档 chunk、联机包、地块事件），再建内容
        Exhaust.register();   // 把废气方块类暴露给 json/hjson：type: ExhaustBlock / ExhaustPump ...
        ExhaustStates.init();
        ExhaustReactions.init();
        DarksteelBlocks.load();
    }
}
