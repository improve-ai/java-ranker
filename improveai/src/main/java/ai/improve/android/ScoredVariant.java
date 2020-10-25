package ai.improve.android;

public class ScoredVariant {

    private Object variant;

    private double score;

    public ScoredVariant(Object variant, double score) {
        this.variant = variant;
        this.score = score;
    }


    public void setVariant(Object variant) {
        this.variant = variant;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
