package ai.improve.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ai.improve.android.IMPDecisionModel;
import ai.improve.android.IMPDecisionTracker;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String Tag = "MainActivity";

    private TextView mGreetingTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGreetingTV = findViewById(R.id.greeting_tv);
        findViewById(R.id.root_view).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.root_view) {
            chooseFrom();
            track();
        }
    }

    private void chooseFrom() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        IMPDecisionModel model = new IMPDecisionModel("orange");
        String greeting = (String) model.chooseFrom(variants).get();
        Log.d(Tag, "greeting=" + greeting);

        if(!TextUtils.isEmpty(greeting)) {
            mGreetingTV.setText(greeting);
            mGreetingTV.setTextColor(getColor(R.color.black));
        } else {
            mGreetingTV.setText("greeting is null or empty");
            mGreetingTV.setTextColor(getColor(R.color.red));
        }
    }

    private void track() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");

        IMPDecisionTracker tracker = new IMPDecisionTracker(getApplicationContext(), "");

        Object variant = new IMPDecisionModel("orange").track(tracker).chooseFrom(variants).get();
        Log.d(Tag, "variant = " + variant);
    }
}