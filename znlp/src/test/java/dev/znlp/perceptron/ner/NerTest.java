package dev.znlp.perceptron.ner;

import org.junit.jupiter.api.Test;
import dev.znlp.segment.Nature;
import dev.znlp.segment.Token;
import dev.znlp.segment.WordAndNature;
import dev.znlp.segment.impl.ner.NERPerceptron;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NerTest {

    @Test
    public void decodeEntitySpans() throws Exception {

        NERPerceptron ner = NERPerceptron.loadFromClasspath("znlp/ner/ner.model");

        List<WordAndNature> sentence = new ArrayList<>();
        sentence.add(new Token("胜利", Nature.nz));
        sentence.add(new Token("海上", Nature.s));
        sentence.add(new Token("油田", Nature.n));

        List<String> result = ner.decode(sentence);
        assertEquals(3, result.size());
        assertTrue(result.stream().noneMatch(String::isBlank));

        List<WordAndNature> sentence3 = new ArrayList<>();
        sentence3.add(new Token("北京", Nature.ns));
        sentence3.add(new Token("欢迎", Nature.v));
        sentence3.add(new Token("你", Nature.r));
        List<String> result3 = ner.decode(sentence3);
        assertEquals(3, result3.size());
        assertTrue(result3.stream().noneMatch(String::isBlank));
    }
}
