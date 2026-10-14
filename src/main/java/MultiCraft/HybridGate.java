/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.meta.BlockGroup;

public class HybridGate
extends OverflowGate {
    public TextureRegion bottomRegion;
    public float liquidPadding = 0.0f;

    public HybridGate(String name) {
        super(name);
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 10.0f;
        noUpdateDisabled = true;
        canOverdrive = false;
        floating = true;
        update = true;
        group = BlockGroup.transportation;
        buildType = () -> new HybridGateBuild();
    }

    public void load() {
        super.load();
        bottomRegion = Core.atlas.find(name + "-bottom");
    }

    public class HybridGateBuild
    extends OverflowGate.OverflowGateBuild {
        public HybridGateBuild() {
            super((OverflowGate)HybridGate.this);
        }

        public void updateTile() {
            dumpLiquid(liquids.current());
        }

        public void draw() {
            Draw.rect((TextureRegion)HybridGate.bottomRegion, (float)x, (float)y);
            if (liquids.currentAmount() > 0.001f) {
                LiquidBlock.drawTiledFrames((int)HybridGate.size, (float)x, (float)y, (float)HybridGate.liquidPadding, (Liquid)liquids.current(), (float)(liquids.currentAmount() / HybridGate.liquidCapacity));
            }
            Draw.rect((TextureRegion)HybridGate.region, (float)x, (float)y);
        }

        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquids.current() == liquid || liquids.currentAmount() < 0.2f;
        }
    }
}

