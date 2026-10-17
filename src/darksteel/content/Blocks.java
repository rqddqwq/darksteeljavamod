package darksteel.content;

import darksteel.world.SatelliteStation;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import static mindustry.type.ItemStack.with;

public class Blocks {
    public static CoreBlock core;
    public static SatelliteStation satelliteStation;

    public static void load() {
      core = new CoreBlock("core"){{
            requirements(Category.effect, BuildVisibility.coreZoneOnly, with(Items.copper, 1000, Items.lead, 800));
            alwaysUnlocked = true;
            isFirstTier = true;
            unitType = UnitTypes.alpha;
            health = 1100;
            itemCapacity = 4000;
            size = 3;
            buildCostMultiplier = 2f;

            unitCapModifier = 8;
        }};

      satelliteStation = new SatelliteStation("satellite-station"){{
            requirements(Category.effect, BuildVisibility.shown, with(Items.copper, 100, Items.lead, 80, Items.silicon, 50));
            health = 500;
            size = 2;
            update = true;
            configurable = true;
            solid = true;
      }};
    }

}
