package dev.znlp.segment.impl.correction;

import dev.znlp.base.dat.DoubleArrayTrieMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * File版本CorrectionDictionary
 * 文件内容格式：
 * 第几套/房
 * <p>
 * 一行一个规则
 *
 * @author jimichan
 */
public class FileCorrectionDictionary implements CorrectionDictionary {

    private final TreeMap<String, CorrectionWord> dict;
    private final DoubleArrayTrieMap<CorrectionWord> trie;

    public FileCorrectionDictionary(File file) throws Exception {
        this(file, StandardCharsets.UTF_8);
    }

    public FileCorrectionDictionary(File file, Charset charset) throws Exception {
        this.dict = new TreeMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                CorrectionWord correctionWord = CorrectionWord.parse(line);
                dict.put(correctionWord.path(), correctionWord);
            }
        }

        this.trie = new DoubleArrayTrieMap<>(dict);
    }

    @Override
    public DoubleArrayTrieMap<CorrectionWord> getTrie() {
        return trie;
    }
}
