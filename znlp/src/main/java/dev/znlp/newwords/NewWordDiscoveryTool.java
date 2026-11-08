package dev.znlp.newwords;

import dev.znlp.base.logging.InternalLogger;
import dev.znlp.base.logging.InternalLoggerFactory;
import dev.znlp.newwords.engine.INewWordFindEngine;
import dev.znlp.newwords.engine.NewWordFindEngine;
import dev.znlp.newwords.entity.NewWord;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 新词发现工具类。
 *
 * <p>该工具类用于从指定目录的文本文件中自动发现新词。
 * 通过两轮扫描算法，基于互信息（MI）和信息熵（Entropy）等统计指标，
 * 识别出文本中可能是新词的高频词组。</p>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>批量处理</b>：自动扫描目录下的所有文本文件</li>
 *   <li><b>两轮扫描</b>：第一轮统计基本频次，第二轮计算统计指标</li>
 *   <li><b>结果导出</b>：支持将新词结果导出为CSV格式文件</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 创建引擎并设置 scorer
 * NewWordFindEngine engine = new NewWordFindEngine(3, 12, 10, true, coreDictionary);
 * engine.setScorer(DefaultNewWordScorer.create(2.0f, 1.0f));
 *
 * // 使用 Builder 构建工具
 * NewWordDiscoveryTool tool = new NewWordDiscoveryTool.Builder(dataDir, engine)
 *     .charset(StandardCharsets.UTF_8)
 *     .verbose(true)
 *     .build();
 * ArrayList<NewWord> newWords = tool.discover();
 *
 * // 保存结果到文件
 * tool.saveToFile(new File("result.csv"));
 * }</pre>
 *
 * @author jimichan
 * @see NewWordFindEngine
 * @see NewWord
 */
