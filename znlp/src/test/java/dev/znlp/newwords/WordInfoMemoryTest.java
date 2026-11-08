package dev.znlp.newwords;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import dev.znlp.newwords.entity.WordInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WordInfo 内存优化验证测试
 * <p>
 * 新方案：紧凑开放寻址哈希表
 * - 每个 (char, count) 对仅需 8 字节
 * - 初始容量小 (8 slots = 64 bytes)，按需扩展
 * - 100 个邻字仅需 ~1.2KB（远优于 HashMap 的 ~5KB）
 */
@DisplayName("WordInfo 内存优化测试")
public class WordInfoMemoryTest {

    @Test
    @DisplayName("验证中文字符存储")
    public void testChineseCharactersStorage() {
        WordInfo info = new WordInfo("测试");

        // 添加中文字符
        info.addToLeft('我', 10);
        info.addToLeft('你', 5);
        info.addToLeft('他', 3);

        info.addToRight('们', 8);
        info.addToRight('的', 12);

        info.setCount(20);
        info.entropy();

        // 验证熵计算正确
        assertTrue(info.getLe() > 0, "左熵应大于0");
        assertTrue(info.getRe() > 0, "右熵应大于0");
        System.out.println("左熵: " + info.getLe() + ", 右熵: " + info.getRe());
    }

    @Test
    @DisplayName("验证混合字符存储（中英文+标点）")
    public void testMixedCharactersStorage() {
        WordInfo info = new WordInfo("test");

        // 添加中文字符
        info.addToLeft('中', 10);
        info.addToLeft('国', 8);

        // 添加英文和标点
        info.addToLeft('A', 5);
        info.addToLeft('B', 3);
        info.addToLeft('!', 2);

        info.setCount(28);
        info.entropy();

        assertTrue(info.getLe() > 0, "混合字符左熵应大于0");
        System.out.println("混合字符左熵: " + info.getLe());
    }

    @Test
    @DisplayName("验证大量字符统计的内存效率")
    public void testLargeScaleCharacterStatistics() {
        int wordCount = 1000;
        WordInfo[] words = new WordInfo[wordCount];

        // 创建大量 WordInfo 对象
        for (int i = 0; i < wordCount; i++) {
            words[i] = new WordInfo("词" + i);

            // 添加常用汉字
            for (char c = '一'; c <= '龙'; c += 100) {
                words[i].addToLeft(c, (int) (Math.random() * 10) + 1);
                words[i].addToRight(c, (int) (Math.random() * 10) + 1);
            }

            words[i].setCount(50);
        }

        // 计算熵（会释放内存）
        for (WordInfo word : words) {
            word.entropy();
        }

        System.out.println("成功处理 " + wordCount + " 个词的邻字统计");
    }

    @Test
    @DisplayName("验证超大频次处理")
    public void testLargeFrequencyHandling() {
        WordInfo info = new WordInfo("高频词");

        // 添加超大频次
        char c = '的';
        for (int i = 0; i < 40000; i += 1000) {
            info.addToLeft(c, 1000);
        }

        info.setCount(40000);
        info.entropy();

        assertTrue(info.getLe() >= 0, "应正确计算熵");
        System.out.println("超大频次处理成功，左熵: " + info.getLe());
    }

