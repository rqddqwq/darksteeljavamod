package darksteel.exhaust;

/**
 * 能够容纳「废气」的建筑。
 * <p>
 * 废气是一层独立于电力/热量的模拟：本模组的废气方块会随时间<b>自产</b>废气，
 * 攒到 {@link #totalExhaust()} 上限就会泄漏（或按 {@link ExhaustExplosive} 爆炸）。
 * <p>
 * 实现该接口即代表"我参与废气场"，可以：
 * <ul>
 *     <li>被 {@link ExhaustStates#addExhaust} 注入废气并显示进度条</li>
 *     <li>被 {@code ExhaustPump} 之类的方块抽走废气</li>
 * </ul>
 * 不实现该接口的建筑（原版方块）走 {@link ExhaustStates} 的全局默认容量，
 * 由 {@code ExhaustPump} 负责帮它们排气。
 */
public interface ExhaustTolerant {

    /** 该建筑能容纳的废气总量，攒满即触发泄漏。 */
    float totalExhaust();
}
