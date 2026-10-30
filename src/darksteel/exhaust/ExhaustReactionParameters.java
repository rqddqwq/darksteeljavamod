package darksteel.exhaust;

import arc.graphics.Color;
import mindustry.entities.Effect;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;

/**
 * 一次废气泄漏（爆炸 / 废气）的全部参数。
 * <p>
 * 把「爆炸表现」全部数据化，{@link ExhaustExplosive} 只要返回一组参数，
 * 就不必再改 {@link ExhaustReactions} 的代码：
 * <pre>{@code
 * static final ExhaustReactionParameters params = new ExhaustReactionParameters(5, 300f, 600f, 20f)
 *     .floor(Blocks.darksand)          // 半径内地板变成暗沙
 *     .clearProps(true)                // 吹飞草丛石块
 *     .remnants(remnant1, remnant2, remnant3);
 * }</pre>
 */
public class ExhaustReactionParameters {

    /** 波及半径（格）。反应作用于以爆心为圆心、半径 radius 的圆内所有地块。 */
    public int radius = 4;
    /** 对半径内单位的伤害。 */
    public float unitDamage = 150f;
    /** 对核心的伤害。核心不参与能量积累，所以只能掉血。 */
    public float coreDamage = 300f;
    /** 向半径内每个建筑注入的废气，这才是"连锁"的燃料。 */
    public float propagatedEnergy = 20f;
    /** 半径内地板会被替换成它；null = 不改地板。 */
    public Floor floor;
    /** 半径内的矿石会被替换成它；null = 不改矿石。 */
    public Block ore;
    /** 半径内的静态墙会被替换成它；null = 不改墙。 */
    public Block wall;
    /** 是否清除半径内的植被、石块等装饰物（{@code Prop}）。 */
    public boolean clearProps = false;
    /** 残骸方块：{@code [0]}=1x1，{@code [1]}=2x2，{@code [2]}=3x3；null = 不留残骸。 */
    public Block[] remnants;
    /** 反应特效；null = {@link ExhaustFx#exhaustExplosion}。 */
    public Effect effect;
    /** 反应特效颜色；null = {@link ExhaustFx#exhaustColor}。 */
    public Color color;

    public ExhaustReactionParameters() {
    }

    /** 最常用的四个参数：半径、单位伤害、核心伤害、传导能量。 */
    public ExhaustReactionParameters(int radius, float unitDamage, float coreDamage, float propagatedEnergy) {
        this.radius = radius;
        this.unitDamage = unitDamage;
        this.coreDamage = coreDamage;
        this.propagatedEnergy = propagatedEnergy;
    }

    public ExhaustReactionParameters radius(int radius) {
        this.radius = radius;
        return this;
    }

    public ExhaustReactionParameters unitDamage(float unitDamage) {
        this.unitDamage = unitDamage;
        return this;
    }

    public ExhaustReactionParameters coreDamage(float coreDamage) {
        this.coreDamage = coreDamage;
        return this;
    }

    public ExhaustReactionParameters propagatedEnergy(float propagatedEnergy) {
        this.propagatedEnergy = propagatedEnergy;
        return this;
    }

    public ExhaustReactionParameters floor(Floor floor) {
        this.floor = floor;
        return this;
    }

    public ExhaustReactionParameters ore(Block ore) {
        this.ore = ore;
        return this;
    }

    public ExhaustReactionParameters wall(Block wall) {
        this.wall = wall;
        return this;
    }

    public ExhaustReactionParameters clearProps(boolean clearProps) {
        this.clearProps = clearProps;
        return this;
    }

    /** 传入 1x1 / 2x2 / 3x3 三种残骸，不足三个的位置传 null 即可。 */
    public ExhaustReactionParameters remnants(Block small, Block medium, Block large) {
        this.remnants = new Block[]{small, medium, large};
        return this;
    }

    public ExhaustReactionParameters effect(Effect effect) {
        this.effect = effect;
        return this;
    }

    public ExhaustReactionParameters color(Color color) {
        this.color = color;
        return this;
    }
}
