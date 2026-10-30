package darksteel.world.blocks.exhaust;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import darksteel.exhaust.ExhaustExplosive;
import darksteel.exhaust.ExhaustFx;
import darksteel.exhaust.ExhaustReactionParameters;
import darksteel.exhaust.ExhaustReactions;
import darksteel.exhaust.ExhaustStates;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.graphics.Layer;

/**
 * 废气装置 —— 机制的「手动引爆」入口。
 * <p>
 * 它本身不产气（{@code exhaustRate = 0}），只负责攒：机制层和导管往它身上灌，
 * 装满 {@code totalExhaust} 就自己炸；也可以点开配置面板直接引爆。
 * <p>
 * 它演示的是 {@link ExhaustExplosive} 这条路径：爆炸 → 给周围建筑灌废气
 * → 周围建筑满容量 → 也变成爆炸物 → 连锁。半径 6 格内基本什么都剩不下。
 */
public class ExhaustBomb extends ExhaustBlock {

    /** 爆炸参数，由内容类注入（要引用残骸方块）。 */
    public ExhaustReactionParameters parameters;

    public ExhaustBomb(String name) {
        super(name);
        update = true;
        solid = true;
        sync = true;
        configurable = true;
        canOverdrive = false;
        exhaustRate = 0f;
        // 故意很低：废气场一扩散到这里（>25）就炸，这样"提取器→导管→装置"这条链才连得通
        totalExhaust = 25f;
        buildType = ExhaustBombBuild::new;
    }

    public class ExhaustBombBuild extends Building implements ExhaustExplosive {

        @Override
        public float totalExhaust() {
            return ExhaustBomb.this.totalExhaust;
        }

        @Override
        public ExhaustReactionParameters exhaustParameters() {
            return parameters;
        }

        /** 引爆。任何时候都能调，废气不满也炸。 */
        public void detonate() {
            ExhaustReactions.request(this);
        }

        @Override
        public void updateTile() {
            if (Vars.net.client() || !enabled) {
                return;
            }
            if (ExhaustStates.atCapacity(this)) {
                detonate();
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.button("引爆", () -> configure(Boolean.TRUE)).size(180f, 60f).row();
            table.label(() -> Core.bundle.format("bar.exhaust") + " "
                + (int) ExhaustStates.exhaust(this) + " / " + (int) ExhaustStates.totalExhaust(this));
        }

        @Override
        public void draw() {
            super.draw();

            float fill = ExhaustStates.fillFraction(this);
            if (fill <= 0.01f) {
                return;
            }

            Draw.z(Layer.blockAdditive);
            Draw.color(ExhaustFx.exhaustColor, 0.3f + 0.5f * Mathf.absin(Time.time, 4f, 0.4f));
            Lines.stroke(1.5f);
            Lines.circle(x, y, 4f + 10f * fill);
            Draw.color();
            Draw.z(Layer.block);
        }
    }
}
