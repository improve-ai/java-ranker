package ai.improve.android;

public class ScoredVariant implements Comparable<ScoredVariant> {

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

    @Override
    public int compareTo(ScoredVariant another) {
        if(another == null) {
            return -1;
        }
        return Double.compare(this.score, another.score);
    }

    @Override
    public String toString() {
        return "ScoredVariant{" +
                "score=" + score +
                ", variant=" + variant +
                "}\n";
    }
}
