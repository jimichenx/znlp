package dev.znlp.segment.impl.pos;


import org.junit.jupiter.api.Test;
import dev.znlp.base.common.PkuParser;
import dev.znlp.base.common.PkuWord;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 测试：验证 PkuParser 对复合词的解析行为。
 */
public class PkuParserTest {

    @Test
    public void testCompoundLine() {
        String line = "陈/nr 汝烨/nr 压题/vn 照片/n ：/w [绵阳/ns 高新技术/n 产业/n 开发区/n]nt 内/f [长虹/nz 家电城/n]ns 一角/n 。/w";
        List<PkuWord> words = PkuParser.parseToWords(line);
        // 保证 parseToWords 返回非空结果
        assertNotNull(words);
        assertTrue(words.size() > 0);

        String flat = PkuParser.parseToFlatString(line);
        System.out.println(flat );
        // 展开后的字符串应包含某些子词
        assertTrue( flat.contains("绵阳/ns"),"应包含 绵阳/ns");
        assertTrue( flat.contains("高新技术/n"),"应包含 高新技术/n");
        assertTrue( flat.contains("长虹/nz"),"应包含 长虹/nz");
    }

    @Test
    public void testSingleBracket() {
        String line = "[８６３计划/nz]nz";
        List<PkuWord> words = PkuParser.parseToWords(line);
        assertNotNull(words);
        assertTrue(words.size() > 0);

        String flat = PkuParser.parseToFlatString(line);
        assertTrue(flat.contains("８６３计划/nz"),"应包含 ８６３计划/nz");
    }
}

