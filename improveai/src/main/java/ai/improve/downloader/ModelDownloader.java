package ai.improve.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import ai.improve.log.IMPLog;
import ai.improve.xgbpredictor.ImprovePredictor;

public class ModelDownloader {
    public static final String Tag = "ModelDownloader";

    private URL url;

    private static ModelLoader assetModelLoader;

    public ModelDownloader(URL url) {
        this.url = url;
    }

    public static void download(URL url, ModelDownloadListener listener) {
        new ModelDownloader(url).downloadInternal(listener);
    }

    public static void setAssetModelLoader(ModelLoader loader) {
        assetModelLoader = loader;
    }

    private void downloadInternal(ModelDownloadListener listener) {
        new Thread() {
            @Override
            public void run() {
                InputStream inputStream = null;
                try {
                    String urlString = url.toString();
                    if(urlString.startsWith("http")) {
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
                    } else if(urlString.startsWith("file:///android_asset")) {
                        // Only Android would reach here
                        // When running in pure Java, new URL("file:///android_asset/") is interpreted
                        // as new URL("file:/android_asset").
                        String path = urlString.substring("file:///android_asset/".length());
                        if(urlString.endsWith(".gz")) {
                            inputStream = new GZIPInputStream(assetModelLoader.load(path));
                        } else {
                            inputStream = assetModelLoader.load(path);
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
                } catch (Throwable e) {
                    e.printStackTrace();
                    IMPLog.e(Tag, url + ", model download exception: " + e.getMessage());
                    if(listener != null) {
                        listener.onFinish(null, new IOException(e.getMessage()));
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
        void onFinish(ImprovePredictor predictor, IOException e);
    }
}
