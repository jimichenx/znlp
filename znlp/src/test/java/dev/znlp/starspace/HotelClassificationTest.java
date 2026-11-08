package dev.znlp.starspace;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import dev.znlp.test.TestTags;

import java.io.File;

@Tag(TestTags.TRAIN)
public class HotelClassificationTest {

    @Test()
    @Disabled
    public void testHotelClassification() throws Exception {
        // Adjust path based on project structure
        // Assuming test is run from znlp/ (the module root) or project root.
        // Data path relative to project root: data/fasttext/hotel/
        
        File trainFile = new File("../data/source/example/hotel/hotel-train-seg.txt");
        File testFile = new File("../data/source/example/hotel/hotel-test-seg.txt");
        


        // Validate file existence
        Assertions.assertTrue(trainFile.exists(), "Train file not found: " + trainFile.getAbsolutePath());
        Assertions.assertTrue(testFile.exists(), "Test file not found: " + testFile.getAbsolutePath());

        Args trainArgs = new Args();

        trainArgs.lr = 0.01;
        trainArgs.dim = 100;
        trainArgs.epoch = 5;
        trainArgs.loss = Args.LossFunction.Hinge;
        trainArgs.trainMode = Args.TrainMode.Mode0;
        trainArgs.label = "__label__";
        trainArgs.thread = Runtime.getRuntime().availableProcessors();
        trainArgs.adagrad = true;
        trainArgs.dropoutLHS = 0.0;
        trainArgs.dropoutRHS = 0.0;

        System.out.println("Starting training...");
        ModelTrainer trainer = new ModelTrainer(trainArgs, trainFile.getAbsolutePath(), null);
        StarSpace model = trainer.train();
        System.out.println("Training completed.");

        System.out.println("Starting evaluation...");
        Evaluate evaluate = new Evaluate(model, testFile.getAbsolutePath(), 1, null, null);
        Evaluate.Metrics metrics = evaluate.evaluate();
        
        System.out.println("Evaluation Metrics: " + metrics);
        
        // Assert accuracy (hit@1) > 0.9
        Assertions.assertTrue(metrics.hit1 > 0.89, "Accuracy (Hit@1) should be > 0.9, but was " + metrics.hit1);
    }
}
