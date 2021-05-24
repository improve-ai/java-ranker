package ai.improve;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Basic wrapper for HttpURLConnection
 */
public class HttpUtil {

    private Logger logger = Logger.getLogger(HttpUtil.class.getName());

    private Map<String, String> headers;
    private Map<String, Object> body;

    private URL url;
    private HttpURLConnection connection;


    HttpUtil(String url) throws MalformedURLException {
            this.url = new URL(url);
    }

    public static HttpUtil withUrl(String url) throws MalformedURLException {
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
        try {
            connection = (HttpURLConnection) url.openConnection();
            try {
                for(Map.Entry<String, String> header: headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setChunkedStreamingMode(0);
                String jsonBody = new JSONObject(body).toString();
                connection.getOutputStream().write(jsonBody.getBytes());
                connection.getOutputStream().flush();
                int code = connection.getResponseCode();
                if(code >= 400) {
                    logger.severe("Error posting HTTP Data to " + url.toString() + ": status code " + code);
                }

            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.severe("Error posting HTTP data");
        }
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


}
