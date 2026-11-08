package dev.znlp.segment.impl.userdict;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.graph.Wordnet;
import dev.znlp.segment.graph.Wordpath;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomDictionaryProcessor 测试类
 * 测试自定义词典处理器的功能
 *
 * @author jimichan
 */
public class UserDictionaryProcessorTest {

    @Test
    public void testProcessorCreation() {
        MemUserDictionary dictionary = new MemUserDictionary();
        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(dictionary));

        assertNotNull(processor);
        assertEquals(1, processor.getDictionaries().size());
        assertEquals(dictionary, processor.getDictionaries().get(0));
    }

    @Test
    public void testProcessorProperties() {
        MemUserDictionary dictionary = new MemUserDictionary();
        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(dictionary));

        // 测试默认属性
        assertNotNull(processor.getName());
        assertTrue(processor.getName().startsWith("CustomDictionaryProcessor"));
        assertTrue(processor.isEnabled());
        assertEquals(200, processor.getOrder());

        // 测试属性设置
        processor.setEnabled(false);
        assertFalse(processor.isEnabled());

        processor.setOrder(150);
        assertEquals(150, processor.getOrder());

        MemUserDictionary newDictionary = new MemUserDictionary();
        processor.setDictionaries(Collections.singletonList(newDictionary));
        assertEquals(1, processor.getDictionaries().size());
        assertEquals(newDictionary, processor.getDictionaries().get(0));
    }

    @Test
    public void testProcessingWithEmptyDictionary() {
        MemUserDictionary emptyDictionary = new MemUserDictionary();
        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(emptyDictionary));

        // 创建一个简单的 Wordpath 实例用于测试
        Wordpath wordPath = createSimpleWordpath("测试文本");

        Wordpath processedPath = processor.process(wordPath);

        // 当词典为空时，处理后应该返回原样
        assertNotNull(processedPath);
    }

    @Test
    public void testProcessingWithNonEmptyDictionary() {
        MemUserDictionary dictionary = new MemUserDictionary();
        dictionary.addWord("测试文本");
        dictionary.rebuild();

        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(dictionary));

        Wordpath wordPath = createSimpleWordpath("测试文本");

        Wordpath processedPath = processor.process(wordPath);

        assertNotNull(processedPath);
    }

    @Test
    public void testEnableDisable() {
        MemUserDictionary dictionary = new MemUserDictionary();
        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(dictionary));

        processor.disable();
        assertFalse(processor.isEnabled());

        processor.enable();
        assertTrue(processor.isEnabled());
    }

    @Test
    public void testComponentInterface() {
        MemUserDictionary dictionary = new MemUserDictionary();
        UserDictionaryProcessor processor = new UserDictionaryProcessor(Collections.singletonList(dictionary));

        // 测试 PipelineComponent 接口
        assertNotNull(processor.getName());
        processor.setEnabled(false);
        assertFalse(processor.isEnabled());

        processor.setOrder(100);
        assertEquals(100, processor.getOrder());
    }

    // 辅助方法：创建一个简单的 Wordpath 实例
    private Wordpath createSimpleWordpath(String text) {
        Wordnet wordnet = new Wordnet(text.toCharArray());
        return new Wordpath(wordnet);
    }
}
