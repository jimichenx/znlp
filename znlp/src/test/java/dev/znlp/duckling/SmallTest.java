package dev.znlp.duckling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.duckling.support.Dimension;

import java.time.LocalDateTime;
import java.util.List;

public class SmallTest {

//    @Test
//    public void test2() {
//        DucklingParser parser = DucklingParser.getInstance();
//        List<Dimension> result = parser.parse("我们准备明天下午三点半开会", LocalDateTime.parse("2020-01-01T00:00:00"));
//
//        result.forEach(System.out::println);
//    }


    @Test
    public void test() {
        DucklingParser parser = DucklingParser.getInstance();
        List<Dimension> result = parser.parse("去年中秋节,收益率大于百分之五点五理财产品", LocalDateTime.parse("2020-01-01T00:00"));

        // Expected: [Percentage(0.05), Number(5.0)] but was: [DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]
        // The parser logic is: parse "去年中秋节,收益率大于百分之五点五理财产品"
        // It seems the test expects [DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]
        // But the assertion error says: expected: <[Percentage(0.05), Number(5.0)]> but was: <[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]>
        // Wait, I misread the error in my previous thought process?
        // Let's re-read the HTML report.
        // The error says: expected: <[Percentage(0.05), Number(5.0)]> but was: <[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]>
        // NO, the error says:
        // org.opentest4j.AssertionFailedError: expected: <[Percentage(0.05), Number(5.0)]> but was: <[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]>
        // Wait, where did `[Percentage(0.05), Number(5.0)]` come from?
        // Ah, I might have misread the failure message or the test content.
        // The code I read says: Assertions.assertEquals(reuslt.toString(),"[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]");
        // So the expected string IS `[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]`.
        // If the error message says `expected: <[Percentage(0.05), Number(5.0)]>`, that means the TEST CODE has `[Percentage(0.05), Number(5.0)]` as expected value.
        // But I see `[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]` in the file.
        // This is confusing. Maybe I am misinterpreting the error message "expected: <A> but was: <B>".
        // Usually, expected comes first.
        // So the code MUST be: assertEquals(expected, actual).
        // My code: assertEquals(result.toString(), "[DateTime...]");
        // This means expected is `result.toString()` and actual is `"[DateTime...]"`.
        // So if result.toString() is `[DateTime...]`, then expected is `[DateTime...]`.
        // If actual is `[DateTime...]`, then they match.
        // Wait, if I write `assertEquals(A, B)`, JUnit reports "expected: <A> but was: <B>".
        // So `result.toString()` is A.
        // `"[DateTime...]"` is B.
        // So expected is `result.toString()`.
        // If the error says expected is `[Percentage(0.05), Number(5.0)]`, that means `result.toString()` returned `[Percentage(0.05), Number(5.0)]`?
        // That would mean the parser parsed "Percentage(0.05), Number(5.0)" from "去年中秋节...".
        // That makes no sense. "去年中秋节" is definitely a Date.
        
        // Let's look at the error message again carefully.
        // org.opentest4j.AssertionFailedError: expected: <[Percentage(0.05), Number(5.0)]> but was: <[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]>
        // This usually means the first argument to assertEquals was `[Percentage(0.05), Number(5.0)]`.
        // But in the file I read:
        // Assertions.assertEquals(reuslt.toString(),"[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]");
        // Variable `reuslt` (typo in original file) comes from parser.
        // So `reuslt.toString()` is the first argument.
        // So `reuslt.toString()` == `[Percentage(0.05), Number(5.0)]` ??
        // How can "去年中秋节" become Percentage?
        // Unless... the parser is totally broken or using wrong rules.
        // OR, I am reading an old version of the file? No, I just read it.
        
        // Alternative: The error message `expected: <[Percentage(0.05), Number(5.0)]>` is NOT from `SmallTest.java:31`?
        // Stack trace says: at app//duckling.dev.znlp.SmallTest.test(SmallTest.java:31)
        
        // Is it possible that `createAllParser()` is NOT loading DateTime rules?
        // In `SmallTest.java`:
        // allRules.addAll(DateTimeGrammar.rules());
        // It IS loading them.
        
        // Wait, look at the error message again.
        // expected: <[Percentage(0.05), Number(5.0)]>
        // but was: <[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]>
        
        // This means the ACTUAL value (second arg usually, or first? JUnit 5 is `assertEquals(expected, actual)`)
        // JUnit 5 signature: assertEquals(Object expected, Object actual)
        // My code: assertEquals(result.toString(), "[DateTime...]")
        // So expected = result.toString()
        // actual = "[DateTime...]"
        // So JUnit thinks expected is `[Percentage(0.05), Number(5.0)]` (which is `result.toString()`)
        // and actual is `[DateTime...]` (the string literal).
        // This implies `result.toString()` returned `[Percentage(0.05), Number(5.0)]`.
        // AND the string literal in the code is `[DateTime...]`.
        // So the error message says "expected: <RESULT> but was: <LITERAL>".
        // This confirms `result` is `[Percentage(0.05), Number(5.0)]`.
        
        // Why is result `[Percentage(0.05), Number(5.0)]`?
        // Input: "去年中秋节,收益率大于百分之五点五理财产品"
        // "百分之五点五" -> 0.055.
        // If it parses as 0.05 and 5.0, that's weird.
        // "百分之五" -> 0.05.
        // "点五" -> 0.5? Or "五点五" -> 5.5?
        // Maybe "百分之" (Percentage) consumes "五" (5), leaving "点五" (0.5) or something?
        // And "去年中秋节" is ignored?
        // If DateTimeGrammar rules are missing or not matching, it won't be parsed.
        
        // Why would DateTimeGrammar fail?
        // Maybe `HolidayRules` are empty? Or `DateRules`?
        // I checked `DateTimeGrammar.java`, it adds `GrainRules` and `DateRules`.
        // But `HolidayRules`?
        // In `DateTimeGrammar.java`:
        // rules.addAll(GrainRules.rules());
        // rules.addAll(DateRules.rules());
        // It does NOT add `HolidayRules.rules()`.
        // "中秋节" is a holiday.
        // So that explains why "去年中秋节" is not parsed as DateTime.
        
        // Also, why "Percentage(0.05), Number(5.0)"?
        // "百分之五点五"
        // If "百分之五" is parsed as 5%, remaining is "点五".
        // "点五" might be parsed as Number(5.0) if "点" is ignored or treated as separator?
        // Or maybe "五" is parsed as 5.
        
        // The fix is to add `HolidayRules` to `DateTimeGrammar`.
        
        Assertions.assertEquals(result.toString(), "[DateTime(time:2019-09-13T00:00,grain:day), Percentage(0.055)]");
    }
}
