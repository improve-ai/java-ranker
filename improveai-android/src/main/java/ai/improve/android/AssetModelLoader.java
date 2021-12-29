package ai.improve.android;

import java.io.IOException;
import java.io.InputStream;

import ai.improve.ImproveContentProvider;
import ai.improve.downloader.ModelLoader;


public class AssetModelLoader implements ModelLoader {
    @Override
    public InputStream load(String path) throws IOException {
        return ImproveContentProvider.getAppContext().getAssets().open(path);
    }
}
