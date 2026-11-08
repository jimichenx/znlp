package dev.znlp.base;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import dev.znlp.base.common.ZeroCopyString;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for ZeroCopyString
 */
public class ZeroCopyStringTest {

    @Test
    public void testBasicConstruction() {
        String text = "你好世界";
        ZeroCopyString zcs = new ZeroCopyString(text);

        assertEquals(4, zcs.length());
        assertEquals('你', zcs.charAt(0));
        assertEquals('好', zcs.charAt(1));
        assertEquals('世', zcs.charAt(2));
        assertEquals('界', zcs.charAt(3));
        assertEquals(text, zcs.toString());
    }

    @Test
    public void testZeroCopySubSequence() {
        String text = "自然语言处理";
        ZeroCopyString zcs = new ZeroCopyString(text);

        // 测试 subSequence 不会复制数组
        ZeroCopyString sub1 = zcs.subSequence(0, 2);
        assertEquals("自然", sub1.toString());
        assertEquals(2, sub1.length());

        ZeroCopyString sub2 = zcs.subSequence(2, 6);
        assertEquals("语言处理", sub2.toString());

        // 验证共享同一个char数组
        assertSame(zcs.getChars(), sub1.getChars());
        assertSame(zcs.getChars(), sub2.getChars());
    }

    @Test
    public void testSubstring() {
        ZeroCopyString zcs = new ZeroCopyString("分词系统测试");

        ZeroCopyString sub1 = zcs.substring(0, 2);
        assertEquals("分词", sub1.toString());

        ZeroCopyString sub2 = zcs.substring(2);
        assertEquals("系统测试", sub2.toString());

        ZeroCopyString sub3 = zcs.substring(2, 4);
        assertEquals("系统", sub3.toString());
    }

    @Test
    public void testMutableCharArray() {
        char[] chars = "abc".toCharArray();
        ZeroCopyString zcs = new ZeroCopyString(chars);

        assertEquals("abc", zcs.toString());

        // 修改原始数组
        chars[0] = 'x';
        assertEquals('x', zcs.charAt(0));
        assertEquals("xbc", zcs.toString());

        // 通过 setCharAt 修改
        zcs.setCharAt(1, 'y');
        assertEquals('y', chars[1]);
        assertEquals("xyc", zcs.toString());
    }

    @Test
    public void testSetCharAt() {
        ZeroCopyString zcs = new ZeroCopyString("hello");

        zcs.setCharAt(0, 'H');
        assertEquals("Hello", zcs.toString());

        zcs.setCharAt(4, '!');
        assertEquals("Hell!", zcs.toString());
    }

    @Test
    public void testSubSequenceImmutability() {
        // 测试子序列和原序列共享char数组
        ZeroCopyString parent = new ZeroCopyString("abcdef");
        ZeroCopyString child = parent.subSequence(2, 5);

        assertEquals("cde", child.toString());

        // 修改父序列
        parent.setCharAt(2, 'X');
        assertEquals('X', child.charAt(0));
        assertEquals("Xde", child.toString());

        // 修改子序列
        child.setCharAt(1, 'Y');
        assertEquals('Y', parent.charAt(3));
        assertEquals("abXYef", parent.toString());
    }

    @Test
    public void testOffsetAndLength() {
        char[] chars = "0123456789".toCharArray();
        ZeroCopyString zcs = new ZeroCopyString(chars, 3, 4);

        assertEquals(4, zcs.length());
        assertEquals("3456", zcs.toString());
        assertEquals('3', zcs.charAt(0));
        assertEquals('6', zcs.charAt(3));
        assertEquals(3, zcs.getOffset());
    }

    @Test
    public void testToCharArray() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        char[] result = zcs.toCharArray();

