/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import MultiCraft.OmniBridge;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;

public class HybridOmniBridge
extends OmniBridge {
    public boolean canOmni = false;

    public HybridOmniBridge(String name) {
        super(name);
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 10.0f;
        group = BlockGroup.transportation;
        buildType = () -> new HybridOmniBridgeBuild();
    }

    public boolean linkValid(Tile tile, Tile other, boolean checkDouble) {
        if (!super.linkValid(tile, other, checkDouble)) {
            return false;
        }
        Block block = tile.block();
        if (block instanceof HybridOmniBridge) {
            HybridOmniBridge a = (HybridOmniBridge)block;
            block = other.block();
            if (block instanceof HybridOmniBridge) {
                HybridOmniBridge b = (HybridOmniBridge)block;
                return a.canOmni == b.canOmni;
            }
        }
        return true;
    }

    @Override
    public boolean positionsValid(int x1, int y1, int x2, int y2) {
        if (canOmni) {
            return Mathf.dst((float)x1, (float)y1, (float)x2, (float)y2) <= (float)range;
        }
        if (x1 == x2) {
            return Math.abs(y1 - y2) <= range;
        }
        if (y1 == y2) {
            return Math.abs(x1 - x2) <= range;
        }
        return false;
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        if (canOmni) {
            Placement.calculateNodes(points, (Block)this, (int)rotation, (point, other) -> Mathf.dst((float)point.x, (float)point.y, (float)other.x, (float)other.y) <= (float)range);
        } else {
            Placement.calculateNodes(points, (Block)this, (int)rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        if (canOmni) {
            super.drawPlace(x, y, rotation, valid);
        } else {
            Tile link = findLink(x, y);
            for (int i = 0; i < 4; ++i) {
                Drawf.dashLine((Color)Pal.placing, (float)((float)(x * 8) + (float)Geometry.d4[i].x * 6.0f), (float)((float)(y * 8) + (float)Geometry.d4[i].y * 6.0f), (float)(x * 8 + Geometry.d4[i].x * range * 8), (float)(y * 8 + Geometry.d4[i].y * range * 8));
            }
            Draw.reset();
            Draw.color((Color)Pal.placing);
            Lines.stroke((float)1.0f);
            if (link != null && Math.abs(link.x - x) + Math.abs(link.y - y) > 1) {
                byte rot = link.absoluteRelativeTo(x, y);
                float w = link.x == x ? 8 : Math.abs(link.x - x) * 8 - 8;
                float h = link.y == y ? 8 : Math.abs(link.y - y) * 8 - 8;
                Lines.rect((float)((float)(x + link.x) / 2.0f * 8.0f - w / 2.0f), (float)((float)(y + link.y) / 2.0f * 8.0f - h / 2.0f), (float)w, (float)h);
                Draw.rect((String)"bridge-arrow", (float)(link.x * 8 + Geometry.d4((int)rot).x * 8), (float)(link.y * 8 + Geometry.d4((int)rot).y * 8), (float)(rot * 90));
            }
            Draw.reset();
        }
    }

    @Override
    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list) {
        if (canOmni) {
            super.drawPlanConfigTop(plan, list);
        } else {
            Object object = plan.config;
            if (object instanceof Point2) {
                Point2 p = (Point2)object;
                if (Math.abs(p.x) <= range && Math.abs(p.y) <= range && (p.x == 0 || p.y == 0)) {
                    currentFindX = plan.x + p.x;
                    currentFindY = plan.y + p.y;
                    currentPlan = plan;
                    BuildPlan otherReq = HybridOmniBridge.findPlan(list, (int)currentFindX, (int)currentFindY, (Boolf)planFinder);
                    if (otherReq != null) {
                        drawBridge(plan, otherReq.drawx(), otherReq.drawy(), 0.0f);
                    }
                }
            }
        }
    }

    public class HybridOmniBridgeBuild
    extends OmniBridge.OmniBridgeBuild {
        public HybridOmniBridgeBuild() {
            super(HybridOmniBridge.this);
        }

        public void updateTransport(Building other) {
            super.updateTransport(other);
            if (warmup >= 0.25f) {
                moved |= moveLiquid(other, liquids.current()) > 0.05f;
            }
        }

        public void doDump() {
            super.doDump();
            dumpLiquid(liquids.current(), 1.0f);
        }

        @Override
        public void drawConfigure() {
            if (HybridOmniBridge.canOmni) {
                super.drawConfigure();
            } else {
                Drawf.select((float)x, (float)y, (float)((float)(block.size * 8) / 2.0f + 2.0f), (Color)Pal.accent);
                for (int i = 1; i <= HybridOmniBridge.range; ++i) {
                    for (int j = 0; j < 4; ++j) {
                        Tile other = tile.nearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                        if (!HybridOmniBridge.linkValid(tile, other)) continue;
                        boolean linked = other.pos() == link;
                        Drawf.select((float)other.drawx(), (float)other.drawy(), (float)((float)(other.block().size * 8) / 2.0f + 2.0f + (linked ? 0.0f : Mathf.absin((float)Time.time, (float)4.0f, (float)1.0f))), (Color)(linked ? Pal.place : Pal.breakInvalid));
                    }
                }
            }
        }

        @Override
        public void playerPlaced(Object config) {
            if (config != null) {
                if (config instanceof Point2 p) {
                    configure(p);
                } else if (config instanceof Integer i) {
                    configure(i);
                }
            }
            if (link == -1) {
                Tile nearest = null;
                float nearestDist = Float.MAX_VALUE;
                for (int dx = -HybridOmniBridge.range; dx <= HybridOmniBridge.range; ++dx) {
                    for (int dy = -HybridOmniBridge.range; dy <= HybridOmniBridge.range; ++dy) {
                        float dst;
                        Building building;
                        Tile other;
                        if (dx == 0 && dy == 0 || !HybridOmniBridge.canOmni && dx != 0 && dy != 0 || (other = Vars.world.tile(tile.x + dx, tile.y + dy)) == null || other.build == null || !((building = other.build) instanceof OmniBridge.OmniBridgeBuild)) continue;
                        OmniBridge.OmniBridgeBuild ob = (OmniBridge.OmniBridgeBuild)building;
                        if (!HybridOmniBridge.linkValid(tile, other) || ob.link != -1 || !((dst = Mathf.dst((float)dx, (float)dy)) < nearestDist)) continue;
                        nearestDist = dst;
                        nearest = other;
                    }
                }
                if (nearest != null) {
                    nearest.build.configure((Object)tile.pos());
                }
            }
            HybridOmniBridge.lastBuild = this;
        }

        @Override
        public void onProximityAdded() {
            Building building;
            Tile existing;
            if (link != -1 && (existing = Vars.world.tile(link)) != null && (building = existing.build) instanceof OmniBridge.OmniBridgeBuild) {
                int pos;
                OmniBridge.OmniBridgeBuild other = (OmniBridge.OmniBridgeBuild)building;
                if (existing.build.team == team && HybridOmniBridge.linkValid(tile, existing) && !other.incoming.contains(pos = tile.pos())) {
                    other.incoming.add(pos);
                }
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();
        }
    }
}

