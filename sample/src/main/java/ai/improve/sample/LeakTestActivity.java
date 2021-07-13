package ai.improve.sample;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.net.MalformedURLException;
import java.net.URL;

import ai.improve.DecisionModel;
import leakcanary.LeakCanary;

import static ai.improve.sample.MainActivity.Model_URL;

public class LeakTestActivity extends AppCompatActivity {
    public static final String Tag = "LeakTestActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leak_test);

        LeakCanary.Config config = LeakCanary.getConfig().newBuilder()
                .retainedVisibleThreshold(1)
                .build();
        LeakCanary.setConfig(config);

        loadModelAsync();
    }

    private void loadModelAsync() {
        try {
            DecisionModel decisionModel = new DecisionModel("");
            decisionModel.loadAsync(new URL(Model_URL), new DecisionModel.IMPDecisionModelLoadListener() {
                @Override
                public void onFinish(DecisionModel decisionModel, Exception e) {
                    if(e != null) {
                        Log.d(Tag, "on finish loading model, err=" + e.getMessage());
                    } else {
                        Log.d(Tag, "on finish loading model");
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

}
