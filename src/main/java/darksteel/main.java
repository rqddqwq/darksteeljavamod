package darksteel;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import darksteel.content.DPlanets;
import darksteel.content.Blocks;
//import multicrafter.multiCrafter;


public class main extends Mod {

    public static Mods.LoadedMod mod;

    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
        Blocks.load();
        DPlanets.load();
        }
    public main(){
   // new multiCrafter();
    }
  /*  @Override
    public void init() {
        new MultiCrafterMod();
    }*/
    }