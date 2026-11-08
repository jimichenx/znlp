package dev.znlp;

import org.jetbrains.annotations.NotNull;
import dev.znlp.base.service.ServiceNotFoundException;
import dev.znlp.converter.TransformService;
import dev.znlp.duckling.DucklingParser;
import dev.znlp.duckling.support.Dimension;
import dev.znlp.highlight.Fragment;
import dev.znlp.highlight.SmartHighlightConfig;
import dev.znlp.highlight.SmartHighlighter;
import dev.znlp.newwords.NewWordDiscoveryTool;
import dev.znlp.newwords.engine.INewWordFindEngine;
import dev.znlp.newwords.entity.NewWord;
import dev.znlp.newwords.scorer.NewWordScorerFactory;
import dev.znlp.pinyin.PinyinResult;
import dev.znlp.pinyin.PinyinService;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.impl.personname.PersonNamePerceptron;
import dev.znlp.segment.recipe.SegmentRecipe;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 进程级默认入口。分词、拼音、繁简转换、结构化解析和高亮从这里调用。
 * <p>
 * {@link #segment} 固定使用 {@link SegmentRecipe#standard()}：词典、二元、人名、词性。
 * NER、用户词典、纠错和感知机分词从 {@link #recipe()} 继续组装，不要把租户词表放进这个类。
 *
 * @author jimichan
 */
public final class Znlp {

    /**
     * 默认的高亮配置实例（启用拼音匹配和模糊匹配，高亮标签为 "em"）。
     * 由于配置对象在 {@link SmartHighlighter} 中只被读取使用，可以安全复用。
     */
    private static final SmartHighlightConfig DEFAULT_HIGHLIGHT_CONFIG = new SmartHighlightConfig();

    /**
     * 私有构造方法，防止实例化。
     */
    private Znlp() {
        // no instances
    }

    /**
     * 从进程级 {@link ZnlpSystem} 取服务。组合分词用 {@link SegmentRecipe}，不要靠这个方法拼流水线。
     */
    public static @NotNull <T> T getService(@NotNull Class<T> serviceClass) {
        return ZnlpSystem.getInstance().getService(serviceClass);
    }

    /**
     * 默认配方：词典、原子识别、人名、二元语法、词性。可继续 {@code withNer()}、{@code withUserDictionary} 等。
     */
    public static @NotNull SegmentRecipe recipe() {
        return SegmentRecipe.standard();
    }

    /**
     * 默认分词器，等于 {@code SegmentRecipe.standard().build()}。惰性创建，可多线程复用。
     */
    public static @NotNull WordSegmenter segmenter() {
        return DefaultSegmenterHolder.INSTANCE;
    }

    /**
     * 用默认分词器切分。返回带词性和偏移的词；空串返回空列表。{@code text} 为 {@code null} 时抛出 {@link IllegalArgumentException}。
     */
    public static @NotNull List<Token> segment(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return segmenter().segment(text);
    }

    /**
     * 用默认分词器切分，只返回词面。空串返回空列表。{@code text} 为 {@code null} 时抛出 {@link IllegalArgumentException}。
     */
    public static @NotNull List<String> segmentToStrings(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return segmenter().segmentToStrings(text);
    }

    /**
     * 转成拼音结果，可继续调用 {@link PinyinResult#asString()}、{@link PinyinResult#asList()}。
     *
     * @throws IllegalArgumentException 如果 {@code text} 为 {@code null}
     * @throws ServiceNotFoundException 如果未注册拼音服务
     */
    public static PinyinResult pinyin(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        PinyinService service = getPinyinService();
        return service.convert(text);
    }

    /**
     * 转成带声调的拼音，音节之间用空格分隔。
     *
     * @throws IllegalArgumentException 如果 {@code text} 为 {@code null}
     * @throws ServiceNotFoundException 如果未注册拼音服务
     */
    public static String pinyinString(String text) {
        return pinyin(text).asString();
    }

    /**
     * 转成拼音首字母，字母之间用空格分隔。
     *
     * @throws IllegalArgumentException 如果 {@code text} 为 {@code null}
     * @throws ServiceNotFoundException 如果未注册拼音服务
     */
    public static String pinyinHeadString(String text) {
        return pinyin(text).asHeadString();
    }

    /**
     * 简体转繁体。{@code text} 为 {@code null} 时抛出 {@link IllegalArgumentException}。
     *
     * @throws ServiceNotFoundException 如果未注册繁简转换服务
     */
    public static String s2t(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return getTransformService().s2t(text);
    }

    /**
     * 繁体转简体。{@code text} 为 {@code null} 时抛出 {@link IllegalArgumentException}。
     *
     * @throws ServiceNotFoundException 如果未注册繁简转换服务
     */
    public static String t2s(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return getTransformService().t2s(text);
    }

    /**
     * 解析数字、金额、时间、电话等结构化片段。不进入分词。参考时间取当前时刻。没有命中时返回空列表。
     */
    public static @NotNull List<Dimension> parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return DucklingParser.getInstance().parse(text);
    }

    /**
     * 同 {@link #parse(String)}，并用 {@code clock} 解释「明天」这类相对时间。
     */
    public static @NotNull List<Dimension> parse(String text, LocalDateTime clock) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        return DucklingParser.getInstance().parse(text, clock);
    }

    /**
     * 在目录上发现新词。使用默认配置和自适应打分，并排除核心词典里已有的词。结果按分数从高到低。
     *
     * @param corpusDir 语料目录，其下文本文件会被递归扫描
     * @throws NullPointerException     如果 {@code corpusDir} 为 {@code null}
     * @throws RuntimeException         如果目录不存在或无法读取
     * @throws ServiceNotFoundException 如果未注册核心词典
     */
    public static List<NewWord> discoverNewWords(File corpusDir) {
        Objects.requireNonNull(corpusDir, "corpusDir must not be null");

        ZnlpSystem system = ZnlpSystem.getInstance();
        CoreDictionary coreDictionary = system.getService(CoreDictionary.class);

        INewWordFindEngine.Config config = INewWordFindEngine.Config.defaultConfig();
        boolean excludeCoreDict = true;

        INewWordFindEngine engine = INewWordFindEngine.create(config, excludeCoreDict, coreDictionary);
        engine.setScorer(NewWordScorerFactory.createAdaptive());

        NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(corpusDir, engine)
                .charset(StandardCharsets.UTF_8)
                .verbose(false)
                .build();

        return tool.discover();
    }

    /**
     * 同 {@link #discoverNewWords(File)}。{@code corpusDirPath} 为 {@code null} 时抛出 {@link NullPointerException}。
     */
    public static List<NewWord> discoverNewWords(String corpusDirPath) {
        Objects.requireNonNull(corpusDirPath, "corpusDirPath must not be null");
        return discoverNewWords(new File(corpusDirPath));
    }

    /**
     * 按查询给文本加高亮标签。默认开启拼音和模糊匹配，标签是 {@code em}。空查询返回原文。
     *
     * @throws IllegalArgumentException 如果 {@code text} 或 {@code query} 为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static String highlight(String text, String query) {
        return highlight(text, query, DEFAULT_HIGHLIGHT_CONFIG);
    }

    /**
     * 同 {@link #highlight(String, String)}，匹配和标签由 {@code config} 决定。
     *
     * @throws IllegalArgumentException 如果任一参数为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static String highlight(String text, String query, SmartHighlightConfig config) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        SmartHighlighter service = getSmartHighlighter();
        return service.highlight(text, query, config);
    }

    /**
     * 批量高亮。查询只预处理一次，结果顺序与 {@code texts} 相同。默认配置同 {@link #highlight(String, String)}。
     *
     * @throws IllegalArgumentException 如果 {@code texts} 或 {@code query} 为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static List<String> highlight(List<String> texts, String query) {
        return highlight(texts, query, DEFAULT_HIGHLIGHT_CONFIG);
    }

    /**
     * 同 {@link #highlight(List, String)}，匹配和标签由 {@code config} 决定。
     *
     * @throws IllegalArgumentException 如果任一参数为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static List<String> highlight(List<String> texts, String query, SmartHighlightConfig config) {
        if (texts == null) {
            throw new IllegalArgumentException("texts must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        SmartHighlighter service = getSmartHighlighter();
        return service.highlight(texts, query, config);
    }

    /**
     * 按查询把文本切成 {@link Fragment}。和 {@link #highlight(String, String)} 使用同一套匹配，不生成 HTML。
     * 空文本返回空列表，空查询返回一段未高亮的原文。
     */
    public static List<Fragment> highlightFragments(String text, String query) {
        return highlightFragments(text, query, DEFAULT_HIGHLIGHT_CONFIG);
    }

    /**
     * 同 {@link #highlightFragments(String, String)}，匹配由 {@code config} 决定。
     *
     * @throws IllegalArgumentException 如果任一参数为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static List<Fragment> highlightFragments(String text, String query, SmartHighlightConfig config) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        SmartHighlighter service = getSmartHighlighter();
        return service.highlightFragments(text, query, config);
    }

    /**
     * 批量切成高亮片段。查询只预处理一次。
     */
    public static List<List<Fragment>> highlightFragments(List<String> texts, String query) {
        return highlightFragments(texts, query, DEFAULT_HIGHLIGHT_CONFIG);
    }

    /**
     * 同 {@link #highlightFragments(List, String)}，匹配由 {@code config} 决定。
     *
     * @throws IllegalArgumentException 如果任一参数为 {@code null}
     * @throws ServiceNotFoundException 如果未注册高亮服务
     */
    public static List<List<Fragment>> highlightFragments(List<String> texts, String query, SmartHighlightConfig config) {
        if (texts == null) {
            throw new IllegalArgumentException("texts must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        SmartHighlighter service = getSmartHighlighter();
        return service.highlightFragments(texts, query, config);
    }

    /**
     * 默认分词器使用的人名模型。要改切分请用 {@link SegmentRecipe}，不要把这个模型当成另一种分词器。
     */
    public static PersonNamePerceptron getPersonNamePerceptron() {
        return getService(PersonNamePerceptron.class);
    }

    private static PinyinService getPinyinService() {
        ZnlpSystem system = ZnlpSystem.getInstance();
        return system.getService(PinyinService.class);
    }

    private static SmartHighlighter getSmartHighlighter() {
        ZnlpSystem system = ZnlpSystem.getInstance();
        return system.getService(SmartHighlighter.class);
    }

    private static TransformService getTransformService() {
        ZnlpSystem system = ZnlpSystem.getInstance();
        return system.getService(TransformService.class);
    }

    private static final class DefaultSegmenterHolder {
        private static final WordSegmenter INSTANCE = SegmentRecipe.standard().build();
    }
}
