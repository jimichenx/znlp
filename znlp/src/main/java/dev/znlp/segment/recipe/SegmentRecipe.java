package dev.znlp.segment.recipe;

import org.jetbrains.annotations.NotNull;
import dev.znlp.ZnlpSystem;
import dev.znlp.base.service.ServiceLocator;
import dev.znlp.segment.PipelineSegmenter;
import dev.znlp.segment.WordSegmenter;
import dev.znlp.segment.collector.SentenceCollectorBuilder;
import dev.znlp.segment.collector.SimpleWordTermCollector;
import dev.znlp.segment.impl.atom.AtomSplitAlgorithm;
import dev.znlp.segment.impl.bigram.BiGramTableDictionary;
import dev.znlp.segment.impl.bigram.ViterbiBestPathAlgorithm;
import dev.znlp.segment.impl.correction.CorrectionDictionary;
import dev.znlp.segment.impl.correction.CorrectionWordpathProcessor;
import dev.znlp.segment.impl.cws.CwsPerceptron;
import dev.znlp.segment.impl.cws.CwsSplitAlgorithm;
import dev.znlp.segment.impl.cws.LinearPathAlgorithm;
import dev.znlp.segment.impl.dict.CoreDictionary;
import dev.znlp.segment.impl.dict.CoreDictionarySplitAlgorithm;
import dev.znlp.segment.impl.ner.NERPerceptron;
import dev.znlp.segment.impl.ner.NerProcessor;
import dev.znlp.segment.impl.personname.PersonNameAlgorithm;
import dev.znlp.segment.impl.personname.PersonNamePerceptron;
import dev.znlp.segment.impl.personname.PersonNameProcessor;
import dev.znlp.segment.impl.pos.PosPerceptron;
import dev.znlp.segment.impl.pos.PosPerceptronProcessor;
import dev.znlp.segment.impl.rule.RuleBasedWordSplitAlgorithm;
import dev.znlp.segment.impl.rule.RuleManager;
import dev.znlp.segment.impl.userdict.UserDictionary;
import dev.znlp.segment.impl.userdict.UserDictionaryProcessor;
import dev.znlp.segment.pipeline.BestPathAlgorithm;
import dev.znlp.segment.pipeline.CandidateSource;
import dev.znlp.segment.pipeline.CharNormalize;
import dev.znlp.segment.pipeline.SpanConflict;
import dev.znlp.segment.pipeline.SpanConstraint;
import dev.znlp.segment.pipeline.SplitCandidate;
import dev.znlp.segment.pipeline.TokenAnnotator;
import dev.znlp.segment.pipeline.WordTermCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 分词配方。调用方只声明要哪些阶段，不排列插件、order 或服务容器。
 * <p>
 * {@link #dictionary()} 是核心词典、原子识别和二元 Viterbi。
 * {@link #standard()} 在此之上加人名和词性，也是 {@code Znlp.segment} 的默认配方。
 * {@link #perceptron()} 用感知机边界铺候选，再走线性路径，不跑二元语法。
 * <p>
 * 词从三条路进结果，不要都当成同一种词典：
 * <ul>
 *   <li>核心词典铺候选。Viterbi 用词的 {@code wordID}、词频和二元表。感知机配方不走这条选路。</li>
 *   <li>{@link #withRules} 也是铺候选。命中后把词频抬高，让 Viterbi 选中。新词没有二元统计。线性路径不看词频，所以规则只影响词典配方。</li>
 *   <li>{@link #withUserDictionary} 在选路之后合并。用户词的两端必须已经落在词边界上。它不进词图，不参与 Viterbi，也不能从别的词中间切开。词已经在核心词图里、Viterbi 把它切碎了，只要两端对齐，仍然会合并。</li>
 * </ul>
 * 三字及以内的人名随候选进入词图，靠 Viterbi 竞争。更长的人名在选路之后合并，同样不能切开两侧已有的词。
 * {@link #withCorrections} 也在选路之后，优先级高于用户词典，可以在一个跨度内部重切，同样不能切开两侧已经成词的边界。
 * <p>
 * 配方本身不可变。{@code with*} 返回新实例。{@link #build()} 得到可多线程复用的分词器。
 */
public final class SegmentRecipe {

    public enum PathKind {
        VITERBI,
        LINEAR
    }

    private final ServiceLocator locator;
    private final PathKind pathKind;
    private final boolean coreDictionary;
    private final boolean atom;
    private final boolean personNames;
    private final boolean pos;
    private final boolean ner;
    private final boolean indexTerms;
    private final List<UserDictionary> userDictionaries;
    private final CorrectionDictionary corrections;
    private final RuleManager rules;
    private final List<CandidateSource> extraCandidates;
    private final List<SpanConstraint> extraConstraints;
    private final List<TokenAnnotator> extraAnnotators;
    private final List<CharNormalize> normalizers;

    private SegmentRecipe(ServiceLocator locator,
                          PathKind pathKind,
                          boolean coreDictionary,
                          boolean atom,
                          boolean personNames,
                          boolean pos,
                          boolean ner,
                          boolean indexTerms,
                          List<UserDictionary> userDictionaries,
                          CorrectionDictionary corrections,
                          RuleManager rules,
                          List<CandidateSource> extraCandidates,
                          List<SpanConstraint> extraConstraints,
                          List<TokenAnnotator> extraAnnotators,
                          List<CharNormalize> normalizers) {
        this.locator = locator;
        this.pathKind = pathKind;
        this.coreDictionary = coreDictionary;
        this.atom = atom;
        this.personNames = personNames;
        this.pos = pos;
        this.ner = ner;
        this.indexTerms = indexTerms;
        this.userDictionaries = userDictionaries;
        this.corrections = corrections;
        this.rules = rules;
        this.extraCandidates = extraCandidates;
        this.extraConstraints = extraConstraints;
        this.extraAnnotators = extraAnnotators;
        this.normalizers = normalizers;
    }

    /**
     * 核心词典 + 原子识别 + 二元 Viterbi。不含人名和词性。
     */
    public static @NotNull SegmentRecipe dictionary() {
        return dictionary(ZnlpSystem.getInstance().registry());
    }

    public static @NotNull SegmentRecipe dictionary(@NotNull ServiceLocator locator) {
        Objects.requireNonNull(locator, "locator");
        return new SegmentRecipe(locator, PathKind.VITERBI,
                true, true, false, false, false, false,
                List.of(), null, null, List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 默认配方：词典、原子、人名、二元 Viterbi、词性。
     */
    public static @NotNull SegmentRecipe standard() {
        return standard(ZnlpSystem.getInstance().registry());
    }

    public static @NotNull SegmentRecipe standard(@NotNull ServiceLocator locator) {
        return dictionary(locator).withPersonNames().withPos();
    }

    /**
     * 感知机分词：CWS 边界加上原子补洞，线性路径，不跑二元语法。
     */
    public static @NotNull SegmentRecipe perceptron() {
        return perceptron(ZnlpSystem.getInstance().registry());
    }

    public static @NotNull SegmentRecipe perceptron(@NotNull ServiceLocator locator) {
        Objects.requireNonNull(locator, "locator");
        return new SegmentRecipe(locator, PathKind.LINEAR,
                false, true, false, false, false, false,
                List.of(), null, null, List.of(), List.of(), List.of(), List.of());
    }

    /**
     * 三字及以内的人名铺进词图，参与 Viterbi。更长的人名在选路之后合并，且不切开两侧已有的词。
     */
    public @NotNull SegmentRecipe withPersonNames() {
        return copy(pathKind, coreDictionary, atom, true, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe withoutPersonNames() {
        return copy(pathKind, coreDictionary, atom, false, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe withPos() {
        return copy(pathKind, coreDictionary, atom, personNames, true, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    /**
     * 关掉词性。NER 依赖词性，开着 NER 时这个调用不改变配方。
     */
    public @NotNull SegmentRecipe withoutPos() {
        if (ner) {
            return this;
        }
        return copy(pathKind, coreDictionary, atom, personNames, false, false, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    /**
     * 打开 NER，并带上它依赖的词性标注。
     */
    public @NotNull SegmentRecipe withNer() {
        return copy(pathKind, coreDictionary, atom, personNames, true, true, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    /**
     * 选路之后合并用户词。两端都在已有词边界上才合并；卡在某个词中间的用户词不会改切分。
     */
    public @NotNull SegmentRecipe withUserDictionary(UserDictionary... dictionaries) {
        List<UserDictionary> merged = new ArrayList<>(userDictionaries);
        if (dictionaries != null) {
            for (UserDictionary dictionary : dictionaries) {
                if (dictionary != null) {
                    merged.add(dictionary);
                }
            }
        }
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                List.copyOf(merged), corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe withUserDictionary(@NotNull List<UserDictionary> dictionaries) {
        Objects.requireNonNull(dictionaries, "dictionaries");
        return withUserDictionary(dictionaries.toArray(UserDictionary[]::new));
    }

    /**
     * 选路之后按纠错规则重切。优先级高于用户词典，不能切开两侧已经成词的边界。
     */
    public @NotNull SegmentRecipe withCorrections(@NotNull CorrectionDictionary dictionary) {
        Objects.requireNonNull(dictionary, "dictionary");
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, dictionary, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    /**
     * 把规则命中铺进词图并抬高词频，参与 Viterbi。感知机的线性路径不看词频。
     */
    public @NotNull SegmentRecipe withRules(@NotNull RuleManager ruleManager) {
        Objects.requireNonNull(ruleManager, "ruleManager");
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, ruleManager, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    /**
     * 主切分不变，收集阶段附上索引子词。
     */
    public @NotNull SegmentRecipe indexTerms() {
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, true,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe addCandidate(@NotNull CandidateSource source) {
        Objects.requireNonNull(source, "source");
        List<CandidateSource> merged = new ArrayList<>(extraCandidates);
        merged.add(source);
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, rules, List.copyOf(merged), extraConstraints, extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe addConstraint(@NotNull SpanConstraint constraint) {
        Objects.requireNonNull(constraint, "constraint");
        List<SpanConstraint> merged = new ArrayList<>(extraConstraints);
        merged.add(constraint);
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, List.copyOf(merged), extraAnnotators, normalizers);
    }

    public @NotNull SegmentRecipe addAnnotator(@NotNull TokenAnnotator annotator) {
        Objects.requireNonNull(annotator, "annotator");
        List<TokenAnnotator> merged = new ArrayList<>(extraAnnotators);
        merged.add(annotator);
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, List.copyOf(merged), normalizers);
    }

    /**
     * 分词前的字符规范化。运行时复制输入，不改调用方的 {@code char[]}。
     * 规范化应保持长度不变，这样 offset 仍对应原文。
     */
    public @NotNull SegmentRecipe addNormalizer(@NotNull CharNormalize normalizer) {
        Objects.requireNonNull(normalizer, "normalizer");
        List<CharNormalize> merged = new ArrayList<>(normalizers);
        merged.add(normalizer);
        return copy(pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, List.copyOf(merged));
    }

    public @NotNull WordSegmenter build() {
        CoreDictionary dictionary = locator.getService(CoreDictionary.class);

        List<CandidateSource> sources = new ArrayList<>();
        if (pathKind == PathKind.LINEAR) {
            CwsPerceptron perceptron = locator.getService(CwsPerceptron.class);
            sources.add(new SplitCandidate(new CwsSplitAlgorithm(perceptron, dictionary), SpanConflict.REPLACE));
        }
        if (coreDictionary) {
            sources.add(new SplitCandidate(new CoreDictionarySplitAlgorithm(dictionary), SpanConflict.REPLACE));
        }
        if (personNames) {
            PersonNamePerceptron perceptron = locator.getService(PersonNamePerceptron.class);
            sources.add(new SplitCandidate(new PersonNameAlgorithm(perceptron), SpanConflict.FILL_IF_ABSENT));
        }
        if (rules != null) {
            sources.add(new SplitCandidate(new RuleBasedWordSplitAlgorithm(rules), SpanConflict.REPLACE));
        }
        sources.addAll(extraCandidates);
        if (atom) {
            AtomSplitAlgorithm algorithms = locator.getService(AtomSplitAlgorithm.class);
            sources.add(new SplitCandidate(algorithms.withConflict(SpanConflict.FILL_IF_ABSENT), SpanConflict.FILL_IF_ABSENT));
        }

        BestPathAlgorithm path;
        if (pathKind == PathKind.LINEAR) {
            path = new LinearPathAlgorithm();
        } else {
            BiGramTableDictionary bigram = locator.getService(BiGramTableDictionary.class);
            path = new ViterbiBestPathAlgorithm(bigram, dictionary);
        }

        List<SpanConstraint> constraints = new ArrayList<>();
        if (corrections != null) {
            constraints.add(new CorrectionWordpathProcessor(corrections));
        }
        if (!userDictionaries.isEmpty()) {
            constraints.add(new UserDictionaryProcessor(userDictionaries));
        }
        if (personNames) {
            constraints.add(new PersonNameProcessor(locator.getService(PersonNamePerceptron.class)));
        }
        constraints.addAll(extraConstraints);
        constraints.sort(Comparator.comparingInt(SpanConstraint::priority).reversed());

        List<TokenAnnotator> annotators = new ArrayList<>();
        if (pos || ner) {
            annotators.add(new PosPerceptronProcessor(locator.getService(PosPerceptron.class)));
        }
        if (ner) {
            annotators.add(new NerProcessor(locator.getService(NERPerceptron.class)));
        }
        annotators.addAll(extraAnnotators);

        WordTermCollector collector = indexTerms
                ? new SentenceCollectorBuilder().indexSubword().build()
                : new SimpleWordTermCollector();

        return new PipelineSegmenter(normalizers, sources, path, constraints, annotators, collector);
    }

    private SegmentRecipe copy(PathKind pathKind,
                               boolean coreDictionary,
                               boolean atom,
                               boolean personNames,
                               boolean pos,
                               boolean ner,
                               boolean indexTerms,
                               List<UserDictionary> userDictionaries,
                               CorrectionDictionary corrections,
                               RuleManager rules,
                               List<CandidateSource> extraCandidates,
                               List<SpanConstraint> extraConstraints,
                               List<TokenAnnotator> extraAnnotators,
                               List<CharNormalize> normalizers) {
        return new SegmentRecipe(locator, pathKind, coreDictionary, atom, personNames, pos, ner, indexTerms,
                userDictionaries, corrections, rules, extraCandidates, extraConstraints, extraAnnotators, normalizers);
    }
}
