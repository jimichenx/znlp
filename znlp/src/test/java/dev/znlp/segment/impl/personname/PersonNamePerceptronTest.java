package dev.znlp.segment.impl.personname;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonNamePerceptronTest {

    @Test
    void findPersonNameInSentence() throws Exception {
        PersonNamePerceptron perceptron = PersonNamePerceptron.load("znlp/personname/nr-name.model");
        var names = perceptron.findPersonName("这是陈宝奇的快递");
        assertFalse(names.isEmpty());
        assertEquals(2, names.get(0).offset());
        assertEquals(3, names.get(0).length());
    }

    @Test
    void emptyInputReturnsEmpty() throws Exception {
        PersonNamePerceptron perceptron = PersonNamePerceptron.load("znlp/personname/nr-name.model");
        assertTrue(perceptron.findPersonName("").isEmpty());
        assertTrue(perceptron.findPersonName((String) null).isEmpty());
    }
}
