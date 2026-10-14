/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import mindustry.gen.Building;
import mindustry.type.Item;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.meta.BlockStatus;

public class IncineratingSorter
extends Sorter {
    public float powerUse = 1.0f;

    public IncineratingSorter(String name) {
        this(name, false);
    }

    public IncineratingSorter(String name, boolean invert) {
        super(name);
        invert = invert;
        hasPower = true;
        consumesPower = true;
        consumePower(powerUse);
        buildType = () -> new IncineratingSorterBuild();
    }

    public class IncineratingSorterBuild
    extends Sorter.SorterBuild {
        public IncineratingSorterBuild() {
            super((Sorter)IncineratingSorter.this);
        }

        private boolean shouldIncinerate(Item item) {
            if (!enabled || power.status <= 0.0f) {
                return false;
            }
            if (sortItem == null) {
                return IncineratingSorter.invert;
            }
            return IncineratingSorter.invert == (item != sortItem);
        }

        public BlockStatus status() {
            if (!enabled) {
                return BlockStatus.logicDisable;
            }
            if (power.status <= 0.0f) {
                return BlockStatus.noInput;
            }
            return BlockStatus.active;
        }

        public boolean acceptItem(Building source, Item item) {
            if (shouldIncinerate(item)) {
                return source.relativeTo((int)tile.x, (int)tile.y) != -1;
            }
            return super.acceptItem(source, item);
        }

        public void handleItem(Building source, Item item) {
            if (shouldIncinerate(item)) {
                return;
            }
            super.handleItem(source, item);
        }

        public Building getTileTarget(Item item, Building source, boolean flip) {
            if (power.status > 0.0f && !shouldIncinerate(item)) {
                byte dir = source.relativeTo((int)tile.x, (int)tile.y);
                if (dir == -1) {
                    return null;
                }
                return nearby(dir);
            }
            return super.getTileTarget(item, source, flip);
        }

        public void draw() {
            if (sortItem == null) {
                Draw.rect((TextureRegion)IncineratingSorter.cross, (float)x, (float)y);
            } else {
                Draw.color((Color)sortItem.color);
                Fill.circle((float)x, (float)y, (float)3.0f);
                Draw.color();
            }
            super.draw();
        }
    }
}

