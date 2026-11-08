package dev.znlp.base.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 下载工具类，用于下载和处理资源文件。
 * <p>
 * 该类提供了文件下载和 ZIP 文件解压缩功能。
 * </p>
 *
 * @author jimichan
 */
public class DownloadUtils {

    /**
     * 下载文件
     *
     * @param url  文件的 URL 地址
     * @param file 保存到的目标文件
     * @throws IOException 如果下载过程中发生错误
     */
    public static void download(String url, File file) throws IOException {
        // 先完全读入到内存中去。然后一次性写入文件
        URI uri = URI.create(url);
        try (BufferedInputStream in = new BufferedInputStream(uri.toURL().openStream());
             BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 解压缩 ZIP 文件
     *
     * @param file 要解压缩的 ZIP 文件
     * @throws Exception 如果解压缩过程中发生错误
     */
    public static void unzip(File file) throws Exception {
        Path filePath = file.toPath();
        Path parentPath = filePath.getParent();
        if (parentPath == null) {
            parentPath = Paths.get(".");
        }
        parentPath = parentPath.normalize().toAbsolutePath();
        
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(filePath)))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                // 规范化路径以防止路径遍历攻击
                Path targetPath = parentPath.resolve(name).normalize();
                
                // 确保解析后的路径仍然在父目录内（防止路径遍历攻击）
                if (!targetPath.startsWith(parentPath)) {
                    throw new IOException("Zip entry outside target directory: " + name);
                }

                // 确保目标文件的父目录存在
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Path parent = targetPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(targetPath))) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }
}
