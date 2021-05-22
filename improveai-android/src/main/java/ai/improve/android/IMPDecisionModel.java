package ai.improve.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import ai.improve.android.hasher.XXHashAPI;
import ai.improve.android.xgbpredictor.ImprovePredictor;

public class IMPDecisionModel extends BaseIMPDecisionModel {
    public static final String Tag = "IMPDecisionModel";

    private final Object lock = new Object();

    public static IMPDecisionModel load(URL url) throws Exception {
        final Exception[] loadException = {null};
        IMPDecisionModel decisionModel = new IMPDecisionModel("");
        decisionModel.loadAsync(url, new IMPDecisionModelLoadListener(){
            @Override
            public void onFinish(ImprovePredictor predictor, Exception e) {
                loadException[0] = e;
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

        if(loadException[0] != null) {
            IMPLog.e(Tag, "model loading failed, " + url.toString());
            throw loadException[0];
        }

        IMPLog.d(Tag, "load, finish loading model, " + url.toString());

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
                        IMPLog.d(Tag, "loadAsync, model loaded, " + url.toString());
                        // callback in main thread
                        listener.onFinish(predictor, null);
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
                        IMPLog.d(Tag, "loadAsync, model loaded");
                        listener.onFinish(predictor, null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    IMPLog.e(Tag, "loadAsync exception: " + e.getLocalizedMessage());
                    listener.onFinish(null, e);
                }
            }
        }.start();
    }

    public interface IMPDecisionModelLoadListener {
        /**
         * @param predictor null when error occurred while loading the model
         * */
        void onFinish(ImprovePredictor predictor, Exception e);
    }
}
