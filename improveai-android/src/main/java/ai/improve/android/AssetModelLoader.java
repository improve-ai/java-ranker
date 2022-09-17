package ai.improve.android;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

import ai.improve.downloader.ModelLoader;

public class AssetModelLoader implements ModelLoader {

    private Context context;

    public AssetModelLoader(Context context) {
        this.context = context;
    }

    @Override
    public InputStream load(String path) throws IOException {
        return context.getAssets().open(path);
    }
}
