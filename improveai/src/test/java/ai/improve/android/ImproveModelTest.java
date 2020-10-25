/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ai.improve.android;

import ai.improve.android.spi.DefaultDecisionModel;
import ai.improve.android.xgbpredictor.ImprovePredictor;
import biz.k11i.xgboost.util.ModelReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

@RunWith(RobolectricTestRunner.class)
public class ImproveModelTest {
    private static final Logger jul = Logger.getLogger(ImproveTrackerTest.class.getName());


    public ImproveModelTest() {
    }

    @Test
    public void testImproveModel() throws IOException {
        ImprovePredictor p = new ImprovePredictor(getClass().getResourceAsStream("/model_appended.xgb"));
        System.out.println(p.getModelMetadata().getUserDefinedMetadata());
        DecisionModel model = DefaultDecisionModel.initWithModel(p);

        String choice = (String) model.choose(Arrays.asList("test", "data", "which", "22222", "00000"));
        jul.info("Choice: " + choice);

        jul.severe("SEVERE");
        jul.info("INFO");
        jul.fine("FINE");
        jul.finer("FINER");
        jul.finest("FINEST");

    }

    @Test
    public void testModelReader() throws IOException {
        ModelReader r = new ModelReader(getClass().getResourceAsStream("/model_appended.xgb"));
        r.skip(2832417);
    }
}