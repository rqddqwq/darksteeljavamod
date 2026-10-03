package darksteel.world;

import mindustry.world.*;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.ui.Styles;
import mindustry.content.Blocks;

import arc.util.Time;
import arc.util.io.*;
import arc.struct.*;
import arc.scene.ui.layout.Table;
import arc.scene.ui.*;
import arc.graphics.g2d.*;
import arc.Core;

import static mindustry.Vars.*;

/**
 * AssemblyCore
 * 一个多方块构造
 * @author xxxxxxxx
 */
public class AssemblyCore extends Block {
    public AssemblyCore(String name) {
        super(name);
        configurable = true;
        canOverdrive = false;
        solid = true;
        destructible = true;
        update = true;
        copyConfig = true;
        config(TmpPack.class, (AssemblyCoreBuild build, TmpPack value) -> {
            build.progress = value.progress;
            build.isAssembling = value.isAssembling;
        });
    }
    private OrderedMap<String, Float> blockMap = new OrderedMap<>();
    private Seq<Recipe[]> recipeList = new Seq<>();
    public void add(String blockName, float time, Recipe[] list){
        if(blockName == null || blockName.equals("")) return;
        if(time <= 0 || list.length == 0) return;
        blockMap.put(blockName, time);
        recipeList.add(list);
    }
    public class AssemblyCoreBuild extends Building {
        public float progress = 0f;
        public boolean isAssembling = false;
        public int index = -1;
        public int updateTime = 0;
        private float buildTime = 0f;
        private Block buildBlock = null;
        private float time = 0f;
        private Recipe[] recipes = null;
        private final TmpPack tmpPack = new TmpPack();
        @Override
        public void updateTile() {
            if(!isAssembling || !assembling() || time <= 0f || buildBlock == Blocks.air) {
                progress = 0f;
                buildTime = 0f;
                return;
            }
            buildTime += delta();
            progress = buildTime / time;
            if(++updateTime % 30 == 0) {
                updateTime = 0;
                updateTmpPack();
                configure(tmpPack);
            }
            if(progress >= 1) {
                buildBlock.placeEffect.at(tile.drawx(), tile.drawy(), buildBlock.size);
                buildBlock.placeSound.at(tile);
                tile.setBlock(buildBlock, team);
            }
        }
        @Override
        public void draw() {
            super.draw();
            if(buildBlock == null || recipes == null) return;
            Drawf.text("建造进度" + (int)(progress * 100) + "%", x, y + ((buildBlock.size / 2) * tilesize) + 14f, Pal.accent);
            if(isAssembling && assembling()) {
                if(buildBlock == null || buildBlock == Blocks.air) return;
                Draw.draw(Layer.blockBuilding, () -> {
                    Draw.color(Pal.accent);
                    float drawX = x, drawY = y;
                    if(buildBlock.size % 2 == 0) {
                        drawX += tilesize;
                        drawY += tilesize;
                    }
                    int i = 0;
                    boolean noOverrides = buildBlock.regionRotated1 == -1 && buildBlock.regionRotated2 == -1;
                    for(TextureRegion region : buildBlock.getGeneratedIcons()) {
                        
                        Shaders.blockbuild.region = region;
                        Shaders.blockbuild.time = Time.time;
                        Shaders.blockbuild.progress = progress;
                        Draw.rect(
                            region, drawX, drawY,
                            buildBlock.rotate && (noOverrides || buildBlock.regionRotated2 == i || buildBlock.regionRotated1 == i)
                                ? rotdeg() + buildBlock.visualRotationOffset : 0
                        );
                        Draw.flush();
                        i++;
                    }
                    Draw.color();
                });
            }else{
                Block drawBlock;
                float drawX, drawY, drawSize;
                for(int i = 0;i < recipes.length;i++) {
                    drawBlock = content.block(recipes[i].name);
                    if(drawBlock != null) {
                        Draw.color(1f, 1f, 1f, 0.5f);
                        drawSize = drawBlock.size * 0.7f * tilesize;
                        drawX = x + (recipes[i].x * tilesize);
                        drawY = y + (recipes[i].y * tilesize);
                        if(drawBlock.size % 2 == 0) {
                            drawX += tilesize;
                            drawY += tilesize;
                        }
                        Draw.rect(Core.atlas.find(recipes[i].name), drawX, drawY, drawSize, drawSize);
                    }
                }
            }
        }
        @Override
        public TmpPack config() {
            return tmpPack;
        }
        @Override
        public void buildConfiguration(Table table) {
            table.center();
            table.background(Styles.black6);
            table.defaults().padBottom(5f);
            String string = assembling() ? "[green]可以构建" : "[red]无法构建";
            table.add("当前状态：" + string, 1.4f).row();
            TextButton button = new TextButton("开始构建");
            button.changed(() -> {
                if(isAssembling) {
                    isAssembling = false;
                    updateTmpPack();
                    configure(tmpPack);
                }else{
                    if(assembling()) {
                        isAssembling = true;
                        updateTmpPack();
                        configure(tmpPack);
                    }
                }
            });
            table.add(button).size(240f, 60f).row();
            if(blockMap.size < 1) return;
            for(int i = 0;i < blockMap.size;i++) {
                String blockName = blockMap.orderedKeys().get(i);
                Button blockButton = null;
                if(i == index) {
                    blockButton = new Button(Styles.flatDown);
                }else{
                    blockButton = new Button(Styles.flatBordert);
                }
                blockButton.left();
                blockButton.add("  ");
                blockButton.add(new Image(Core.atlas.find(blockName))).size(40f);
                blockButton.add("   " + content.block(blockName).localizedName);
                int tmpIndex = i;
                blockButton.changed(() -> {
                    if(tmpIndex == index) {
                        index = -1;
                    }else{
                        index = tmpIndex;
                    }
                    updateTmpPack();
                    configure(tmpPack);
                    table.clear();
                    buildConfiguration(table);
                });
                table.add(blockButton).size(240f, 60f).row();
            }
        }
        public boolean assembling() {
            if(index == -1 || index >= blockMap.size) return false;
            if(blockMap.size == 0) return false;
            updateReicpe();
            int buildX, buildY;
            for(int i = 0;i < recipes.length;i++){
                buildX = tileX() + recipes[i].x;
                buildY = tileY() + recipes[i].y;
                if(!world.tiles.in(buildX, buildY)) return false;
                Tile blockTile = world.tile(buildX, buildY);
                if(blockTile.build == null) return false;
                if(!blockTile.block().name.equals(recipes[i].name)) return false;
                if(blockTile.build.tileX() != buildX || blockTile.build.tileY() != buildY) return false;
            }
            return true;
        }
        public void updateTmpPack() {
            tmpPack.progress = progress;
            tmpPack.isAssembling = isAssembling;
        }
        public void updateReicpe() {
            if(index < 0 || index >= blockMap.size) {
                buildBlock = null;
                recipes = null;
                time = 0f;
                return;
            }
            buildBlock = content.block(blockMap.orderedKeys().get(index));
            time = blockMap.get(blockMap.orderedKeys().get(index));
            recipes = recipeList.get(index);
            if(buildBlock == null) {
                recipes = null;
                time = 0f;
            }
        }
        @Override
        public void read(Reads read) {
            super.read(read);
            progress = read.f();
            index = read.i();
            isAssembling = read.bool();
        }
        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.i(index);
            write.bool(isAssembling);
        }
    }
    public static class Recipe {
        public String name;
        public int x;
        public int y;
        public Recipe(String name, int x, int y){
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }
    public static class TmpPack {
        public float progress;
        public boolean isAssembling;
    }
}