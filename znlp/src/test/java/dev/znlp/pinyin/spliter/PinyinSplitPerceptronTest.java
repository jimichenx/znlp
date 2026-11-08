package dev.znlp.pinyin.spliter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PinyinSplitPerceptronTest {

    @Test
    void loadBundledModelAndSplit() throws Exception {
        PinyinSplitPerceptron perceptron = PinyinSplitPerceptron.load("znlp/pinyin/pinyin-split.model");
        List<String> parts = perceptron.split("fudandaxue");
        assertFalse(parts.isEmpty());
        assertEquals("fudandaxue", String.join("", parts));
    }

    @Test
    void emptyInputReturnsEmpty() throws Exception {
        PinyinSplitPerceptron perceptron = PinyinSplitPerceptron.load("znlp/pinyin/pinyin-split.model");
        assertTrue(perceptron.split("").isEmpty());
    }
}
