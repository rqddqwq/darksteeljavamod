/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import MultiCraft.MultiCrafterPayloadFragment;
import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType;

public class MultiCrafterMod
{
    public static MultiCrafterPayloadFragment payloadFragment;

    public void init() {
        Events.run((Object)EventType.Trigger.uiDrawBegin, () -> {
            Table itemInv;
            if (payloadFragment == null && (itemInv = (Table)Vars.ui.hudGroup.find("inventory")) != null) {
                payloadFragment = new MultiCrafterPayloadFragment();
                payloadFragment.build(itemInv.parent);
            }
        });
        Events.run((Object)EventType.Trigger.update, () -> {
            if (payloadFragment != null) {
                Table itemInv = (Table)Vars.ui.hudGroup.find("inventory");
                MultiCrafterMod.payloadFragment.table.visible = itemInv != null && itemInv.visible && !Vars.state.isMenu();
                payloadFragment.rebuild();
            }
        });
    }
}

