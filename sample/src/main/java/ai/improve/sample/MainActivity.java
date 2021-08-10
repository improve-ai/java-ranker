package ai.improve.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.improve.DecisionModel;
import ai.improve.DecisionTracker;
import ai.improve.log.IMPLog;
import ai.improve.android.AppGivensProviderImp;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String Tag = "MainActivity";

    public static final String Model_URL = "http://192.168.1.101/dummy_v6.xgb";

    private TextView mGreetingTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGreetingTV = findViewById(R.id.greeting_tv);
        findViewById(R.id.root_view).setOnClickListener(this);
        findViewById(R.id.leak_test_btn).setOnClickListener(this);

        enableHttpResponseCache();

        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);

        AppGivensProviderImp provider = new AppGivensProviderImp(this);
        Map givens = provider.getGivens();
        IMPLog.d(Tag, "givens = " + givens);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.root_view) {
            try {
                chooseFrom();
            } catch (Exception e){
                e.printStackTrace();
            }
            track();

            new Thread() {
                @Override
                public void run() {
                    testHttpUrlConnection();
                }
            }.start();
        } else if(id == R.id.leak_test_btn) {
            Intent intent = new Intent(this, LeakTestActivity.class);
            startActivity(intent);
        }
    }

    private void chooseFrom() throws Exception {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        URL url = new URL(Model_URL);
        DecisionModel model = DecisionModel.load(url);
        String greeting = (String) model.chooseFrom(variants).get();
        Log.d(Tag, "greeting=" + greeting);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!TextUtils.isEmpty(greeting)) {
                    mGreetingTV.setText(greeting);
                    mGreetingTV.setTextColor(getColor(R.color.black));
                } else {
                    mGreetingTV.setText("greeting is null or empty");
                    mGreetingTV.setTextColor(getColor(R.color.red));
                }
            }
        });
    }

    private void track() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        DecisionTracker tracker = new DecisionTracker("");

        Object variant = new DecisionModel("orange").trackWith(tracker).chooseFrom(variants).get();
        Log.d(Tag, "variant = " + variant);
    }

    private void testHttpUrlConnection() {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL("http://10.254.115.144:8080/dummy_v6.xgb");
            urlConnection = (HttpURLConnection)url.openConnection();
            urlConnection.setReadTimeout(15000);
            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());

            int totalBytes = 0;
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
//            DataInputStream dis = new DataInputStream(inputStream);
            while(-1 != (bytesRead = inputStream.read(buffer))) {
                totalBytes += bytesRead;
            }
            Log.d(Tag, "totalBytesRead: " + totalBytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private void enableHttpResponseCache() {
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i(Tag, "HTTP response cache installation failed:" + e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
            Log.i(Tag, "Cache flushed");
        }
    }
}