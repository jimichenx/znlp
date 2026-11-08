package dev.znlp.examples.train;

import dev.znlp.duckling.DucklingParser;
import dev.znlp.duckling.grammar.Grammars;
import dev.znlp.duckling.ml.Example;
import dev.znlp.duckling.ml.MlModel;
import dev.znlp.duckling.ml.NaiveBayesTrainer;
import dev.znlp.duckling.rule.DefaultRuleSet;
import dev.znlp.duckling.rule.Rule;
import dev.znlp.duckling.rule.RuleSet;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class DucklingModelTrainer {


    public static void main(String[] args) {
        System.out.println("Start training Duckling model...");

        // 1. Collect all rules
        List<Rule> rules = Grammars.rules();

        System.out.println("Loaded " + rules.size() + " rules.");

        // 2. Collect all examples
        List<Example> examples = Grammars.examples();
        System.out.println("Loaded " + examples.size() + " examples.");

        // 3. Prepare parser and trainer
        RuleSet ruleSet = new DefaultRuleSet(rules);
        DucklingParser parser = new DucklingParser(ruleSet);
        NaiveBayesTrainer trainer = new NaiveBayesTrainer();

        // 4. Train
        // Use the test clock from TrainingData to match the examples' expectations
        MlModel model = trainer.train(rules, examples, parser, TrainingData.TEST_TIME);
        System.out.println("Training completed.");

        // 5. Save model
        File resourcesDir = new File("znlp-resources/src/main/resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        File modelFile = new File(resourcesDir, "/znlp/duckling.model");
        modelFile.getParentFile().mkdirs();
        
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(modelFile))) {
            model.write(out);
            System.out.println("Model saved to: " + modelFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save model: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
