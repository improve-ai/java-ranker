package ai.improve.downloader;

import java.io.IOException;
import java.io.InputStream;

public interface ModelLoader {
    InputStream load(String path) throws IOException;
}
