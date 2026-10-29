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
import darksteel.content.DPlanets;
import darksteel.content.Blocks;
import dev.jojofr.multicrafter.MultiCrafterMod;

public class main extends Mod {

    public static Mods.LoadedMod mod;
    
    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
        Blocks.load();
        DPlanets.load();
    }

    @Override
    public void init() {
        new MultiCrafterMod();
    }

    public static void mount(String nodeName, String planetName, UnlockableContent content){
        if(nodeName == null || planetName == null || content == null) return;
        Planet planet = (Planet)Vars.content.byName(planetName);
        MappableContent nodeContent = Vars.content.byName(nodeName);
        if(planet == null) return;
        Cons<ContentInitEvent> listener = new Cons<>(){
            @Override 
            public void get(ContentInitEvent event){
                TechNode[] nodes = {null};
                planet.techTree.each(n  -> {
                    if(n.content == (UnlockableContent)nodeContent) nodes[0] = n;
                });
                new TechNode(nodes[0], content, ItemStack.empty);
                Events.remove(ContentInitEvent.class, this);
            }
        };
        Events.on(ContentInitEvent.class, listener);
    }
}