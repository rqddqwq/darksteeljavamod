package darksteel.world.blocks.exhaust;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import darksteel.exhaust.ExhaustExplosive;
import darksteel.exhaust.ExhaustFx;
import darksteel.exhaust.ExhaustReactionParameters;
import darksteel.exhaust.ExhaustReactions;
import darksteel.exhaust.ExhaustStates;
import mindustry.Vars;
import mindustry.gen.Building;

/**
 * 废气提取器 —— 机制的「重灾区」演示。
 * <p>
 * 本模组的废气方块会自己产气（{@link ExhaustStates} 机制层负责），而提取器的
 * {@code exhaustRate} 故意设得非常高：它是最快装满、最先爆的那个。
 * 攒满 {@code totalExhaust} 之后立刻变成一颗炸弹：半径内建筑被灌废气、
 * 地板变成废气焦土、普通建筑被泄成残骸。
 * <p>
 * 换句话说：<b>它不是发电机，而是一颗定时炸弹</b>。玩家要用泵把它的废气抽走，
 * 或者接受它炸掉 —— 这就是"废气"和电力、热量最大的区别：<b>废气本身就是灾祸。</b>
 */
public class ExhaustExtractor extends ExhaustBlock {

    /** 爆炸参数，由内容类注入（因为要引用残骸方块，不能在构造函数里写死）。 */
    public ExhaustReactionParameters parameters;

    /** 废气可视化贴图，覆盖在本体上。缺失时不报错。 */
    public TextureRegion heatRegion;

    public ExhaustExtractor(String name) {
        super(name);
        update = true;
        solid = true;
        rotate = true;
        rotateDraw = true;
        sync = true;
        canOverdrive = false;
        exhaustRate = 3f;
        totalExhaust = 35f;
        buildType = ExhaustExtractorBuild::new;
    }

    @Override
    public void load() {
        super.load();
        heatRegion = Core.atlas.find(name + "-heat");
    }

    public class ExhaustExtractorBuild extends Building implements ExhaustExplosive {

        private float heat;

        @Override
        public float totalExhaust() {
            return ExhaustExtractor.this.totalExhaust;
        }

        @Override
        public ExhaustReactionParameters exhaustParameters() {
            return parameters;
        }

        @Override
        public void updateTile() {
            // 废气由 ExhaustStates 的机制层逐帧注入，这里只做可视化和兜底引爆
            float fill = ExhaustStates.fillFraction(this);
            heat = Mathf.approachDelta(heat, fill, 0.03f);

            if (Vars.net.client() || !enabled) {
                return;
            }

            // 满了就炸：request 走队列，不会在 update 里递归
            if (ExhaustStates.atCapacity(this)) {
                ExhaustReactions.request(this);
            }
        }

        @Override
        public void draw() {
            super.draw();

            if (!Core.atlas.isFound(heatRegion) || heat <= 0.001f) {
                return;
            }

            float fill = ExhaustStates.fillFraction(this);

            Draw.blend(Blending.additive);
            // 越接近上限越红，给玩家"要炸了"的预警
            Draw.color(ExhaustFx.exhaustColor, Color.scarlet, Mathf.clamp((fill - 0.6f) / 0.4f));
            Draw.alpha(heat * (0.7f + Mathf.absin(Time.time, 5f, 0.3f)));
            Draw.rect(heatRegion, x, y, drawrot());
            Draw.blend();
            Draw.color();
        }
    }
}
