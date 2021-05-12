package ai.improve.android.spi;

import android.content.Context;

import org.json.JSONException;

import ai.improve.android.HttpUtil;
import ai.improve.android.xgbpredictor.ImprovePredictor;

import java.io.IOException;
import java.io.InputStream;

public class ModelDownloader {


    public static ImprovePredictor fromUrl(String url) throws IOException, JSONException {
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

    public static ImprovePredictor fromAsset(Context context, String filename) throws IOException, JSONException {
        InputStream inputStream = context.getAssets().open(filename);
        ImprovePredictor predictor = new ImprovePredictor(inputStream);
        return predictor;
    }
}
