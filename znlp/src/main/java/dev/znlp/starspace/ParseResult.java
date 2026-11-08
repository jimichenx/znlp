package dev.znlp.starspace;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析结果类，表示一个解析后的训练样本，包含左侧tokens、右侧tokens和可选的权重
 * <p>
 * 该类用于存储数据解析器解析后的训练样本，包含样本的权重、左侧特征、右侧特征等信息。
 * 在训练过程中，模型会使用这些信息进行参数更新。
 */
public class ParseResult {
    /** 样本权重，默认为 1.0 */
    public float weight = 1.0f;
    /** 左侧 tokens 列表，每个元素是一个 (token, 权重) 对 */
    public List<Utils.XPair> lhsTokens = new ArrayList<>();
    /** 右侧 tokens 列表，每个元素是一个 (token, 权重) 对 */
    public List<Utils.XPair> rhsTokens = new ArrayList<>();
    /** 右侧特征列表，用于分层数据格式 */
    public List<List<Utils.XPair>> rhsFeatures = new ArrayList<>();
}
