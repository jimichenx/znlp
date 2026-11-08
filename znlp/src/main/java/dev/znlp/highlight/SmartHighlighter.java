package dev.znlp.highlight;

import dev.znlp.pinyin.PinyinService;
import dev.znlp.pinyin.enums.SimplePinyin;

import java.util.*;

/**
 * 智能高亮服务
 *
 * @author jimichan
 */
public class SmartHighlighter {

    private final PinyinService pinyinService;

    public SmartHighlighter(PinyinService pinyinService) {
        this.pinyinService = pinyinService;
    }

    /**
     * Query 预处理上下文，用于批量处理时复用预计算结果
     */
    private static class QueryContext {
        final String safeQuery;
        final boolean usePinyin;
        final String lowerQuery;      // 拼音模式使用
        final Set<Character> queryChars; // 文本模式使用
        final boolean fuzzy;
        final String openTag;
        final String closeTag;

        QueryContext(String query, SmartHighlightConfig config) {
            this.safeQuery = query.trim();
            this.usePinyin = config.enablePinyin() && isPureAscii(safeQuery);
            this.lowerQuery = usePinyin ? safeQuery.toLowerCase() : null;
            this.queryChars = usePinyin ? null : buildQueryChars(safeQuery);
            this.fuzzy = config.enableFuzzy();
            this.openTag = "<" + config.highlightTag() + ">";
            this.closeTag = "</" + config.highlightTag() + ">";
        }

        boolean isEmpty() {
            return safeQuery.isEmpty();
        }

        private static Set<Character> buildQueryChars(String query) {
            Set<Character> chars = new HashSet<>();
            for (int i = 0; i < query.length(); i++) {
                chars.add(query.charAt(i));
            }
            return chars;
        }

        private static boolean isPureAscii(String s) {
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) > 127) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 高亮单个文本
     *
     * @param text   原始文本
     * @param query  查询词
     * @param config 配置
     * @return 高亮后的文本
     */
    public String highlight(String text, String query, SmartHighlightConfig config) {
        if (text == null || text.isEmpty() || query == null || query.isEmpty()) {
            return text;
        }
        QueryContext ctx = new QueryContext(query, config);
        return highlightWithContext(text, ctx);
    }

