package darksteel.exhaust;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import mindustry.entities.Effect;

/**
 * 废气机制的视觉表现。
 * <p>
 * 废气的主色是青绿色（{@code 42f5b3}）：一圈向外扩散的实心环 + 24 条放射状裂纹，
 * 整体用 {@link Interp#pow2Out} 做"先快后慢"的膨胀，看起来像空间被撕开。
 */
public class ExhaustFx {

    /** 废气的本体色，进度条、光效、残骸都用它。 */
    public static final Color exhaustColor = Color.valueOf("42f5b3");

    /** 废气爆炸：半径由 {@code e.rotation} 传入（世界单位）。 */
    public static final Effect exhaustExplosion = new Effect(48f, 120f, e -> {
        float fin = Interp.pow2Out.apply(e.fin());
        float fout = e.fout();

        Draw.color(e.color);
        Draw.alpha(fout);
        Lines.stroke(5.5f * fout);
        Lines.circle(e.x, e.y, 2f + fin * e.rotation);

        for (int i = 0; i < 24; i++) {
            long seed = (long) e.id * 31L + i;
            float angle = i * 15f + Mathf.randomSeedRange(seed, 6f);
            float distance = fin * e.rotation * Mathf.randomSeed(seed + 1L, 0.72f, 1.08f);
            float length = Mathf.randomSeed(seed + 2L, 8f, 16f);
            float px = e.x + Angles.trnsx(angle, distance);
            float py = e.y + Angles.trnsy(angle, distance);

            Draw.alpha(fout * Mathf.randomSeed(seed + 3L, 0.65f, 1f));
            Lines.stroke(2.8f * fout + 0.1f);
            Lines.lineAngle(px, py, angle, length);
        }

        Draw.reset();
    });

    /** 能量注入时的一圈小涟漪，给"能量正在灌进去"一点反馈。 */
    public static final Effect energyPulse = new Effect(22f, 24f, e -> {
        Draw.color(e.color);
        Draw.alpha(e.fout() * 0.7f);
        Lines.stroke(2f * e.fout() + 0.2f);
        Lines.circle(e.x, e.y, 2f + Interp.pow2Out.apply(e.fin()) * e.rotation);
        Draw.reset();
    });

    /** 泵向大气排气时冒出的烟团。{@code e.rotation} 是排气方向（度）。 */
    public static final Effect ventPuff = new Effect(30f, 26f, e -> {
        float fin = e.fin();
        Draw.color(e.color);
        Draw.alpha(e.fout() * 0.55f);
        for (int i = 0; i < 4; i++) {
            long seed = (long) e.id * 13L + i;
            float angle = e.rotation + Mathf.randomSeedRange(seed, 28f);
            float dist = (2f + fin * 14f) * (0.6f + Mathf.randomSeed(seed + 1L, 0f, 0.8f));
            Fill.circle(e.x + Angles.trnsx(angle, dist), e.y + Angles.trnsy(angle, dist), 1.2f + fin * 3.4f);
        }
        Draw.reset();
    });
}
