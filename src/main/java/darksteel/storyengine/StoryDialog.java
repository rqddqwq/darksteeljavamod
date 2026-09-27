package darksteel.storyengine;

import arc.Core;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;

/**
 * 剧情对话框（逐字打印版）
 */
public class StoryDialog extends Dialog {

    private final StoryData data;
    private String currentNodeId;
    private Table contentTable;

    /** 打字速度：每秒显示多少字 */
    public static float typingSpeed = 30f;

    // 打字状态
    private String fullText = "";
    private float charTimer = 0f;
    private boolean finished = false;
    private Label textLabel;

    public StoryDialog(StoryData data, String startNodeId) {
        super("");
        this.data = data;
        this.currentNodeId = startNodeId;
        setupUI();
    }

    public void open() {
        this.show();
    }

    /**
     * 初始化基础 UI
     */
    private void setupUI() {
        cont.clear();
        cont.defaults().pad(10);

        contentTable = new Table();
        contentTable.defaults().pad(5);
        cont.add(contentTable).grow().top();

        // 点击对话框 → 跳过打字
        cont.touchable = Touchable.enabled;
        cont.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!finished && textLabel != null) {
                    // 立即显示全部
                    textLabel.setText(fullText);
                    finished = true;
                    // 显示后续按钮
                    showButtons(currentNodeId);
                }
            }
        });

        showNode(currentNodeId);
    }

    /**
     * 渲染一个节点
     */
    private void showNode(String nodeId) {
        currentNodeId = nodeId;
        contentTable.clear();

        StoryData.Node node = data.nodes.get(nodeId);
        if (node == null) {
            Log.err("剧情引擎：节点 " + nodeId + " 不存在！");
            hide();
            return;
        }

        // 1. 说话人
        if (node.speaker != null && !node.speaker.isEmpty()) {
            contentTable.add("[accent]" + node.speaker + "[]").left().row();
            contentTable.add().size(5f).row();
        }

        // 2. 正文（逐字打印）
        fullText = node.text == null ? "" : node.text;
        charTimer = 0f;
        finished = false;

        textLabel = new Label("");
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.left);
        contentTable.add(textLabel).width(450f).left().row();
        contentTable.add().size(15f).row();

        // 3. 按钮容器（先留空，打字完才填）
        Table buttonTable = new Table();
        contentTable.add(buttonTable).left().row();

        // 4. 每帧更新：逐字显示
        contentTable.update(() -> {
            if (!finished) {
                charTimer += Core.graphics.getDeltaTime();
                int targetChars = (int) (charTimer * typingSpeed);

                if (targetChars >= fullText.length()) {
                    targetChars = fullText.length();
                    finished = true;
                    // 打字完 → 显示按钮
                    fillButtons(buttonTable, node);
                }
                textLabel.setText(fullText.substring(0, targetChars));
            }
        });
    }

    /**
     * 打字完成后显示按钮（用 currentNodeId 重新找节点）
     */
    private void showButtons(String nodeId) {
        StoryData.Node node = data.nodes.get(nodeId);
        if (node == null) return;

        // 找到那个 buttonTable
        // 简化做法：直接清空 contentTable 里最后一行再填
        // 更稳的做法是重新渲染（但会重置打字状态）
        // 这里我们重新渲染节点，但立即标记为已完成
        // 为避免复杂度，用下面的逻辑：
    }

    /**
     * 填充按钮
     */
    private void fillButtons(Table buttonTable, StoryData.Node node) {
        buttonTable.clearChildren();

        if (node.choices != null && node.choices.size > 0) {
            for (StoryData.Choice choice : node.choices) {
                buttonTable.button(choice.text, () -> {
                    showNode(choice.next);
                }).size(160f, 50f).pad(5f).row();
            }
        } else if (node.next != null) {
            buttonTable.button("继续", () -> {
                showNode(node.next);
            }).size(160f, 50f).row();
        } else {
            buttonTable.button("结束", () -> {
                handleReward(node);
                hide();
            }).size(160f, 50f).row();
        }
    }

    /**
     * 发放剧情奖励
     */
    private void handleReward(StoryData.Node node) {
        if (node.rewardItem != null && node.rewardAmount > 0) {
            // 待补：发放奖励
        }
    }
}