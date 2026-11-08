package dev.znlp.segment.impl.atom;

import dev.znlp.segment.Nature;

/**
 * 原子识别结果
 * <p>
 * 表示在文本中识别到的一个原子模式，包含位置、长度和类型信息。
 *
 * @author jimichan
 */
public class AtomRecognitionResult {

    /**
     * 在文本中的起始位置
     */
    private final int offset;

    /**
     * 识别到的文本长度
     */
    private final int length;

    /**
     * 识别类型（模板类型）
     */
    private final TemplateType templateType;

    /**
     * 对应的词性
     */
    private final Nature nature;

    /**
     * 构造识别结果
     *
     * @param offset       起始位置
     * @param length       长度
     * @param templateType 模板类型
     */
    public AtomRecognitionResult(int offset, int length, TemplateType templateType) {
        this.offset = offset;
        this.length = length;
        this.templateType = templateType;
        this.nature = mapTemplateTypeToNature(templateType);
    }

    /**
     * 将模板类型映射到词性
     */
    private static Nature mapTemplateTypeToNature(TemplateType templateType) {
        switch (templateType) {
            case TIME:
                return Nature.t;
            case NUMBER:
                return Nature.m;
            case MQ:
                return Nature.mq;
            case WORD:
                return Nature.x;
            case CONNECT:
                return null; // 连接符不需要词性
            default:
                return Nature.x;
        }
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public Nature getNature() {
        return nature;
    }

    @Override
    public String toString() {
        return String.format("AtomRecognitionResult{offset=%d, length=%d, type=%s, nature=%s}",
                offset, length, templateType, nature);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AtomRecognitionResult that = (AtomRecognitionResult) o;
        return offset == that.offset &&
                length == that.length &&
                templateType == that.templateType;
    }

    @Override
    public int hashCode() {
        int result = offset;
        result = 31 * result + length;
        result = 31 * result + (templateType != null ? templateType.hashCode() : 0);
        return result;
    }
}

