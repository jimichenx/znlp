package dev.znlp.newwords.engine;

import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.entity.WordInfo;
import dev.znlp.newwords.scorer.NewWordScorer;
import dev.znlp.segment.impl.dict.CoreDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 新词发现引擎接口
 *
 * <p>定义新词发现的核心流程：两轮扫描算法</p>
 * <ul>
 *   <li>第一轮扫描：统计基本频次</li>
 *   <li>第二轮扫描：计算互信息和信息熵</li>
 * </ul>
 *
 * @author jimichan
 */
public interface INewWordFindEngine {

    /**
     * 新词发现引擎配置类
     *
     * <p>封装新词发现引擎的核心参数，用于控制词组长度范围和最小出现频次。
     * 合理配置这些参数可以在发现质量和计算性能之间取得平衡。</p>
     *
     * <h3>参数选择建议</h3>
     * <ul>
     *   <li><b>minGroup</b>: 通常设为2-3，太小会产生大量单字噪音</li>
     *   <li><b>maxGroup</b>: 通常设为6-12，过大会增加计算量且长词组出现频次通常较低</li>
     *   <li><b>minOccurCount</b>: 根据语料规模调整，小语料(1MB以下)建议3-5，大语料(10MB以上)建议10-20</li>
     * </ul>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * // 使用默认配置
     * INewWordFindEngine engine = INewWordFindEngine.create();
     *
     * // 使用自定义配置
     * INewWordFindEngine.Config config = new INewWordFindEngine.Config(2, 8, 5);
     * INewWordFindEngine engine = INewWordFindEngine.create(config, true, coreDictionary);
     * }</pre>
     *
     * @param minGroup      发现词组的最小字符长度
     *
     *                      <p>控制新词发现时考虑的最短词组长度。
     *                      该值用于过滤掉过短的字符组合，避免产生无意义的单字或双字组合。</p>
     *
     *                      <p><b>取值范围</b>: 通常为 2-4</p>
     *                      <p><b>推荐值</b>: 2（用于发现常见词汇）或 3（用于发现专业术语）</p>
     *                      <p><b>注意事项</b>: 设置过小会导致大量噪音结果，设置过大可能遗漏短词</p>
     * @param maxGroup      发现词组的最大字符长度
     *
     *                      <p>控制新词发现时考虑的最长词组长度。
     *                      该值决定了算法搜索的上限范围，直接影响计算复杂度和内存消耗。</p>
     *
     *                      <p><b>取值范围</b>: 通常为 6-15</p>
     *                      <p><b>推荐值</b>: 8-12（平衡发现质量和性能）</p>
     *                      <p><b>注意事项</b>: 设置过大会显著增加计算时间，且长词组通常频次较低难以满足过滤条件</p>
     * @param minOccurCount 词组在语料中的最小出现次数
     *
     *                      <p>控制候选词组的频次过滤阈值。
     *                      只有出现次数达到该阈值的词组才会被保留为候选新词。</p>
     *
     *                      <p><b>取值范围</b>: 通常为 3-50，取决于语料规模</p>
     *                      <p><b>推荐值</b>:
     *                      <ul>
     *                      <li>小语料（1MB以下）: 3-5</li>
     *                      <li>中等语料（1-10MB）: 5-10</li>
     *                      <li>大语料（10MB以上）: 10-20</li>
     *                      </ul>
     *                      </p>
     *                      <p><b>注意事项</b>: 设置过低会保留大量低频噪音，设置过高可能遗漏有价值的低频新词</p>
     */
        record Config(int minGroup, int maxGroup, int minOccurCount) {
        /**
         * 创建新词发现配置
         *
         * @param minGroup      发现词组的最小字符长度（推荐值: 2-3）
         * @param maxGroup      发现词组的最大字符长度（推荐值: 8-12）
         * @param minOccurCount 词组在语料中的最小出现次数（推荐值: 根据语料规模调整）
         * @throws IllegalArgumentException 如果参数不满足 minGroup &lt;= maxGroup 且均为正数
         */
        public Config {
            if (minGroup <= 0 || maxGroup <= 0 || minOccurCount <= 0) {
                throw new IllegalArgumentException("所有参数必须为正数");
            }
            if (minGroup > maxGroup) {
                throw new IllegalArgumentException("minGroup 不能大于 maxGroup");
            }
        }