public class NewWordDiscoveryTool {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NewWordDiscoveryTool.class);

    private final File directory;
    private final Charset charset;
    private final INewWordFindEngine engine;
    private final boolean verbose;

    /**
     * 使用 Builder 构造实例（推荐方式）
     */
    private NewWordDiscoveryTool(Builder builder) {
        this.directory = builder.directory;
        this.charset = builder.charset;
        this.engine = builder.engine;
        this.verbose = builder.verbose;
    }

    /**
     * 使用默认参数的构造函数
     *
     * @param directory 文本文件所在目录
     * @param engine    新词发现引擎
     */
    public NewWordDiscoveryTool(File directory, NewWordFindEngine engine) {
        this(new Builder(directory, engine));
    }

    /**
     * 执行新词发现
     *
     * <p>调用前需确保已通过 engine.setScorer() 设置打分器</p>
     *
     * @return 发现的新词列表，按评分降序排列
     * @throws RuntimeException 如果目录不存在或无法读取
     */
    public ArrayList<NewWord> discover() {
        validateDirectory();

        List<File> textFiles = collectTextFiles();
        logInfo("共找到 {} 个文本文件", textFiles.size());

        performFirstScan(engine, textFiles);
        engine.finishFirst();

        performSecondScan(engine, textFiles);
        engine.endSecond();

        ArrayList<NewWord> results = engine.result();
        logInfo("新词发现完成，共发现 {} 个新词", results.size());

        return results;
    }

    /**
     * 保存结果到CSV文件（使用默认参数）
     *
     * @param outputFile 输出文件
     * @throws IOException 如果文件操作失败
     */
    public void saveToFile(File outputFile) throws IOException {
        saveToFile(outputFile, true, ",", false, StandardCharsets.UTF_8);
    }

    /**
     * 保存结果到CSV文件
     *
     * @param outputFile    输出文件
     * @param includeHeader 是否包含表头
     * @param outputCharset 输出文件字符编码
     * @throws IOException 如果文件操作失败
     */
    public void saveToFile(File outputFile, boolean includeHeader, Charset outputCharset) throws IOException {
        saveToFile(outputFile, includeHeader, ",", false, outputCharset);
    }

    /**
     * 保存结果到文件（完整参数）
     *
     * @param outputFile    输出文件
     * @param includeHeader 是否包含表头
     * @param delimiter     字段分隔符（"," 为CSV格式，"\t" 为TSV格式）
     * @param chineseHeader 是否使用中文表头
     * @param outputCharset 输出文件字符编码
     * @throws IOException 如果文件操作失败
     */
    public void saveToFile(File outputFile, boolean includeHeader, String delimiter,
                           boolean chineseHeader, Charset outputCharset) throws IOException {
        ArrayList<NewWord> results = discover();
        writeResultsToFile(results, outputFile, includeHeader, delimiter, chineseHeader, outputCharset);
        logInfo("结果已保存到: {}", outputFile.getAbsolutePath());
    }

    /**
     * 静态方法：保存新词列表到文件（不执行发现过程）
     *
     * @param results       新词列表
     * @param outputFile    输出文件
     * @param includeHeader 是否包含表头
     * @param delimiter     字段分隔符
     * @param chineseHeader 是否使用中文表头
     * @param outputCharset 输出文件字符编码
     * @throws IOException 如果文件操作失败
     */
    public static void saveResults(ArrayList<NewWord> results, File outputFile,
                                   boolean includeHeader, String delimiter,
                                   boolean chineseHeader, Charset outputCharset) throws IOException {
        writeResultsToFile(results, outputFile, includeHeader, delimiter, chineseHeader, outputCharset);
    }

    /**
     * 验证目录是否有效
     */
    private void validateDirectory() {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("无效的目录路径: " +
                    (directory == null ? "null" : directory.getAbsolutePath()));
        }
    }

    /**
     * 收集目录下的所有文本文件（递归遍历子目录）
     */
    private List<File> collectTextFiles() {
        List<File> textFiles = new ArrayList<>();
        collectTextFilesRecursively(directory, textFiles);
        return textFiles;
    }

    /**
     * 递归收集文本文件
     *
     * @param dir       当前目录
     * @param collector 文件收集器
     */
    private void collectTextFilesRecursively(File dir, List<File> collector) {
        File[] allFiles = dir.listFiles();
        if (allFiles == null) {
            return;
        }

        for (File file : allFiles) {
            if (file.isDirectory()) {
                // 递归处理子目录
                collectTextFilesRecursively(file, collector);
            } else if (file.isFile()) {
                // 收集普通文件
                collector.add(file);
            }
        }
    }

    /**
     * 执行第一轮扫描：统计基本频次
     */
    private void performFirstScan(INewWordFindEngine engine, List<File> files) {
        logInfo("开始第一轮扫描（统计词频）...");
        long startTime = System.currentTimeMillis();

        processFiles(files, (file, lineNumber, line) -> {
            try {
                engine.firstScan(line);
            } catch (IOException e) {
                throw new UncheckedIOException("第一轮扫描失败", e);
            }
        });

        long duration = System.currentTimeMillis() - startTime;
        logInfo("第一轮扫描完成，耗时 {} ms", duration);
    }

    /**
     * 执行第二轮扫描：计算统计指标
     */
    private void performSecondScan(INewWordFindEngine engine, List<File> files) {
        logInfo("开始第二轮扫描（计算统计指标）...");
        long startTime = System.currentTimeMillis();

        processFiles(files, (file, lineNumber, line) -> {
            try {
                engine.secondScan(line);
            } catch (IOException e) {
                throw new UncheckedIOException("第二轮扫描失败", e);
            }
        });

        long duration = System.currentTimeMillis() - startTime;
        logInfo("第二轮扫描完成，耗时 {} ms", duration);
    }

    /**
     * 处理所有文件
     */
    private void processFiles(List<File> files, FileLineProcessor processor) {
        int totalFiles = files.size();
        int processedFiles = 0;

        for (File file : files) {
            processedFiles++;

            if (verbose && logger.isDebugEnabled()) {
                int percentage = (int) ((processedFiles * 100.0f) / totalFiles);
                logger.debug("进度: {}% ({}/{})", percentage, processedFiles, totalFiles);
            }

            processFile(file, processor);
        }
    }

    /**
     * 处理单个文件
     */
    private void processFile(File file, FileLineProcessor processor) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), charset))) {

            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                processor.process(file, lineNumber, line);
            }
        } catch (IOException e) {
            logger.warn("读取文件失败 {}", file.getAbsolutePath(), e);
        }
    }

    /**
     * 将结果写入文件
     */
    private static void writeResultsToFile(ArrayList<NewWord> results, File outputFile,
                                           boolean includeHeader, String delimiter,
                                           boolean chineseHeader, Charset outputCharset) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset))) {

            if (includeHeader) {
                writer.write(getHeaderLine(delimiter, chineseHeader));
                writer.newLine();
            }

            for (NewWord word : results) {
                writer.write(formatNewWord(word, delimiter));
                writer.newLine();
            }
        }
    }

    /**
     * 获取表头行
     */
    private static String getHeaderLine(String delimiter, boolean chineseHeader) {
        if (chineseHeader) {
            return String.join(delimiter,
                    "词语", "词长", "评分", "频次", "文档频次", "频次/文档频次",
                    "IDF", "互信息", "平均互信息", "熵", "左熵", "右熵");
        } else {
            return String.join(delimiter,
                    "Word", "WordLen", "Score", "Freq", "DocFreq", "Freq/DocFreq",
                    "Idf", "Mi", "AvgMi", "Entropy", "LE", "RE");
        }
    }

    /**
     * 格式化新词为数据行
     */
    private static String formatNewWord(NewWord word, String delimiter) {
        return String.format("%s%s%d%s%.2f%s%d%s%d%s%.2f%s%.2f%s%.2f%s%.2f%s%.2f%s%.2f%s%.2f",
                word.getWord(), delimiter,
                word.getLen(), delimiter,
                word.getScore(), delimiter,
                word.getFreq(), delimiter,
                word.getDocFreq(), delimiter,
                word.getDocFreq() > 0 ? word.getFreq() / (float) word.getDocFreq() : 0f, delimiter,
                word.getIdf(), delimiter,
                word.getMi(), delimiter,
                word.getAvg_mi(), delimiter,
                word.getEntropy(), delimiter,
                word.getLe(), delimiter,
                word.getRe()
        );
    }

    /**
     * 输出日志信息
     */
    private void logInfo(String format, Object... args) {
        if (verbose) {
            logger.info("[新词发现] " + format, args);
        }
    }

    /**
     * 文件行处理器接口
     */
    @FunctionalInterface
    private interface FileLineProcessor {
        void process(File file, int lineNumber, String line);
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final File directory;
        private final INewWordFindEngine engine;

        private Charset charset = StandardCharsets.UTF_8;
        private boolean verbose = true;

        public Builder(File directory, INewWordFindEngine engine) {
            this.directory = directory;
            this.engine = engine;
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public NewWordDiscoveryTool build() {
            return new NewWordDiscoveryTool(this);
        }
    }
}
