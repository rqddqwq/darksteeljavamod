package darksteel.content;

import darksteel.exhaust.ExhaustReactionParameters;
import darksteel.exhaust.ExhaustReactions;
import darksteel.world.blocks.exhaust.ExhaustBomb;
import darksteel.world.blocks.exhaust.ExhaustConduit;
import darksteel.world.blocks.exhaust.ExhaustExtractor;
import darksteel.world.blocks.exhaust.ExhaustPump;
import mindustry.content.Items;
import mindustry.type.Category;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.meta.BuildVisibility;

import static mindustry.type.ItemStack.with;

/**
 * 废气机制的演示内容。
 * <p>
 * <b>本模组的废气方块会自己产气</b>（由 {@link darksteel.exhaust.ExhaustStates} 的机制层
 * 统一注入，产率写在各自的 {@code exhaustRate} 上）；原版方块不主动产气，
 * 只有被爆炸波及才沾上废气 —— 所以这套机制不会成为"全图通杀"。
 * <p>
 * 这几个方块只是把这套机制摆出来给人看：
 * <pre>
 *   提取器（产气最快，装满自爆）
 *        ↓  废气
 *   导管（把废气摊平，上限高）
 *        ↓  废气
 *   废气装置（上限低，气一到就爆 / 也可手动引爆）
 *
 *   废气泵（放在陆地上，朝谁抽谁，把废气排到大气）
 * </pre>
 * 另外还有被废气烧过的地形和三种尺寸的残骸，都是给爆炸收尾用的。
 */
public class DarksteelBlocks {

    /** 被废气过的地面。 */
    public static Floor scorchedFloor;

    /** 残骸：1x1 / 2x2 / 3x3。 */
    public static Block remnantSmall, remnantMedium, remnantLarge;

    public static ExhaustExtractor exhaustExtractor;
    public static ExhaustConduit exhaustConduit;
    public static ExhaustBomb exhaustBomb;
    /** 排气泵：抽掉任何建筑身上的废气（原版方块也行）。 */
    public static ExhaustPump exhaustPump;

    public static void load() {
        loadEnvironment();
        loadBlocks();

        // 普通建筑被"废气"（非爆炸）时铺的残骸
        ExhaustReactions.conversionRemnants = new Block[]{remnantSmall, remnantMedium, remnantLarge};
    }

    private static void loadEnvironment() {
        scorchedFloor = new Floor("exhaust-scorched-floor", 3) {
            {
                localizedName = "废气焦土";
            }
        };

        remnantSmall = new Prop("exhaust-remnant-1") {
            {
                localizedName = "废气残骸";
                variants = 0;
                solid = false;
                alwaysReplace = true;
            }
        };

        remnantMedium = new Prop("exhaust-remnant-2") {
            {
                localizedName = "废气残骸";
                variants = 0;
                solid = false;
                alwaysReplace = true;
            }
        };

        remnantLarge = new Prop("exhaust-remnant-3") {
            {
                localizedName = "废气残骸";
                variants = 0;
                solid = false;
                alwaysReplace = true;
            }
        };
    }

    private static void loadBlocks() {
        exhaustExtractor = new ExhaustExtractor("exhaust-extractor") {
            {
                localizedName = "废气提取器";
                description = "疯狂产气的机器：每秒 3 点废气，攒满 35 就自爆，把周围的一切泄成残骸。";
                details = "废气不占背包、不走电网，它只存在于建筑身上。装满 = 死。\n用废气泵对着它抽，可以续命。";

                requirements(Category.production, BuildVisibility.shown, with(Items.silicon, 60, Items.lead, 40, Items.graphite, 30));
                health = 260;
                // 演示用：免研究，进游戏就能建
                alwaysUnlocked = true;

                parameters = new ExhaustReactionParameters(4, 240f, 500f, 20f)
                    .floor(scorchedFloor)
                    .clearProps(true)
                    .remnants(remnantSmall, remnantMedium, remnantLarge);
            }
        };

        exhaustConduit = new ExhaustConduit("exhaust-conduit") {
            {
                localizedName = "废气导管";
                description = "在相邻的废气建筑之间搬运废气，像热量一样从高往低摊平。";
                details = "上限 60，正常情况下撑不爆它；一旦真的被灌满，就会原地泄漏成残骸。\n只连接同样能容纳废气的建筑，原版建筑对它绝缘。";

                requirements(Category.production, BuildVisibility.shown, with(Items.silicon, 20, Items.lead, 15));
                health = 120;
                alwaysUnlocked = true;
            }
        };

        exhaustBomb = new ExhaustBomb("exhaust-bomb") {
            {
                localizedName = "废气装置";
                description = "储存废气，上限只有 25，废气一漫过来就炸；也可以点开面板手动引爆。";
                details = "爆炸会给半径 6 格内所有建筑灌 25 点废气，因此很容易连锁。";

                requirements(Category.effect, BuildVisibility.shown, with(Items.silicon, 120, Items.thorium, 80, Items.plastanium, 60));
                health = 400;
                alwaysUnlocked = true;

                parameters = new ExhaustReactionParameters(6, 600f, 1200f, 25f)
                    .floor(scorchedFloor)
                    .clearProps(true)
                    .remnants(remnantSmall, remnantMedium, remnantLarge);
            }
        };

        exhaustPump = new ExhaustPump("exhaust-pump") {
            {
                localizedName = "废气泵";
                description = "放在陆地上，朝向任意一个建筑，把它身上的废气抽出来排到大气里。";
                details = "本模组方块会自己产气，原版建筑被爆炸波及后也会沾气 —— 泵都能抽。\n只抽「正面一格」，放置后用扳手转方向，抽气时会冒烟。";

                requirements(Category.effect, BuildVisibility.shown, with(Items.surgeAlloy, 45, Items.lead, 30, Items.silicon, 20));
                health = 180;
                alwaysUnlocked = true;
            }
        };
    }
}
