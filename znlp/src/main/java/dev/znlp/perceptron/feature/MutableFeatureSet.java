package dev.znlp.perceptron.feature;

import dev.znlp.base.dat.DoubleArrayTrie;
import dev.znlp.base.dat.DoubleArrayTrieBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 可扩展特征集实现，支持运行时动态添加新特征（带extMap）。
 * 适用于训练场景，可以动态添加新特征。
 *
 * @author jimichan
 */
public class MutableFeatureSet {

    private final DoubleArrayTrie dat;

    /**
     * keys存在的话有个好处，模型可以被压缩
     */
    private final List<String> keys;

    // 预留的多一点，就不会有hash冲突
    private final Map<String, Integer> extMap = new HashMap<>(1000);

    private int nextId;

    public MutableFeatureSet(DoubleArrayTrie dat, List<String> keys) {
        this.dat = dat;
        this.keys = keys;
        this.nextId = dat.size();
    }

    public int featureId(String feature) {
        int id = dat.indexOf(feature);

        if (id >= 0) {
            return id;
        }

        if (!extMap.isEmpty()) {
            Integer extId = extMap.get(feature);
            return extId != null ? extId : -1;
        } else {
            return id;
        }
    }


    public int featureId(CharSequence feature) {
        int id = dat.indexOf(feature);

        if (id >= 0) {
            return id;
        }
        if (!extMap.isEmpty()) {
            Integer extId = extMap.get(feature.toString());
            return extId != null ? extId : -1;
        } else {
            return id;
        }
    }


    public int newExtId(String feature) {
        if (dat.indexOf(feature) < 0 && !extMap.containsKey(feature)) {
            int newId = nextId++;
            extMap.put(feature, newId);
            return newId;
        }

        return featureId(feature);
    }


    public int size() {
        return dat.size() + extMap.size();
    }


    public List<String> getKeys() {
        return keys;
    }


    public void save(File datFile, File textFile) throws IOException {
        // 简化实现：如果有keys，保存为文本文件
        if (keys != null && textFile != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(textFile.toURI()))) {
                for (String line : keys) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        }
    }

    /**
     * 只读取文本文件并构建ExtendableFeatureSet
     */
    public static MutableFeatureSet readFromText(InputStream textInput) throws IOException {
        List<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(textInput))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        }

        // 使用builder构建DoubleArrayTrie
        DoubleArrayTrieBuilder builder = DoubleArrayTrie.builder();
        DoubleArrayTrie datArray = builder.build(list);
        return new MutableFeatureSet(datArray, list);
    }
}
