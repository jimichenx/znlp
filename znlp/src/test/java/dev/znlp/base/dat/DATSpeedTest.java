package dev.znlp.base.dat;

import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DATSpeedTest {

    private List<String> keywords;

    @BeforeEach
    public void setUp() {
        // 读取 文件 data/dat/ner-model/feature.txt，保存到 List<String>中
        keywords = loadKeywords();
    }

    /**
     * 从文件或资源中加载关键词列表
     * 优先尝试从指定路径读取 data/dat/ner-model/feature.txt
     * 如果文件不存在，使用示例数据
     */
    private List<String> loadKeywords() {
        List<String> result = new ArrayList<>();

        // 尝试从不同位置读取文件
        String[] possiblePaths = {
                "data/dat/ner-model/feature.txt",
                "../data/dat/ner-model/feature.txt",
        };

        for (String path : possiblePaths) {
            try {
                result = Files.readAllLines(Paths.get(path)).stream()
                        .filter(line -> !line.trim().isEmpty())
                        .collect(Collectors.toList());
                if (!result.isEmpty()) {
                    System.out.println("成功加载文件: " + path + ", 共 " + result.size() + " 条");
                    return result;
                }
            } catch (IOException e) {
                // 继续尝试下一个路径
            }
        }

        // 如果文件不存在，使用示例测试数据
        System.out.println("未找到特征文件，使用示例数据进行测试");
        result = Collections.emptyList();

        return result;
    }


    //    @Test
    public void testSpeed() {
        if (keywords == null || keywords.isEmpty()) {
            System.out.println("警告：未加载任何关键词数据");
            return;
        }

        // 移除重复并排序
        List<String> sortedKeywords = keywords.stream()
                .sorted()
                .collect(Collectors.toList());

        System.out.println("=============== DAT 构建性能测试 ================");
        System.out.println("关键词总数: " + sortedKeywords.size());

        testBuilderPerformance(new DoubleArrayTrieBuilder2(), "DoubleArrayTrieBuilder2", sortedKeywords);

        System.out.println("=============== 性能测试完成 ================");
    }

    /**
     * 测试指定 Builder 的性能
     */
    private void testBuilderPerformance(DoubleArrayTrieBuilder builder, String builderName, List<String> keywords) {
        long startTime = System.currentTimeMillis();

        try {
            DoubleArrayTrie trie = builder.build(keywords);

            long endTime = System.currentTimeMillis();
            long buildTime = endTime - startTime;

            System.out.println("\n" + builderName + " 构建时间: " + buildTime + "ms");
            System.out.println("Trie 大小: " + trie.size());
            System.out.println("Check 数组长度: " + trie.checkSize());
            System.out.println("Base 数组长度: " + trie.baseSize());
            System.out.println("非零元素数量: " + trie.getNonzeroSize());

            // 简单的查询性能测试
            long queryStartTime = System.currentTimeMillis();
            int foundCount = 0;

            // 随机查询一些关键词
            for (int i = 0; i < Math.min(10000, keywords.size()); i++) {
                if (trie.indexOf(keywords.get(i)) >= 0) {
                    foundCount++;
                }
            }

            long queryEndTime = System.currentTimeMillis();
            long queryTime = queryEndTime - queryStartTime;

            System.out.println("查询 " + Math.min(100, keywords.size()) + " 个关键词耗时: " + queryTime + "ms");
            System.out.println("成功找到: " + foundCount + " 个");

        } catch (Exception e) {
            System.out.println(builderName + " 构建失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
