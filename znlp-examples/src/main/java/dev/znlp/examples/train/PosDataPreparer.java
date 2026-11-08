package dev.znlp.examples.train;

import dev.znlp.base.common.FileUtils;
import dev.znlp.base.common.PkuParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用途：
 * 参考 com.mayabot 的 PosPerceptronUtils.kt，实现一个 Java 版本的数据生成工具。
 * 程序会读取仓库下相对路径 data/source/train 目录下的所有文件，按行解析为“词/词性”的扁平格式（过滤掉空词性项），
 * 将所有行收集并随机打乱，按每 50000 条一份写入到 data.work/pos.data 目录下的分片文件中。
 *
 * 入口：main(String[] args)
 *
 * NOTE:
 * - 如果数据是 PKU 格式（如 word/pos 或 word_pos 或 word\tpos），PkuParser 会尝试解析成扁平 "word/pos" 形式。
 * - 本程序定位为词性训练前的数据准备工具，方便调试与数据准备。
 */
public class PosDataPreparer {

    public static void main(String[] args) throws Exception {
        // 输入数据目录（相对仓库根目录）
        File cnDir = new File("data/source/train/corpus/cncorpus");
        File pkDir = new File("data/source/train/corpus/pku");

        List<String> lines = new ArrayList<>();

        readDirToLines(cnDir, lines);
        readDirToLines(pkDir, lines);

        if (lines.isEmpty()) {
            System.err.println("未找到任何训练行数据，请检查输入目录: data/source/train/corpus/{cncorpus,pku}");
            return;
        }

        // 打乱顺序
        Collections.shuffle(lines);

        // 划分训练/测试 90% / 10%
        int total = lines.size();
        int trainCount = (int) Math.round(total * 0.9);
        List<String> trainLines = lines.subList(0, trainCount);
        List<String> testLines = lines.subList(trainCount, total);

        // 输出目录：data.work/pos.data/train 和 data.work/pos.data/test
        File outBase = new File("data/work/pos.data");
        File outTrain = new File(outBase, "train");
        File outTest = new File(outBase, "test");
        outTrain.mkdirs();
        outTest.mkdirs();

        int chunkSize = 50000;
        int trainParts = writeChunks(trainLines, outTrain, chunkSize, "train");
        int testParts = writeChunks(testLines, outTest, chunkSize, "test");

        System.out.println("完成：总行数=" + total + "，训练行数=" + trainLines.size() + "，测试行数=" + testLines.size()
                + "。训练分片=" + trainParts + "，测试分片=" + testParts);
    }

    private static void readDirToLines(File dir, List<String> lines) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在或不是目录: " + (dir == null ? "null" : dir.getAbsolutePath()));
            return;
        }
        for (File f : FileUtils.listAllFiles(dir)) {
            try {
                java.nio.file.Files.lines(f.toPath(), StandardCharsets.UTF_8)
                        .forEach(line -> {
                            if (line != null && !line.trim().isEmpty()) {
                                String parsed = PkuParser.parseToFlatString(line);
                                if (parsed != null && !parsed.trim().isEmpty()) {
                                    lines.add(parsed);
                                }
                            }
                        });
            } catch (IOException e) {
                System.err.println("读取文件失败: " + f.getAbsolutePath() + " -> " + e.getMessage());
            }
        }
    }

    private static int writeChunks(List<String> lines, File outDir, int chunkSize, String prefix) {
        if (lines == null || lines.isEmpty()) return 0;
        int k = 0;
        for (int i = 0; i < lines.size(); i += chunkSize) {
            k++;
            int end = Math.min(i + chunkSize, lines.size());
            File part = new File(outDir, prefix + "-part-" + k + ".txt");
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(part), StandardCharsets.UTF_8))) {
                for (int j = i; j < end; j++) {
                    writer.write(lines.get(j));
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("写出文件失败: " + part.getAbsolutePath() + " -> " + e.getMessage());
            }
        }
        return k;
    }
}

