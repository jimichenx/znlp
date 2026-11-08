package dev.znlp.examples.demo;

import dev.znlp.segment.impl.personname.PersonNamePerceptron;

import java.io.IOException;

public class PersonNamePerceptronLoadDemo {
    public static void main(String[] args) throws IOException {
        PersonNamePerceptron perceptron = PersonNamePerceptron.load("znlp/personname/nr-name.model");
        System.out.println(perceptron.findPersonName("这是陈宝奇的快递"));
    }
}
