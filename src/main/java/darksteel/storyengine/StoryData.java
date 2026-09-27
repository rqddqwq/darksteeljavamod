package darksteel.storyengine;

import arc.struct.ObjectMap;
import arc.struct.Seq;

/**
 * 剧情数据结构
 * 纯数据类，只负责描述 JSON 里存了什么，不干别的
 */
public class StoryData {

    // 从哪个节点开始
    public String start;

    // 所有节点，用节点 ID 索引
    public ObjectMap<String, Node> nodes = new ObjectMap<>();

    /**
     * 单个剧情节点（一句话 or 一个选项）
     */
    public static class Node {
        public String speaker;   // 谁在说话
        public String text;      // 说什么
        public String next;      // 下一句的 ID（null 表示剧情结束）
        public Seq<Choice> choices;  // 分支选项（可选）

        // 奖励（可选）
        public String rewardItem;
        public int rewardAmount;
    }

    /**
     * 分支选项
     */
    public static class Choice {
        public String text;      // 按钮上显示的文字
        public String next;      // 点了之后跳到哪个节点
    }
}