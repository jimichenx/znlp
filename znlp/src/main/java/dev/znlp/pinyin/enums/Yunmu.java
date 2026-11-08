package dev.znlp.pinyin.enums;

/**
 * 韵母枚举
 * <p>
 * 使用v 代替 ü
 * https://github.com/System-T/DimSim/blob/master/dimsim/utils/maps.py
 * </p>
 *
 * @author hankcs
 * @author jimichan
 */
public enum Yunmu {
    a(1.0f, 0.0f),
    ai(8.0f, 0.0f),
    an(1.0f, 1.0f),
    ang(1.0f, 1.5f),
    ao(5.0f, 0.0f),
    e(41f, 0.0f),
    ei(40f, 4.0f),
    en(42f, 0.5f),
    eng(42f, 1.0f),
    er(41f, 1f),
    i(60f, 1.0f),
    ia(0.0f, 0.0f),
    ian(0.0f, 1.0f),
    iang(0.0f, 1.5f),
    iao(5.0f, 1.5f),
    ie(40f, 4.5f),
    in(60f, 2.5f),
    ing(60f, 3.0f),
    iong(20, 9.5f),
    iu(20f, 4f),
    o(20f, 0.0f),
    ong(20f, 8.0f),
    ou(20f, 5.5f),
    u(80f, 0.0f),
    ua(2.0f, 0.0f),
    uai(8.0f, 1.5f),
    uan(2.0f, 1.0f),
    uang(2.0f, 1.5f),
    ue(40f, 5.0f),
    ui(40, 3.0f),
    un(43, 0.5f),
    uo(20f, 6.0f),
    v(80, 0.0f),
    ve(40, 5.0f),
    /**
     * 仅用于null类型的拼音
     */
    none(9999f, 9999f);

    private final float twoDCode1;
    private final float twoDCode2;

    Yunmu(float twoDCode1, float twoDCode2) {
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