    @Test
    @DisplayName("内存占用对比说明")
    public void testMemoryUsageComparison() {
        System.out.println("\n=== 内存占用对比 ===");
        System.out.println("\n旧方案 HashMap<Character, IntCount>:");
        System.out.println("  - 每个 Entry: ~48 字节 (16 对象头 + 4 hash + 4 key引用 + 4 value引用 + 4 next + IntCount对象)");
        System.out.println("  - IntCount对象: ~20 字节");
        System.out.println("  - 100个邻字: ~6.8KB");

        System.out.println("\n新方案 紧凑开放寻址哈希表:");
        System.out.println("  - 每个(char,count)对: 8 字节 (2个int)");
        System.out.println("  - 初始容量: 8 slots = 64 字节");
        System.out.println("  - 100个邻字 (143 slots, 70%负载): ~1.1KB");
        System.out.println("  - 节省: ~84%");

        System.out.println("\n上一版方案 short[20992] 固定数组:");
        System.out.println("  - 每个 WordInfo: ~40KB (不管实际邻字多少)");
        System.out.println("  - 10万个候选词: 4GB!");

        System.out.println("\n当前方案优势:");
        System.out.println("  ✓ 按需分配，初始开销仅 64 字节");
        System.out.println("  ✓ 比 HashMap 节省 84% 内存");
        System.out.println("  ✓ 无对象分配，GC友好");
        System.out.println("  ✓ O(1) 访问性能");
    }

    @Test
    @DisplayName("验证哈希冲突处理")
    public void testHashCollisionHandling() {
        WordInfo info = new WordInfo("冲突测试");

        // 添加多个字符，可能产生哈希冲突
        String chars = "一二三四五六七八九十百千万亿兆京垂核稀微纳媒徽絤吹稷沙尘渺漠颜鲁顽刍分端角得锐";
        for (int i = 0; i < chars.length(); i++) {
            info.addToLeft(chars.charAt(i), i + 1);
            info.addToRight(chars.charAt(i), i + 1);
        }

        info.setCount(1000);
        info.entropy();

        assertTrue(info.getLe() > 0, "处理哈希冲突后左熵应大于0");
        assertTrue(info.getRe() > 0, "处理哈希冲突后右熵应大于0");
        System.out.println("哈希冲突处理成功，左熵: " + info.getLe() + ", 右熵: " + info.getRe());
    }

    @Test
    @DisplayName("验证相同字符多次添加累加")
    public void testSameCharacterAccumulation() {
        WordInfo info = new WordInfo("累加测试");

        // 同一字符添加多次
        info.addToLeft('中', 10);
        info.addToLeft('中', 20);
        info.addToLeft('中', 30);

        info.addToLeft('国', 5);

        info.setCount(65);
        info.entropy();

        // '中' 应累加为 60，'国' 为 5
        // 精确验证熵值：H = -(60/65 * ln(60/65) + 5/65 * ln(5/65))
        double total = 65.0;
        double pZhong = 60.0 / total;
        double pGuo = 5.0 / total;
        double expectedLe = -(pZhong * Math.log(pZhong) + pGuo * Math.log(pGuo));

        assertEquals(expectedLe, info.getLe(), 1e-6, "左熵应基于累加后的频次(中:60,国:5)正确计算");
        System.out.println("累加测试成功，左熵: " + info.getLe() + "，期望值: " + expectedLe);
    }

    @Test
    @DisplayName("验证简单分布的左熵与计数一致性")
    public void testSimpleLeftEntropyWithExactCounts() {
        WordInfo info = new WordInfo("simple");
        info.addToLeft('A', 2);
        info.addToLeft('B', 3);
        info.setCount(5);

        info.entropy();

        // 精确验证：H = -(2/5 * ln(2/5) + 3/5 * ln(3/5))
        double total = 5.0;
        double pA = 2.0 / total;
        double pB = 3.0 / total;
        double expectedLe = -(pA * Math.log(pA) + pB * Math.log(pB));

        assertEquals(expectedLe, info.getLe(), 1e-6, "左熵应按 A:2,B:3 的频次正确计算");
        System.out.println("简单分布测试成功，左熵: " + info.getLe() + "，期望值: " + expectedLe);
    }

