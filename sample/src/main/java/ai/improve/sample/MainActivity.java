package ai.improve.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ai.improve.DecisionModel;
import ai.improve.DecisionTracker;
import ai.improve.log.IMPLog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String Tag = "MainActivity";

    public static final String Model_URL = "http://192.168.1.101/dummy_v6.xgb";

    private TextView mGreetingTV;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGreetingTV = findViewById(R.id.greeting_tv);
        findViewById(R.id.root_view).setOnClickListener(this);
        findViewById(R.id.leak_test_btn).setOnClickListener(this);

        IMPLog.setLogLevel(IMPLog.LOG_LEVEL_ALL);

        enableHttpResponseCache();

        testCache();
    }

    /**
     * How do you know that cache is used???
     * Method 1: Run tcpdump in the server.
     * When cache is used, there would be no request to the server or only a header request
     * which returns '304, Not Modified'
     *
     * Method 2: Perhaps turn off the internet
     * If the model can still be loaded, then we assume that cache is used.
     * */
    private void testCache() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(Model_URL);
                    DecisionModel.load(url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 2000);
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

    private void enableHttpResponseCache() {
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 300 * 1024 * 1024;
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
            Log.d(Tag, "cache enabled");
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