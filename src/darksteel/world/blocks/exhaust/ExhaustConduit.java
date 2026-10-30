package darksteel.world.blocks.exhaust;

import darksteel.exhaust.ExhaustReactions;
import darksteel.exhaust.ExhaustStates;
import darksteel.exhaust.ExhaustTolerant;
import mindustry.Vars;
import mindustry.gen.Building;

/**
 * 废气导管 —— 机制的「输运」部分，对应电力里的导线。
 * <p>
 * 每 {@link #transferInterval} 刻往两边看一次：
 * 先把废气比自己多的邻居抽过来，再往废气比自己少的邻居送过去，
 * 于是废气会像热量一样从"浓"往"稀"扩散，最终把提取器的高废气摊平到整条链路上。
 * <p>
 * 只有实现了 {@link ExhaustTolerant} 的建筑才参与传导（这是刻意的：
 * 原版建筑"绝缘"，否则随手放一根导管就能把自家基地泄干净）。
 * 自己装满 {@code totalExhaust} 之后会被泄成残骸。
 */
public class ExhaustConduit extends ExhaustBlock {

    /** 每次传导搬运的废气。 */
    public float transferRate = 1.5f;
    /** 传导间隔（游戏刻）。 */
    public float transferInterval = 8f;

    public ExhaustConduit(String name) {
        super(name);
        update = true;
        solid = true;
        sync = true;
        canOverdrive = false;
        // 管子不自产，只搬运
        exhaustRate = 0f;
        // 比提取器的 35 高很多：厚壁导管只是"管子"，正常情况下不该被撑爆
        totalExhaust = 60f;
        buildType = ExhaustConduitBuild::new;
    }

    public class ExhaustConduitBuild extends Building implements ExhaustTolerant {

        private float timer;

        @Override
        public float totalExhaust() {
            return ExhaustConduit.this.totalExhaust;
        }

        @Override
        public void updateTile() {
            if (Vars.net.client() || !enabled) {
                return;
            }

            timer += delta();
            if (timer < transferInterval) {
                return;
            }
            timer %= transferInterval;

            // 抽：邻居比自己多
            eachConduitNeighbor(other -> {
                if (ExhaustStates.exhaust(other) > ExhaustStates.exhaust(this)) {
                    ExhaustStates.transferExhaust(other, this, transferRate);
                }
            });

            // 送：邻居比自己少
            eachConduitNeighbor(other -> {
                if (ExhaustStates.exhaust(this) > ExhaustStates.exhaust(other)) {
                    ExhaustStates.transferExhaust(this, other, transferRate);
                }
            });

            // 顶满了就泄漏（走 requestConversion，同帧不会重复排队）
            if (ExhaustStates.atCapacity(this)) {
                ExhaustReactions.requestConversion(this);
            }
        }

        /** 只和"参与废气场"的相邻建筑打交道。 */
        private void eachConduitNeighbor(arc.func.Cons<Building> consumer) {
            for (int i = 0; i < 4; i++) {
                Building other = nearby(i);
                if (other == null || other == this || !(other instanceof ExhaustTolerant)) {
                    continue;
                }
                consumer.get(other);
            }
        }
    }
}
