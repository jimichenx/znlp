package dev.znlp.examples.demo;

import dev.znlp.segment.impl.pos.PosPerceptron;

import java.io.IOException;
import java.util.List;

public class PosExample {
    public static void main(String[] args) throws IOException {
        PosPerceptron pos = PosPerceptron.loadFromClasspath("znlp/pos-model/pos.model");

        System.out.println(pos.decode(List.of("我", "爱", "北京")));
    }
}
