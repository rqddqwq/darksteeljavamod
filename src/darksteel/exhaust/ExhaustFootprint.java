package darksteel.exhaust;

import arc.func.Cons;
import mindustry.world.Tile;

/**
 * 自定义「占地足迹」的建筑。
 * <p>
 * 默认情况下，一个建筑在泄漏时会被清空的地块，就是它 {@code size x size} 覆盖的那些格子；
 * 多格异形建筑（比如 1x3 的提取器、L 形的机器）可以实现本接口，自己列出真正占用的地块，
 * 免得泄漏时留下半截身子。
 *
 * @see ExhaustReactions
 */
public interface ExhaustFootprint {

    /** 遍历该建筑实际占用的所有地块（泄漏时会被一起清空）。 */
    void eachExhaustTile(Cons<Tile> consumer);
}
