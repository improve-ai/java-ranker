package ai.improve.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Utils {
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
        }
        return false;
    }

    public static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: [" + url + "]");
        }
    }

    /**
     * This method is likely to be changed in the future. Try not to use it in your code.
     * @param variants A list of variants to be ranked.
     * @param scores Scores of the variants.
     * @return a list of the variants ranked from best to worst by scores
     * @throws IllegalArgumentException Thrown if variants or scores is null; Thrown if
     * variants.size() not equal to scores.size().
     * @hidden
     */
    public static <T> List<T> rank(List<T> variants, List<Double> scores) {
        if(variants == null || scores == null) {
            throw new IllegalArgumentException("variants or scores can't be null");
        }

        if(variants.size() != scores.size()) {
            throw new IllegalArgumentException("variants.size() must be equal to scores.size()");
        }

        Integer[] indices = new Integer[variants.size()];
        for(int i = 0; i < variants.size(); ++i) {
            indices[i] = i;
        }

        Arrays.sort(indices, Collections.reverseOrder(new Comparator<>() {
            public int compare(Integer obj1, Integer obj2) {
                return Double.compare(scores.get(obj1), scores.get(obj2));
            }
        }));

        List<T> result = new ArrayList<>(variants.size());
        for(int i = 0; i < indices.length; ++i) {
            result.add(variants.get(indices[i]));
        }

        return result;
    }

    public static boolean isValidModelName(String modelName) {
        return modelName != null && modelName.matches("^[a-zA-Z0-9][\\w\\-.]{0,63}$");
    }
}
