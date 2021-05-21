package ai.improve.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@RunWith(AndroidJUnit4.class)
public class DownloaderTest {

    public void testDownloadWithCache() {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL("http://10.254.115.144:8080/dummy_v6.xgb").openConnection();
            urlConnection.setReadTimeout(15000);
            urlConnection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
