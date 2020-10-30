package ai.improve.android;

public class ScoredVariant {

    private Object variant;

    private double score;

    public ScoredVariant(Object variant, double score) {
        this.variant = variant;
        this.score = score;
    }


    public Object getVariant() {
        return variant;
    }

    public double getScore() {
        return score;
    }
}
