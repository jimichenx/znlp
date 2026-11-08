package dev.znlp.segment.impl.userdict;

import dev.znlp.base.resources.NlpResourceLoader;
import dev.znlp.base.service.ServiceRegistry;
import dev.znlp.config.ZnlpSystemConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CustomDictionaryPlugin 的构建器。
 * <p>
 * 提供两种构造函数：
 * <ul>
 *   <li>无参构造：不持有 ServiceRegistry，只能添加 {@link FileUserDictionary}、{@link MemUserDictionary}
 *       或任意 {@link UserDictionary} 实例，不能添加资源词典。</li>
 *   <li>带 ServiceRegistry 的构造：可从 {@link ResourceUserDictionary} 中选择一个或多个词典
 *       （通过 {@link ResourceDictKind}）。</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * CustomDictionaryPluginBuilder b = CustomDictionaryPluginBuilder.create();
 * MemCustomDictionary mem = b.addMemDict();  // 直接返回句柄，放弃链
 * mem.addWord("新词");
 * FileCustomDictionary fileDict = b.addFileDict(new File("dict.txt"));  // 直接返回句柄
 * b.addResourceDict(ResourceDictKind.CUSTOM_DICT);
 * List&lt;UserDictionary&gt; dicts = b.dictionaries();
 * </pre>
 *
 * @author jimichan
 */
public final class UserDictionaryPluginBuilder {

    private final ServiceRegistry registry;
    private final List<UserDictionary> dictionaries;
    private ResourceUserDictionary resourceDict;

    /**
     * 无参构造，不持有 ServiceRegistry。
     * 只能通过 {@link #addFileDict(File)}、{@link #addMemDict()}、{@link #addDict(UserDictionary)} 添加词典，
     * 不能使用 {@link #addResourceDict(ResourceDictKind)} / {@link #addResourceDicts(ResourceDictKind...)}。
     */
    public UserDictionaryPluginBuilder() {
        this.registry = null;
        this.dictionaries = new ArrayList<>();
    }

    /**
     * 持有 ServiceRegistry 的构造。
     * 除文件/内存/自定义词典外，还可通过 {@link #addResourceDict(ResourceDictKind)} 或
     * {@link #addResourceDicts(ResourceDictKind...)} 从 ResourceCustomDictionary 中选择一个或多个词典。
     *
     * @param registry 服务注册表，用于获取 {@link ZnlpSystemConfig} 和 {@link NlpResourceLoader} 创建 ResourceCustomDictionary
     */
    public UserDictionaryPluginBuilder(ServiceRegistry registry) {
        this.registry = registry;
        this.dictionaries = new ArrayList<>();
    }

    /**
     * 创建无 ServiceRegistry 的构建器。
     */
    public static UserDictionaryPluginBuilder create() {
        return new UserDictionaryPluginBuilder();
    }

    /**
     * 创建持有 ServiceRegistry 的构建器，可选用资源词典。
     *
     * @param registry 服务注册表，可为 null（等同于 {@link #create()}）
     */
    public static UserDictionaryPluginBuilder create(ServiceRegistry registry) {
        return registry != null ? new UserDictionaryPluginBuilder(registry) : new UserDictionaryPluginBuilder();
    }

    /**
     * 添加文件词典（UTF-8）。直接返回词典句柄，不参与链式。
     *
     * @param file 词典文件
     * @return 刚添加的 FileCustomDictionary 句柄，可用于 {@link FileUserDictionary#refresh()} 等
     * @throws IOException 文件读取失败时抛出
     */
    public FileUserDictionary addFileDict(File file) throws IOException {
        return addFileDict(file, StandardCharsets.UTF_8);
    }

    /**
     * 添加文件词典，指定字符集。直接返回词典句柄，不参与链式。
     *
     * @param file    词典文件
     * @param charset 字符集
     * @return 刚添加的 FileCustomDictionary 句柄
     * @throws IOException 文件读取失败时抛出
     */
    public FileUserDictionary addFileDict(File file, Charset charset) throws IOException {
        FileUserDictionary dict = new FileUserDictionary(file, charset);
        dictionaries.add(dict);
        return dict;
    }

    /**
     * 添加一个空的内存词典（MemCustomDictionary）。直接返回词典句柄，不参与链式。
     *
     * @return 刚添加的 MemCustomDictionary 句柄，可用于 {@link MemUserDictionary#addWord(String)} 等
     */
    public MemUserDictionary addMemDict() {
        MemUserDictionary dict = new MemUserDictionary();
        dictionaries.add(dict);
        return dict;
    }

    /**
     * 添加已有的 CustomDictionary 实例。
     *
     * @param dict 自定义词典，非 null
     * @return this
     */
    public UserDictionaryPluginBuilder addDict(UserDictionary dict) {
        if (dict != null) {
            dictionaries.add(dict);
        }
        return this;
    }

    /**
     * 从 ResourceCustomDictionary 中添加指定种类的词典。
     * 仅当构建器通过带 ServiceRegistry 的构造函数创建时可用，否则抛出 {@link IllegalStateException}。
     *
     * @param kind 资源词典种类
     * @return this
     * @throws IllegalStateException 未提供 ServiceRegistry 时
     */
    public UserDictionaryPluginBuilder addResourceDict(ResourceDictKind kind) {
        ensureRegistryForResource();
        ensureResourceDict();
        dictionaries.add(new ResourceDictAdapter(resourceDict, kind));
        return this;
    }

    /**
     * 从 ResourceCustomDictionary 中添加多种词典。
     * 仅当构建器通过带 ServiceRegistry 的构造函数创建时可用。
     *
     * @param kinds 资源词典种类，可传一个或多个
     * @return this
     * @throws IllegalStateException 未提供 ServiceRegistry 时
     */
    public UserDictionaryPluginBuilder addResourceDicts(ResourceDictKind... kinds) {
        if (kinds == null || kinds.length == 0) {
            return this;
        }
        ensureRegistryForResource();
        ensureResourceDict();
        for (ResourceDictKind kind : kinds) {
            dictionaries.add(new ResourceDictAdapter(resourceDict, kind));
        }
        return this;
    }

    private void ensureRegistryForResource() {
        if (registry == null) {
            throw new IllegalStateException(
                "未提供 ServiceRegistry，不能使用资源词典。请使用 CustomDictionaryPluginBuilder(ServiceRegistry) 或 create(ServiceRegistry)。");
        }
    }

    private void ensureResourceDict() {
        if (resourceDict == null) {
            ZnlpSystemConfig config = registry.getService(ZnlpSystemConfig.class);
            NlpResourceLoader loader = registry.getService(NlpResourceLoader.class);
            resourceDict = new ResourceUserDictionary(config, loader);
        }
    }

    /**
     * 返回已添加的词典，交给 {@code SegmentRecipe.withUserDictionary}。
     */
    public List<UserDictionary> dictionaries() {
        return Collections.unmodifiableList(new ArrayList<>(dictionaries));
    }
}
