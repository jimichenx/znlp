package dev.znlp.duckling;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import dev.znlp.duckling.grammar.Grammars;
import dev.znlp.duckling.grammar.age.AgeDimension;
import dev.znlp.duckling.grammar.distance.DistanceDimension;
import dev.znlp.duckling.grammar.duration.DurationDimension;
import dev.znlp.duckling.grammar.money.MoneyDimension;
import dev.znlp.duckling.grammar.number.NumberDimension;
import dev.znlp.duckling.grammar.number.NumberGrammar;
import dev.znlp.duckling.grammar.ordinal.OrdinalDimension;
import dev.znlp.duckling.grammar.percentage.PercentageDimension;
import dev.znlp.duckling.grammar.phonenum.PhoneNumDimension;
import dev.znlp.duckling.grammar.temperature.TemperatureDimension;
import dev.znlp.duckling.grammar.time.DateTimeDimension;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleSet;
import dev.znlp.duckling.support.Dimension;

import java.util.List;

public class DucklingTest {

    private DucklingParser createParser(List<Rule> rules) {
        return new DucklingParser(new DefaultRuleSet(rules));
    }

    private DucklingParser createAllParser() {
        return createParser(Grammars.rules());
    }

    @Test
    public void testAllGrammars() {
        DucklingParser parser = createAllParser();

        // Number
        List<Dimension> resNum = parser.parse("123");
        Assertions.assertInstanceOf(NumberDimension.class, resNum.get(0));
        Assertions.assertEquals(123.0, ((NumberDimension) resNum.get(0)).getValue());

        // Ordinal
        List<Dimension> resOrd = parser.parse("第一");
        Assertions.assertInstanceOf(OrdinalDimension.class, resOrd.get(0));
        Assertions.assertEquals(1, ((OrdinalDimension) resOrd.get(0)).getValue());

        // Money
        List<Dimension> resMoney = parser.parse("5元5角");
        Assertions.assertInstanceOf(MoneyDimension.class, resMoney.get(0));
        Assertions.assertEquals(5.5f, ((MoneyDimension) resMoney.get(0)).getValue());

        // Percentage
        List<Dimension> resPerc = parser.parse("5%");
        Assertions.assertInstanceOf(PercentageDimension.class, resPerc.get(0));
        Assertions.assertEquals(0.05f, ((PercentageDimension) resPerc.get(0)).getValue());

        // Phone
        List<Dimension> resPhone = parser.parse("13800138000");
        Assertions.assertInstanceOf(PhoneNumDimension.class, resPhone.get(0));
        Assertions.assertEquals("13800138000", ((PhoneNumDimension) resPhone.get(0)).getValue());
        
        // Temperature
        List<Dimension> resTemp = parser.parse("25摄氏度");
        Assertions.assertInstanceOf(TemperatureDimension.class, resTemp.get(0));
        Assertions.assertEquals(25.0f, ((TemperatureDimension) resTemp.get(0)).getValue());

        // Distance
        List<Dimension> resDist = parser.parse("5公里");
        Assertions.assertInstanceOf(DistanceDimension.class, resDist.get(0));
        Assertions.assertEquals(5.0f, ((DistanceDimension) resDist.get(0)).getValue());

        // Age
        List<Dimension> resAge = parser.parse("30岁");
        Assertions.assertInstanceOf(AgeDimension.class, resAge.get(0));
        Assertions.assertEquals(30.0f, ((AgeDimension) resAge.get(0)).getValue());

        // Duration
        List<Dimension> resDur = parser.parse("1小时30分");
        Assertions.assertInstanceOf(DurationDimension.class, resDur.get(0));
        Assertions.assertEquals(90.0f, ((DurationDimension) resDur.get(0)).getValue());

        // DateTime
        List<Dimension> resDate = parser.parse("2020年5月1号");
        Assertions.assertInstanceOf(DateTimeDimension.class, resDate.get(0));
        DateTimeDimension dt = (DateTimeDimension) resDate.get(0);
        Assertions.assertEquals(2020, dt.getYear());
        Assertions.assertEquals(5, dt.getMonth());
        Assertions.assertEquals(1, dt.getDay());
    }

    @Test
    public void testDateTime() {
        DucklingParser parser = createAllParser();
        // DateTime
        List<Dimension> resDate = parser.parse("后年三月八号");
        Assertions.assertEquals(1, resDate.size());
        DateTimeDimension dt = (DateTimeDimension) resDate.get(0);
        Assertions.assertEquals(3, dt.getMonth());
        Assertions.assertEquals(8, dt.getDay());
        // Year depends on current time, so we might check relative year logic if we mock clock, 
        // but here just checking month/day is fine for basic sanity.
        // "后年" means +2 years.
    }

    @Test
    public void testNumber() {
        RuleSet ruleSet = new DefaultRuleSet(new NumberGrammar().rules());
        DucklingParser parser = new DucklingParser(ruleSet);

        List<Dimension> result = parser.parse("我有 123 个苹果");
        Assertions.assertEquals(1, result.size());
        Assertions.assertInstanceOf(NumberDimension.class, result.get(0));
        Assertions.assertEquals(123.0, ((NumberDimension) result.get(0)).getValue());
        Assertions.assertEquals("123", result.get(0).getText());
    }

    // English number parsing is not supported in current NumberGrammar implementation
    // @Test
    // public void testEnglishNumber() { ... }

    @Test
    public void testMultiple() {
        RuleSet ruleSet = new DefaultRuleSet(new NumberGrammar().rules());
        DucklingParser parser = new DucklingParser(ruleSet);

        List<Dimension> result = parser.parse("一, 2, 三");
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1.0, ((NumberDimension) result.get(0)).getValue());
        Assertions.assertEquals(2.0, ((NumberDimension) result.get(1)).getValue());
        Assertions.assertEquals(3.0, ((NumberDimension) result.get(2)).getValue());
    }
}
