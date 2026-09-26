package darksteel;

import mindustry.mod.Mod;
import mindustry.Vars;
import mindustry.mod.Mods;
import darksteel.content.DPlanets;
import darksteel.content.Blocks;
import dev.jojofr.multicrafter.MultiCrafterMod;

public class main extends Mod {
    public static Mods.LoadedMod mod;

    @Override
    public void loadContent() {
        mod = Vars.mods.getMod(this.getClass());
    // load core blocks before planets so defaultCore references e
    Blocks.load();
    DPlanets.load();
    }
    @Override
    public void init(){
    new MultiCrafterMod();
    }
}