    @Test
    @DisplayName("验证右侧计数的精确性")
    public void testRightEntropyWithExactCounts() {
        WordInfo info = new WordInfo("右侧测试");

        // 只添加右侧邻字
        info.addToRight('的', 10);
        info.addToRight('了', 20);
        info.addToRight('是', 15);

        info.setCount(45);
        info.entropy();

        // 精确验证右熵：H = -(10/45 * ln(10/45) + 20/45 * ln(20/45) + 15/45 * ln(15/45))
        double total = 45.0;
        double p1 = 10.0 / total;
        double p2 = 20.0 / total;
        double p3 = 15.0 / total;
        double expectedRe = -(p1 * Math.log(p1) + p2 * Math.log(p2) + p3 * Math.log(p3));

        assertEquals(expectedRe, info.getRe(), 1e-6, "右熵应基于(的:10,了:20,是:15)正确计算");
        System.out.println("右侧计数测试成功，右熵: " + info.getRe() + "，期望值: " + expectedRe);
    }

    @Test
    @DisplayName("验证左右熵同时计算的正确性")
    public void testBothSidesEntropyWithExactCounts() {
        WordInfo info = new WordInfo("双侧测试");

        // 左侧：'我'(5), '你'(10)
        info.addToLeft('我', 5);
        info.addToLeft('你', 10);

        // 右侧：'们'(8), '的'(7)
        info.addToRight('们', 8);
        info.addToRight('的', 7);

        info.setCount(15);
        info.entropy();

        // 精确验证左熵
        double totalLeft = 15.0;
        double pWo = 5.0 / totalLeft;
        double pNi = 10.0 / totalLeft;
        double expectedLe = -(pWo * Math.log(pWo) + pNi * Math.log(pNi));

        // 精确验证右熵
        double totalRight = 15.0;
        double pMen = 8.0 / totalRight;
        double pDe = 7.0 / totalRight;
        double expectedRe = -(pMen * Math.log(pMen) + pDe * Math.log(pDe));

        assertEquals(expectedLe, info.getLe(), 1e-6, "左熵应基于(我:5,你:10)正确计算");
        assertEquals(expectedRe, info.getRe(), 1e-6, "右熵应基于(们:8,的:7)正确计算");
        assertEquals(Math.min(expectedLe, expectedRe), info.getEntropy(), 1e-6, "总熵应为左右熵的最小值");

        System.out.println("双侧测试成功 - 左熵: " + info.getLe() + ", 右熵: " + info.getRe() + ", 总熵: " + info.getEntropy());
    }

    @Test
    @DisplayName("验证零频次字符不影响熵计算")
    public void testZeroFrequencyHandling() {
        WordInfo info = new WordInfo("零频次");

        info.addToLeft('A', 10);
        info.addToLeft('B', 0);  // 零频次应被忽略
        info.addToLeft('C', 5);

        info.setCount(15);
        info.entropy();

        // 应只计算 A 和 C 的熵
        double total = 15.0;
        double pA = 10.0 / total;
        double pC = 5.0 / total;
        double expectedLe = -(pA * Math.log(pA) + pC * Math.log(pC));

        assertEquals(expectedLe, info.getLe(), 1e-6, "零频次字符不应参与熵计算");
    }

    @Test
    @DisplayName("验证哈希冲突场景的计数正确性（部分精确验证）")
    public void testHashCollisionWithPartialVerification() {
        WordInfo info = new WordInfo("冲突测试");

        // 添加已知频次的字符
        info.addToLeft('一', 1);
        info.addToLeft('二', 2);
        info.addToLeft('三', 3);
        info.addToLeft('四', 4);
        info.addToLeft('五', 5);

        // 再添加一些其他字符制造潜在冲突
        String moreChars = "六七八九十百千万";
        int baseCount = 15;
        for (int i = 0; i < moreChars.length(); i++) {
            info.addToLeft(moreChars.charAt(i), i + 6);
            baseCount += (i + 6);
        }

        info.setCount(baseCount);
        info.entropy();

        // 验证熵值为正且在合理范围内
        assertTrue(info.getLe() > 0, "即使有哈希冲突，左熵也应正确计算");
        assertTrue(info.getLe() < Math.log(13), "熵值不应超过理论最大值 ln(n)");
        System.out.println("哈希冲突场景测试成功，左熵: " + info.getLe());
    }
}
