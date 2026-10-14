/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Position;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.core.Renderer;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class OmniBridge
extends ItemBridge {
    public int range;
    public float speed = 30.0f;
    public float bridgeWidth = 8.0f;
    public float arrowTimeScl = 1.5f;
    public float arrowPeriod = 1.5f;
    public TextureRegion topRegion;
    protected static int currentFindX;
    protected static int currentFindY;
    protected static BuildPlan currentPlan;
    protected static final Boolf<BuildPlan> planFinder;

    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    public void init() {
        super.init();
        transportTime = 60.0f / speed;
    }

    public OmniBridge(String name) {
        super(name);
        hasPower = false;
        range = 6;
        itemCapacity = 10;
        buildType = () -> new OmniBridgeBuild();
    }

    public void setStats() {
        super.setStats();
        stats.add(Stat.range, (float)range, StatUnit.blocks);
    }

    public boolean positionsValid(int x1, int y1, int x2, int y2) {
        return Mathf.dst((float)x1, (float)y1, (float)x2, (float)y2) <= (float)range;
    }

    public void changePlacementPath(Seq<Point2> points, int rotation) {
        Placement.calculateNodes(points, (Block)this, (int)rotation, (point, other) -> Mathf.dst((float)point.x, (float)point.y, (float)other.x, (float)other.y) <= (float)range);
    }

    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile linkTile = findLink(x, y);
        Drawf.dashCircle((float)(x * 8), (float)(y * 8), (float)(range * 8), (Color)Pal.placing);
        Draw.reset();
        Draw.color((Color)Pal.placing);
        Lines.stroke((float)1.0f);
        if (linkTile != null && Math.abs(linkTile.x - x) + Math.abs(linkTile.y - y) > 1) {
            Lines.line((float)(x * 8), (float)(y * 8), (float)(linkTile.x * 8), (float)(linkTile.y * 8));
            Draw.rect((String)"bridge-arrow", (float)((float)(x * 8 + linkTile.x * 8) / 2.0f), (float)((float)(y * 8 + linkTile.y * 8) / 2.0f), (float)Angles.angle((float)linkTile.x, (float)linkTile.y, (float)x, (float)y));
        }
        Draw.reset();
    }

    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list) {
        Object object = plan.config;
        if (object instanceof Point2) {
            Point2 p = (Point2)object;
            if (Mathf.dst((float)p.x, (float)p.y) <= (float)range) {
                currentFindX = plan.x + p.x;
                currentFindY = plan.y + p.y;
                currentPlan = plan;
                BuildPlan otherReq = OmniBridge.findPlan(list, (int)currentFindX, (int)currentFindY, planFinder);
                if (otherReq != null) {
                    drawBridge(plan, otherReq.drawx(), otherReq.drawy(), 0.0f);
                }
            }
        }
    }

    public void drawBridge(BuildPlan req, float ox, float oy, float flip) {
        if (Mathf.zero((float)Renderer.bridgeOpacity)) {
            return;
        }
        Draw.alpha((float)Renderer.bridgeOpacity);
        Lines.stroke((float)bridgeWidth);
        Tmp.v1.set(ox, oy).sub(req.drawx(), req.drawy()).setLength(4.0f);
        Lines.line((TextureRegion)bridgeRegion, (float)(req.drawx() + Tmp.v1.x), (float)(req.drawy() + Tmp.v1.y), (float)(ox - Tmp.v1.x), (float)(oy - Tmp.v1.y), (boolean)false);
        Draw.rect((TextureRegion)arrowRegion, (float)((req.drawx() + ox) / 2.0f), (float)((req.drawy() + oy) / 2.0f), (float)(Angles.angle((float)req.drawx(), (float)req.drawy(), (float)ox, (float)oy) + flip));
        Draw.reset();
    }

    static {
        planFinder = other -> other.block == OmniBridge.currentPlan.block && currentPlan != other && currentFindX == other.x && currentFindY == other.y;
    }

    public class OmniBridgeBuild
    extends ItemBridge.ItemBridgeBuild {
        protected float warmup;

        public OmniBridgeBuild() {
            super((ItemBridge)OmniBridge.this);
            warmup = 0.0f;
        }

        public void playerPlaced(Object config) {
            if (config != null) {
                if (config instanceof Point2 p) {
                    configure(p);
                } else if (config instanceof Integer i) {
                    configure(i);
                }
            }
            if (link == -1) {
                if (OmniBridge.lastBuild != null && OmniBridge.lastBuild != this && OmniBridge.lastBuild.isValid() && OmniBridge.lastBuild.link == -1 && OmniBridge.positionsValid(tile.x, tile.y, OmniBridge.lastBuild.tile.x, OmniBridge.lastBuild.tile.y)) {
                    OmniBridge.lastBuild.configure((Object)pos());
                } else {
                    Tile nearest = null;
                    float nearestDist = Float.MAX_VALUE;
                    for (int dx = -OmniBridge.range; dx <= OmniBridge.range; ++dx) {
                        for (int dy = -OmniBridge.range; dy <= OmniBridge.range; ++dy) {
                            float dst;
                            Building building;
                            Tile other;
                            if (dx == 0 && dy == 0 || Mathf.dst((float)dx, (float)dy) > (float)OmniBridge.range || (other = Vars.world.tile(tile.x + dx, tile.y + dy)) == null || !((building = other.build) instanceof OmniBridgeBuild)) continue;
                            OmniBridgeBuild b = (OmniBridgeBuild)building;
                            if (other.build.team != team || b.link != -1 || !((dst = Mathf.dst((float)dx, (float)dy)) < nearestDist)) continue;
                            nearestDist = dst;
                            nearest = other;
                        }
                    }
                    if (nearest != null) {
                        nearest.build.configure((Object)pos());
                    }
                }
            }
            OmniBridge.lastBuild = this;
        }

        public void onProximityAdded() {
            Building building;
            Tile existing;
            if (link != -1 && (existing = Vars.world.tile(link)) != null && (building = existing.build) instanceof OmniBridgeBuild) {
                int pos;
                OmniBridgeBuild other = (OmniBridgeBuild)building;
                if (existing.build.team == team && OmniBridge.positionsValid(tile.x, tile.y, existing.x, existing.y) && !other.incoming.contains(pos = tile.pos())) {
                    other.incoming.add(pos);
                }
            }
        }

        public void drawConfigure() {
            Drawf.dashCircle((float)x, (float)y, (float)(OmniBridge.range * 8), (Color)Pal.accent);
            Drawf.select((float)x, (float)y, (float)((float)(block.size * 8) / 2.0f + 2.0f), (Color)Pal.accent);
            for (int dx = -OmniBridge.range; dx <= OmniBridge.range; ++dx) {
                for (int dy = -OmniBridge.range; dy <= OmniBridge.range; ++dy) {
                    boolean isInput;
                    Building building;
                    Tile other;
                    if (dx == 0 && dy == 0 || Mathf.dst((float)dx, (float)dy) > (float)OmniBridge.range || (other = Vars.world.tile(tile.x + dx, tile.y + dy)) == null || !((building = other.build) instanceof OmniBridgeBuild)) continue;
                    OmniBridgeBuild otherBuild = (OmniBridgeBuild)building;
                    if (other.build.team != team) continue;
                    boolean isOutput = link == other.pos();
                    boolean bl = isInput = otherBuild.link == tile.pos();
                    if (isOutput) {
                        Drawf.select((float)other.drawx(), (float)other.drawy(), (float)((float)(other.block().size * 8) / 2.0f + 2.0f), (Color)Pal.place);
                        continue;
                    }
                    if (isInput) continue;
                    Drawf.select((float)other.drawx(), (float)other.drawy(), (float)((float)(other.block().size * 8) / 2.0f + 2.0f + Mathf.absin((float)Time.time, (float)4.0f, (float)1.0f)), (Color)Pal.breakInvalid);
                }
            }
        }

        public boolean onConfigureBuildTapped(Building other) {
            if (other == this) {
                return false;
            }
            if (other instanceof OmniBridgeBuild && other.team == team && OmniBridge.positionsValid(tile.x, tile.y, other.tileX(), other.tileY())) {
                if (link == other.pos()) {
                    configure(-1);
                } else {
                    if (((OmniBridgeBuild)other).link == pos()) {
                        other.configure((Object)-1);
                    }
                    configure(other.pos());
                }
                return false;
            }
            return true;
        }

        public Point2 config() {
            return link == -1 ? null : Point2.unpack((int)link).sub((int)tile.x, (int)tile.y);
        }

        public void configure(Object value) {
            if (value instanceof Point2 p) {
                Tile target = Vars.world.tile(tile.x + p.x, tile.y + p.y);
                link = target == null ? -1 : target.pos();
            } else if (value instanceof Integer) {
                link = (Integer)value;
            }
        }

        public void updateTile() {
            boolean active = items.total() > 0 || link != -1 && Vars.world.tile(link) != null;
            warmup = Mathf.lerpDelta((float)warmup, (float)(active ? 1.0f : 0.0f), (float)0.1f);
            super.updateTile();
        }

        public void checkIncoming() {
            for (int idx = 0; idx < incoming.size; ++idx) {
                Building building;
                int i = incoming.items[idx];
                Tile other = Vars.world.tile(i);
                if (other != null && other.build != null && (building = other.build) instanceof OmniBridgeBuild) {
                    OmniBridgeBuild b = (OmniBridgeBuild)building;
                    if (b.link == tile.pos() && OmniBridge.positionsValid(tile.x, tile.y, other.x, other.y)) continue;
                }
                incoming.removeIndex(idx);
                --idx;
            }
        }

        public void draw() {
            Draw.rect((TextureRegion)OmniBridge.region, (float)x, (float)y);
            Draw.z((float)70.1f);
            Draw.rect((TextureRegion)OmniBridge.topRegion, (float)x, (float)y);
            Draw.z((float)70.0f);
            Tile other = Vars.world.tile(link);
            if (!OmniBridge.linkValid(tile, other)) {
                return;
            }
            if (Mathf.zero((float)Renderer.bridgeOpacity)) {
                return;
            }
            Draw.alpha((float)(Math.max(warmup, 0.25f) * Renderer.bridgeOpacity));
            Lines.stroke((float)OmniBridge.bridgeWidth);
            Lines.line((TextureRegion)OmniBridge.bridgeRegion, (float)x, (float)y, (float)other.worldx(), (float)other.worldy(), (boolean)false);
            float dst = Mathf.dst((float)x, (float)y, (float)other.worldx(), (float)other.worldy()) - 2.0f;
            float ang = Angles.angle((float)x, (float)y, (float)other.worldx(), (float)other.worldy());
            int seg = Mathf.round((float)(dst / 8.0f));
            for (int i = 0; i < seg; ++i) {
                Tmp.v1.trns(ang, dst / (float)seg * (float)i + 1.0f).add((Position)this);
                Draw.alpha((float)(Mathf.absin((float)((float)i - time / OmniBridge.arrowTimeScl), (float)OmniBridge.arrowPeriod, (float)1.0f) * warmup * Renderer.bridgeOpacity));
                Draw.rect((TextureRegion)OmniBridge.arrowRegion, (float)Tmp.v1.x, (float)Tmp.v1.y, (float)ang);
            }
            Draw.color();
            Draw.reset();
        }

        public void drawSelect() {
            Tile target = Vars.world.tile(link);
            if (target != null && OmniBridge.linkValid(tile, target)) {
                drawInput(target, true);
            }
            checkIncoming();
            incoming.each(pos -> {
                Tile other = Vars.world.tile(pos);
                if (other != null) {
                    drawInput(other, false);
                }
            });
            Draw.reset();
        }

        private void drawInput(Tile other, boolean isOutput) {
            if (!OmniBridge.linkValid(tile, other, false)) {
                return;
            }
            boolean linked = isOutput;
            Tmp.v2.trns(tile.angleTo((Position)other), 2.0f);
            float tx = tile.drawx();
            float ty = tile.drawy();
            float ox = other.drawx();
            float oy = other.drawy();
            float alpha = Math.abs((float)(linked ? 100 : 0) - Time.time * 2.0f % 100.0f) / 100.0f;
            float x = Mathf.lerp((float)ox, (float)tx, (float)alpha);
            float y = Mathf.lerp((float)oy, (float)ty, (float)alpha);
            Tile otherLink = linked ? other : tile;
            byte rel = (linked ? tile : other).absoluteRelativeTo((int)otherLink.x, (int)otherLink.y);
            Draw.color((Color)Pal.gray);
            Lines.stroke((float)2.5f);
            Lines.square((float)ox, (float)oy, (float)2.0f, (float)45.0f);
            Lines.stroke((float)2.5f);
            Lines.line((float)(tx + Tmp.v2.x), (float)(ty + Tmp.v2.y), (float)(ox - Tmp.v2.x), (float)(oy - Tmp.v2.y));
            float color = (linked ? Pal.place : Pal.accent).toFloatBits();
            Draw.color((float)color);
            Lines.stroke((float)1.0f);
            Lines.line((float)(tx + Tmp.v2.x), (float)(ty + Tmp.v2.y), (float)(ox - Tmp.v2.x), (float)(oy - Tmp.v2.y));
            Lines.square((float)ox, (float)oy, (float)2.0f, (float)45.0f);
            Draw.mixcol((float)color);
            Draw.color();
            float angle = Angles.angle((float)tx, (float)ty, (float)ox, (float)oy);
            Draw.rect((TextureRegion)OmniBridge.arrowRegion, (float)x, (float)y, (float)angle);
            Draw.mixcol();
        }
    }
}

