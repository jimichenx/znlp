package dev.znlp.highlight;

/**
 * 智能高亮配置类（不可变）
 * <p>
 * 使用 {@link Builder} 创建自定义配置实例。
 *
 * @param highlightTag 高亮标签，默认为 "em"
 * @param enablePinyin 是否启用拼音匹配，默认为 true
 * @param enableFuzzy  是否启用模糊/非连续匹配，默认为 true
 * @author znlp
 */
public record SmartHighlightConfig(String highlightTag, boolean enablePinyin, boolean enableFuzzy) {

    /**
     * 使用默认配置的构造方法
     */
    public SmartHighlightConfig() {
        this("em", true, true);
    }

    /**
     * 全参构造方法
     *
     * @param highlightTag 高亮标签
     * @param enablePinyin 是否启用拼音匹配
     * @param enableFuzzy  是否启用模糊匹配
     */
    public SmartHighlightConfig(String highlightTag, boolean enablePinyin, boolean enableFuzzy) {
        this.highlightTag = highlightTag != null ? highlightTag : "em";
        this.enablePinyin = enablePinyin;
        this.enableFuzzy = enableFuzzy;
    }

    /**
     * 创建一个新的配置实例，修改高亮标签
     *
     * @param highlightTag 新的高亮标签
     * @return 新的配置实例
     */
    public SmartHighlightConfig withHighlightTag(String highlightTag) {
        return new SmartHighlightConfig(highlightTag, this.enablePinyin, this.enableFuzzy);
    }

    /**
     * 创建一个新的配置实例，修改拼音匹配设置
     *
     * @param enablePinyin 是否启用拼音匹配
     * @return 新的配置实例
     */
    public SmartHighlightConfig withEnablePinyin(boolean enablePinyin) {
        return new SmartHighlightConfig(this.highlightTag, enablePinyin, this.enableFuzzy);
    }

    /**
     * 创建一个新的配置实例，修改模糊匹配设置
     *
     * @param enableFuzzy 是否启用模糊匹配
     * @return 新的配置实例
     */
    public SmartHighlightConfig withEnableFuzzy(boolean enableFuzzy) {
        return new SmartHighlightConfig(this.highlightTag, this.enablePinyin, enableFuzzy);
    }

    /**
     * 配置构建器，用于创建自定义配置
     */
    public static class Builder {
        private String highlightTag = "em";
        private boolean enablePinyin = true;
        private boolean enableFuzzy = true;

        public Builder highlightTag(String highlightTag) {
            this.highlightTag = highlightTag;
            return this;
        }

        public Builder enablePinyin(boolean enablePinyin) {
            this.enablePinyin = enablePinyin;
            return this;
        }

        public Builder enableFuzzy(boolean enableFuzzy) {
            this.enableFuzzy = enableFuzzy;
            return this;
        }

        public SmartHighlightConfig build() {
            return new SmartHighlightConfig(highlightTag, enablePinyin, enableFuzzy);
        }
    }

    /**
     * 创建配置构建器
     *
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
}
