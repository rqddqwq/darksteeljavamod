package darksteel.storyengine;

import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.serialization.Jval;
import mindustry.Vars;

/**
 * 剧情系统核心
 * 负责读取 assets/stories.json，并把它解析成 StoryData 对象
 */
public class StorySystem {

    // 存放所有加载好的剧情
    public static ObjectMap<String, StoryData> stories = new ObjectMap<>();

    /**
     * 加载 JSON 文件（游戏启动时调用）
     */
    public static void load() {
        stories.clear();

        var file = Vars.tree.get("assets/stories.json");
        if (!file.exists()) {
            Log.err("剧情引擎：找不到 stories.json！");
            return;
        }

        try {
            Jval json = Jval.read(file.readString());

            for (String storyId : json.asObject().keys()) {
                Jval storyJson = json.get(storyId);
                StoryData data = new StoryData();

                data.start = storyJson.getString("start", "start");

                Jval nodesJson = storyJson.get("nodes");
                if (nodesJson != null) {
                    for (String nodeId : nodesJson.asObject().keys()) {
                        Jval nodeJson = nodesJson.get(nodeId);
                        StoryData.Node node = new StoryData.Node();

                        node.speaker = nodeJson.getString("speaker", "");
                        node.text    = nodeJson.getString("text", "");
                        node.next    = nodeJson.getString("next", null);

                        // 选项
                        Jval choicesJson = nodeJson.get("choices");
                        if (choicesJson != null && choicesJson.isArray()) {
                            node.choices = new arc.struct.Seq<>();
                            for (Jval choiceJson : choicesJson.asArray()) {
                                StoryData.Choice choice = new StoryData.Choice();
                                choice.text = choiceJson.getString("text", "");
                                choice.next = choiceJson.getString("next", null);
                                node.choices.add(choice);
                            }
                        }

                        // 奖励
                        Jval rewardJson = nodeJson.get("reward");
                        if (rewardJson != null) {
                            node.rewardItem   = rewardJson.getString("item", null);
                            node.rewardAmount = rewardJson.getInt("amount", 0);
                        }

                        data.nodes.put(nodeId, node);
                    }
                }

                stories.put(storyId, data);
            }

            Log.info("[Story] 已加载 @ 个剧情", stories.size);

        } catch (Exception e) {
            Log.err("[Story] 解析 stories.json 失败", e);
        }
    }

    // ============================================================
    // ✅ 供 JS 调用：按 ID 打开剧情
    // ============================================================
    public static void openDialog(String storyId) {
        load();  // 确保已加载
        StoryData data = stories.get(storyId);
        if (data == null) {
            Vars.ui.showInfo("找不到剧情：" + storyId);
            return;
        }
        new StoryDialog(data, data.start).open();
    }
}