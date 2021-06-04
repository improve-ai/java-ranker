package ai.improve.android;

import android.content.Context;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import ai.improve.BaseDecisionModel;
import ai.improve.IMPLog;
import ai.improve.hasher.XXHashAPI;
import ai.improve.xgbpredictor.ImprovePredictor;

public class DecisionModel extends BaseDecisionModel {
    public static final String Tag = "DecisionModel";

    private final Object lock = new Object();

    public static DecisionModel loadFromAsset(Context context, String filename) throws Exception {
        final Exception[] loadException = {null};
        DecisionModel decisionModel = new DecisionModel("");
        decisionModel.loadFromAssetAsync(context, filename, new IMPDecisionModelLoadListener(){
            @Override
            public void onFinish(DecisionModel model, Exception e) {
                IMPLog.d(Tag, "loadFromAsset, onFinish");
                loadException[0] = e;
                synchronized (decisionModel.lock) {
                    decisionModel.lock.notifyAll();
                }
                IMPLog.d(Tag, "loadFromAsset, notifyAll");
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

        if(loadException[0] != null) {
            IMPLog.e(Tag, "loadFromAsset, model loading failed, " + filename);
            throw loadException[0];
        }

        IMPLog.d(Tag, "loadFromAsset, finish loading model, " + filename);

        return decisionModel;
    }

    public void loadFromAssetAsync(Context context, String filename, IMPDecisionModelLoadListener listener) {
        new Thread() {
            @Override
            public void run() {
                try {
                    IMPLog.d(Tag, "loadFromAssetAsync, start...");
                    InputStream inputStream = context.getAssets().open(filename);
                    ImprovePredictor predictor = new ImprovePredictor(inputStream);
                    DecisionModel.this.setModel(predictor);

                    listener.onFinish(DecisionModel.this, null);
                } catch (IOException | JSONException e) {
                    IMPLog.d(Tag, "loadFromAssetAsync, " + e.getLocalizedMessage());
                    e.printStackTrace();
                    listener.onFinish(null, e);
                }
            }
        }.start();
    }

    public static DecisionModel load(URL url) throws Exception {
        final Exception[] loadException = {null};
        DecisionModel decisionModel = new DecisionModel("");
        decisionModel.loadAsync(url, new IMPDecisionModelLoadListener(){
            @Override
            public void onFinish(DecisionModel model, Exception e) {
                loadException[0] = e;
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

        if(loadException[0] != null) {
            IMPLog.e(Tag, "model loading failed, " + url.toString());
            throw loadException[0];
        }

        IMPLog.d(Tag, "load, finish loading model, " + url.toString());

        return decisionModel;
    }

    public void loadAsync(URL url, IMPDecisionModelLoadListener listener) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if(url.toString().startsWith("http")) {
                        IMPLog.d(Tag, "loadAsync, start loading model, " + url.toString());
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setReadTimeout(15000);
                        InputStream inputStream;
                        if (url.getPath().endsWith(".gz")) {
                            inputStream = new GZIPInputStream(urlConnection.getInputStream());
                        } else {
                            inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        }
                        ImprovePredictor predictor = new ImprovePredictor(inputStream);
                        DecisionModel.this.setModel(predictor);
                        IMPLog.d(Tag, "loadAsync, model loaded, " + url.toString());
                        // callback in main thread
                        listener.onFinish(DecisionModel.this, null);
                    } else {
                        // local model files
                        // If local model files is not in the sandbox of the app,
                        // READ_EXTERNAL_STORAGE might be required to read the file.
                        // We are leaving any permission request stuff to sdk users.
                        new File(url.toURI());
                        InputStream inputStream;
                        if(url.getPath().endsWith(".gz")) {
                            inputStream = new GZIPInputStream(new FileInputStream(new File(url.toURI())));
                        } else {
                            inputStream = new FileInputStream(new File(url.toURI()));
                        }
                        ImprovePredictor predictor = new ImprovePredictor(inputStream);
                        DecisionModel.this.setModel(predictor);

                        IMPLog.d(Tag, "loadAsync, model loaded");
                        listener.onFinish(DecisionModel.this, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    IMPLog.e(Tag, "loadAsync exception: " + e.getLocalizedMessage());
                    listener.onFinish(null, e);
                }
            }
        }.start();
    }

    public DecisionModel(String modelName) {
        super(modelName, new XXHashAPI());
    }

    public interface IMPDecisionModelLoadListener {
        /**
         * @param decisionModel null when error occurred while loading the model
         * */
        void onFinish(DecisionModel decisionModel, Exception e);
    }
}
