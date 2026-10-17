package darksteel.world;

import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.gen.Building;
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
        saveConfig = true;
        hasPower = false;
        config(LaunchConfig.class, (SatelliteStationBuild build, LaunchConfig value) -> {
            build.satelliteCount = value.satelliteCount;
            build.launching = value.launching;
            build.launchTimer = value.launchTimer;
        });
    }

    public class SatelliteStationBuild extends Building {
        public int satelliteCount = 0;
        public boolean launching = false;
        public float launchTimer = 0f;
        public boolean effectPlayed = false;

        @Override
        public void updateTile() {
            if(!launching) return;

            if(!effectPlayed && !headless) {
                Fx.padlaunch.at(tile.drawx(), tile.drawy(), 0f);
                effectPlayed = true;
            }

            launchTimer += delta();
            if(launchTimer >= 10f) {
                satelliteCount++;
                launching = false;
                launchTimer = 0f;
                effectPlayed = false;
                configure(new LaunchConfig(satelliteCount, false, 0f));
            }
        }

        @Override
        public void draw() {
            super.draw();
        }

        @Override
        public LaunchConfig config() {
            return new LaunchConfig(satelliteCount, launching, launchTimer);
        }

        @Override
        public void buildConfiguration(Table table) {
            table.center();
            table.defaults().pad(6f);

            table.add("当前卫星数量：" + satelliteCount).left().row();

            if(launching) {
                int countdown = 10 - Math.round(launchTimer);
                if(countdown < 0) countdown = 0;
                table.add("倒计时：" + countdown).color(Pal.accent).row();
            }

            TextButton launchButton = new TextButton(launching ? "发射中" : "发射卫星");
            launchButton.clicked(() -> {
                if(launching) return;

                launching = true;
                launchTimer = 0f;
                effectPlayed = false;
                if(!headless) {
                    Fx.padlaunch.at(tile.drawx(), tile.drawy(), 0f);
                    effectPlayed = true;
                }
                configure(new LaunchConfig(satelliteCount, true, 0f));
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
            write.bool(effectPlayed);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            satelliteCount = read.i();
            launching = read.bool();
            launchTimer = read.f();
            effectPlayed = read.bool();
        }
    }

    public static class LaunchConfig {
        public int satelliteCount;
        public boolean launching;
        public float launchTimer;

        public LaunchConfig(int satelliteCount, boolean launching, float launchTimer) {
            this.satelliteCount = satelliteCount;
            this.launching = launching;
            this.launchTimer = launchTimer;
        }
    }
}
