package dev.znlp.fasttext.train;

import dev.znlp.base.common.AbstractIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件样本行可迭代类，用于从文件中读取和处理训练样本行。
 * 支持将文件内容转换为 SampleLine 列表、分割文件为多个子文件。
 * 分割产生的临时文件在 {@link #toMemList()} 或 {@link #deleteIfOwned()} 时删除。
 */
public class FileSampleLineIterable implements Iterable<SampleLine> {

    private static final Pattern TOKEN_SPLIT = Pattern.compile("\\s");

    private final File file;
    private final boolean ownedTemp;

    public FileSampleLineIterable(File file) {
        this(file, false);
    }

    FileSampleLineIterable(File file, boolean ownedTemp) {
        this.file = file;
        this.ownedTemp = ownedTemp;
    }

    /**
     * 将文件内容转换为 SampleLine 列表。若本实例持有临时文件，读完后删除。
     */
    public List<SampleLine> toMemList() {
        List<SampleLine> list = new ArrayList<>();
        try {
            for (SampleLine line : this) {
                list.add(line);
            }
            return list;
        } finally {
            deleteIfOwned();
        }
    }

    /**
     * 将文件分割成多个子文件。子文件为临时文件，调用方负责 {@link #deleteIfOwned()} 或 {@link #toMemList()}。
     */
    public List<FileSampleLineIterable> splitMutiFiles(int num) {
        if (num <= 0) {
            throw new IllegalArgumentException("split count must be positive");
        }
        List<File> subFiles = new ArrayList<>();
        List<BufferedWriter> writers = new ArrayList<>();
        try {
            for (int i = 0; i < num; i++) {
                File temp = Files.createTempFile(file.getName(), ".ftpart").toFile();
                temp.deleteOnExit();
                subFiles.add(temp);
            }
            for (File subFile : subFiles) {
                writers.add(Files.newBufferedWriter(subFile.toPath(), StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    BufferedWriter writer = writers.get(count % num);
                    writer.write(line);
                    writer.write('\n');
                    count++;
                }
            }
        } catch (IOException e) {
            for (File subFile : subFiles) {
                try {
                    Files.deleteIfExists(subFile.toPath());
                } catch (IOException ignored) {
                    // ignore cleanup failure
                }
            }
            throw new RuntimeException(e);
        } finally {
            for (BufferedWriter writer : writers) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }

        return subFiles.stream()
                .map(f -> new FileSampleLineIterable(f, true))
                .collect(Collectors.toList());
    }

    /**
     * 删除由 {@link #splitMutiFiles(int)} 创建的临时文件。
     */
    public void deleteIfOwned() {
        if (ownedTemp) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    public long size() {
        return file.length();
    }

    public int lines() {
        try (Stream<String> stream = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            return (int) stream.count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<SampleLine> iterator() {
        try {
            return new FileSampleIterator(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class FileSampleIterator extends AbstractIterator<SampleLine> implements Closeable {
        private final BufferedReader reader;
        private boolean closed;

        FileSampleIterator(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        protected void computeNext() {
            try {
                String line = reader.readLine();
                if (line == null) {
                    closeQuietly();
                    done();
                } else {
                    String[] split = TOKEN_SPLIT.split(line);
                    List<String> words = new ArrayList<>();
                    for (String s : split) {
                        if (!s.isEmpty()) {
                            words.add(s);
                        }
                    }
                    setNext(new SampleLine(words));
                }
            } catch (IOException e) {
                closeQuietly();
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                reader.close();
            }
        }

        private void closeQuietly() {
            try {
                close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
