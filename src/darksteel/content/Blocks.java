package darksteel.content;

import mindustry.*;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import darksteel.multicrafter.MultiCrafterBlock;
import darksteel.multicrafter.type.Recipe;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.with;

public class Blocks {
    public static CoreBlock core;
    /** 多配方加工厂示例（darksteel.multicrafter）。 */
    public static MultiCrafterBlock multiCrafter;

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

        // 多配方示例：同一个方块里放两条配方，游戏内点击方块即可切换。
        // 配方名(item) 里的名字只是内部标识，localizedName 才是显示名。
        multiCrafter = new MultiCrafterBlock("multi-crafter"){{
            requirements(Category.production, BuildVisibility.shown, with(Items.copper, 60, Items.lead, 40));
            alwaysUnlocked = true;

            size = 2;
            health = 320;
            itemCapacity = 40;
            liquidCapacity = 40f;

            addRecipe(
                new Recipe("multi-smelt",
                    in -> in.withItems(Items.coal, 1, Items.copper, 1),
                    out -> out.withItems(Items.graphite, 1), 90f)
                    .withLocalizedName("铜冶炼")
                    .withCraftEffect(Fx.smeltsmoke)
                    .withUpdateEffect(Fx.smeltsmoke, 0.05f)
                    .isAlwaysUnlocked(),

                new Recipe("multi-alloy",
                    in -> in.withItems(Items.copper, 2, Items.lead, 2),
                    out -> out.withItems(Items.silicon, 1), 120f)
                    .withLocalizedName("合金提纯")
                    .withCraftEffect(Fx.smeltsmoke)
                    .withUpdateEffect(Fx.smeltsmoke, 0.05f)
                    .isAlwaysUnlocked()
            );
        }};
    }

}
