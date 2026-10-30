package darksteel.world.blocks.exhaust;

import darksteel.exhaust.ExhaustStates;
import mindustry.world.Block;

/**
 * 参与废气场（exhaust field）的方块基类。
 * <p>
 * 在 {@link Block} 上加了两个<b>对 JSON / hjson 内容开放</b>的公开字段，
 * 内容文件里 {@code type: ExhaustBlock}（或本包任何子类）即可直接配置：
 * <pre>{@code
 * type: ExhaustPump
 * exhaustRate: 0f      // 每秒自产废气量，0 = 不自产
 * totalExhaust: 40f    // 废气总容量，攒满即泄漏/爆炸
 * }</pre>
 * <p>
 * 不继承本类的方块（原版方块）<b>不会自己产气</b>（{@link ExhaustStates#defaultExhaustRate} 为 0），
 * 但它们只要被爆炸波及或被手动灌气，同样会积累废气、同样会泄漏 —— 参与与否是机制层的事，
 * 需不需要方块配合是另一回事。
 */
public class ExhaustBlock extends Block {

    /** 该方块每秒自产的废气量。JSON 字段名：{@code exhaustRate}。 */
    public float exhaustRate = ExhaustStates.defaultExhaustRate;

    /** 该方块能容纳的废气总量，攒满即触发泄漏（或爆炸）。JSON 字段名：{@code totalExhaust}。 */
    public float totalExhaust = ExhaustStates.defaultTotalExhaust;

    public ExhaustBlock(String name) {
        super(name);
    }
}
