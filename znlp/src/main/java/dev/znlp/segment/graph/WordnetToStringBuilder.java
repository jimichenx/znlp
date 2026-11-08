package dev.znlp.segment.graph;

import java.util.BitSet;

/**
 * 格式化输出 wordnet 的可视化工具
 * 
 * <p>使用清晰的文本格式展示词图结构，不依赖任何颜色工具。
 * 输出格式采用表格形式，每行显示一个字符位置的所有可能词。
 *
 * @author jimichan
 */
class WordnetToStringBuilder {

    private final Wordnet wordnet;
    private final boolean showAttr;

    WordnetToStringBuilder(Wordnet wordnet, boolean showAttr) {
        this.wordnet = wordnet;
        this.showAttr = showAttr;
    }

    @Override
    public String toString() {
        BitSet noover = this.wordnet.findNoOverWords();
        StringBuilder sb = new StringBuilder();
        char[] text = wordnet.getCharArray();

        // 计算列宽，使输出对齐
        int indexWidth = calculateIndexWidth();
        int charWidth = 3; // 字符列宽度

        // 打印表头
        sb.append(String.format("%-" + indexWidth + "s | %-" + charWidth + "s | %s\n", 
            "位置", "字符", "词节点"));
        sb.append(String.format("%-" + indexWidth + "s-+-%-" + charWidth + "s-+-%s\n",
            repeat("-", indexWidth), repeat("-", charWidth), repeat("-", 30)));

        // 遍历所有位置
        for (int i = -1; i <= wordnet.getCharSizeLength(); i++) {
            VertexRow row = wordnet.row(i);
            
            // 格式化位置索引
            String indexStr = formatIndex(i, indexWidth);
            
            // 格式化字符
            String charStr = formatChar(i, text, charWidth);
            
            // 格式化词节点
            String vertexStr = formatVertices(i, row, text, noover);
            
            sb.append(String.format("%-" + indexWidth + "s | %-" + charWidth + "s | %s\n",
                indexStr, charStr, vertexStr));
        }

        return sb.toString();
    }

    /**
     * 计算位置索引列的宽度
     */
    private int calculateIndexWidth() {
        int maxIndex = wordnet.getCharSizeLength();
        // 考虑 -1 和 maxIndex 的情况，至少需要 4 个字符宽度
        return Math.max(4, String.valueOf(maxIndex).length() + 1);
    }

    /**
     * 格式化位置索引
     */
    private String formatIndex(int i, int width) {
        if (i == -1) {
            return String.format("%-" + width + "s", "BEGIN");
        } else if (i >= wordnet.getCharSizeLength()) {
            return String.format("%-" + width + "s", "END");
        } else {
            return String.format("%-" + width + "d", i);
        }
    }

    /**
     * 格式化字符显示
     */
    private String formatChar(int i, char[] text, int width) {
        char char_;
        if (i >= 0 && i < text.length) {
            char_ = text[i];
        } else {
            if (i == -1) {
                char_ = '◄'; // 起始标记
            } else {
                char_ = '►'; // 结束标记
            }
        }
        return String.format("%-" + width + "c", char_);
    }

    /**
     * 格式化词节点列表
     */
    private String formatVertices(int i, VertexRow row, char[] text, BitSet noover) {
        if (row.isEmpty()) {
            // 空行：根据是否被覆盖显示不同标记
            if (i >= 0 && i < text.length && !noover.get(i)) {
                return "NULL ⚠"; // 未被覆盖的空行（警告）
            } else {
                return "NULL"; // 正常的空行
            }
        }

        if (i == -1) {
            return "BEGIN";
        } else if (i >= text.length) {
            return "END";
        }

        // 格式化多个词节点
        StringBuilder vertexStr = new StringBuilder();
        int count = 0;
        for (Vertex v : row) {
            if (count > 0) {
                vertexStr.append("  |  "); // 多个词节点之间的分隔符
            }

            // 原始词文本
            int abs = wordnet.origin() + i;
            if (i >= 0 && abs + v.length <= text.length) {
                vertexStr.append(new String(text, abs, v.length));
            } else {
                vertexStr.append("?");
            }

            // 抽象词标记
            if (v.isAbsWord()) {
                vertexStr.append("[").append(v.absWordLabel()).append("]");
            }

            // 词性信息（如果启用）
            if (showAttr && v.nature != null) {
                vertexStr.append("/").append(v.nature.name());
            }

            // 权重信息
            if (v.weight != 0) {
                vertexStr.append("(").append(formatWeight(v.weight)).append(")");
            }

            count++;
        }

        return vertexStr.toString();
    }

    /**
     * 格式化权重值
     */
    private String formatWeight(double weight) {
        if (weight == (long) weight) {
            return String.valueOf((long) weight);
        } else {
            return String.format("%.2f", weight);
        }
    }

    /**
     * 重复字符串
     */
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
