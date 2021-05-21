package ai.improve.android;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ai.improve.android.hasher.XXHashAPI;
import ai.improve.android.xgbpredictor.ImprovePredictor;

public class IMPDecisionModel extends BaseIMPDecisionModel {
    public static final String Tag = "IMPDecisionModel";

    private final Object lock = new Object();

    public static IMPDecisionModel load(URL url) {
        IMPDecisionModel decisionModel = new IMPDecisionModel("");
        decisionModel.loadAsync(url, new IMPDecisionModelLoadListener(){
            @Override
            public void onFinish(ImprovePredictor predictor) {
                decisionModel.setModel(predictor);
                synchronized (decisionModel.lock) {
                    decisionModel.lock.notifyAll();
                }
            }
        });
        synchronized (decisionModel.lock) {
            try {
                decisionModel.lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                IMPLog.e(Tag, e.getLocalizedMessage());
            }
        }

        return decisionModel;
    }

    public IMPDecisionModel(String modelName) {
        super(modelName, new XXHashAPI());
    }

    public void loadAsync(URL url, IMPDecisionModelLoadListener listener) {
        new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setReadTimeout(15000);
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    ImprovePredictor predictor = new ImprovePredictor(inputStream);

                    // callback in main thread
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onFinish(predictor);
                        }
                    });
                    return ;
                } catch (Exception e) {
                    e.printStackTrace();
                    IMPLog.e(Tag, "loadAsync exception: " + e.getLocalizedMessage());
                }

                // callback in main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onFinish(null);
                    }
                });
            }
        }.start();
    }

    public interface IMPDecisionModelLoadListener {
        /**
         * @param predictor null when error occurred while loading the model
         * */
        void onFinish(ImprovePredictor predictor);
    }
}
