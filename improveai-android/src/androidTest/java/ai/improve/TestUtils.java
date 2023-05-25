package ai.improve;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public class TestUtils {
    public static JSONObject loadJson(Context context, String path) throws IOException, JSONException {
        InputStream inputStream = context.getAssets().open(path);
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();

        String content = new String(buffer);
        return new JSONObject(content);
    }
}
