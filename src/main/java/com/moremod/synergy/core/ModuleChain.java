package com.moremod.synergy.core;

import java.util.*;

/**
 * 模块链结构 - 为未来 GUI 拖拽连线预留
 *
 * 说明：
 * - 表示模块之间的链接关系（有向图）
 * - 支持线性链（A → B → C）和复杂图（A → B, A → C, B → D）
 * - 可序列化为 JSON，便于保存和加载
 *
 * 设计思路：
 * - GUI 中用户拖动模块节点，用连线连接
 * - 保存时序列化为 ModuleChain
 * - 加载时反序列化，并检查是否满足激活条件
 *
 * 示例：
 * - 线性链："MAGIC_ABSORB" → "NEURAL_SYNCHRONIZER" → "ENERGY_EFFICIENCY"
 * - 分支链："DAMAGE_BOOST" → "ATTACK_SPEED", "DAMAGE_BOOST" → "CRITICAL_HIT"
 */
public class ModuleChain {

    /**
     * 节点 - 代表一个模块
     */
    public static class Node {
        private final String moduleId;
        private final List<Node> next;

        public Node(String moduleId) {
            this.moduleId = moduleId;
            this.next = new ArrayList<>();
        }

        public String getModuleId() {
            return moduleId;
        }

        public List<Node> getNext() {
            return Collections.unmodifiableList(next);
        }

        public void addNext(Node node) {
            if (!next.contains(node)) {
                next.add(node);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(moduleId, node.moduleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleId);
        }

        @Override
        public String toString() {
            return moduleId;
        }
    }

    private final List<Node> roots;     // 根节点列表（链的起点）
    private final Set<String> allModules; // 所有涉及的模块 ID

    private ModuleChain(List<Node> roots) {
        this.roots = Collections.unmodifiableList(new ArrayList<>(roots));
        this.allModules = new HashSet<>();
        for (Node root : roots) {
            collectModules(root, allModules);
        }
    }

    private void collectModules(Node node, Set<String> collector) {
        if (node == null) return;
        collector.add(node.getModuleId());
        for (Node next : node.next) {
            collectModules(next, collector);
        }
    }

    public List<Node> getRoots() {
        return roots;
    }

    public Set<String> getAllModuleIds() {
        return Collections.unmodifiableSet(allModules);
    }

    /**
     * 检查链是否完整（所有节点的模块都已安装）
     *
     * @param installedModules 已安装的模块 ID 集合
     * @return true 表示链完整
     */
    public boolean isComplete(Set<String> installedModules) {
        return installedModules.containsAll(allModules);
    }

    /**
     * 获取线性链的字符串表示（仅适用于简单链）
     *
     * @return 如 "A → B → C"
     */
    public String toLinearString() {
        if (roots.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder();
        toLinearString(roots.get(0), sb, new HashSet<>());
        return sb.toString();
    }

    private void toLinearString(Node node, StringBuilder sb, Set<Node> visited) {
        if (node == null || visited.contains(node)) return;
        visited.add(node);

        if (sb.length() > 0) sb.append(" → ");
        sb.append(node.getModuleId());

        if (!node.next.isEmpty()) {
            toLinearString(node.next.get(0), sb, visited);
        }
    }

    @Override
    public String toString() {
        return "ModuleChain{" + toLinearString() + "}";
    }

    /**
     * 构建器
     */
    public static class Builder {
        private final List<Node> roots = new ArrayList<>();
        private final Map<String, Node> nodeMap = new HashMap<>();

        /**
         * 添加根节点
         *
         * @param moduleId 模块 ID
         * @return 节点对象
         */
        public Node addRoot(String moduleId) {
            Node node = getOrCreateNode(moduleId);
            if (!roots.contains(node)) {
                roots.add(node);
            }
            return node;
        }

        /**
         * 添加边（from → to）
         *
         * @param fromModuleId 起始模块 ID
         * @param toModuleId 目标模块 ID
         * @return Builder 自身（链式调用）
         */
        public Builder addEdge(String fromModuleId, String toModuleId) {
            Node from = getOrCreateNode(fromModuleId);
            Node to = getOrCreateNode(toModuleId);
            from.addNext(to);
            return this;
        }

        /**
         * 创建线性链（A → B → C）
         *
         * @param moduleIds 模块 ID 列表
         * @return Builder 自身
         */
        public Builder linear(String... moduleIds) {
            if (moduleIds.length == 0) return this;

            Node prev = addRoot(moduleIds[0]);
            for (int i = 1; i < moduleIds.length; i++) {
                Node current = getOrCreateNode(moduleIds[i]);
                prev.addNext(current);
                prev = current;
            }
            return this;
        }

        private Node getOrCreateNode(String moduleId) {
            return nodeMap.computeIfAbsent(moduleId, Node::new);
        }

        public ModuleChain build() {
            if (roots.isEmpty()) {
                throw new IllegalStateException("ModuleChain must have at least one root");
            }
            return new ModuleChain(roots);
        }
    }

    /**
     * 快捷创建线性链
     *
     * @param moduleIds 模块 ID 列表
     * @return ModuleChain 对象
     */
    public static ModuleChain linear(String... moduleIds) {
        return new Builder().linear(moduleIds).build();
    }
}
