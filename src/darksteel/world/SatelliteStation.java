package darksteel.world;

import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class SatelliteStation extends Block {
    public SatelliteStation(String name) {
        super(name);
        configurable = true;
        solid = true;
        destructible = true;
        update = true;
        sync = true;
        hasPower = false;
        config(Integer.class, (SatelliteStationBuild build, Integer value) -> {
            build.satelliteCount = value;
            build.launching = false;
            build.launchTimer = 0f;
        });
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.text("卫星: 0", x * tilesize + offset, y * tilesize + offset, Pal.accent);
    }

    public class SatelliteStationBuild extends Building {
        public int satelliteCount = 0;
        public boolean launching = false;
        public float launchTimer = 0f;

        @Override
        public void updateTile() {
            if(!launching) return;

            launchTimer += delta();
            if(launchTimer >= 1.5f) {
                satelliteCount++;
                launching = false;
                launchTimer = 0f;
                configure(satelliteCount);
            }
        }

        @Override
        public void draw() {
            super.draw();

            float labelY = y + tilesize * 0.9f;
            Drawf.text("卫星: " + satelliteCount, x, labelY, Pal.accent);
        }

        @Override
        public Integer config() {
            return satelliteCount;
        }

        @Override
        public void buildConfiguration(Table table) {
            table.center();
            table.defaults().pad(6f);

            table.add("当前卫星数量：" + satelliteCount).left().row();

            if(launching) {
                table.add("发射中... " + (1.5f - launchTimer) + "s").color(Pal.accent).row();
            }

            TextButton launchButton = new TextButton(launching ? "发射中" : "发射卫星");
            launchButton.clicked(() -> {
                if(launching) return;

                launching = true;
                launchTimer = 0f;
                Fx.padlaunch.at(x, y, 0f);
                configure(satelliteCount);
                table.clear();
                buildConfiguration(table);
            });

            table.add(launchButton).size(220f, 52f);
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(satelliteCount);
            write.bool(launching);
            write.f(launchTimer);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            satelliteCount = read.i();
            launching = read.bool();
            launchTimer = read.f();
        }
    }
}
