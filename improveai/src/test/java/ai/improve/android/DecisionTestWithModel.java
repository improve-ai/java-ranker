package ai.improve.android;


import ai.improve.android.hasher.GuavaMmh3Hasher;
import ai.improve.android.spi.DecisionMaker;
import ai.improve.android.spi.DefaultDecisionModel;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import com.google.common.hash.Hashing;
import junit.framework.Assert;
import org.apache.commons.codec.digest.MurmurHash3;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.charset.Charset;
import java.util.*;


@RunWith(RobolectricTestRunner.class)
public class DecisionTestWithModel {

    @Test
    public void testGenerateScores() throws Exception {

        ImprovePredictor p = new ImprovePredictor(getClass().getResourceAsStream("/improve-messages-2.0.xgb"));
//        ImprovePredictor p = new ImprovePredictor(getClass().getResourceAsStream("/model_w_metadata.xgb"));
        //System.out.println(p.getModelMetadata().getUserDefinedMetadata());
        DecisionModel model = DefaultDecisionModel.initWithModel(p);

        List<Object> variants = loadVariants("/datasets/2bs_bible_verses_full.json");


        Map<String, Object> context = loadContext("/datasets/context_sample_1.json");


        int total_tests = 5;
        Map<Object, MutableInt> counter = new HashMap<>(total_tests);
        for (int i = 0; i < total_tests; ++i) {
            Decision d = new DecisionMaker(variants, model, context);
            if(counter.containsKey(d.best())) {
                counter.get(d.best()).increment();
            } else {
                counter.put(d.best(), new MutableInt(1));
            }
            System.out.println(i + ":    " + d.scored());
//            System.out.println(i + ":    " + d.best());
            if (i % 500 == 0) {
                System.out.println(counter.size() + " quotes chosen out of " + i);
            }
        }
        System.out.println(counter.size() + " quotes chosen out of 10000");

        System.out.println("\n\n\n");
        List<Map.Entry<Object, MutableInt>> entries = new ArrayList<>(counter.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<Object, MutableInt>>() {
            @Override
            public int compare(Map.Entry<Object, MutableInt> lhs, Map.Entry<Object, MutableInt> rhs) {
                return rhs.getValue().intValue() - lhs.getValue().intValue();
            }
        });

        for(Map.Entry<Object, MutableInt> entry : entries) {
            System.out.println(entry.getValue() + " => " + entry.getKey());
        }
        System.out.flush();
        return;
    }

    private long unsignedMmh3Hash(String value, int modelSeed) throws Exception {
        long hash = MurmurHash3.hash32x86(value.getBytes("UTF-8"), 0, value.length(), modelSeed);
        return hash & 0x0ffffffffl; //convert to unsigned
    }

    private long mmhHashGuava(String value, int modelSeed) throws Exception {

        long hash = new GuavaMmh3Hasher(modelSeed).hashBytes(value.getBytes("UTF-8")).asInt();
        return hash & 0x0ffffffffl; //convert to unsigned
    }

    @Test
    public void mmh3hashtest() throws Exception {
        System.out.println(unsignedMmh3Hash("Looking at them, Jesus said, \u201cWith men this is impossible, but with God all things are possible.\u201d", 1778527324));
        System.out.println(mmhHashGuava("Looking at them, Jesus said, \u201cWith men this is impossible, but with God all things are possible.\u201d", 1778527324));
    }

    @Test
    public void minDoubleRandomTest() throws Exception {
        RandomGenerator randomGenerator = new JDKRandomGenerator();
        double value = randomGenerator.nextDouble();

        System.out.println(value);
        for(int i = 0; i < 50; ++i) {
            double noise = randomGenerator.nextDouble();
            System.out.println(value + (noise / 1000000000000000d));
        }

    }

    static Map<String, Object> loadContext(String resource) throws Exception {
        String json = IOUtils.toString(DecisionTestWithModel.class.getResourceAsStream(resource), "UTF-8");
        JSONObject obj = new JSONObject(json);
        Map<String, Object> result = new HashMap<>();
        for(Iterator<String> r = obj.keys(); r.hasNext();) {
            String key = r.next();
            result.put(key, obj.get(key));
        }
        return result;
    }


    static List<Object> loadVariants(String resource) throws Exception {
        String json = IOUtils.toString(DecisionTestWithModel.class.getResourceAsStream(resource));
        JSONArray arr = new JSONArray(json);
        List<Object> result = new ArrayList();
        for (int i = 0; i < arr.length(); ++i) {
            result.add(arr.getJSONObject(i));
        }
        return result;
    }
}
