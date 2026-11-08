package dev.znlp.pinyin;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.pinyin.enums.Pinyin;
import dev.znlp.pinyin.enums.SimplePinyin;

import java.util.Map;

/**
 * 内置拼音词典
 * 提供拼音表和单字拼音快速查询功能
 *
 * @author jimichan
 */
public class PinyinDict {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PinyinDict.class);

    private final Map<String, Pinyin[]> pinyinTable;
    private final SimplePinyin[][] charPinyin;

    /**
     * 私有构造函数，使用静态工厂方法创建实例
     *
     * @param pinyinTable 拼音映射表
     * @param charPinyin  单字拼音索引
     */
    private PinyinDict(Map<String, Pinyin[]> pinyinTable, SimplePinyin[][] charPinyin) {
        this.pinyinTable = pinyinTable;
        this.charPinyin = charPinyin;
    }

    /**
     * 创建拼音词典实例
     *
     * @param resourceLoader 资源加载器
     * @return 拼音词典实例
     */
    public static PinyinDict create(NlpResourceLoader resourceLoader) {
        PinyinDictLoader loader = new PinyinDictLoader(resourceLoader);
        PinyinDictLoader.PinyinDictData data = loader.load();
        return new PinyinDict(data.pinyinTable(), data.charPinyin());
    }

    /**
     * 查询单个字符的拼音
     *
     * @param ch 字符
     * @return 字符对应的拼音数组，不存在则返回 null
     */
    public SimplePinyin[] charPinyin(char ch) {
        if ((int) ch >= charPinyin.length) {
            return null;
        }
        return charPinyin[ch];
    }

    /**
     * 获取完整拼音映射表
     *
     * @return 拼音映射表
     */
    public Map<String, Pinyin[]> getPinyinTable() {
        return pinyinTable;
    }
}
