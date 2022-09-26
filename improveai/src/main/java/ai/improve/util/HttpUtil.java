package ai.improve.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import ai.improve.log.IMPLog;
import com.google.gson.GsonBuilder;

/**
 * Basic wrapper for HttpURLConnection
 */
public class HttpUtil {
    private static final String Tag = "HttpUtil";

    private Map<String, String> headers;

    private Map<String, Object> body;

    private URL url;

    HttpUtil(URL url) {
        this.url = url;
    }

    public static HttpUtil withUrl(URL url) {
        return new HttpUtil(url);
    }

    public HttpUtil withHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public HttpUtil withBody(Map<String, Object> body) {
        this.body = body;
        return this;
    }

    public void post() {
        if(!isJsonEncodable(body)) {
            IMPLog.w(Tag, "track request body not json encodable");
            return ;
        }

        String jsonBody = serializeBody(body);

        new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    try {
                        for(Map.Entry<String, String> header: headers.entrySet()) {
                            connection.setRequestProperty(header.getKey(), header.getValue());
                        }
                        connection.setRequestMethod("POST");
                        connection.setDoOutput(true);
                        connection.setChunkedStreamingMode(0);
                        IMPLog.d(Tag, "tracker request body, " + jsonBody);
                        connection.getOutputStream().write(jsonBody.getBytes());
                        connection.getOutputStream().flush();
                        int code = connection.getResponseCode();
                        if(code == 200) {
                            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            String body = "";
                            String line;
                            while ((line = br.readLine()) != null) {
                                body += line;
                            }
                            IMPLog.d(Tag, "tracker response 200, " + body);
                        }
                        if(code >= 400) {
                            IMPLog.e(Tag, "Error posting HTTP Data to " + url.toString() + ": status code " + code);
                        }
                    } finally {
                        connection.disconnect();
                    }
                }
                catch (Exception e) {
                    IMPLog.e(Tag, "Error posting HTTP data, " + e);
                }
            }
        }.start();
    }

    /**
     * Streams the file as an InputStream, handles gzip compression on the fly
     * @return InputStream
     */
    public InputStream stream() throws IOException, SecurityException {
        URL u = url;
        URLConnection c = u.openConnection();
        c.setUseCaches(true);
        c.setDefaultUseCaches(true);
        InputStream is = c.getInputStream();
        GZIPInputStream dis = new GZIPInputStream(is);
        return dis;
    }

    public static String serializeBody(Map<String, Object> body) {
        return new GsonBuilder().serializeNulls().create().toJson(body);
    }

    public static boolean isJsonEncodable(Object node) {
        if(node instanceof Boolean) {
            return true;
        } else if(node instanceof Number && !Double.isNaN(((Number)node).doubleValue())) {
            return true;
        } else if(node instanceof String) {
            return true;
        } else if (node instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>)node).entrySet()) {
                if(!isJsonEncodable(entry.getValue())) {
                    return false;
                }
            }
            return true;
        } else if (node instanceof List) {
            List list = (List)node;
            for (int i = 0; i < list.size(); ++i) {
                if(!isJsonEncodable(list.get(i))) {
                    return false;
                }
            }
            return true;
        } else if(node == null) {
            return true;
        } else {
            return false;
        }
    }
}
