package ai.improve.android;

import java.io.IOException;
import java.io.InputStream;


public class AssetModelLoader {
    public static InputStream loadFromAsset(String path) throws IOException {
        return ImproveContentProvider.getAppContext().getAssets().open(path);
    }
}
