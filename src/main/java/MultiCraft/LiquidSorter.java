/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Unit;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.Stat;

public class LiquidSorter
extends LiquidBlock {
    public boolean invert = false;
    public TextureRegion cross;

    public LiquidSorter(String name) {
        super(name);
        update = false;
        destructible = true;
        underBullets = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        configurable = true;
        unloadable = false;
        saveConfig = true;
        clearOnDoubleTap = true;
        hasItems = false;
        config(Liquid.class, (tile, liquid) -> {
            tile.sortLiquid = liquid;
        });
        configClear(tile -> {
            tile.sortLiquid = null;
        });
    }

    public void setBars() {
        super.setBars();
        removeBar("liquid");
    }

    public void load() {
        super.load();
        cross = Core.atlas.find(name + "-cross", "cross-full");
    }

    public void setStats() {
        super.setStats();
        stats.remove(Stat.liquidCapacity);
    }

    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list) {
        drawPlanConfigCenter(plan, plan.config, "center", true);
    }

    public boolean outputsItems() {
        return false;
    }

    public int minimapColor(Tile tile) {
        LiquidSorterBuild build = (LiquidSorterBuild)tile.build;
        return build == null || build.sortLiquid == null ? 0 : build.sortLiquid.color.rgba();
    }

    public TextureRegion[] icons() {
        return new TextureRegion[]{region};
    }

    private boolean shouldForward(Liquid liquid, Liquid sortLiquid) {
        if (sortLiquid == null) {
            return !invert;
        }
        return liquid == sortLiquid != invert;
    }

    public class LiquidSorterBuild
    extends LiquidBlock.LiquidBuild {
        @Nullable
        public Liquid sortLiquid;
        private int rotation;

        public LiquidSorterBuild() {
            super((LiquidBlock)LiquidSorter.this);
            rotation = 0;
        }

        public void configured(Unit player, Object value) {
            super.configured(player, value);
            if (!Vars.headless) {
                Vars.renderer.minimap.update(tile);
            }
        }

        public void draw() {
            if (sortLiquid == null) {
                Draw.rect((TextureRegion)LiquidSorter.cross, (float)x, (float)y);
            } else {
                Draw.color((Color)sortLiquid.color);
                Fill.circle((float)x, (float)y, (float)3.5f);
                Draw.color();
            }
            Draw.rect((TextureRegion)LiquidSorter.region, (float)x, (float)y);
        }

        private int[] getOutputDirs(Liquid liquid, int inputDir) {
            if (inputDir == -1) {
                return new int[0];
            }
            int outputBase = inputDir;
            if (LiquidSorter.shouldForward(liquid, sortLiquid)) {
                return new int[]{outputBase};
            }
            int left = Mathf.mod((int)(outputBase - 1), (int)4);
            int right = Mathf.mod((int)(outputBase + 1), (int)4);
            return new int[]{left, right};
        }

        public boolean acceptLiquid(Building source, Liquid liquid) {
            int[] dirs;
            if (!enabled) {
                return false;
            }
            byte dir = source.relativeTo((int)tile.x, (int)tile.y);
            if (dir == -1) {
                return false;
            }
            for (int d : dirs = getOutputDirs(liquid, dir)) {
                Building next = nearby(d);
                if (next == null || !next.acceptLiquid((Building)this, liquid)) continue;
                return true;
            }
            return false;
        }

        public void handleLiquid(Building source, Liquid liquid, float amount) {
            byte dir = source.relativeTo((int)tile.x, (int)tile.y);
            if (dir == -1) {
                return;
            }
            int[] dirs = getOutputDirs(liquid, dir);
            Seq targets = new Seq();
            for (int d : dirs) {
                Building next = nearby(d);
                if (next == null || !next.acceptLiquid((Building)this, liquid)) continue;
                targets.add((Object)next);
            }
            if (targets.isEmpty()) {
                return;
            }
            if (targets.size == 1) {
                ((Building)targets.first()).handleLiquid((Building)this, liquid, amount);
            } else {
                int idx = rotation & 1;
                Building to = (Building)targets.get(idx);
                to.handleLiquid((Building)this, liquid, amount);
                rotation ^= 1;
            }
        }

        public Building getLiquidDestination(Building source, Liquid liquid) {
            return this;
        }

        public void updateTile() {
        }

        public void buildConfiguration(Table table) {
            ItemSelection.buildTable((Block)LiquidSorter.this, (Table)table, (Seq)Vars.content.liquids(), () -> sortLiquid, arg_0 -> ((LiquidSorterBuild)this).configure(arg_0), (int)LiquidSorter.selectionRows, (int)LiquidSorter.selectionColumns);
        }

        public Liquid config() {
            return sortLiquid;
        }

        public void write(Writes write) {
            super.write(write);
            write.s(sortLiquid == null ? -1 : (int)sortLiquid.id);
            write.i(rotation);
        }

        public void read(Reads read, byte revision) {
            super.read(read, revision);
            sortLiquid = Vars.content.liquid((int)read.s());
            rotation = read.i();
        }
    }
}

