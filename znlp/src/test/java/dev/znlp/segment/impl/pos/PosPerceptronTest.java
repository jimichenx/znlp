package dev.znlp.segment.impl.pos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PosPerceptronTest {

    @Test
    void loadAndDecodeBundledModel() throws Exception {
        PosPerceptron pos = PosPerceptron.loadFromClasspath("znlp/pos-model/pos.model");
        List<String> tags = pos.decode(List.of("我", "爱", "自然语言处理"));
        assertEquals(3, tags.size());
        assertTrue(tags.stream().allMatch(tag -> tag != null && !tag.isBlank()));
    }
}
