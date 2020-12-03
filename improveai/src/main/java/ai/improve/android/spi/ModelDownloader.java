package ai.improve.android.spi;

import ai.improve.android.HttpUtil;
import ai.improve.android.xgbpredictor.ImprovePredictor;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

public class ModelDownloader {


    public static ImprovePredictor fromUrl(String url) throws IOException {
        String abspath = HttpUtil.withUrl(url).download("model.xgb");
        ImprovePredictor p = new ImprovePredictor(new FileInputStream(abspath));
        return p;
    }
}
