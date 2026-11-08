package dev.znlp.pinyin;

import dev.znlp.pinyin.enums.SimplePinyin;
import dev.znlp.pinyin.spliter.PinyinSplitPerceptron;

import java.util.List;

/**
 * 拼音服务类
 * <p>
 * 提供对外的拼音转换接口，负责协调内部组件完成文本到拼音的转换，
 * 支持系统词典和用户自定义词典的拼音转换功能
 *
 * @author jimichan
 */
public final class PinyinService {

    /**
     * 内置拼音词典
     */
    private final PinyinDict innerDict;

    /**
     * 自定义拼音管理器
     */
    private final CustomPinyin customPinyin;

    /**
     * 文本转拼音计算机
     */
    private final Tex2PinyinComputer computer;

    private final PinyinSplitPerceptron splitPerceptron;

    /**
     * 构造方法
     *
     * @param innerDict 内置拼音词典
     */
    public PinyinService(PinyinDict innerDict, PinyinSplitPerceptron splitPerceptron) {
        this.innerDict = innerDict;
        this.customPinyin = new CustomPinyin();
        this.splitPerceptron = splitPerceptron;
        this.computer = new Tex2PinyinComputer(innerDict, Tex2PinyinComputer.convertToPinyinArrayMap(customPinyin.getMap()));
    }

    /**
     * 重建拼音转换引擎
     * 当自定义拼音规则发生变化时，需要调用此方法刷新转换引擎
     */
    public void rebuild() {
        computer.setCustomPinyin(Tex2PinyinComputer.convertToPinyinArrayMap(customPinyin.getMap()));
    }

    public List<String> split(String text) {
        return splitPerceptron.split(text);
    }

    public List<String> split(char[] textArray) {
        return splitPerceptron.split(textArray);
    }

    /**
     * 将文本转换为拼音
     *
     * @param text 要转换的文本
     * @return 拼音转换结果
     */
    public PinyinResult convert(String text) {
        return computer.convert(text);
    }

    /**
     * 查询单个字符的拼音
     *
     * @param ch 要查询的字符
     * @return 字符对应的拼音数组，不存在则返回 null
     */
    public SimplePinyin[] charPinyin(char ch) {
        return innerDict.charPinyin(ch);
    }

    /**
     * 获取自定义拼音管理器
     *
     * @return 自定义拼音管理器
     */
    public CustomPinyin getCustomPinyin() {
        return customPinyin;
    }
}
