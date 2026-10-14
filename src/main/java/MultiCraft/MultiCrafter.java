/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.func.Boolf;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.IntSet;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Structs;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import java.lang.invoke.LambdaMetafactory;
import java.util.Arrays;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.content.TechTree;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.EventType;
import mindustry.game.Objectives;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Sounds;
import mindustry.gen.Teamc;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.io.TypeIO;
import mindustry.logic.LAccess;
import mindustry.logic.Senseable;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.type.PayloadSeq;
import mindustry.type.PayloadStack;
import mindustry.type.UnitType;
import mindustry.ui.Bar;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.heat.HeatBlock;
import mindustry.world.blocks.heat.HeatConductor;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.payloads.Payload;
import mindustry.world.blocks.payloads.PayloadBlock;
import mindustry.world.blocks.payloads.UnitPayload;
import mindustry.world.blocks.production.HeatCrafter;
import mindustry.world.consumers.Consume;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.draw.DrawBlock;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

public class MultiCrafter
extends HeatCrafter {
    public Seq<Recipe> recipes = new Seq();
    public float updateEffectChance = 0.05f;
    public Effect updateEffect = Fx.none;
    public boolean showWarmup = false;
    public boolean hasPayloads = false;
    public int payloadCapacity = 1;
    public float payloadSpeed = 0.7f;
    public float payloadRotateSpeed = 5.0f;
    public Recipe[] recipe;
    protected boolean hasItemOutput = false;
    protected boolean hasLiquidOutput = false;

    public MultiCrafter(String name) {
        super(name);
        heatRequirement = 0.0f;
        configurable = true;
        hasItems = true;
        hasLiquids = true;
        saveConfig = true;
        update = true;
        solid = true;
        group = BlockGroup.none;
        buildType = () -> new MultiCrafterBuild();
        maxEfficiency = 1.0f;
        acceptsPayload = true;
        outputsPayload = true;
        rotate = false;
        drawArrow = false;
        config(Integer.class, (build, value) -> {
            int newVal = Mathf.clamp((int)value, (int)0, (int)recipes.size);
            if (build.selectRecipe != newVal) {
                build.selectRecipe = newVal;
                build.progress = 0.0f;
            }
        });
        config(UnitCommand.class, (build, cmd) -> {
            build.command = cmd;
        });
    }

    @Nullable
    public static Effect findEffectByPath(String path) {
        if (path == null || path.isEmpty()) {
            return Fx.none;
        }
        try {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0) {
                return Fx.none;
            }
            String className = path.substring(0, lastDot);
            String fieldName = path.substring(lastDot + 1);
            Class<?> clazz = Class.forName(className);
            return (Effect)clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            return Fx.none;
        }
    }

    @Nullable
    public static Sound findSoundByPath(String path) {
        if (path == null || path.isEmpty()) {
            return Sounds.none;
        }
        try {
            int lastDot = path.lastIndexOf('.');
            if (lastDot < 0) {
                return Sounds.none;
            }
            String className = path.substring(0, lastDot);
            String fieldName = path.substring(lastDot + 1);
            Class<?> clazz = Class.forName(className);
            return (Sound)clazz.getField(fieldName).get(null);
        } catch (Exception e) {
            return Sounds.none;
        }
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat, float craftTime) {
        recipes.add((Object)new Recipe(inputItems, inputLiquids, inputPower, inputHeat, outputItems, outputLiquids, outputPower, outputHeat, craftTime));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float craftTime) {
        addRecipe(inputItems, inputLiquids, inputPower, 0.0f, outputItems, outputLiquids, outputPower, 0.0f, craftTime);
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat, float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads, @Nullable DrawBlock visual) {
        recipes.add((Object)new Recipe(inputItems, inputLiquids, inputPower, inputHeat, outputItems, outputLiquids, outputPower, outputHeat, craftTime, inputPayloads, outputPayloads, visual));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads) {
        addRecipe(inputItems, inputLiquids, inputPower, 0.0f, outputItems, outputLiquids, outputPower, 0.0f, craftTime, inputPayloads, outputPayloads);
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat, float craftTime, PayloadStack[] inputPayloads, PayloadStack[] outputPayloads) {
        recipes.add((Object)new Recipe(inputItems, inputLiquids, inputPower, inputHeat, outputItems, outputLiquids, outputPower, outputHeat, craftTime, inputPayloads, outputPayloads, null));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat, float craftTime, @Nullable DrawBlock visual) {
        recipes.add((Object)new Recipe(inputItems, inputLiquids, inputPower, inputHeat, outputItems, outputLiquids, outputPower, outputHeat, craftTime, visual));
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float craftTime, @Nullable DrawBlock visual) {
        addRecipe(inputItems, inputLiquids, inputPower, 0.0f, outputItems, outputLiquids, outputPower, 0.0f, craftTime, visual);
    }

    public void addRecipe(ItemStack[] inputItems, LiquidStack[] inputLiquids, float inputPower, float inputHeat, PayloadStack[] inputPayloads, ItemStack[] outputItems, LiquidStack[] outputLiquids, float outputPower, float outputHeat, PayloadStack[] outputPayloads, float craftTime, float maxEfficiency, DrawBlock visual, String craftEffectPath, String updateEffectPath, String switchEffectPath, float updateEffectChance, String craftSoundPath, String updateSoundPath) {
        recipes.add((Object)new Recipe(inputItems, inputLiquids, inputPower, inputHeat, inputPayloads, outputItems, outputLiquids, outputPower, outputHeat, outputPayloads, craftTime, maxEfficiency, visual, craftEffectPath, updateEffectPath, switchEffectPath, updateEffectChance, craftSoundPath, updateSoundPath));
    }

    public void init() {
        boolean hasAnyPower;
        if (recipe != null && recipe.length > 0) {
            for (Recipe r2 : recipe) {
                r2.ensureArrays();
                if (r2.inputPayloads == null) {
                    r2.inputPayloads = new String[0];
                }
                if (r2.outputPayloads == null) {
                    r2.outputPayloads = new String[0];
                }
                r2.cachedInputPayloads = parsePayloadStacks(r2.inputPayloads);
                r2.cachedOutputPayloads = parsePayloadStacks(r2.outputPayloads);
                recipes.add((Object)r2);
            }
            recipe = null;
        }
        for (Recipe rec : recipes) {
            rec.ensureArrays();
            if (rec.cachedInputPayloads == null && rec.inputPayloads != null) {
                rec.cachedInputPayloads = parsePayloadStacks(rec.inputPayloads);
            }
            if (rec.cachedOutputPayloads == null && rec.outputPayloads != null) {
                rec.cachedOutputPayloads = parsePayloadStacks(rec.outputPayloads);
            }
            if (rec.cachedInputPayloads == null) {
                rec.cachedInputPayloads = new PayloadStack[0];
            }
            if (rec.cachedOutputPayloads == null) {
                rec.cachedOutputPayloads = new PayloadStack[0];
            }
            if (rec.cachedInputPayloads.length > 0 || rec.cachedOutputPayloads.length > 0) {
                hasPayloads = true;
            }
            rec.cachedCraftEffect = MultiCrafter.findEffectByPath(rec.craftEffect);
            rec.cachedUpdateEffect = MultiCrafter.findEffectByPath(rec.updateEffect);
            rec.cachedSwitchEffect = MultiCrafter.findEffectByPath(rec.switchEffect);
            rec.cachedCraftSound = MultiCrafter.findSoundByPath(rec.craftSound);
            rec.cachedUpdateSound = MultiCrafter.findSoundByPath(rec.updateSound);
        }
        ObjectSet allOutputItems = new ObjectSet();
        ObjectSet allOutputLiquids = new ObjectSet();
        for (Object rec : recipes) {
            for (ItemStack s : ((Recipe)rec).outputItems) {
                if (s == null || s.item == null) continue;
                allOutputItems.add((Object)s.item);
            }
            for (ItemStack s : ((Recipe)rec).outputLiquids) {
                if (s == null || s.liquid == null) continue;
                allOutputLiquids.add((Object)s.liquid);
            }
        }
        if (allOutputItems.size > 0) {
            outputItems = new ItemStack[allOutputItems.size];
            int i = 0;
            for (Item item : allOutputItems) {
                outputItems[i++] = new ItemStack(item, 1);
            }
        }
        if (allOutputLiquids.size > 0) {
            outputLiquids = new LiquidStack[allOutputLiquids.size];
            int i = 0;
            for (Liquid liq : allOutputLiquids) {
                outputLiquids[i++] = new LiquidStack(liq, 0.1f);
            }
        }
        hasItemOutput = outputItems != null;
        hasLiquidOutput = outputLiquids != null;
        boolean hasPowerInput = recipes.contains(r -> r.inputPower > 0.0f);
        if (hasPowerInput) {
            consume((Consume)new ConsumePower(0.0f, 0.0f, false){

                public float requestedPower(Building entity) {
                    Recipe rec;
                    if (entity instanceof MultiCrafterBuild build && (rec = build.getCurrentRecipe()) != null && rec.inputPower > 0.0f) {
                        return rec.inputPower;
                    }
                    return 0.0f;
                }

                public float efficiency(Building build) {
                    if (build instanceof MultiCrafterBuild) {
                        MultiCrafterBuild multi = (MultiCrafterBuild)build;
                        Recipe rec = multi.getCurrentRecipe();
                        if (rec != null && rec.inputPower > 0.0f) {
                            return build.power != null ? build.power.status : 0.0f;
                        }
                        return 1.0f;
                    }
                    return super.efficiency(build);
                }
            });
        }
        for (Object r3 : recipes) {
            if (((Recipe)r3).visual == null) continue;
            ((Recipe)r3).visual.load((Block)this);
        }
        boolean hasUnitOutput = recipes.contains(r -> {
            for (PayloadStack stack : r.cachedOutputPayloads) {
                if (!(stack.item instanceof UnitType)) continue;
                return true;
            }
            return false;
        });
        if (hasUnitOutput) {
            rotate = true;
            drawArrow = true;
            commandable = true;
        }
        for (Recipe rec : recipes) {
            for (PayloadStack stack : rec.cachedInputPayloads) {
                if (MultiCrafterBuild.allPayloadTypes.contains((Object)stack.item)) continue;
                MultiCrafterBuild.allPayloadTypes.add((Object)stack.item);
            }
            for (PayloadStack stack : rec.cachedOutputPayloads) {
                if (MultiCrafterBuild.allPayloadTypes.contains((Object)stack.item)) continue;
                MultiCrafterBuild.allPayloadTypes.add((Object)stack.item);
            }
        }
        boolean hasPowerOutput = recipes.contains(r -> r.outputPower > 0.0f);
        if (hasPowerOutput) {
            outputsPower = true;
        }
        if (hasAnyPower = recipes.contains(r -> r.inputPower > 0.0f || r.outputPower > 0.0f)) {
            hasPower = true;
        }
        for (int i = 0; i < recipes.size; ++i) {
            boolean forced;
            final Recipe rec = (Recipe)recipes.get(i);
            boolean bl = forced = rec.alwaysUnlocked != null;
            final boolean hasUnlockConditions = forced ? rec.alwaysUnlocked == false : rec.parent != null || rec.objectives.any();
            String blockName = name + "-recipe-" + i;
            Block existing = (Block)Vars.content.getByName(ContentType.block, blockName);
            if (existing != null) {
                rec.unlockBlock = existing;
                continue;
            }
            Block block = new Block(blockName){
                {
                    super(arg0);
                    placeablePlayer = false;
                    buildVisibility = BuildVisibility.sandboxOnly;
                    requirements = ItemStack.empty;
                    alwaysUnlocked = !hasUnlockConditions;
                    databaseCategory = rec.databaseCategory != null ? rec.databaseCategory : "recipe";
                    databaseTag = rec.databaseTag != null ? rec.databaseTag : "default";
                }

                public boolean isHidden() {
                    return false;
                }
            };
            block.minfo.mod = minfo.mod;
            block.localizedName = localizedName + " Recipe " + (i + 1);
            rec.unlockBlock = block;
        }
        super.init();
        outputsLiquid = hasLiquidOutput;
        if (outputsLiquid && (liquidOutputDirections == null || liquidOutputDirections.length == 0)) {
            liquidOutputDirections = new int[]{-1};
        }
        consumesPower = consPower != null;
    }

    public void load() {
        super.load();
        for (Recipe rec : recipes) {
            if (rec.unlockBlock == null) continue;
            rec.unlockBlock.fullIcon = fullIcon;
            rec.unlockBlock.uiIcon = fullIcon;
        }
        TechTree.TechNode factoryNode = techNode;
        for (int i = 0; i < recipes.size; ++i) {
            boolean hasUnlockConditions;
            boolean forced;
            Recipe rec = (Recipe)recipes.get(i);
            boolean bl = forced = rec.alwaysUnlocked != null;
            if (forced) {
                hasUnlockConditions = rec.alwaysUnlocked == false;
            } else {
                boolean bl2 = hasUnlockConditions = rec.parent != null || rec.objectives.any();
            }
            if (rec.unlockBlock == null || !hasUnlockConditions || rec.unlockBlock.techNode != null) continue;
            TechTree.TechNode node = new TechTree.TechNode(null, (UnlockableContent)rec.unlockBlock, ItemStack.empty);
            node.objectives.addAll(rec.objectives);
            if (rec.objectives.isEmpty() && rec.parent == null && forced && !rec.alwaysUnlocked.booleanValue()) {
                ObjectSet allContent = new ObjectSet();
                for (ItemStack stack : rec.inputItems) {
                    allContent.add((Object)stack.item);
                }
                for (ItemStack stack : rec.outputItems) {
                    allContent.add((Object)stack.item);
                }
                for (ItemStack stack : rec.inputLiquids) {
                    allContent.add((Object)stack.liquid);
                }
                for (ItemStack stack : rec.outputLiquids) {
                    allContent.add((Object)stack.liquid);
                }
                if (rec.cachedInputPayloads != null) {
                    for (ItemStack stack : rec.cachedInputPayloads) {
                        if (stack.item == null) continue;
                        allContent.add((Object)stack.item);
                    }
                }
                if (rec.cachedOutputPayloads != null) {
                    for (ItemStack stack : rec.cachedOutputPayloads) {
                        if (stack.item == null) continue;
                        allContent.add((Object)stack.item);
                    }
                }
                for (UnlockableContent c : allContent) {
                    node.objectives.add((Object)new Objectives.Research(c));
                }
            }
            if (rec.parent != null) {
                TechTree.TechNode parentNode = (TechTree.TechNode)TechTree.all.find(t -> t.content.name.equals(rec.parent) || t.content.name.equals((String)(minfo.mod != null ? minfo.mod.name + "-" : "") + rec.parent));
                if (parentNode != null) {
                    parentNode.children.add((Object)node);
                    node.parent = parentNode;
                    node.depth = parentNode.depth + 1;
                    node.planet = parentNode.planet;
                } else if (factoryNode != null) {
                    factoryNode.children.add((Object)node);
                    node.parent = factoryNode;
                    node.depth = factoryNode.depth + 1;
                    node.planet = factoryNode.planet;
                }
            } else if (factoryNode != null) {
                factoryNode.children.add((Object)node);
                node.parent = factoryNode;
                node.depth = factoryNode.depth + 1;
                node.planet = factoryNode.planet;
            }
            TechTree.all.add((Object)node);
        }
    }

    private PayloadStack[] parsePayloadStacks(String[] strs) {
        if (strs == null || strs.length == 0) {
            return new PayloadStack[0];
        }
        Seq list = new Seq();
        for (String s : strs) {
            String[] parts;
            if (s == null || s.isEmpty() || (parts = s.split("/")).length != 2) continue;
            UnlockableContent content = (UnlockableContent)Vars.content.getByName(ContentType.block, parts[0]);
            if (content == null) {
                content = (UnlockableContent)Vars.content.getByName(ContentType.unit, parts[0]);
            }
            if (content == null) continue;
            try {
                int amount = Integer.parseInt(parts[1]);
                if (amount <= 0) continue;
                list.add((Object)new PayloadStack(content, amount));
            } catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        return (PayloadStack[])list.toArray(PayloadStack.class);
    }

    public boolean outputsItems() {
        return hasItemOutput;
    }

    public void setStats() {
        stats.timePeriod = craftTime;
        stats.add(Stat.size, "@x@", new Object[]{size, size});
        if (synthetic()) {
            stats.add(Stat.health, (float)health, StatUnit.none);
            if (armor > 0.0f) {
                stats.add(Stat.armor, armor, StatUnit.none);
            }
        }
        if (canBeBuilt() && requirements.length > 0) {
            stats.add(Stat.buildTime, buildTime / 60.0f, StatUnit.seconds);
            stats.add(Stat.buildCost, StatValues.items((boolean)false, (ItemStack[])requirements));
        }
        for (Consume c : consumers) {
            c.display(stats);
        }
        if (hasLiquids) {
            stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        }
        if (hasItems && itemCapacity > 0) {
            stats.add(Stat.itemCapacity, (float)itemCapacity, StatUnit.items);
        }
        stats.add(Stat.output, table -> {
            table.clearChildren();
            table.left();
            int i = 0;
            while (i < recipes.size) {
                Recipe rec = (Recipe)recipes.get(i);
                rec.ensureArrays();
                int idx = i++;
                table.table(Styles.grayPanel, t -> {
                    boolean hasOutput;
                    boolean hasInput;
                    boolean locked;
                    t.left().defaults().left().padLeft(4.0f);
                    Block unlock = rec.unlockBlock;
                    boolean banned = unlock != null && unlock.isBanned();
                    boolean bl = locked = unlock != null && !unlock.unlockedNow();
                    if (banned) {
                        t.align(1);
                        t.image((Drawable)Icon.cancel).color(Pal.remove).size(40.0f).pad(8.0f).center();
                        return;
                    }
                    if (locked) {
                        t.align(1);
                        t.image((Drawable)Icon.lock).color(Pal.darkerGray).size(40.0f).pad(8.0f).center();
                        return;
                    }
                    String title = "[accent]Recipe " + (idx + 1) + "[]";
                    t.add((CharSequence)title).padTop(4.0f).padBottom(4.0f);
                    t.row();
                    boolean bl2 = hasInput = rec.inputItems.length > 0 || rec.inputLiquids.length > 0 || rec.inputPower > 0.0f || rec.inputHeat > 0.0f || rec.cachedInputPayloads.length > 0;
                    if (hasInput) {
                        t.add((CharSequence)("[lightgray]" + Core.bundle.get("stat.input") + ":[]")).padRight(8.0f);
                        for (ItemStack itemStack : rec.inputItems) {
                            t.add((Element)StatValues.displayItem((Item)itemStack.item, (int)itemStack.amount, (float)rec.craftTime, (boolean)true)).padRight(8.0f);
                        }
                        for (ItemStack itemStack : rec.inputLiquids) {
                            t.add((Element)StatValues.displayLiquid((Liquid)itemStack.liquid, (float)(itemStack.amount * 60.0f), (boolean)true)).padRight(8.0f);
                        }
                        for (ItemStack itemStack : rec.cachedInputPayloads) {
                            t.table(arg_0 -> MultiCrafter.lambda$setStats$8((PayloadStack)itemStack, rec, arg_0)).padRight(8.0f);
                        }
                        if (rec.inputPower > 0.0f) {
                            t.table(p -> StatValues.number((float)(rec.inputPower * 60.0f), (StatUnit)StatUnit.powerSecond).display(p)).padRight(8.0f);
                        }
                        if (rec.inputHeat > 0.0f) {
                            t.table(h -> StatValues.number((float)rec.inputHeat, (StatUnit)StatUnit.heatUnits).display(h)).padRight(8.0f);
                        }
                        t.row();
                    }
                    boolean bl3 = hasOutput = rec.outputItems.length > 0 || rec.outputLiquids.length > 0 || rec.outputHeat > 0.0f || rec.outputPower > 0.0f || rec.cachedOutputPayloads.length > 0 || rec.randomResults != null;
                    if (hasOutput) {
                        void var12_27;
                        void var12_25;
                        void var12_23;
                        t.add((CharSequence)("[lightgray]" + Core.bundle.get("stat.output") + ":[]")).padRight(8.0f);
                        ItemStack[] itemStackArray = rec.outputItems;
                        int n = itemStackArray.length;
                        boolean bl4 = false;
                        while (var12_23 < n) {
                            ItemStack itemStack = itemStackArray[var12_23];
                            t.add((Element)StatValues.displayItem((Item)itemStack.item, (int)itemStack.amount, (float)rec.craftTime, (boolean)true)).padRight(8.0f);
                            ++var12_23;
                        }
                        itemStackArray = rec.outputLiquids;
                        n = itemStackArray.length;
                        boolean bl5 = false;
                        while (var12_25 < n) {
                            ItemStack itemStack = itemStackArray[var12_25];
                            t.add((Element)StatValues.displayLiquid((Liquid)itemStack.liquid, (float)(itemStack.amount * 60.0f), (boolean)true)).padRight(8.0f);
                            ++var12_25;
                        }
                        itemStackArray = rec.cachedOutputPayloads;
                        n = itemStackArray.length;
                        boolean bl6 = false;
                        while (var12_27 < n) {
                            ItemStack itemStack = itemStackArray[var12_27];
                            t.table(arg_0 -> MultiCrafter.lambda$setStats$11((PayloadStack)itemStack, rec, arg_0)).padRight(8.0f);
                            ++var12_27;
                        }
                        if (rec.outputPower > 0.0f) {
                            t.table(p -> StatValues.number((float)(rec.outputPower * 60.0f), (StatUnit)StatUnit.powerSecond).display(p)).padRight(8.0f);
                        }
                        if (rec.outputHeat > 0.0f) {
                            t.table(h -> StatValues.number((float)rec.outputHeat, (StatUnit)StatUnit.heatUnits).display(h)).padRight(8.0f);
                        }
                        if (rec.randomResults != null) {
                            void var13_38;
                            void var13_36;
                            t.row();
                            float sum = 0.0f;
                            ItemStack[] itemStackArray2 = rec.randomResults;
                            int n2 = itemStackArray2.length;
                            boolean bl7 = false;
                            while (var13_36 < n2) {
                                ItemStack s = itemStackArray2[var13_36];
                                sum += (float)s.amount;
                                ++var13_36;
                            }
                            itemStackArray2 = rec.randomResults;
                            int n3 = itemStackArray2.length;
                            boolean bl8 = false;
                            while (var13_38 < n3) {
                                ItemStack stack = itemStackArray2[var13_38];
                                int percent = (int)((float)stack.amount / sum * 100.0f);
                                t.add((Element)StatValues.displayItemPercent((Item)stack.item, (int)percent, (boolean)true)).padRight(5.0f);
                                ++var13_38;
                            }
                        }
                    }
                    if (rec.attribute != null) {
                        t.row();
                        Table affTable = new Table();
                        StatValues.blocks((Attribute)rec.attribute, (boolean)rec.floating, (float)(rec.boostScale * (float)size * (float)size), (!rec.displayEfficiency ? 1 : 0) != 0).display(affTable);
                        t.add((Element)affTable).padLeft(4.0f);
                    }
                    t.add((CharSequence)("[lightgray]" + Core.bundle.get("stat.productiontime") + ":[] " + Strings.autoFixed((float)(rec.craftTime / 60.0f), (int)3) + " " + Core.bundle.get("unit.seconds"))).padTop(4.0f);
                    if (rec.inputHeat > 0.0f) {
                        t.add((CharSequence)("  [lightgray]\u6700\u5927\u6548\u7387:[] " + Strings.autoFixed((float)(rec.maxEfficiency * 100.0f), (int)0) + "%"));
                    }
                }).growX().pad(5.0f).row();
            }
        });
    }

    public void setBars() {
        boolean hasUnitOutput;
        super.setBars();
        removeBar("items");
        removeBar("heat");
        removeBar("liquid");
        if (showWarmup) {
            addBar("warmup", entity -> new Bar(() -> Core.bundle.get("bar.warmup"), () -> Pal.accent, () -> entity.warmup));
        }
        ObjectSet allLiquids = new ObjectSet();
        for (Recipe rec : recipes) {
            for (LiquidStack stack : rec.inputLiquids) {
                allLiquids.add((Object)stack.liquid);
            }
            for (LiquidStack stack : rec.outputLiquids) {
                allLiquids.add((Object)stack.liquid);
            }
        }
        if (allLiquids.size > 0) {
            for (Object liquid : allLiquids) {
                addBar("liquid-" + ((Liquid)liquid).name, arg_0 -> lambda$setBars$22((Liquid)liquid, arg_0));
            }
        }
        if (recipes.contains(r -> r.outputPower > 0.0f)) {
            addBar("power-output", entity -> new Bar(() -> Core.bundle.format("bar.poweroutput", new Object[]{Float.valueOf(entity.getPowerProduction() * 60.0f * entity.timeScale())}), () -> Pal.powerBar, () -> entity.getPowerStat()));
        }
        if (recipes.contains(r -> r.inputHeat > 0.0f)) {
            addBar("heat-input", entity -> {
                Recipe rec = entity.getCurrentRecipe();
                float need = rec != null && rec.inputHeat > 0.0f ? rec.inputHeat : (recipes.find(r2 -> r2.inputHeat > 0.0f) != null ? ((Recipe)recipes.find((Boolf)(Boolf)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$setBars$30(MultiCraft.MultiCrafter$Recipe ), (LMultiCraft/MultiCrafter$Recipe;)Z)())).inputHeat : 1.0f);
                return new Bar(() -> "\u70ed\u91cf\u8f93\u5165\uff1a" + (int)(entity.heatInput + 0.01f) + " (" + (int)(entity.heatEfficiencyScale() * 100.0f + 0.01f) + "%)", () -> Pal.lightOrange, () -> need > 0.0f ? Mathf.clamp((float)(entity.heatInput / need)) : 0.0f);
            });
        }
        if (recipes.contains(r -> r.outputHeat > 0.0f)) {
            addBar("heat-output", entity -> {
                Recipe rec = entity.getCurrentRecipe();
                float max = rec != null && rec.outputHeat > 0.0f ? rec.outputHeat : (recipes.find(r2 -> r2.outputHeat > 0.0f) != null ? ((Recipe)recipes.find((Boolf)(Boolf)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Z, lambda$setBars$37(MultiCraft.MultiCrafter$Recipe ), (LMultiCraft/MultiCrafter$Recipe;)Z)())).outputHeat : 1.0f);
                return new Bar(() -> "\u70ed\u91cf\u8f93\u51fa\uff1a" + (int)(entity.heatOutput + 0.01f) + " (" + (int)(entity.heatOutput / max * 100.0f + 0.01f) + "%)", () -> Pal.lightOrange, () -> max > 0.0f ? Mathf.clamp((float)(entity.heatOutput / max)) : 0.0f);
            });
        }
        addBar("efficiency", entity -> {
            Recipe rec = entity.getCurrentRecipe();
            if (rec == null || rec.attribute == null || !rec.displayEfficiency) {
                return null;
            }
            return new Bar(() -> Core.bundle.format("bar.efficiency", new Object[]{(int)(entity.efficiencyMultiplier() * 100.0f)}), () -> Pal.lightOrange, entity::efficiencyMultiplier);
        });
        ObjectSet allUnitOutputs = new ObjectSet();
        for (Recipe rec : recipes) {
            for (PayloadStack stack : rec.cachedOutputPayloads) {
                if (!(stack.item instanceof UnitType)) continue;
                allUnitOutputs.add((Object)((UnitType)stack.item));
            }
        }
        if (!allUnitOutputs.isEmpty()) {
            addBar("units", e -> {
                UnitType type = e.getDisplayedUnitType();
                return new Bar(() -> type == null ? "[lightgray]\ue815" : Core.bundle.format("bar.unitcap", new Object[]{Fonts.getUnicodeStr((String)type.name), e.team.data().countType(type), type.useUnitCap ? Units.getStringCap((Team)e.team) : "\u221e"}), () -> Pal.power, () -> type == null ? 0.0f : (type.useUnitCap ? (float)e.team.data().countType(type) / (float)Units.getCap((Team)e.team) : 1.0f));
            });
        }
        if (hasUnitOutput = recipes.contains(r -> {
            for (PayloadStack stack : r.cachedOutputPayloads) {
                if (!(stack.item instanceof UnitType)) continue;
                return true;
            }
            return false;
        })) {
            addBar("progress", e -> new Bar("bar.progress", Pal.ammo, () -> {
                Recipe rec = e.getCurrentRecipe();
                return rec != null && rec.cachedOutputPayloads != null && Structs.contains((Object[])rec.cachedOutputPayloads, s -> s.item instanceof UnitType) ? e.progress : 0.0f;
            }));
        }
    }

    public boolean rotatedOutput(int fromX, int fromY, Tile destination) {
        if (!(destination.build instanceof Conduit.ConduitBuild)) {
            return false;
        }
        Building crafter = Vars.world.build(fromX, fromY);
        if (crafter instanceof MultiCrafterBuild) {
            MultiCrafterBuild build = (MultiCrafterBuild)crafter;
            Recipe rec = build.getCurrentRecipe();
            if (rec == null || rec.outputLiquids == null || rec.outputLiquids.length == 0) {
                return false;
            }
            int relative = Mathf.mod((int)(crafter.relativeTo(destination) - crafter.rotation), (int)4);
            for (int dir : rec.liquidOutputDirections) {
                if (dir != -1 && dir != relative) continue;
                return false;
            }
        }
        return true;
    }

    public boolean configSenseable() {
        return configurations.containsKey(Integer.class) || super.configSenseable();
    }

    public boolean canPlaceOn(Tile tile, Team team, int rotation) {
        for (Recipe rec : recipes) {
            float sum;
            float eff;
            if (rec.attribute == null || !(rec.minEfficiency >= 0.0f) || !((eff = rec.baseEfficiency + Math.min(rec.maxBoost, rec.boostScale * (sum = sumAttribute(rec.attribute, tile.x, tile.y))) + rec.attribute.env()) < rec.minEfficiency)) continue;
            return false;
        }
        return true;
    }

    private static /* synthetic */ boolean lambda$setBars$37(Recipe r2) {
        return r2.outputHeat > 0.0f;
    }

    private static /* synthetic */ boolean lambda$setBars$30(Recipe r2) {
        return r2.inputHeat > 0.0f;
    }

    private /* synthetic */ Bar lambda$setBars$22(Liquid liquid, MultiCrafterBuild entity) {
        return new Bar(() -> liquid.localizedName, () -> ((Liquid)liquid).barColor(), () -> entity.liquids.get(liquid) / liquidCapacity);
    }

    private static /* synthetic */ void lambda$setStats$11(PayloadStack s, Recipe rec, Table pl) {
        pl.image(s.item.uiIcon).size(32.0f).padRight(2.0f);
        pl.add((CharSequence)s.item.localizedName).color(Color.lightGray).padRight(4.0f);
        pl.add((CharSequence)(Strings.autoFixed((float)((float)s.amount / (rec.craftTime / 60.0f)), (int)1) + StatUnit.perSecond.localized())).color(Color.lightGray);
    }

    private static /* synthetic */ void lambda$setStats$8(PayloadStack s, Recipe rec, Table pl) {
        pl.image(s.item.uiIcon).size(32.0f).padRight(2.0f);
        pl.add((CharSequence)s.item.localizedName).color(Color.lightGray).padRight(4.0f);
        pl.add((CharSequence)(Strings.autoFixed((float)((float)s.amount / (rec.craftTime / 60.0f)), (int)1) + StatUnit.perSecond.localized())).color(Color.lightGray);
    }

    public static class Recipe {
        public ItemStack[] inputItems = new ItemStack[0];
        public ItemStack[] outputItems = new ItemStack[0];
        public LiquidStack[] inputLiquids = new LiquidStack[0];
        public LiquidStack[] outputLiquids = new LiquidStack[0];
        public float inputPower;
        public float inputHeat;
        public float outputPower;
        public float outputHeat;
        public float craftTime = 60.0f;
        public int payloadOutputDirection = -1;
        public String[] inputPayloads = new String[0];
        public String[] outputPayloads = new String[0];
        @Nullable
        public DrawBlock visual;
        public float maxEfficiency = 4.0f;
        public float updateEffectChance = 0.04f;
        public String craftEffect = "";
        public String updateEffect = "";
        public String switchEffect = "";
        public String craftSound = "";
        public String updateSound = "";
        public int[] liquidOutputDirections = new int[]{-1};
        @Nullable
        public transient Effect cachedCraftEffect;
        @Nullable
        public transient Effect cachedUpdateEffect;
        @Nullable
        public transient Effect cachedSwitchEffect;
        @Nullable
        public transient Sound cachedCraftSound;
        @Nullable
        public transient Sound cachedUpdateSound;
        public transient PayloadStack[] cachedInputPayloads = new PayloadStack[0];
        public transient PayloadStack[] cachedOutputPayloads = new PayloadStack[0];
        public Boolean dumpExtraLiquid = true;
        public Boolean ignoreLiquidFullness = false;
        public int[] heatOutputDirections = new int[]{-1};
        @Nullable
        public Attribute attribute;
        public float baseEfficiency = 1.0f;
        public float boostScale = 1.0f;
        public float maxBoost = 1.0f;
        public float minEfficiency = -1.0f;
        public boolean scaleLiquidConsumption = false;
        public boolean displayEfficiency = true;
        public boolean floating = false;
        @Nullable
        public String parent;
        public Seq<Objectives.Objective> objectives = new Seq();
        @Nullable
        public String databaseCategory;
        @Nullable
        public String databaseTag;
        @Nullable
        public transient Block unlockBlock;
        @Nullable
        public Boolean alwaysUnlocked;
        @Nullable
        public ItemStack[] randomResults;

        public Recipe() {
            ensureArrays();
        }

        public void ensureArrays() {
            if (inputItems == null) {
                inputItems = new ItemStack[0];
            }
            if (inputLiquids == null) {
                inputLiquids = new LiquidStack[0];
            }
            if (outputItems == null) {
                outputItems = new ItemStack[0];
            }
            if (outputLiquids == null) {
                outputLiquids = new LiquidStack[0];
            }
            if (inputPayloads == null) {
                inputPayloads = new String[0];
            }
            if (outputPayloads == null) {
                outputPayloads = new String[0];
            }
            if (cachedInputPayloads == null) {
                cachedInputPayloads = new PayloadStack[0];
            }
            if (cachedOutputPayloads == null) {
                cachedOutputPayloads = new PayloadStack[0];
            }
            if (liquidOutputDirections == null) {
                liquidOutputDirections = new int[]{-1};
            }
            if (payloadOutputDirection < -1 || payloadOutputDirection > 3) {
                payloadOutputDirection = -1;
            }
            if (heatOutputDirections == null) {
                heatOutputDirections = new int[]{-1};
            }
        }

        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, PayloadStack[] ipay, PayloadStack[] opay, @Nullable DrawBlock v) {
            inputItems = i;
            inputLiquids = il;
            inputPower = ip;
            inputHeat = ih;
            outputItems = o;
            outputLiquids = ol;
            outputPower = op;
            outputHeat = oh;
            craftTime = ct;
            cachedInputPayloads = ipay;
            cachedOutputPayloads = opay;
            visual = v;
            inputPayloads = new String[0];
            outputPayloads = new String[0];
            ensureArrays();
        }

        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct) {
            this(i, il, ip, ih, o, ol, op, oh, ct, new PayloadStack[0], new PayloadStack[0], null);
        }

        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, @Nullable DrawBlock v) {
            this(i, il, ip, ih, o, ol, op, oh, ct, new PayloadStack[0], new PayloadStack[0], v);
        }

        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, ItemStack[] o, LiquidStack[] ol, float op, float oh, float ct, PayloadStack[] ipay, PayloadStack[] opay) {
            this(i, il, ip, ih, o, ol, op, oh, ct, ipay, opay, null);
        }

        public Recipe(ItemStack[] i, LiquidStack[] il, float ip, float ih, PayloadStack[] ipay, ItemStack[] o, LiquidStack[] ol, float op, float oh, PayloadStack[] opay, float ct, float maxE, DrawBlock v, String ce, String ue, String se, float uec, String cs, String us) {
            inputItems = i;
            inputLiquids = il;
            inputPower = ip;
            inputHeat = ih;
            cachedInputPayloads = ipay;
            outputItems = o;
            outputLiquids = ol;
            outputPower = op;
            outputHeat = oh;
            cachedOutputPayloads = opay;
            craftTime = ct;
            maxEfficiency = maxE;
            visual = v;
            craftEffect = ce;
            updateEffect = ue;
            switchEffect = se;
            updateEffectChance = uec;
            craftSound = cs;
            updateSound = us;
            inputPayloads = new String[0];
            outputPayloads = new String[0];
            ensureArrays();
        }
    }

    public class MultiCrafterBuild
    extends HeatCrafter.HeatCrafterBuild
    implements HeatBlock {
        public ObjectSet<Item> outputItemsSet;
        public ObjectSet<Liquid> outputLiquidsSet;
        public int selectRecipe;
        public float heatInput;
        public float heatOutput;
        public float warmupRate;
        @Nullable
        public Payload payload;
        public Vec2 payVector;
        public float payRotation;
        public boolean carried;
        public IntMap<Integer> payloadCounts;
        @Nullable
        public Recipe currentVisualRecipe;
        public ObjectSet<UnlockableContent> outputPayloadsSet;
        @Nullable
        public Payload outputPayload;
        public Vec2 outPayVector;
        public float outPayRotation;
        public float time;
        public float speedScl;
        @Nullable
        public Vec2 commandPos;
        @Nullable
        public UnitCommand command;
        public static Seq<UnlockableContent> allPayloadTypes = new Seq();
        public float attrsum;
        public int seed;

        public MultiCrafterBuild() {
            super((HeatCrafter)MultiCrafter.this);
            outputItemsSet = new ObjectSet();
            outputLiquidsSet = new ObjectSet();
            selectRecipe = 0;
            warmupRate = 0.15f;
            payVector = new Vec2();
            payloadCounts = new IntMap();
            outputPayloadsSet = new ObjectSet();
            outPayVector = new Vec2();
        }

        private void updateAttrsum() {
            Recipe rec = getCurrentRecipe();
            attrsum = rec != null && rec.attribute != null ? MultiCrafter.sumAttribute(rec.attribute, tile.x, tile.y) : 0.0f;
        }

        public float efficiencyMultiplier() {
            Recipe rec = getCurrentRecipe();
            if (rec == null || rec.attribute == null) {
                return 1.0f;
            }
            return rec.baseEfficiency + Math.min(rec.maxBoost, rec.boostScale * attrsum) + rec.attribute.env();
        }

        public void created() {
            seed = Mathf.randomSeed((long)tile.pos(), (int)0, (int)0x7FFFFFFE);
        }

        public void placed() {
            super.placed();
            syncLiquidOutputs();
            updateAttrsum();
        }

        private void syncLiquidOutputs() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.outputLiquids.length > 0) {
                MultiCrafter.outputLiquids = rec.outputLiquids;
                MultiCrafter.liquidOutputDirections = rec.liquidOutputDirections;
            } else {
                MultiCrafter.outputLiquids = null;
                MultiCrafter.liquidOutputDirections = new int[]{-1};
            }
        }

        public float heatEfficiencyScale() {
            int idx;
            Recipe rec = getCurrentRecipe();
            if (rec == null && selectRecipe > 0 && (idx = selectRecipe - 1) < MultiCrafter.recipes.size) {
                rec = (Recipe)MultiCrafter.recipes.get(idx);
            }
            if (rec == null || rec.inputHeat <= 0.0f) {
                return heatInput > 0.001f ? 1.0f : 0.0f;
            }
            float req = rec.inputHeat;
            float over = Math.max(heatInput - req, 0.0f);
            return Math.min(Mathf.clamp((float)(heatInput / req)) + over / req * MultiCrafter.overheatScale, rec.maxEfficiency);
        }

        @Nullable
        public PayloadSeq getPayloads() {
            boolean has;
            boolean bl = has = payload != null || !payloadCounts.isEmpty() || outputPayload != null;
            if (!has) {
                return null;
            }
            PayloadSeq seq = new PayloadSeq();
            if (payload != null) {
                seq.add(payload.content(), 1);
            }
            for (IntMap.Entry e : payloadCounts.entries()) {
                Block c = Vars.content.block(e.key);
                if (c == null) {
                    c = Vars.content.unit(e.key);
                }
                if (c == null) continue;
                seq.add((UnlockableContent)c, ((Integer)e.value).intValue());
            }
            if (outputPayload != null) {
                seq.add(outputPayload.content(), 1);
            }
            return seq;
        }

        /*
         * Could not resolve type clashes
         */
        public void buildConfiguration(Table table) {
            table.clearChildren();
            TextButton autoBtn = new TextButton("\u81ea\u52a8", Styles.flatTogglet);
            autoBtn.setChecked(selectRecipe == 0);
            autoBtn.update(() -> autoBtn.setChecked(selectRecipe == 0));
            autoBtn.changed(() -> {
                if (selectRecipe != 0) {
                    configure(0);
                }
            });
            table.add((Element)autoBtn).height(40.0f).growX().pad(4.0f).row();
            table.image().color(Pal.gray).height(2.0f).growX().padBottom(4.0f).row();
            Table recipeList = new Table().left();
            for (int i = 0; i < MultiCrafter.recipes.size; ++i) {
                int recipeNum = i + 1;
                Recipe rec = (Recipe)MultiCrafter.recipes.get(i);
                rec.ensureArrays();
                Block unlock = rec.unlockBlock;
                if (unlock != null && unlock.isBanned()) continue;
                boolean locked = unlock != null && !unlock.unlockedNow();
                TextButton recipeBtn = new TextButton("", Styles.flatTogglet);
                recipeBtn.setChecked(selectRecipe == recipeNum);
                if (locked) {
                    recipeBtn.setDisabled(true);
                    recipeBtn.add((CharSequence)("[gray]\ue88d " + recipeNum)).growX().pad(2.0f);
                } else {
                    Table content = new Table().left();
                    content.add((CharSequence)("[accent]" + recipeNum + "[]")).padLeft(6.0f).padRight(8.0f).width(24.0f);
                    Table inputTable = new Table().left();
                    if (rec.inputItems.length > 0 || rec.inputLiquids.length > 0 || rec.cachedInputPayloads.length > 0 || rec.inputPower > 0.0f || rec.inputHeat > 0.0f) {
                        for (ItemStack s : rec.inputItems) {
                            inputTable.image(s.item.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        for (ItemStack s : rec.inputLiquids) {
                            inputTable.image(s.liquid.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        for (ItemStack s : rec.cachedInputPayloads) {
                            inputTable.image(s.item.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        if (rec.inputPower > 0.0f) {
                            inputTable.add((CharSequence)"[accent]\ue810[]").padRight(2.0f);
                        }
                        if (rec.inputHeat > 0.0f) {
                            inputTable.add((CharSequence)"[red]\ue83b[]").padRight(2.0f);
                        }
                    } else {
                        inputTable.add((CharSequence)"[darkGray]-[]");
                    }
                    content.add((Element)inputTable).padRight(8.0f);
                    content.add((CharSequence)"[accent]\ue803[]").padRight(8.0f).padLeft(4.0f);
                    Table outputTable = new Table().left();
                    if (rec.outputItems.length > 0 || rec.outputLiquids.length > 0 || rec.cachedOutputPayloads.length > 0 || rec.outputPower > 0.0f || rec.outputHeat > 0.0f || rec.randomResults != null) {
                        for (ItemStack s : rec.outputItems) {
                            outputTable.image(s.item.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        if (rec.randomResults != null) {
                            for (ItemStack stack : rec.randomResults) {
                                outputTable.image(stack.item.uiIcon).size(24.0f).padRight(2.0f);
                            }
                        }
                        for (ItemStack s : rec.outputLiquids) {
                            outputTable.image(s.liquid.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        for (ItemStack s : rec.cachedOutputPayloads) {
                            outputTable.image(s.item.uiIcon).size(24.0f).padRight(2.0f);
                        }
                        if (rec.outputPower > 0.0f) {
                            outputTable.add((CharSequence)"[accent]\ue810[]").padRight(2.0f);
                        }
                        if (rec.outputHeat > 0.0f) {
                            outputTable.add((CharSequence)"[red]\ue83b[]").padRight(2.0f);
                        }
                    }
                    content.add((Element)outputTable);
                    recipeBtn.add((Element)content).growX().pad(2.0f);
                }
                recipeBtn.changed(() -> configure(selectRecipe == recipeNum ? 0 : recipeNum));
                recipeBtn.update(() -> recipeBtn.setChecked(selectRecipe == recipeNum));
                recipeList.add((Element)recipeBtn).growX().height(44.0f).padBottom(3.0f).row();
            }
            ScrollPane pane = new ScrollPane((Element)recipeList, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            pane.setFadeScrollBars(false);
            pane.setClamp(true);
            pane.setCancelTouchFocus(true);
            pane.setFlickScroll(true);
            table.add((Element)pane).growX().maxHeight((float)Math.min(MultiCrafter.recipes.size, 3) * 48.0f).pad(4.0f);
            buildCommandUI(table);
        }

        private void buildCommandUI(Table parent) {
            if (!isCommandable()) {
                return;
            }
            parent.row();
            Table commands = new Table();
            commands.top().left();
            Runnable rebuildCommands = () -> {
                commands.clear();
                UnitType unitType = getCommandableUnitType();
                if (unitType != null) {
                    commands.background(Styles.black6);
                    ButtonGroup group = new ButtonGroup();
                    group.setMinCheckCount(0);
                    int cols = 3;
                    Seq list = unitType.commands;
                    commands.image(Tex.whiteui, Pal.gray).height(4.0f).growX().colspan(cols).row();
                    for (int i = 0; i < list.size; ++i) {
                        UnitCommand cmd = (UnitCommand)list.get(i);
                        ImageButton button = (ImageButton)commands.button((Drawable)cmd.getIcon(), Styles.clearNoneTogglei, 40.0f, () -> configure(cmd)).tooltip(cmd.localized()).group(group).get();
                        button.update(() -> button.setChecked(command == cmd || command == null && unitType.defaultCommand == cmd));
                        if (i % cols != cols - 1) continue;
                        commands.row();
                    }
                }
            };
            rebuildCommands.run();
            parent.add((Element)commands).fillX().left().padTop(4.0f);
        }

        public boolean isCommandable() {
            Recipe rec = currentVisualRecipe;
            if (rec != null && rec.cachedOutputPayloads != null) {
                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    UnlockableContent unlockableContent = stack.item;
                    if (!(unlockableContent instanceof UnitType)) continue;
                    UnitType ut = (UnitType)unlockableContent;
                    if (ut.commands.size <= 0) continue;
                    return true;
                }
            }
            return false;
        }

        public Vec2 getCommandPosition() {
            return commandPos;
        }

        public void onCommand(Vec2 target) {
            commandPos = target;
        }

        @Nullable
        private UnitType getCommandableUnitType() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.cachedOutputPayloads != null) {
                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    UnlockableContent unlockableContent = stack.item;
                    if (!(unlockableContent instanceof UnitType)) continue;
                    UnitType ut = (UnitType)unlockableContent;
                    if (ut.commands.size <= 0) continue;
                    return ut;
                }
            }
            return null;
        }

        public Object config() {
            return selectRecipe;
        }

        public void configured(Unit builder, Object value) {
            if (value instanceof Integer val && val >= 0 && val <= MultiCrafter.recipes.size) {
                if (selectRecipe != val) {
                    selectRecipe = val;
                    progress = 0.0f;
                    outputLiquidsSet.clear();
                    currentVisualRecipe = selectRecipe != 0 ? (Recipe)MultiCrafter.recipes.get(selectRecipe - 1) : null;
                    Recipe newRec = selectRecipe > 0 && selectRecipe - 1 < MultiCrafter.recipes.size ? (Recipe)MultiCrafter.recipes.get(selectRecipe - 1) : null;
                    Effect switchEff = newRec != null && newRec.cachedSwitchEffect != null ? newRec.cachedSwitchEffect : Fx.rotateBlock;
                    switchEff.at(x, y, (float)block.size);
                    outputPayloadsSet.clear();
                    if (outputPayload != null) {
                        if (!outputPayload.dump()) {
                            outputPayload = null;
                        }
                        outPayVector.setZero();
                    }
                    if (newRec != null) {
                        ObjectSet validInputs = new ObjectSet();
                        for (PayloadStack s : newRec.cachedInputPayloads) {
                            validInputs.add((Object)s.item);
                        }
                        Seq toRemove = new Seq();
                        for (IntMap.Entry e : payloadCounts.entries()) {
                            Block c = Vars.content.block(e.key);
                            if (c == null) {
                                c = Vars.content.unit(e.key);
                            }
                            if (c == null || validInputs.contains((Object)c)) continue;
                            toRemove.add((Object)c);
                        }
                        for (UnlockableContent c : toRemove) {
                            payloadCounts.remove((int)c.id);
                        }
                    } else {
                        payloadCounts.clear();
                    }
                    syncLiquidOutputs();
                    updateAttrsum();
                }
            } else if (value instanceof UnitCommand) {
                UnitCommand cmd;
                command = cmd = (UnitCommand)value;
            }
        }

        public double sense(LAccess sensor) {
            return sensor == LAccess.config ? (double)selectRecipe : super.sense(sensor);
        }

        public Object senseObject(LAccess sensor) {
            return sensor == LAccess.config ? Senseable.noSensed : super.senseObject(sensor);
        }

        public void control(LAccess type, double p1, double p2, double p3, double p4) {
            if (type == LAccess.config) {
                int val = (int)Math.round(p1);
                if (val >= 0 && val <= MultiCrafter.recipes.size) {
                    selectRecipe = val;
                    progress = 0.0f;
                    outputLiquidsSet.clear();
                    syncLiquidOutputs();
                    updateAttrsum();
                }
                return;
            }
            super.control(type, p1, p2, p3, p4);
        }

        @Nullable
        public Recipe getCurrentRecipe() {
            if (selectRecipe == 0) {
                for (Recipe rec : MultiCrafter.recipes) {
                    if (rec.unlockBlock != null && rec.unlockBlock.isBanned() || rec.unlockBlock != null && !rec.unlockBlock.unlockedNow() || rec.inputHeat > 0.0f && heatInput <= 0.0f || rec.inputPower > 0.0f && (power == null || power.status <= 0.0f) || !checkInput(rec) || !checkOutput(rec)) continue;
                    return rec;
                }
                return null;
            }
            int idx = selectRecipe - 1;
            if (idx >= 0 && idx < MultiCrafter.recipes.size) {
                Recipe rec = (Recipe)MultiCrafter.recipes.get(idx);
                if (rec.unlockBlock != null && rec.unlockBlock.isBanned()) {
                    return null;
                }
                if (rec.unlockBlock != null && !rec.unlockBlock.unlockedNow()) {
                    return null;
                }
                if (checkInput(rec) && checkOutput(rec)) {
                    return rec;
                }
            }
            return null;
        }

        private boolean checkInput(Recipe rec) {
            if (rec.inputPower > 0.0f && (power == null || power.status <= 0.0f)) {
                return false;
            }
            if (rec.inputHeat > 0.0f && heatInput <= 0.0f) {
                return false;
            }
            for (ItemStack itemStack : rec.inputItems) {
                if (items.get(itemStack.item) >= itemStack.amount) continue;
                return false;
            }
            for (ItemStack itemStack : rec.cachedInputPayloads) {
                if (getPayloadCount(itemStack.item) >= itemStack.amount) continue;
                return false;
            }
            for (ItemStack itemStack : rec.inputLiquids) {
                if (!(liquids.get(itemStack.liquid) <= 0.001f)) continue;
                return false;
            }
            return true;
        }

        private boolean checkOutput(Recipe rec) {
            for (ItemStack stack : rec.outputItems) {
                if (items.get(stack.item) + stack.amount <= MultiCrafter.itemCapacity) continue;
                return false;
            }
            if (rec.outputLiquids.length > 0) {
                boolean ignoreFull;
                boolean dumpExtra = rec.dumpExtraLiquid != null ? rec.dumpExtraLiquid : MultiCrafter.dumpExtraLiquid;
                boolean bl = ignoreFull = rec.ignoreLiquidFullness != null ? rec.ignoreLiquidFullness : MultiCrafter.ignoreLiquidFullness;
                if (!ignoreFull) {
                    boolean allFull = true;
                    LiquidStack[] liquidStackArray = rec.outputLiquids;
                    int n = liquidStackArray.length;
                    for (int i = 0; i < n; ++i) {
                        float netOutput;
                        LiquidStack output = liquidStackArray[i];
                        float inputAmount = 0.0f;
                        for (LiquidStack input : rec.inputLiquids) {
                            if (input.liquid != output.liquid) continue;
                            inputAmount = input.amount;
                            break;
                        }
                        if ((netOutput = output.amount - inputAmount) > 0.0f) {
                            if (liquids.get(output.liquid) < MultiCrafter.liquidCapacity - 0.001f) {
                                allFull = false;
                                break;
                            }
                            if (dumpExtra) continue;
                            return false;
                        }
                        allFull = false;
                    }
                    if (allFull) {
                        return false;
                    }
                }
            }
            if (rec.cachedOutputPayloads.length > 0) {
                boolean hasUnitOutput = false;
                int willProduceBlocks = 0;
                for (PayloadStack stack : rec.cachedOutputPayloads) {
                    UnlockableContent unlockableContent = stack.item;
                    if (unlockableContent instanceof UnitType) {
                        UnitType ut = (UnitType)unlockableContent;
                        hasUnitOutput = true;
                        continue;
                    }
                    willProduceBlocks += stack.amount;
                }
                if (hasUnitOutput && outputPayload != null) {
                    return false;
                }
                int currentBlocks = 0;
                for (IntMap.Entry e : payloadCounts.entries()) {
                    Block c = Vars.content.block(e.key);
                    if (c == null) {
                        c = Vars.content.unit(e.key);
                    }
                    if (c == null || !outputPayloadsSet.contains((Object)c)) continue;
                    currentBlocks += ((Integer)e.value).intValue();
                }
                if (currentBlocks + willProduceBlocks > MultiCrafter.payloadCapacity) {
                    return false;
                }
            }
            return true;
        }

        public int getPayloadCount(UnlockableContent content) {
            return (Integer)payloadCounts.get((int)content.id, (Object)0);
        }

        public void addPayload(UnlockableContent content, int amount) {
            payloadCounts.put((int)content.id, (Object)(getPayloadCount(content) + amount));
        }

        public void removePayload(UnlockableContent content, int amount) {
            int cur = getPayloadCount(content);
            if (cur <= amount) {
                payloadCounts.remove((int)content.id);
            } else {
                payloadCounts.put((int)content.id, (Object)(cur - amount));
            }
        }

        public float calculateHeat(float[] sideHeat) {
            return calculateHeat(sideHeat, null);
        }

        public float calculateHeat(float[] sideHeat, IntSet cameFrom) {
            Arrays.fill(sideHeat, 0.0f);
            if (cameFrom != null) {
                cameFrom.clear();
            }
            float heat = 0.0f;
            IntSet visited = new IntSet();
            visited.add(id());
            for (Building b : proximity) {
                HeatConductor.HeatConductorBuild hc;
                int dir;
                boolean split;
                HeatConductor c;
                if (b == null || b.team != team || !(b instanceof HeatBlock)) continue;
                HeatBlock heater = (HeatBlock)b;
                if (b == this || visited.contains(b.id)) continue;
                visited.add(b.id);
                Block block = b.block;
                boolean bl = block instanceof HeatConductor && null != (c = (HeatConductor)block) && c.splitHeat ? true : (split = false);
                if (b.block.rotate && (split || (relativeTo(b) + 2) % 4 != b.rotation) && (!split || relativeTo(b) == b.rotation)) continue;
                float diff = Math.min(Math.abs(b.x - x), Math.abs(b.y - y)) / 8.0f;
                int pts = Math.min((int)((float)block.size / 2.0f + (float)b.block.size / 2.0f - diff), Math.min(b.block.size, block.size));
                float add = heater.heat() / (float)b.block.size * (float)pts;
                if (split) {
                    add /= 3.0f;
                }
                if ((dir = Mathf.mod((int)relativeTo(b), (int)4)) >= 0) {
                    int n = dir;
                    sideHeat[n] = sideHeat[n] + add;
                    heat += add;
                }
                if (!(heater instanceof HeatConductor.HeatConductorBuild) || null == (hc = (HeatConductor.HeatConductorBuild)heater)) continue;
                hc.updateHeat();
            }
            return heat;
        }

        public boolean acceptPayload(Building source, Payload p) {
            if (!MultiCrafter.hasPayloads || payload != null) {
                return false;
            }
            for (Recipe rec : MultiCrafter.recipes) {
                for (PayloadStack stack : rec.cachedInputPayloads) {
                    if (stack.item != p.content()) continue;
                    return getPayloadCount(stack.item) < MultiCrafter.payloadCapacity;
                }
            }
            return false;
        }

        public void handlePayload(Building source, Payload p) {
            payload = p;
            payVector.set((Position)source).sub((Position)this).clamp((float)(-MultiCrafter.size * 8) / 2.0f, (float)(-MultiCrafter.size * 8) / 2.0f, (float)(MultiCrafter.size * 8) / 2.0f, (float)(MultiCrafter.size * 8) / 2.0f);
            payRotation = p.rotation();
            updatePayload();
        }

        public Payload getPayload() {
            return payload;
        }

        public Payload takePayload() {
            Payload t = payload;
            payload = null;
            return t;
        }

        public boolean moveInPayload() {
            return moveInPayload(true);
        }

        public boolean moveInPayload(boolean rotate) {
            if (payload == null) {
                return false;
            }
            updatePayload();
            if (rotate) {
                payRotation = Mathf.approachDelta((float)payRotation, (float)(block.rotate ? rotdeg() : 90.0f), (float)(MultiCrafter.payloadRotateSpeed * delta()));
            }
            payVector.approach(Vec2.ZERO, MultiCrafter.payloadSpeed * delta());
            if (payVector.isZero(0.01f)) {
                addPayload(payload.content(), 1);
                payload = null;
                return true;
            }
            return false;
        }

        public void updatePayload() {
            if (payload != null) {
                payload.set(x + payVector.x, y + payVector.y, payRotation);
            }
        }

        public void moveOutPayload() {
            if (outputPayload == null) {
                return;
            }
            Payload p = outputPayload;
            p.update(null, (Building)this);
            int outDir = rotation;
            float targetAngle = (float)outDir * 90.0f;
            Vec2 dest = Tmp.v1.trns(targetAngle, (float)(MultiCrafter.size * 8) / 2.0f);
            outPayRotation = Angles.moveToward((float)outPayRotation, (float)targetAngle, (float)(MultiCrafter.payloadRotateSpeed * delta()));
            outPayVector.approach(dest, MultiCrafter.payloadSpeed * delta());
            p.set(x + outPayVector.x, y + outPayVector.y, outPayRotation);
            float checkOffset = (float)(MultiCrafter.size * 8) / 2.0f + 0.1f;
            float wx = x + Angles.trnsx((float)targetAngle, (float)checkOffset);
            float wy = y + Angles.trnsy((float)targetAngle, (float)checkOffset);
            Tile exitTile = Vars.world.tileWorld(wx, wy);
            boolean canMove = false;
            boolean canDump = false;
            Building front = null;
            if (exitTile != null) {
                front = exitTile.build;
                canMove = front != null && front != this && (front.block.outputsPayload || front.block.acceptsPayload);
                boolean bl = canDump = front == null && !exitTile.solid();
            }
            if (canDump && !canMove) {
                PayloadBlock.pushOutput((Payload)p, (float)(1.0f - outPayVector.dst(dest) / ((float)(MultiCrafter.size * 8) / 2.0f)));
            }
            if (outPayVector.within((Position)dest, 0.001f)) {
                outPayVector.clamp((float)(-MultiCrafter.size * 8) / 2.0f, (float)(-MultiCrafter.size * 8) / 2.0f, (float)(MultiCrafter.size * 8) / 2.0f, (float)(MultiCrafter.size * 8) / 2.0f);
                if (canMove) {
                    p.set(wx, wy, targetAngle);
                    if (movePayload(p)) {
                        outputPayload = null;
                        outPayVector.setZero();
                    }
                } else if (canDump) {
                    p.set(wx, wy, targetAngle);
                    if (p.dump()) {
                        outputPayload = null;
                        outPayVector.setZero();
                    }
                }
            }
        }

        public void pickedUp() {
            carried = true;
            attrsum = 0.0f;
            warmup = 0.0f;
        }

        public void drawTeamTop() {
            carried = false;
        }

        public void onRemoved() {
            super.onRemoved();
            if (payload != null && !carried) {
                payload.dump();
            }
        }

        public void onDestroyed() {
            if (payload != null) {
                payload.destroyed();
            }
            super.onDestroyed();
        }

        public void onProximityAdded() {
            super.onProximityAdded();
            for (Building o : proximity) {
                if (!(o instanceof Conduit.ConduitBuild)) continue;
                o.onProximityUpdate();
            }
        }

        public void onProximityUpdate() {
            super.onProximityUpdate();
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.attribute != null) {
                attrsum = MultiCrafter.sumAttribute(rec.attribute, tile.x, tile.y);
            }
        }

        public void updateTile() {
            if (payload != null) {
                payload.update(null, (Building)this);
                moveInPayload();
            }
            heatInput = heat = calculateHeat(sideHeat);
            Recipe active = getCurrentRecipe();
            if (active != null && active.heatOutputDirections != null) {
                boolean allDirections = false;
                for (int d : active.heatOutputDirections) {
                    if (d != -1) continue;
                    allDirections = true;
                    break;
                }
                if (!allDirections) {
                    for (int i = 0; i < 4; ++i) {
                        boolean allowed = false;
                        int[] nArray = active.heatOutputDirections;
                        int n = nArray.length;
                        for (int j = 0; j < n; ++j) {
                            int d = nArray[j];
                            if (Mathf.mod((int)(d + rotation), (int)4) != i) continue;
                            allowed = true;
                            break;
                        }
                        if (allowed) continue;
                        sideHeat[i] = 0.0f;
                    }
                    float total = 0.0f;
                    for (float s : sideHeat) {
                        total += s;
                    }
                    heat = total;
                    heatInput = total;
                }
            }
            if (selectRecipe == 0) {
                if (active != null && active != currentVisualRecipe) {
                    currentVisualRecipe = active;
                    outputLiquidsSet.clear();
                    syncLiquidOutputs();
                    updateAttrsum();
                } else if (active == null && currentVisualRecipe == null) {
                    for (Recipe r : MultiCrafter.recipes) {
                        if (r.visual == null) continue;
                        currentVisualRecipe = r;
                        break;
                    }
                }
            }
            float totalRatio = active != null ? efficiencyScale() : 0.0f;
            boolean canProduce = totalRatio > 0.001f;
            float targetHeat = canProduce && active.outputHeat > 0.0f ? active.outputHeat * efficiencyScale() : 0.0f;
            heatOutput = Mathf.approachDelta((float)heatOutput, (float)targetHeat, (float)(warmupRate * delta()));
            if (canProduce) {
                Effect eff;
                for (LiquidStack stack : active.inputLiquids) {
                    float consume = stack.amount * delta() * totalRatio;
                    if (active != null && active.scaleLiquidConsumption) {
                        consume *= efficiencyMultiplier();
                    }
                    if (!(consume > 0.0f)) continue;
                    liquids.remove(stack.liquid, consume);
                }
                progress += delta() / active.craftTime * totalRatio;
                warmup = Mathf.lerpDelta((float)warmup, (float)warmupTarget(), (float)warmupRate);
                if (active.outputLiquids.length > 0) {
                    float inc = delta() * totalRatio;
                    for (LiquidStack out : active.outputLiquids) {
                        float canAdd;
                        float amt = out.amount * inc;
                        if (!(amt > 0.0f) || !((canAdd = Math.min(amt, MultiCrafter.liquidCapacity - liquids.get(out.liquid))) > 0.0f)) continue;
                        handleLiquid((Building)this, out.liquid, canAdd);
                        outputLiquidsSet.add((Object)out.liquid);
                    }
                }
                if (active.cachedUpdateSound != null && active.cachedUpdateSound != Sounds.none && wasVisible && Mathf.chanceDelta((double)(active.updateEffectChance > 0.0f ? (double)active.updateEffectChance : (double)MultiCrafter.updateEffectChance))) {
                    active.cachedUpdateSound.at(x, y);
                }
                Effect effect = eff = active.cachedUpdateEffect != null && active.cachedUpdateEffect != Fx.none ? active.cachedUpdateEffect : MultiCrafter.updateEffect;
                if (wasVisible && eff != Fx.none && Mathf.chanceDelta((double)MultiCrafter.updateEffectChance)) {
                    eff.at(x + (float)Mathf.range((int)(MultiCrafter.size * 4)), y + (float)Mathf.range((int)(MultiCrafter.size * 4)), 0.0f, Color.white);
                }
                if (progress >= 1.0f) {
                    craft(active);
                }
            } else {
                warmup = Mathf.lerpDelta((float)warmup, (float)0.0f, (float)warmupRate);
            }
            boolean unitRecipe = false;
            if (active != null) {
                for (PayloadStack s : active.cachedOutputPayloads) {
                    if (!(s.item instanceof UnitType)) continue;
                    unitRecipe = true;
                    break;
                }
            }
            if (canProduce && unitRecipe) {
                time += edelta() * speedScl * Vars.state.rules.unitBuildSpeed(team);
                speedScl = Mathf.lerpDelta((float)speedScl, (float)1.0f, (float)0.05f);
            } else {
                speedScl = Mathf.lerpDelta((float)speedScl, (float)0.0f, (float)0.05f);
            }
            moveOutPayload();
            dumpOutputs();
            totalProgress += warmup * Time.delta;
        }

        /*
         * WARNING - void declaration
         */
        public void craft(Recipe rec) {
            block16: {
                int n;
                block15: {
                    void var5_17;
                    void var5_14;
                    for (ItemStack itemStack : rec.inputItems) {
                        items.remove(itemStack.item, itemStack.amount);
                    }
                    for (ItemStack itemStack : rec.cachedInputPayloads) {
                        removePayload(itemStack.item, itemStack.amount);
                    }
                    if (rec.randomResults == null) break block15;
                    int sum = 0;
                    ItemStack[] itemStackArray = rec.randomResults;
                    n = itemStackArray.length;
                    boolean bl = false;
                    while (var5_14 < n) {
                        ItemStack[] stack2 = itemStackArray[var5_14];
                        sum += stack2.amount;
                        ++var5_14;
                    }
                    if (sum <= 0) break block16;
                    int i = Mathf.randomSeed((long)seed++, (int)0, (int)(sum - 1));
                    int count = 0;
                    Object var5_15 = null;
                    for (ItemStack stack : rec.randomResults) {
                        if (i >= count && i < count + stack.amount) {
                            Item item = stack.item;
                            break;
                        }
                        count += stack.amount;
                    }
                    if (var5_17 == null || items.get((Item)var5_17) >= MultiCrafter.itemCapacity) break block16;
                    offload((Item)var5_17);
                    outputItemsSet.add((Object)var5_17);
                    break block16;
                }
                ItemStack[] sum = rec.outputItems;
                int n2 = sum.length;
                for (n = 0; n < n2; ++n) {
                    ItemStack itemStack = sum[n];
                    for (int i = 0; i < itemStack.amount; ++i) {
                        offload(itemStack.item);
                    }
                    outputItemsSet.add((Object)itemStack.item);
                }
            }
            for (PayloadStack payloadStack : rec.cachedOutputPayloads) {
                for (int i = 0; i < payloadStack.amount; ++i) {
                    Payload p = createPayload(payloadStack.item);
                    if (p instanceof UnitPayload) {
                        UnitPayload up = (UnitPayload)p;
                        Unit unit = up.unit;
                        if (unit.isCommandable()) {
                            if (commandPos != null) {
                                unit.command().commandPosition(commandPos);
                            }
                            unit.command().command(command == null && unit.type.defaultCommand != null ? unit.type.defaultCommand : command);
                        }
                        if (outputPayload != null) {
                            outputPayload = null;
                        }
                        outputPayload = p;
                        outPayVector.setZero();
                        outPayRotation = rotdeg();
                        Events.fire((Object)new EventType.UnitCreateEvent(((UnitPayload)p).unit, (Building)this));
                        continue;
                    }
                    addPayload(payloadStack.item, 1);
                    outputPayloadsSet.add((Object)payloadStack.item);
                }
            }
            progress = 0.0f;
            if (wasVisible) {
                Effect effect = rec.cachedCraftEffect != null && rec.cachedCraftEffect != Fx.none ? rec.cachedCraftEffect : MultiCrafter.craftEffect;
                effect.at(x, y);
                if (rec.cachedCraftSound != null && rec.cachedCraftSound != Sounds.none) {
                    rec.cachedCraftSound.at(x, y);
                }
            }
        }

        public void dumpOutputs() {
            Seq toRemove;
            for (Item item : outputItemsSet) {
                while (items.has(item) && dump(item)) {
                }
                if (items.has(item)) continue;
                outputItemsSet.remove((Object)item);
            }
            Seq liquidsToDump = new Seq();
            if (selectRecipe == 0) {
                liquidsToDump.addAll(outputLiquidsSet);
            } else {
                int idx = selectRecipe - 1;
                if (idx >= 0 && idx < MultiCrafter.recipes.size) {
                    Recipe cur = (Recipe)MultiCrafter.recipes.get(idx);
                    for (Object s : cur.outputLiquids) {
                        liquidsToDump.add((Object)((LiquidStack)s).liquid);
                    }
                }
            }
            Recipe activeRecipe = getCurrentRecipe();
            for (Liquid liquid : liquidsToDump) {
                if (!(liquids.get(liquid) > 0.001f)) continue;
                int dir = -1;
                if (activeRecipe != null && activeRecipe.outputLiquids.length > 0) {
                    for (int i = 0; i < activeRecipe.outputLiquids.length; ++i) {
                        if (activeRecipe.outputLiquids[i].liquid != liquid || i >= activeRecipe.liquidOutputDirections.length) continue;
                        dir = activeRecipe.liquidOutputDirections[i];
                        break;
                    }
                }
                dumpLiquid(liquid, 2.0f, dir);
            }
            if (selectRecipe == 0) {
                toRemove = new Seq();
                for (Liquid l : outputLiquidsSet) {
                    if (!(liquids.get(l) <= 0.001f)) continue;
                    toRemove.add((Object)l);
                }
                for (Liquid l : toRemove) {
                    outputLiquidsSet.remove((Object)l);
                }
            }
            toRemove = new Seq();
            for (UnlockableContent c : outputPayloadsSet) {
                Payload p;
                while (getPayloadCount(c) > 0 && (p = createPayload(c)) != null && dumpPayload(p)) {
                    removePayload(c, 1);
                }
                if (getPayloadCount(c) != 0) continue;
                toRemove.add((Object)c);
            }
            for (UnlockableContent c : toRemove) {
                outputPayloadsSet.remove((Object)c);
            }
        }

        private Payload createPayload(UnlockableContent c) {
            if (c instanceof Block b) {
                return new BuildPayload(b, team);
            }
            if (c instanceof UnitType u) {
                return new UnitPayload(u.create(team));
            }
            return null;
        }

        public boolean shouldConsume() {
            if (selectRecipe == 0) {
                for (Recipe r : MultiCrafter.recipes) {
                    if (!checkOutput(r)) continue;
                    return enabled;
                }
                return false;
            }
            int idx = selectRecipe - 1;
            return idx >= 0 && idx < MultiCrafter.recipes.size && checkOutput((Recipe)MultiCrafter.recipes.get(idx)) && enabled;
        }

        public float warmupTarget() {
            Recipe rec = getCurrentRecipe();
            if (rec != null && rec.inputHeat > 0.0f) {
                if (heatRequirement() <= 0.0f) {
                    return 1.0f;
                }
                return Mathf.clamp((float)(heatInput / heatRequirement()));
            }
            return 1.0f;
        }

        public float efficiencyScale() {
            if (!enabled) {
                return 0.0f;
            }
            Recipe rec = getCurrentRecipe();
            if (rec == null) {
                return 0.0f;
            }
            float scale = 1.0f;
            if (rec.inputHeat > 0.0f) {
                float req = rec.inputHeat;
                float over = Math.max(heatInput - req, 0.0f);
                scale = Math.min(Mathf.clamp((float)(heatInput / req)) + over / req * MultiCrafter.overheatScale, rec.maxEfficiency);
            }
            if (rec.inputPower > 0.0f) {
                scale *= power == null ? 0.0f : power.status;
            }
            for (LiquidStack liquidStack : rec.inputLiquids) {
                float have = liquids.get(liquidStack.liquid);
                float need = liquidStack.amount * delta() * scale;
                if (!(need > 0.0f)) continue;
                scale *= Math.min(have / need, 1.0f);
            }
            for (LiquidStack liquidStack : rec.inputItems) {
                if (items.get(liquidStack.item) >= liquidStack.amount) continue;
                return 0.0f;
            }
            for (LiquidStack liquidStack : rec.cachedInputPayloads) {
                if (getPayloadCount(liquidStack.item) >= liquidStack.amount) continue;
                return 0.0f;
            }
            return scale *= efficiencyMultiplier();
        }

        public float getProgressIncrease(float base) {
            return super.getProgressIncrease(base) * efficiencyMultiplier();
        }

        public float heatRequirement() {
            Recipe rec = getCurrentRecipe();
            return rec == null ? 0.0f : rec.inputHeat;
        }

        public float heat() {
            Recipe rec = getCurrentRecipe();
            return rec != null && rec.outputHeat > 0.0f && heatOutput > 0.001f ? heatOutput : 0.0f;
        }

        public float heatFrac() {
            Recipe rec = getCurrentRecipe();
            return rec != null && rec.outputHeat > 0.0f ? heatOutput / rec.outputHeat : 0.0f;
        }

        public float getPowerProduction() {
            Recipe rec = getCurrentRecipe();
            return rec != null && rec.outputPower > 0.0f ? rec.outputPower * efficiencyScale() : 0.0f;
        }

        public float getPowerStat() {
            Recipe rec = getCurrentRecipe();
            return rec != null && rec.outputPower > 0.0f ? efficiencyScale() : 0.0f;
        }

        @Nullable
        public UnitType getDisplayedUnitType() {
            PayloadStack[] payloadStackArray = outputPayload;
            if (payloadStackArray instanceof UnitPayload) {
                UnitPayload up = (UnitPayload)payloadStackArray;
                return up.unit.type;
            }
            Recipe rec = getCurrentRecipe();
            if (rec != null) {
                for (PayloadStack s : rec.cachedOutputPayloads) {
                    if (!(s.item instanceof UnitType)) continue;
                    return (UnitType)s.item;
                }
            }
            return null;
        }

        public boolean acceptItem(Building source, Item item) {
            if (items.get(item) >= MultiCrafter.itemCapacity) {
                return false;
            }
            return MultiCrafter.recipes.contains(r -> {
                for (ItemStack s : r.inputItems) {
                    if (s.item != item) continue;
                    return true;
                }
                return false;
            });
        }

        public boolean acceptLiquid(Building source, Liquid liquid) {
            return MultiCrafter.recipes.contains(r -> {
                for (LiquidStack s : r.inputLiquids) {
                    if (s.liquid != liquid) continue;
                    return true;
                }
                return false;
            });
        }

        public void handleItem(Building source, Item item) {
            items.add(item, 1);
        }

        public int acceptStack(Item item, int amount, Teamc source) {
            return Math.min(amount, MultiCrafter.itemCapacity - items.get(item));
        }

        public void handleStack(Item item, int amount, Teamc source) {
            items.add(item, amount);
        }

        public void draw() {
            MultiCrafter.drawer.draw((Building)this);
            if (currentVisualRecipe != null && currentVisualRecipe.visual != null) {
                currentVisualRecipe.visual.draw((Building)this);
            }
            drawPayload();
            Recipe active = getCurrentRecipe();
            if (active != null && progress > 0.0f) {
                for (PayloadStack s : active.cachedOutputPayloads) {
                    UnlockableContent unlockableContent = s.item;
                    if (!(unlockableContent instanceof UnitType)) continue;
                    UnitType unit = (UnitType)unlockableContent;
                    Draw.draw((float)35.0f, () -> Drawf.construct((Building)this, (UnlockableContent)unit, (float)(rotdeg() - 90.0f), (float)progress, (float)speedScl, (float)time));
                    break;
                }
            }
            if (outputPayload != null) {
                Draw.z((float)35.0f);
                outputPayload.draw();
            }
            drawStatus();
        }

        public void drawPayload() {
            if (payload != null) {
                updatePayload();
                Draw.z((float)35.0f);
                payload.draw();
            }
        }

        public void drawStatus() {
            if (block.enableDrawStatus) {
                float m = block.size > 1 ? 1.0f : 0.64f;
                float bx = x + (float)block.size * 8.0f / 2.0f - 8.0f * m / 2.0f;
                float by = y - (float)block.size * 8.0f / 2.0f + 8.0f * m / 2.0f;
                Draw.z((float)71.0f);
                Draw.color((Color)Pal.gray);
                Fill.square((float)bx, (float)by, (float)(2.5f * m), (float)45.0f);
                Draw.color((Color)status().color);
                Fill.square((float)bx, (float)by, (float)(1.5f * m), (float)45.0f);
                Draw.color();
            }
        }

        public BlockStatus status() {
            if (!enabled) {
                return BlockStatus.logicDisable;
            }
            if (!shouldConsume()) {
                return BlockStatus.noOutput;
            }
            Recipe rec = getCurrentRecipe();
            if (rec == null) {
                return BlockStatus.noInput;
            }
            if (efficiencyScale() > 0.0f && productionValid()) {
                return Vars.state.tick / 30.0 % 1.0 < (double)efficiencyScale() ? BlockStatus.active : BlockStatus.noInput;
            }
            return BlockStatus.noInput;
        }

        public byte version() {
            return 2;
        }

        public void write(Writes w) {
            super.write(w);
            w.i(selectRecipe);
            w.f(heatInput);
            w.f(heatOutput);
            w.s(outputItemsSet.size);
            for (Item item : outputItemsSet) {
                w.s((int)item.id);
            }
            w.s(outputLiquidsSet.size);
            for (Liquid l : outputLiquidsSet) {
                w.s((int)l.id);
            }
            w.s(outputPayloadsSet.size);
            for (UnlockableContent c : outputPayloadsSet) {
                w.s((int)c.id);
            }
            w.f(payVector.x);
            w.f(payVector.y);
            w.f(payRotation);
            Payload.write((Payload)payload, (Writes)w);
            w.s(payloadCounts.size);
            for (IntMap.Entry e : payloadCounts.entries()) {
                w.s(e.key);
                w.i(((Integer)e.value).intValue());
            }
            w.f(time);
            w.f(speedScl);
            Payload.write((Payload)outputPayload, (Writes)w);
            w.i(currentVisualRecipe == null ? -1 : MultiCrafter.recipes.indexOf((Object)currentVisualRecipe));
            w.f(outPayVector.x);
            w.f(outPayVector.y);
            w.f(outPayRotation);
            TypeIO.writeVecNullable((Writes)w, (Vec2)commandPos);
            TypeIO.writeCommand((Writes)w, (UnitCommand)command);
            w.f(attrsum);
            w.i(seed);
        }

        public void read(Reads r, byte rev) {
            int idx;
            super.read(r, rev);
            selectRecipe = r.i();
            selectRecipe = Mathf.clamp((int)selectRecipe, (int)0, (int)MultiCrafter.recipes.size);
            heatInput = r.f();
            heatOutput = r.f();
            outputItemsSet.clear();
            int is = r.s();
            for (int i = 0; i < is; ++i) {
                outputItemsSet.add((Object)Vars.content.item((int)r.s()));
            }
            outputLiquidsSet.clear();
            int ls = r.s();
            for (int i = 0; i < ls; ++i) {
                outputLiquidsSet.add((Object)Vars.content.liquid((int)r.s()));
            }
            outputPayloadsSet.clear();
            int ps = r.s();
            for (int i = 0; i < ps; ++i) {
                short id = r.s();
                Block c = Vars.content.block((int)id);
                if (c == null) {
                    c = Vars.content.unit((int)id);
                }
                if (c == null) continue;
                outputPayloadsSet.add((Object)c);
            }
            payVector.set(r.f(), r.f());
            payRotation = r.f();
            payload = Payload.read((Reads)r);
            payloadCounts.clear();
            int pcs = r.s();
            for (int i = 0; i < pcs; ++i) {
                short id = r.s();
                int count = r.i();
                payloadCounts.put((int)id, (Object)count);
            }
            if (rev >= 1) {
                time = r.f();
                speedScl = r.f();
                outputPayload = Payload.read((Reads)r);
                idx = r.i();
                currentVisualRecipe = idx >= 0 && idx < MultiCrafter.recipes.size ? (Recipe)MultiCrafter.recipes.get(idx) : null;
                outPayVector.set(r.f(), r.f());
                outPayRotation = r.f();
                commandPos = TypeIO.readVecNullable((Reads)r);
                command = TypeIO.readCommand((Reads)r);
                if (rev >= 2) {
                    attrsum = r.f();
                    seed = r.i();
                } else {
                    attrsum = 0.0f;
                    seed = 0;
                }
            } else {
                idx = r.i();
                currentVisualRecipe = idx >= 0 && idx < MultiCrafter.recipes.size ? (Recipe)MultiCrafter.recipes.get(idx) : null;
                time = 0.0f;
                speedScl = 0.0f;
                outputPayload = null;
                outPayVector.setZero();
                outPayRotation = 0.0f;
                commandPos = null;
                command = null;
                attrsum = 0.0f;
                seed = 0;
            }
            syncLiquidOutputs();
        }
    }
}