    /**
     * 批量高亮（性能优化版本）
     * <p>
     * 预处理 query 相关数据，批量处理时复用，避免重复计算。
     *
     * @param texts  原始文本列表
     * @param query  查询词
     * @param config 配置
     * @return 高亮后的文本列表，顺序与输入一致
     */
    public List<String> highlight(List<String> texts, String query, SmartHighlightConfig config) {
        if (texts == null || texts.isEmpty()) {
            return texts;
        }
        if (query == null || query.isEmpty()) {
            return new ArrayList<>(texts);
        }

        // 预处理 query，创建上下文
        QueryContext ctx = new QueryContext(query, config);
        if (ctx.isEmpty()) {
            return new ArrayList<>(texts);
        }

        // 批量处理，复用上下文
        List<String> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(highlightWithContext(text, ctx));
        }
        return results;
    }

    /**
     * 按查询把文本切成高亮片段。空文本返回空列表，空查询返回一段未高亮的原文。
     */
    public List<Fragment> highlightFragments(String text, String query, SmartHighlightConfig config) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isEmpty()) {
            return Collections.singletonList(new Fragment(text, false));
        }
        QueryContext ctx = new QueryContext(query, config);
        return fragmentsWithContext(text, ctx);
    }

    /**
     * 按查询把一批文本切成高亮片段。查询预处理只做一次。
     */
    public List<List<Fragment>> highlightFragments(List<String> texts, String query, SmartHighlightConfig config) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isEmpty()) {
            List<List<Fragment>> results = new ArrayList<>(texts.size());
            for (String text : texts) {
                results.add(Collections.singletonList(new Fragment(text, false)));
            }
            return results;
        }

        QueryContext ctx = new QueryContext(query, config);
        if (ctx.isEmpty()) {
            List<List<Fragment>> results = new ArrayList<>(texts.size());
            for (String text : texts) {
                results.add(Collections.singletonList(new Fragment(text, false)));
            }
            return results;
        }

        List<List<Fragment>> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(fragmentsWithContext(text, ctx));
        }
        return results;
    }

    /**
     * 使用预处理上下文进行高亮（返回字符串）
     */
    private String highlightWithContext(String text, QueryContext ctx) {
        if (text == null || text.isEmpty() || ctx.isEmpty()) {
            return text;
        }
        BitSet highlighted = computeHighlights(text, ctx);
        return buildResult(text, highlighted, ctx.openTag, ctx.closeTag);
    }

    private List<Fragment> fragmentsWithContext(String text, QueryContext ctx) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        if (ctx.isEmpty()) {
            return Collections.singletonList(new Fragment(text, false));
        }
        BitSet highlighted = computeHighlights(text, ctx);
        return buildFragments(text, highlighted);
    }

    /**
     * 计算高亮位图
     */
    private BitSet computeHighlights(String text, QueryContext ctx) {
        BitSet highlighted = new BitSet(text.length());
        if (ctx.usePinyin) {
            matchPinyin(text, ctx.lowerQuery, ctx.fuzzy, highlighted);
        } else {
            matchText(text, ctx.queryChars, highlighted);
        }
        return highlighted;
    }

    private void matchText(String text, Set<Character> queryChars, BitSet highlighted) {
        for (int i = 0; i < text.length(); i++) {
            if (queryChars.contains(text.charAt(i))) {
                highlighted.set(i);
            }
        }
    }

    private void matchPinyin(String text, String query, boolean fuzzy, BitSet highlighted) {
        int queryIndex = 0;
        int queryLen = query.length();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            SimplePinyin[] pinyins = pinyinService.charPinyin(ch);

            boolean matched = false;
            int matchedLen = 0;

            if (pinyins != null) {
                // 中文拼音匹配
                for (SimplePinyin p : pinyins) {
                    String pStr = p.name();
                    int pLen = pStr.length();
                    int remainingQueryLen = queryLen - queryIndex;

                    if (remainingQueryLen > 0) {
                        // 1. Query 剩余部分以 Pinyin 开头 (Query 较长或相等)
                        if (query.startsWith(pStr, queryIndex)) {
                            matched = true;
                            matchedLen = pLen;
                            break;
                        }
                        // 2. Pinyin 以 Query 剩余部分开头 (Query 较短)
                        else if (remainingQueryLen < pLen) {
                            // 检查 pStr 是否以 query.substring(queryIndex) 开头
                            // 即 pStr.regionMatches(0, query, queryIndex, remainingQueryLen)
                            if (pStr.regionMatches(0, query, queryIndex, remainingQueryLen)) {
                                matched = true;
                                matchedLen = remainingQueryLen;
                                break;
                            }
                        }
                    }
                }
            } else {
                // 非中文，单字符匹配
                char lowerCh = Character.toLowerCase(ch);
                if (queryIndex < queryLen && query.charAt(queryIndex) == lowerCh) {
                    matched = true;
                    matchedLen = 1;
                }
            }

            if (matched) {
                highlighted.set(i);
                queryIndex += matchedLen;
                if (queryIndex >= queryLen) {
                    queryIndex = 0; // 完成一次全匹配，重置以查找下一次
                }
            } else {
                if (!fuzzy) {
                    // 非模糊模式，断开匹配，重置
                    if (queryIndex > 0) {
                        queryIndex = 0;
                        // 回退一个字符，重新尝试匹配当前字符作为新起点
                        i--;
                    }
                }
                // 如果 fuzzy=true，则直接跳过当前字符 (Skip)，queryIndex 不变。
            }
        }
    }

    private String buildResult(String text, BitSet highlighted, String openTag, String closeTag) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            boolean isHilite = highlighted.get(i);
            boolean prevHilite = (i > 0) && highlighted.get(i - 1);
            boolean nextHilite = (i < text.length() - 1) && highlighted.get(i + 1);

            if (isHilite) {
                if (!prevHilite) {
                    sb.append(openTag);
                }
                sb.append(text.charAt(i));
                if (!nextHilite) {
                    sb.append(closeTag);
                }
            } else {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    private List<Fragment> buildFragments(String text, BitSet highlighted) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Fragment> fragments = new ArrayList<>();
        int start = 0;
        boolean currentHighlight = highlighted.get(0);

        for (int i = 1; i < text.length(); i++) {
            boolean h = highlighted.get(i);
            if (h != currentHighlight) {
                fragments.add(new Fragment(text.substring(start, i), currentHighlight));
                start = i;
                currentHighlight = h;
            }
        }
        fragments.add(new Fragment(text.substring(start), currentHighlight));
        return fragments;
    }
}
