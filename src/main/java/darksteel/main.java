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


public class main extends Mod {

    public static Mods.LoadedMod mod;

    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
        Blocks.load();
        DPlanets.load();
        }
    public main(){
    new MultiCrafterMod();
    }
  /*  @Override
    public void init() {
        new MultiCrafterMod();
    }*/
    }