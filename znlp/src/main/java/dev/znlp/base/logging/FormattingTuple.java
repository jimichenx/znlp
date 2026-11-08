package dev.znlp.base.logging;

/**
 * Holds the results of formatting done by {@link MessageFormatter}.
 */
record FormattingTuple(String message, Throwable throwable) {

}