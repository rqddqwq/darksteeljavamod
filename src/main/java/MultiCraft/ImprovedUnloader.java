/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import mindustry.world.blocks.storage.Unloader;

public class ImprovedUnloader
extends Unloader {
    public ImprovedUnloader(String name) {
        super(name);
    }

    public class ImprovedUnloaderBuild
    extends Unloader.UnloaderBuild {
        protected float counter;

        public ImprovedUnloaderBuild() {
            super((Unloader)ImprovedUnloader.this);
        }

        public void updateTile() {
            counter += edelta();
            while (counter >= ImprovedUnloader.speed) {
                unloadTimer = ImprovedUnloader.speed;
                super.updateTile();
                counter -= ImprovedUnloader.speed;
            }
        }
    }
}

