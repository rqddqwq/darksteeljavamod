package darksteel.storyengine;

import arc.scene.ui.Dialog;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.gen.Call;

/**
 * 剧情对话框
 * 负责把 StoryData 里的节点渲染成 UI，并处理选项跳转
 */
public class StoryDialog extends Dialog {

    // 当前正在播放的剧情数据
    private final StoryData data;
    // 当前停留在哪个节点（比如 "intro"）
    private String currentNodeId;

    // UI 容器
    private Table contentTable;

    /**
     * 构造函数
     * @param data 从 StorySystem 里拿到的剧情数据
     * @param startNodeId 从哪个节点开始
     */
    public StoryDialog(StoryData data, String startNodeId) {
        super(""); // 弹窗标题
        this.data = data;
        this.currentNodeId = startNodeId;

        // 初始化 UI 结构
        setupUI();
        
        
    }
    public void open() {
    this.show();
}
    /**
     * 初始化基础 UI
     */
    private void setupUI() {
        // 清空旧内容
        cont.clear();
        cont.defaults().pad(10);

        // 滚动显示的正文区域
        contentTable = new Table();
        contentTable.defaults().pad(5);
        cont.add(contentTable).grow().top();

        // 底部关闭按钮（仅用于手动放弃剧情）


        // 开始渲染第一个节点
        showNode(currentNodeId);
    }

    /**
     * 渲染一个节点
     * @param nodeId 要渲染的节点 ID
     */
    private void showNode(String nodeId) {
        currentNodeId = nodeId;
        contentTable.clear();

        // 从数据里取出节点
        StoryData.Node node = data.nodes.get(nodeId);
        if (node == null) {
            Log.err("剧情引擎：节点 " + nodeId + " 不存在！");
            hide();
            return;
        }

        // 1. 显示说话人
        if (node.speaker != null && !node.speaker.isEmpty()) {
            contentTable.add("[accent]" + node.speaker + "[]").left().row();
            contentTable.add().size(5f).row(); // 空行
        }

        // 2. 显示对话文本（自动换行，宽度限制一下）
        contentTable.add(node.text).left().wrap().width(450f).row();
        contentTable.add().size(15f).row(); // 空行

        // 3. 处理接下来的动作
        if (node.choices != null && node.choices.size > 0) {
            // 有分支选项
            for (StoryData.Choice choice : node.choices) {
                contentTable.button(choice.text, () -> {
                    showNode(choice.next); // 点击跳转
                }).size(160f, 50f).pad(5f).row();
            }
        } else if (node.next != null) {
            // 单线继续
            contentTable.button("继续", () -> {
                showNode(node.next);
            }).size(160f, 50f).row();
        } else {
            // 剧情结束
            contentTable.button("结束", () -> {
                handleReward(node); // 发放奖励
                hide();             // 关闭弹窗
            }).size(160f, 50f).row();
        }
    }

    /**
     * 发放剧情奖励
     */
    private void handleReward(StoryData.Node node) {
        if (node.rewardItem != null && node.rewardAmount > 0) {
            // 这里先简单打印一下
            // 以后接上幸运方块或者别的模组时，换成真正的给物品逻辑
            Call.sendMessage("[green]剧情结束：获得了 " + node.rewardAmount + " 个 " + node.rewardItem + "！[]");
        }
    }
}

