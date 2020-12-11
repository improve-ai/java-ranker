package ai.improve.android.spi;

import ai.improve.android.HttpUtil;
import ai.improve.android.xgbpredictor.ImprovePredictor;

import java.io.IOException;
import java.io.InputStream;

public class ModelDownloader {


    public static ImprovePredictor fromUrl(String url) throws IOException {
        InputStream modelStream = null;
        try {
            modelStream = HttpUtil.withUrl(url).stream();
            ImprovePredictor p = new ImprovePredictor(modelStream);
            return p;
        } finally {
            if (modelStream != null) {
                modelStream.close();
            }
        }
    }
}
