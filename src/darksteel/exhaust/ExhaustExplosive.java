package darksteel.exhaust;

/**
 * 「会炸」的废气建筑。
 * <p>
 * 普通建筑攒满废气时会被 <b>泄漏</b>（原地消失、留下残骸）；
 * 而实现了本接口的建筑攒满时会 <b>爆</b>：
 * 按 {@link #exhaustParameters()} 给出的半径、伤害、传导废气量、
 * 地形改变和残骸规则，对周围来一次全面的废气泄漏，并可能连锁引爆其它
 * {@link ExhaustExplosive}。
 * <p>
 * 注意：参数为 null 时（例如用 JSON 建的方块填不了这个字段），反应层会退化成
 * 普通泄漏而不是爆炸，不会带着 null 进队列。
 *
 * @see ExhaustReactions#request(mindustry.gen.Building)
 */
public interface ExhaustExplosive extends ExhaustTolerant {

    /** 该建筑爆炸时使用的反应参数。建议做成 static final 常量复用，不要每次新建。 */
    ExhaustReactionParameters exhaustParameters();
}
