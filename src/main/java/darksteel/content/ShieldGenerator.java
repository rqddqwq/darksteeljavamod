package darksteel.content;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.FloatSeq;
import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;

class ShieldState {
    public FloatSeq hp;
    public FloatSeq cd;
    public float rot;

    public ShieldState(int count) {
        hp = new FloatSeq();
        cd = new FloatSeq();
        rot = 0f;
        for (int i = 0; i < count; i++) {
            hp.add(0f);
            cd.add(0f);
        }
    }
}

public class ShieldGenerator extends Ability {
    public int shieldCount = 3;
    public float maxShieldPer = 120f;
    public float regenSpeed = 4f;
    public float sectorFraction = 0.7f;
    public float shieldThickness = 10f;
    public float rotateSpeed = 1.2f;
    public float shieldBreakCooldown = 180f;
    public float shieldRadius = 42f;
    public float shieldHitWidth = 8f;
    public float reflectChance = 0f;

    public transient ShieldState state;
    private static final Color defaultShieldColor = Color.valueOf("ffe48c");

    // 点到线段最短距离
    private static float pointSegDist(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        if (len2 < 0.0001f) return Mathf.dst(px, py, x1, y1);
        float t = ((px - x1) * dx + (py - y1) * dy) / len2;
        t = Mathf.clamp(t, 0f, 1f);
        return Mathf.dst(px, py, x1 + t * dx, y1 + t * dy);
    }

    @Override
    public void update(Unit unit) {
        if (!unit.isValid()) return;

        float delta = Time.delta;

        // 状态初始化 / 数量变化重建
        if (state == null || state.hp.size != shieldCount) {
            state = new ShieldState(shieldCount);
            for (int i = 0; i < shieldCount; i++) {
                state.hp.items[i] = maxShieldPer;
                state.cd.items[i] = 0f;
            }
        }

        // 旋转（取模，避免无限增长）
        state.rot = (state.rot + rotateSpeed * delta) % 360f;

        // 冷却 + 回血
        for (int i = 0; i < shieldCount; i++) {
            float cdt = state.cd.items[i];
            if (cdt > 0f) {
                state.cd.items[i] = cdt - delta;
                state.hp.items[i] = 0f;
            } else if (state.hp.items[i] < maxShieldPer) {
                state.hp.items[i] = Mathf.clamp(state.hp.items[i] + regenSpeed * delta, 0f, maxShieldPer);
            }
        }

        // 护盾几何参数
        float sectorAngle = 360f / shieldCount;
        float halfSectorRad = (sectorAngle * sectorFraction / 2f) * Mathf.degRad;
        float halfLen = shieldRadius * (float) Math.tan(halfSectorRad);

        // 子弹碰撞：先用半径粗筛，再逐片判定
        float detectRadius = shieldRadius + halfLen + shieldHitWidth;

        Groups.bullet.each(bullet -> {
            if (bullet.team == unit.team) return;
            if (!bullet.type.absorbable) return;
            if (!bullet.isAdded()) return;

            float dist = Mathf.dst(unit.x, unit.y, bullet.x, bullet.y);
            if (dist > detectRadius) return;

            for (int i = 0; i < shieldCount; i++) {
                float hp = state.hp.items[i];
                float cdt = state.cd.items[i];
                if (hp <= 0f || cdt > 0f) continue;

                float sliceAngle = state.rot + sectorAngle * i;
                float rad = sliceAngle * Mathf.degRad;
                float cx = unit.x + Mathf.cos(rad) * shieldRadius;
                float cy = unit.y + Mathf.sin(rad) * shieldRadius;
                float lineRad = rad + Mathf.PI / 2f;
                float sx1 = cx - Mathf.cos(lineRad) * halfLen;
                float sy1 = cy - Mathf.sin(lineRad) * halfLen;
                float sx2 = cx + Mathf.cos(lineRad) * halfLen;
                float sy2 = cy + Mathf.sin(lineRad) * halfLen;

                if (pointSegDist(bullet.x, bullet.y, sx1, sy1, sx2, sy2) <= shieldHitWidth) {
                    if (Mathf.random() <= reflectChance) {
                        bullet.vel.x *= -1f;
                        bullet.vel.y *= -1f;
                    } else {
                        float newHp = hp - Math.max(bullet.damage(), 1f);
                        if (newHp <= 0f) {
                            state.hp.items[i] = 0f;
                            state.cd.items[i] = shieldBreakCooldown;
                        } else {
                            state.hp.items[i] = newHp;
                        }
                        bullet.absorb();
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void draw(Unit unit) {
        if (!unit.isValid() || state == null) return;

        Color baseColor;
        if (unit.team != null && unit.team.color != null) {
            baseColor = unit.team.color;
        } else if (unit.type.shieldColor != null) {
            baseColor = unit.type.shieldColor;
        } else {
            baseColor = defaultShieldColor;
        }

        float sectorAngle = 360f / shieldCount;
        float halfSectorRad = (sectorAngle * sectorFraction / 2f) * Mathf.degRad;
        float halfLen = shieldRadius * (float) Math.tan(halfSectorRad);

        Draw.z(Layer.shields);

        for (int i = 0; i < shieldCount; i++) {
            float hp = state.hp.items[i];
            float cdt = state.cd.items[i];
            if (hp <= 0f || cdt > 0f) continue;

            float hpRatio = hp / maxShieldPer;
            float alpha = 0.35f + 0.4f * hpRatio;

            float sliceAngle = state.rot + sectorAngle * i;
            float rad = sliceAngle * Mathf.degRad;
            float cx = unit.x + Mathf.cos(rad) * shieldRadius;
            float cy = unit.y + Mathf.sin(rad) * shieldRadius;
            float lineRad = rad + Mathf.PI / 2f;
            float sx1 = cx - Mathf.cos(lineRad) * halfLen;
            float sy1 = cy - Mathf.sin(lineRad) * halfLen;
            float sx2 = cx + Mathf.cos(lineRad) * halfLen;
            float sy2 = cy + Mathf.sin(lineRad) * halfLen;

            Draw.color(baseColor, alpha);
            Lines.stroke(shieldThickness);
            Lines.line(sx1, sy1, sx2, sy2);
        }
        Draw.reset();
    }

    @Override
    public String toString() {
        return "ShieldGenerator";
    }
}