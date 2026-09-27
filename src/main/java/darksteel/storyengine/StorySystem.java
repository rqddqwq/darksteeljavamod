package darksteel.storyengine;

import arc.struct.ObjectMap;
import arc.util.serialization.Jval;
import mindustry.Vars;

/**
 * 剧情系统核心
 * 负责读取 assets/stories.json，并把它解析成 StoryData 对象
 */
public class StorySystem {

    // 存放所有加载好的剧情，用 ID 作为索引（比如 "engineer_event"）
    public static ObjectMap<String, StoryData> stories = new ObjectMap<>();

    /**
     * 加载 JSON 文件（游戏启动时调用）
     */
    public static void load() {
        // 清空旧数据（方便以后热重载）
        stories.clear();

        // 从 assets 文件夹里拿到 stories.json
        var file = Vars.tree.get("assets/stories.json");
        if (!file.exists()) {
            // 文件不存在就报错退出，防止后续空指针
            arc.util.Log.err("剧情引擎：找不到 stories.json！");
            return;
        }

        try {
            // 1. 读取整个文件，并解析成 JSON 对象
            Jval json = Jval.read(file.readString());

            // 2. 遍历 JSON 里的每一个剧情（每个剧情的 ID 是它的键）
            for (String storyId : json.asObject().keys()) {
                Jval storyJson = json.get(storyId);
                StoryData data = new StoryData();
                
                // 读取起始节点
                data.start = storyJson.getString("start", "start");

                // 读取该剧情下的所有节点
                Jval nodesJson = storyJson.get("nodes");
                if (nodesJson != null) {
                    for (String nodeId : nodesJson.asObject().keys()) {
                        Jval nodeJson = nodesJson.get(nodeId);
                        StoryData.Node node = new StoryData.Node();
                        
                        node.speaker = nodeJson.getString("speaker", "");
                        node.text = nodeJson.getString("text", "");
                        node.next = nodeJson.getString("next", null);

                        // 解析分支选项（choices）
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

                        // 解析奖励（reward）
                        Jval rewardJson = nodeJson.get("reward");
                        if (rewardJson != null) {
                            node.rewardItem = rewardJson.getString("item", null);
                            node.rewardAmount = rewardJson.getInt("amount", 0);
                        }

                        // 把解析好的节点放进剧情数据里
                        data.nodes.put(nodeId, node);
                    }
                }

                // 把构建好的剧情放进全局映射表
                stories.put(storyId, data);
            }

            arc.util.Log.info("剧情引擎：成功加载 " + stories.size + " 段剧情。");

        } catch (Exception e) {
            arc.util.Log.err("剧情引擎：解析 stories.json 失败！", e);
        }
    }
}