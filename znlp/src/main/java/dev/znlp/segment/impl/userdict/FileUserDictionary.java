package dev.znlp.segment.impl.userdict;

import dev.znlp.base.common.CharNormUtils;
import dev.znlp.base.dat.StringIntDATMap;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * File版本CustomDictionary
 * <p>
 * 从文件加载自定义词典。{@link #refresh()} 建好新树后替换 {@code volatile} 引用；
 * 正在使用旧树的分词继续用旧树，下一句看到新树。
 * 词典文件格式：每行一个词条，可以包含权重（忽略），支持#开头的注释行。
 * 示例：
 * <pre>
 * 壁式网球 1
 * 自定义词
 * # 这是注释
 * </pre>
 *
 * @author jimichan
 */
public class FileUserDictionary implements UserDictionary {

    /**
     * 词典文件
     */
    private final File file;

    /**
     * 文件字符集编码
     */
    private final Charset charset;

    /**
     * 词典的双数组Trie树结构
     */
    private volatile StringIntDATMap trie;

    /**
     * 构造函数，使用UTF-8编码
     *
     * @param file 词典文件
     * @throws IOException 文件读取失败时抛出
     */
    public FileUserDictionary(File file) throws IOException {
        this(file, StandardCharsets.UTF_8);
    }

    /**
     * 构造函数，指定字符集编码
     *
     * @param file    词典文件
     * @param charset 文件字符集编码
     * @throws IOException 文件读取失败时抛出
     */
    public FileUserDictionary(File file, Charset charset) throws IOException {
        this.file = file;
        this.charset = charset;
        this.trie = loadDictionary();
    }

    /**
     * 从文件加载词典
     *
     * @return 加载的StringIntDATMap，如果词典为空则返回null
     * @throws IOException 文件读取失败时抛出
     */
    private StringIntDATMap loadDictionary() throws IOException {
        TreeMap<String, Integer> dict = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] params = line.split("\\s");

                if (params.length > 0) {
                    String word = params[0];
                    // 跳过注释行和空行
                    if (!word.startsWith("#") && !word.isEmpty()) {
                        // 添加原始词和规范化后的词
                        String normalizedWord = CharNormUtils.convert(word);
                        dict.put(word, 1000);
                        dict.put(normalizedWord, 1000);
                    }
                }
            }
        }

        return dict.isEmpty() ? null : new StringIntDATMap(dict);
    }

    /**
     * 刷新词典，重新从文件加载
     * <p>
     * 该方法是线程安全的，在重新加载完成后才会替换旧的词典数据。
     *
     * @throws IOException 文件读取失败时抛出
     */
    public void refresh() throws IOException {
        this.trie = loadDictionary();
    }

    @Override
    public StringIntDATMap getTrie() {
        return trie;
    }
}
