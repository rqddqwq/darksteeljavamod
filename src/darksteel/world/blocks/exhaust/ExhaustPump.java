package darksteel.world.blocks.exhaust;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import darksteel.exhaust.ExhaustFx;
import darksteel.exhaust.ExhaustStates;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.meta.StatUnit;

/**
 * 废气泵 —— 机制的「排水口」。
 * <p>
 * 可以放在陆地上，朝向一个相邻建筑，把它的废气抽出来排放到大气里。
 * <b>目标不挑食</b>：本模组建筑能抽，原版建筑也能抽（原版建筑虽然不自己产气，
 * 但被爆炸波及后一样会沾上废气，泵是玩家抢救基地的手段）。
 * <p>
 * 朝向型方块：放置后用扳手旋转，它只抽「正面一格」的邻居。
 * <p>
 * JSON 字段（继承自 {@link ExhaustBlock} 再加自己的）：
 * <pre>{@code
 * type: ExhaustPump
 * exhaustRate: 0f      // 泵自己基本不产气
 * totalExhaust: 80f
 * pumpRate: 8f         // 每秒最多抽走多少
 * ventInterval: 24f    // 排气特效间隔（刻）
 * }</pre>
 */
public class ExhaustPump extends ExhaustBlock {

    /** 每秒最多能抽走多少废气。 */
    public float pumpRate = 8f;
    /** 排气特效间隔（刻）。 */
    public float ventInterval = 24f;

    /** 顶部箭头贴图，表示当前抽哪边。 */
    public TextureRegion arrowRegion;

    public ExhaustPump(String name) {
        super(name);
        update = true;
        solid = true;
        rotate = true;
        rotateDraw = true;
        sync = true;
        canOverdrive = false;
        // 泵自己几乎不产气，容量也大：它是最不容易被撑爆的方块
        exhaustRate = 0.05f;
        totalExhaust = 80f;
        buildType = ExhaustPumpBuild::new;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(ExhaustStates.exhaustPumpRate, pumpRate, StatUnit.perSecond);
    }

    @Override
    public void load() {
        super.load();
        arrowRegion = Core.atlas.find(name + "-arrow");
    }

    @Override
    public TextureRegion[] icons() {
        return arrowRegion != null && Core.atlas.isFound(arrowRegion)
            ? new TextureRegion[]{region, arrowRegion}
            : super.icons();
    }

    public class ExhaustPumpBuild extends Building {

        private float ventTimer;

        /** 朝向一格的建筑；没有就是 null。 */
        public Building target() {
            return nearby(rotation);
        }

        @Override
        public void updateTile() {
            if (Vars.net.client() || !enabled) {
                return;
            }

            Building target = target();
            if (target != null && target != this) {
                // 把目标身上的废气搬进泵自己，再立刻排到大气 —— 泵只是管道，气不会留在它身上
                float moved = ExhaustStates.transferExhaust(target, this, pumpRate / 60f * delta());
                if (moved > 0f) {
                    ExhaustStates.removeExhaust(this, moved);

                    ventTimer += delta();
                    if (ventTimer >= ventInterval) {
                        ventTimer = 0f;
                        vent();
                    }
                }
            }

            // 兜底：万一自产积累到顶（被堵死等），自动泄压
            if (ExhaustStates.atCapacity(this)) {
                ExhaustStates.removeExhaust(this, 1f);
                vent();
            }
        }

        private void vent() {
            float angle = rotdeg();
            ExhaustFx.ventPuff.at(
                x + Angles.trnsx(angle, 6f), y + Angles.trnsy(angle, 6f),
                angle, ExhaustFx.exhaustColor);
        }

        @Override
        public void draw() {
            super.draw();

            if (!Core.atlas.isFound(arrowRegion)) {
                return;
            }

            Building target = target();
            // 有目标时箭头微微呼吸，提示"正在抽"
            float pulse = target != null && target != this
                ? 0.85f + 0.15f * Mathf.absin(Time.time, 4f, 1f)
                : 0.35f;

            Draw.color(ExhaustFx.exhaustColor, pulse);
            Draw.rect(arrowRegion, x, y, rotdeg() - 90f);
            Draw.color();
        }
    }
}
