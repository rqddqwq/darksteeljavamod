package darksteel.content;

import arc.Core;
import arc.files.Fi;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;
import mindustry.gen.Player;

/**
 * 剧情对话系统
 * 从 mods/模组名/assets/dialogue.json 读取剧情
 */
public class DialogueSystem {

    // ============================================================
    // 对话条目
    // ============================================================
    public static class DialogueEntry {
        public String id;
        public String speaker;
        public String text;
        public String next;
        public float delay;
        public String[] choices;
        public String[] choiceNext;
    }

    public static final Seq<DialogueEntry> allDialogues = new Seq<>();
    private static boolean loaded = false;

    /** ✅ 固定 JSON 文件名 */
    private static final String DIALOGUE_FILE = "dialogue.json";

    /** 打字机速度：每秒显示多少字 */
    public static float typingSpeed = 30f;

    // ============================================================
    // 加载
    // ============================================================
    public static void load() {
        if (loaded) return;
        loaded = true;

        Fi file = findDialogueFile();
        if (file == null || !file.exists()) {
            Log.warn("[Dialogue] 找不到 dialogue.json");
            return;
        }

        try {
            Jval json = Jval.read(file.readString());

            if (!json.isArray()) {
                Log.warn("[Dialogue] dialogue.json 顶层必须是数组");
                return;
            }

            for (Jval node : json.asArray()) {
                DialogueEntry entry = new DialogueEntry();
                entry.id      = node.getString("id", "");
                entry.speaker = node.getString("speaker", "");
                entry.text    = node.getString("text", "");
                entry.next    = node.getString("next", null);
                entry.delay   = node.getFloat("delay", 0f);

                Jval choicesNode = node.get("choices");
                if (choicesNode != null && choicesNode.isArray()) {
                    Seq<String> choicesList = new Seq<>();
                    Seq<String> nextList = new Seq<>();
                    for (Jval c : choicesNode.asArray()) {
                        choicesList.add(c.getString("text", ""));
                        nextList.add(c.getString("next", ""));
                    }
                    entry.choices = choicesList.toArray(String.class);
                    entry.choiceNext = nextList.toArray(String.class);
                }

                allDialogues.add(entry);
            }

            Log.info("[Dialogue] 已加载 @ 条对话", allDialogues.size);

        } catch (Exception e) {
            Log.err("[Dialogue] 解析失败", e);
        }
    }

    private static Fi findDialogueFile() {
        for (var mod : Vars.mods.list()) {
            Fi dir = mod.root.child("assets").child(DIALOGUE_FILE);
            if (dir.exists()) return dir;
        }
        Fi fallback = Vars.dataDirectory.child(DIALOGUE_FILE);
        if (fallback.exists()) return fallback;
        return null;
    }

    // ============================================================
    // 触发
    // ============================================================
    public static void start(String dialogueId, Player player) {
        if (!loaded) load();
        DialogueEntry entry = find(dialogueId);
        if (entry == null) {
            Log.warn("[Dialogue] 找不到对话: @", dialogueId);
            return;
        }
        showEntry(entry, player);
    }

    private static void showEntry(DialogueEntry entry, Player player) {
        if (entry.choices == null || entry.choices.length == 0) {
            showTypingDialog(entry, player, null, null);
        } else {
            showTypingDialog(entry, player, entry.choices, entry.choiceNext);
        }
    }

    // ============================================================
    // ✅ 打字机对话框
    // ============================================================
    private static void showTypingDialog(DialogueEntry entry,
                                         Player player,
                                         String[] choices,
                                         String[] choiceNext) {

        Dialog dialog = new Dialog(entry.speaker);
        dialog.setFillParent(false);

        Table table = new Table();
        dialog.cont.add(table).width(520).pad(20).row();

        table.add("[accent]" + entry.speaker + "：").left().row();

        Label textLabel = new Label("");
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.left);
        table.add(textLabel).width(480).left().padTop(8).row();

        Table choiceTable = new Table();
        table.add(choiceTable).left().row();

        final String fullText = entry.text;
        final float[] charTimer = {0f};
        final boolean[] finished = {false};

        // 打字结束回调（用数组绕过 lambda 引用问题）
        final Runnable[] onFinish = new Runnable[1];
        onFinish[0] = () -> {
            if (choices != null && choices.length > 0) {
                choiceTable.clearChildren();
                for (int i = 0; i < choices.length; i++) {
                    final String nextId = choiceNext[i];
                    choiceTable.button(choices[i], () -> {
                        dialog.hide();
                        if (nextId != null && !nextId.isEmpty()) {
                            showEntry(find(nextId), player);
                        }
                    }).size(460, 45).pad(4).row();
                }
            } else {
                choiceTable.clearChildren();
                choiceTable.button("继续", () -> {
                    dialog.hide();
                    if (entry.next != null && !entry.next.isEmpty()) {
                        showEntry(find(entry.next), player);
                    }
                }).size(200, 45).pad(4).row();
            }
        };

        // ✅ 每帧更新：逐字显示
        table.update(() -> {
            if (!finished[0]) {
                charTimer[0] += Core.graphics.getDeltaTime();
                int targetChars = (int)(charTimer[0] * typingSpeed);

                if (targetChars >= fullText.length()) {
                    targetChars = fullText.length();
                    finished[0] = true;
                    onFinish[0].run();
                }
                textLabel.setText(fullText.substring(0, targetChars));
            }
        });

        // ✅ 点击对话框 → 跳过打字
        dialog.cont.touchable = Touchable.enabled;
        dialog.cont.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if (!finished[0]) {
                    textLabel.setText(fullText);
                    finished[0] = true;
                    onFinish[0].run();
                }
            }
        });

        dialog.addCloseButton();
        dialog.show();
    }

    // ============================================================
    // 查找
    // ============================================================
    public static DialogueEntry find(String id) {
        for (DialogueEntry e : allDialogues) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }
}