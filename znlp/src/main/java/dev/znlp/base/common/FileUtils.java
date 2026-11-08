package dev.znlp.base.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单的文件工具：列出目录下所有普通文件（递归）
 */
public class FileUtils {

    /**
     * 列出目录下的所有文件（递归），返回按自然顺序的列表。
     */
    public static List<File> listAllFiles(File dir) {
        List<File> files = new ArrayList<>();
        if (dir == null || !dir.exists()) {
            return files;
        }
        walk(dir, files);
        return files;
    }

    private static void walk(File f, List<File> files) {
        if (f.isFile()) {
            files.add(f);
        } else if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    walk(c, files);
                }
            }
        }
    }
}

