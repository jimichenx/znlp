package dev.znlp.pinyin.enums;

/**
 * 声母枚举
 * <p>
 * twoDCode from
 * https://github.com/System-T/DimSim/blob/master/dimsim/utils/maps.py
 * </p>
 *
 * @author hankcs
 * @author jimichan
 */
public enum Shengmu {
    b(1.0f, 0.5f),
    c(31f, 1.5f),
    ch(31f, 1.7f),
    d(12.0f, 0.5f),
    f(7.0f, 4.0f),
    g(7.0f, 0.5f),
    h(7.0f, 3.0f),
    j(30.0f, 0.5f),
    k(7.0f, 1.5f),
    l(22.5f, 1.5f),
    m(50.0f, 3.5f),
    n(22.5f, 0.5f),
    p(1.0f, 1.5f),
    q(31.0f, 0.5f),
    r(22.5f, 2.5f),
    s(33f, 3.5f),
    sh(33f, 3.7f),
    t(12.0f, 1.5f),
    w(40f, 5.0f),
    x(33f, 2.5f),
    y(40.0f, 0.0f),
    z(30f, 1.5f),
    zh(30f, 1.7f),
    /**
     * 零声母
     */
    none(99999f, 99999f);

    private final float twoDCode1;
    private final float twoDCode2;

    Shengmu(float twoDCode1, float twoDCode2) {
        this.twoDCode1 = twoDCode1;
        this.twoDCode2 = twoDCode2;
    }

    public float getTwoDCode1() {
        return twoDCode1;
    }

    public float getTwoDCode2() {
        return twoDCode2;
    }
}
