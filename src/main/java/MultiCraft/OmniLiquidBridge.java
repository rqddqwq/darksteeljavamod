/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import MultiCraft.OmniBridge;
import mindustry.gen.Building;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;

public class OmniLiquidBridge
extends OmniBridge {
    public OmniLiquidBridge(String name) {
        super(name);
        hasItems = false;
        hasLiquids = true;
        outputsLiquid = true;
        canOverdrive = false;
        group = BlockGroup.liquids;
        envEnabled = -1;
        buildType = () -> new OmniLiquidBridgeBuild();
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemsMoved);
    }

    public class OmniLiquidBridgeBuild
    extends OmniBridge.OmniBridgeBuild {
        public void updateTransport(Building other) {
            if (warmup >= 0.25f) {
                moved |= moveLiquid(other, liquids.current()) > 0.05f;
            }
        }

        public void doDump() {
            dumpLiquid(liquids.current(), 1.0f);
        }
    }
}

