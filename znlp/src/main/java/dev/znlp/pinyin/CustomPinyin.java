package dev.znlp.pinyin;

import java.util.Map;
import java.util.TreeMap;

/**
 * 自定义拼音管理器
 * <p>
 * 负责管理用户自定义的拼音映射，支持添加、移除和查询自定义拼音规则，
 * 使用TreeMap保证映射的有序性，便于后续处理和调试
 *
 * @author jimichan
 */
public class CustomPinyin {

    /**
     * 自定义拼音映射表，使用TreeMap保持有序
     */
    private final Map<String, String> customPinyinMap = new TreeMap<>();

    /**
     * 添加自定义拼音映射
     *
     * @param text   要映射的文本
     * @param pinyin 对应的拼音字符串，多个拼音用逗号分隔
     */
    public void put(String text, String pinyin) {
        customPinyinMap.put(text, pinyin);
    }

    /**
     * 移除自定义拼音映射
     *
     * @param text 要移除的文本
     */
    public void remove(String text) {
        customPinyinMap.remove(text);
    }

    /**
     * 获取自定义拼音映射表
     *
     * @return 自定义拼音映射表
     */
    public Map<String, String> getMap() {
        return customPinyinMap;
    }
}
