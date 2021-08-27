package ai.improve.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import ai.improve.log.IMPLog;
import ai.improve.xgbpredictor.ImprovePredictor;

public class ModelDownloader {
    public static final String Tag = "ModelDownloader";

    private URL url;

    public ModelDownloader(URL url) {
        this.url = url;
    }

    public static void download(URL url, ModelDownloadListener listener) {
        new ModelDownloader(url).downloadInternal(listener);
    }

    private void downloadInternal(ModelDownloadListener listener) {
        new Thread() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    if(url.toString().startsWith("http")) {
                        IMPLog.d(Tag, "loadAsync, start loading model, " + url.toString());
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setReadTimeout(15000);
                        if (url.getPath().endsWith(".gz")) {
                            inputStream = new GZIPInputStream(urlConnection.getInputStream());
                        } else {
                            inputStream = new BufferedInputStream(urlConnection.getInputStream());
                        }

                        ImprovePredictor predictor = new ImprovePredictor(inputStream);
                        if(listener != null) {
                            listener.onFinish(predictor, null);
                        }
                    } else {
                        // local model files
                        // If local model files is not in the sandbox of the app,
                        // READ_EXTERNAL_STORAGE might be required to read the file.
                        // We are leaving any permission request stuff to sdk users.
                        new File(url.toURI());
                        if (url.getPath().endsWith(".gz")) {
                            inputStream = new GZIPInputStream(new FileInputStream(new File(url.toURI())));
                        } else {
                            inputStream = new FileInputStream(new File(url.toURI()));
                        }

                        ImprovePredictor predictor = new ImprovePredictor(inputStream);
                        if(listener != null) {
                            listener.onFinish(predictor, null);
                        }
                    }
                } catch (Exception e) {
                    IMPLog.e(Tag, "model download exception: " + e.getMessage());
                    if(listener != null) {
                        listener.onFinish(null, e);
                    }
                } finally {
                    if(inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }
        }.start();
    }

    public interface ModelDownloadListener {
        void onFinish(ImprovePredictor predictor, Exception e);
    }
}