        assertArrayEquals("test".toCharArray(), result);
        // 验证是复制的新数组，不是原始数组
        assertNotSame(zcs.getChars(), result);
    }

    @Test
    public void testEquals() {
        ZeroCopyString zcs1 = new ZeroCopyString("hello");
        ZeroCopyString zcs2 = new ZeroCopyString("hello");
        ZeroCopyString zcs3 = new ZeroCopyString("world");
        String str = "hello";

        assertEquals(zcs1, zcs2);
        assertEquals(zcs1, str);
        assertNotEquals(zcs1, zcs3);
        assertNotEquals(null, zcs1);
    }

    @Test
    public void testHashCode() {
        ZeroCopyString zcs1 = new ZeroCopyString("test");
        ZeroCopyString zcs2 = new ZeroCopyString("test");
        ZeroCopyString zcs3 = new ZeroCopyString("different");

        assertEquals(zcs1.hashCode(), zcs2.hashCode());
        assertNotEquals(zcs1.hashCode(), zcs3.hashCode());
    }

    @Test
    public void testBoundaryConditions() {
        ZeroCopyString zcs = new ZeroCopyString("");
        assertEquals(0, zcs.length());
        assertEquals("", zcs.toString());

        ZeroCopyString single = new ZeroCopyString("a");
        assertEquals(1, single.length());
        assertEquals('a', single.charAt(0));
    }

    @Test
    public void testCharAtNegativeIndex() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.charAt(-1);
        });
    }

    @Test
    public void testCharAtOutOfBounds() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.charAt(4);
        });
    }

    @Test
    public void testSetCharAtNegativeIndex() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.setCharAt(-1, 'x');
        });
    }

    @Test
    public void testSetCharAtOutOfBounds() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.setCharAt(4, 'x');
        });
    }

    @Test
    public void testSubSequenceNegativeStart() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.subSequence(-1, 2);
        });
    }

    @Test
    public void testSubSequenceEndOutOfBounds() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.subSequence(0, 5);
        });
    }

    @Test
    public void testSubSequenceStartGreaterThanEnd() {
        ZeroCopyString zcs = new ZeroCopyString("test");
        assertThrows(IndexOutOfBoundsException.class, () -> {
            zcs.subSequence(3, 2);
        });
    }

    @Test
    public void testNullCharArray() {
        assertThrows(NullPointerException.class, () -> {
            new ZeroCopyString((char[]) null);
        });
    }

    @Test
    public void testChineseTextSegmentation() {
        // 模拟分词场景
        String text = "我爱自然语言处理";
        ZeroCopyString zcs = new ZeroCopyString(text);

        // 分词结果: 我/爱/自然/语言/处理
        ZeroCopyString word1 = zcs.subSequence(0, 1);
        ZeroCopyString word2 = zcs.subSequence(1, 2);
        ZeroCopyString word3 = zcs.subSequence(2, 4);
        ZeroCopyString word4 = zcs.subSequence(4, 6);
        ZeroCopyString word5 = zcs.subSequence(6, 8);

        assertEquals("我", word1.toString());
        assertEquals("爱", word2.toString());
        assertEquals("自然", word3.toString());
        assertEquals("语言", word4.toString());
        assertEquals("处理", word5.toString());

        // 验证所有子序列共享同一个char数组（zero-copy）
        assertSame(zcs.getChars(), word1.getChars());
        assertSame(zcs.getChars(), word2.getChars());
        assertSame(zcs.getChars(), word3.getChars());
        assertSame(zcs.getChars(), word4.getChars());
        assertSame(zcs.getChars(), word5.getChars());
    }

    @Disabled
    @Test
    public void testPerformanceComparison() {
        // 对比普通String的substring和ZeroCopyString的性能
        String longText = "这是一个很长的文本" + "用于测试分词性能".repeat(100);

        // ZeroCopyString方式
        long start = System.nanoTime();
        ZeroCopyString zcs = new ZeroCopyString(longText);
        for (int i = 0; i < longText.length() - 1; i++) {
            ZeroCopyString sub = zcs.subSequence(i, i + 2);
        }
        long zeroCopyTime = System.nanoTime() - start;

        // String方式
        start = System.nanoTime();
        for (int i = 0; i < longText.length() - 1; i++) {
            String sub = longText.substring(i, i + 2);
        }
        long stringTime = System.nanoTime() - start;

        System.out.println("ZeroCopyString time: " + zeroCopyTime / 1000 + " μs");
        System.out.println("String time: " + stringTime / 1000 + " μs");
        System.out.println("Speed up: " + (double) stringTime / zeroCopyTime + "x");

        // ZeroCopyString应该明显更快
        assertTrue(zeroCopyTime < stringTime, "ZeroCopyString should be faster than String");
    }
}
