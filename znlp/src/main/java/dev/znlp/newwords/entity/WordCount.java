package dev.znlp.newwords.entity;

/**
 * 词和词数量
 */
public record WordCount(String word, int count) implements Comparable<WordCount> {

    @Override
    public int compareTo(WordCount other) {
        return Integer.compare(other.count, this.count);
    }
}
