package dev.znlp.examples.demo;

import dev.znlp.pinyin.spliter.PinyinSplitPerceptron;

import java.io.IOException;

public class PinyinSplitPerceptronLoadDemo {
    public static void main(String[] args) throws IOException {
        PinyinSplitPerceptron perceptron = PinyinSplitPerceptron.load("znlp/pinyin/pinyin-split.model");
        System.out.println(perceptron.split("zhuangzhangjilu"));
    }
}