            /**
             * 获取发现词组的最小字符长度
             */
            @Override
            public int minGroup() {
                return minGroup;
            }

            /**
             * 获取发现词组的最大字符长度
             */
            @Override
            public int maxGroup() {
                return maxGroup;
            }

            /**
             * 获取词组在语料中的最小出现次数
             */
            @Override
            public int minOccurCount() {
                return minOccurCount;
            }

            /**
             * 返回默认配置
             *
             * <p>默认配置:
             * <ul>
             *   <li>minGroup: 3</li>
             *   <li>maxGroup: 12</li>
             *   <li>minOccurCount: 10</li>
             * </ul>
             * </p>
             *
             * @return 默认配置实例
             */
            public static Config defaultConfig() {
                return new Config(3, 12, 10);
            }

            @Override
            public String toString() {
                return String.format("Config{minGroup=%d, maxGroup=%d, minOccurCount=%d}",
                        minGroup, maxGroup, minOccurCount);
            }
        }

    /**
     * 创建系统默认的新词发现引擎
     *
     * <p>使用默认配置（minGroup=3, maxGroup=12, minOccurCount=10），
     * 不排除核心词典中的词汇，不使用核心词典。</p>
     *
     * @return 默认配置的新词发现引擎实例
     */
    static INewWordFindEngine create() {
        return create(Config.defaultConfig(), false, null);
    }

    /**
     * 创建自定义配置的新词发现引擎
     *
     * @param config          引擎配置参数
     * @param excludeCoreDict 是否排除核心词典中已存在的词汇
     * @param coreDictionary  核心词典实例（如果 excludeCoreDict 为 false，可传 null）
     * @return 自定义配置的新词发现引擎实例
     */
    static INewWordFindEngine create(Config config, boolean excludeCoreDict, CoreDictionary coreDictionary) {
        return new NewWordFindEngineOptimized(config, excludeCoreDict, coreDictionary);
    }

    /**
     * 第一轮扫描。该方法可以被调用多次，建议每次传入一篇文章。
     * 第一轮主要完成基本统计
     *
     * @param document 文档内容
     * @throws IOException 读取异常
     */
    void firstScan(String document) throws IOException;

    /**
     * 完成第一轮扫描，构建候选词典
     */
    void finishFirst();

    /**
     * 第二轮扫描。该方法可以被调用多次，建议每次传入一篇文章。
     * 根据第一轮的基本统计，采用互信息和信息熵的方式计算新词
     *
     * @param document 文档内容
     * @throws IOException 读取异常
     */
    void secondScan(String document) throws IOException;

    /**
     * 结束第二轮扫描，计算各项统计指标
     */
    void endSecond();

    /**
     * 返回最终的结果，使用当前设置的打分器（包含过滤和打分功能），按score排序
     * <p>调用前必须先通过 setScorer() 设置打分器</p>
     *
     * @return 新词列表
     */
    ArrayList<NewWord> result();

    /**
     * 获取当前打分器（包含过滤和打分功能）
     */
    NewWordScorer getScorer();

    /**
     * 设置打分器（包含过滤和打分功能）
     */
    void setScorer(NewWordScorer scorer);

    /**
     * 获取文档数量
     */
    int getDocCount();

    /**
     * 获取总字数
     */
    int getZiCountTotal();

    /**
     * 获取字符频率数组
     */
    int[] getZiFreqArray();

    /**
     * 获取片段数量
     */
    int getPartCount();

    /**
     * 获取候选词Map
     */
    HashMap<String, WordInfo> getCandidateMap();

    /**
     * 是否开启详细日志
     */
    boolean isVerbose();

    /**
     * 设置是否开启详细日志
     */
    void setVerbose(boolean verbose);
}
