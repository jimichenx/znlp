package dev.znlp.segment.impl.personname;

import dev.znlp.segment.Nature;
import dev.znlp.segment.graph.Vertex;
import dev.znlp.segment.graph.VertexRow;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.pipeline.BaseSegmentComponent;
import dev.znlp.segment.pipeline.WordSplitAlgorithm;

import java.util.List;

/**
 * 采用感知机制作的人名识别模型。
 * <p>
 * 这个切分算法，为了配合词典分词算法。
 * 我们在构造词图阶段就提取人名。
 * <p>
 * 对于长度<=3的人名，可能性高，作为初始词汇添加到词图中，防止被切断。
 * 例如："陈宝奇怪别人不好" 这种情况，如果不在词图阶段添加"陈宝"，可能会被错误切分。
 *
 * @author jimichan
 */
public class PersonNameAlgorithm extends BaseSegmentComponent implements WordSplitAlgorithm {

    public static final String CONTEXT_KEY = "__person_name__";

    private final PersonNamePerceptron perceptron;

    /**
     * 构造函数
     *
     * @param perceptron 人名识别感知机，通过ServiceLocator注入
     */
    public PersonNameAlgorithm(PersonNamePerceptron perceptron) {
        super(LEVEL3);
        this.perceptron = perceptron;
    }

    @Override
    public void fill(Wordnet wordnet) {
        List<PersonName> names = perceptron.findPersonName(wordnet.getCharArray(), wordnet.origin(), wordnet.length());

        // 将人名列表存储到词图上下文中，供Processor使用
        wordnet.set(CONTEXT_KEY, names);

        if (!names.isEmpty()) {
            for (PersonName name : names) {
                // 人名<=3，可能性高，作为初始词汇。防止被切断。陈宝奇怪别人不好
                if (name.length() <= 3) {
                    int offset = name.offset();
                    int length = name.length();
                    // 如果已经存在该长度的词节点，跳过
                    if (wordnet.row(offset).contains(length)) {
                        continue;
                    }
                    // 若人名切开了更早位置的词典词（如「长江」vs「江大桥」），不要强插
                    if (overlapsPriorDictionaryWord(wordnet, offset, length)) {
                        continue;
                    }
                    // 创建新的词节点并设置词性为人名
                    Vertex v = wordnet.acquire(length, -1, 0);
                    v.setAbsWordNatureAndFreq(Nature.nr);
                    wordnet.put(offset, v);
                }
            }
        }
    }

    /**
     * 人名跨度是否切开了更早位置、长度>=2 的已有词。
     * 例如「长江大桥」中已有「长江」，不应再插入「江大桥」。
     */
    private static boolean overlapsPriorDictionaryWord(Wordnet wordnet, int offset, int length) {
        int nameEnd = offset + length;
        int from = Math.max(0, offset - 8);
        for (int i = from; i < offset; i++) {
            VertexRow row = wordnet.row(i);
            int rowSize = row.size();
            for (int vIndex = 0; vIndex < rowSize; vIndex++) {
                Vertex v = row.vertexAt(vIndex);
                if (v.length >= 2) {
                    int vertexEnd = i + v.length;
                    if (vertexEnd > offset && i < nameEnd) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
