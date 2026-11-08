package dev.znlp.perceptron;

/**
 * Evaluation results for perceptron models, containing precision, recall, and F1 score.
 *
 * @author jimichan
 */
public class EvaluateResult {
    /**
     * Precision rate.
     */
    private final float precision;

    /**
     * Recall rate.
     */
    private final float recall;

    /**
     * Constructs an EvaluateResult with given precision and recall.
     *
     * @param precision the precision rate
     * @param recall    the recall rate
     */
    public EvaluateResult(float precision, float recall) {
        this.precision = precision;
        this.recall = recall;
    }

    /**
     * Constructs an EvaluateResult by calculating precision and recall from raw counts.
     *
     * @param goldTotal total number of items in the gold standard
     * @param predTotal total number of items predicted by the model
     * @param correct   number of items correctly predicted
     */
    public EvaluateResult(int goldTotal, int predTotal, int correct) {
        this.precision = (correct * 100.0f / predTotal);
        this.recall = (correct * 100.0f / goldTotal);
    }


    /**
     * Returns the F1 score, which is the harmonic mean of precision and recall.
     *
     * @return the F1 score
     */
    public float getF1() {
        return (2.0f * precision * recall / (precision + recall));
    }

    /**
     * Returns the precision rate.
     *
     * @return the precision
     */
    public float getPrecision() {
        return precision;
    }

    /**
     * Returns the recall rate.
     *
     * @return the recall
     */
    public float getRecall() {
        return recall;
    }

    @Override
    public String toString() {
        return String.format("Precision(P) %.2f , Recall(R) %.2f , F1 %.2f",
                precision, recall, getF1());
    }
}
