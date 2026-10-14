/*
 * Decompiled with CFR 0.153-SNAPSHOT (c414525).
 */
package MultiCraft;

import MultiCraft.MultiCrafter;
import arc.func.Prov;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Reflect;
import java.util.Iterator;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Tex;
import mindustry.type.PayloadSeq;
import mindustry.world.Block;

public class MultiCrafterPayloadFragment {
    public Table table = new Table();
    private static Table itemInvRef;

    public void build(Group parent) {
        table.name = "multiCrafterPayload";
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild((Element)table);
    }

    /*
     * Enabled aggressive block sorting
     */
    public void rebuild() {
        block5: {
            Iterator iterator;
            PayloadSeq payloads;
            int cols;
            int row;
            block4: {
                block2: {
                    block3: {
                        Block block;
                        MultiCrafter.MultiCrafterBuild build;
                        if (!table.visible) {
                            table.clear();
                            return;
                        }
                        table.clear();
                        table.background(Tex.inventory);
                        table.touchable = Touchable.disabled;
                        table.margin(4.0f);
                        table.defaults().size(40.0f).pad(4.0f);
                        row = 0;
                        cols = 4;
                        Building b = getBuild();
                        if (!(b instanceof MultiCrafter.MultiCrafterBuild) || null == (build = (MultiCrafter.MultiCrafterBuild)b) || !((block = build.block) instanceof MultiCrafter)) break block2;
                        MultiCrafter mc = (MultiCrafter)block;
                        if (!mc.hasPayloads) break block2;
                        payloads = build.getPayloads();
                        if (payloads == null || payloads.isEmpty()) break block3;
                        iterator = MultiCrafter.MultiCrafterBuild.allPayloadTypes.iterator();
                        break block4;
                    }
                    table.visible = false;
                    break block5;
                }
                table.visible = false;
                break block5;
            }
            while (iterator.hasNext()) {
                int amount;
                UnlockableContent content = (UnlockableContent)iterator.next();
                if (!payloads.contains(content) || (amount = payloads.get(content)) <= 0) continue;
                table.add(itemImage(content.uiIcon, (Prov<CharSequence>)((Prov)() -> round(amount))));
                if (row++ % cols != cols - 1) continue;
                table.row();
            }
        }
        updateTablePosition();
    }

    private Building getBuild() {
        try {
            return (Building)Reflect.get((Object)Vars.control.input.inv, (String)"build");
        } catch (Exception e) {
            Log.err((Throwable)e);
            return null;
        }
    }

    private String round(float f) {
        if ((f = (float)((int)f)) >= 1000000.0f) {
            return (int)(f / 1000000.0f) + "[gray]" + UI.millions;
        }
        if (f >= 1000.0f) {
            return (int)(f / 1000.0f) + UI.thousands;
        }
        return "" + (int)f;
    }

    private void updateTablePosition() {
        table.pack();
        if (itemInvRef == null) {
            itemInvRef = (Table)Vars.ui.hudGroup.find("inventory");
        }
        if (itemInvRef != null) {
            table.setPosition(MultiCrafterPayloadFragment.itemInvRef.x, MultiCrafterPayloadFragment.itemInvRef.y + itemInvRef.getPrefHeight() - 4.0f, 12);
        }
    }

    private Element itemImage(TextureRegion region, Prov<CharSequence> text) {
        Stack stack = new Stack();
        Table t = new Table().left().bottom();
        t.label(text);
        stack.add((Element)new Image(region));
        stack.add((Element)t);
        return stack;
    }
}

