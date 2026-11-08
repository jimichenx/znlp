package dev.znlp.segment.pipeline;

/**
 * 强制成词阶段的优先级。数字大的先执行。
 */
public final class StagePriority {

    public static final int PERSON_NAME = 100;
    public static final int USER_DICTIONARY = 200;
    public static final int CORRECTION = 300;

    private StagePriority() {
    }
}
