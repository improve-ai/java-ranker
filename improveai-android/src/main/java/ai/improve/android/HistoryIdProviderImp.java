package ai.improve.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.util.UUID;

import ai.improve.util.HistoryIdProvider;

public class HistoryIdProviderImp implements HistoryIdProvider {
    public static final String Tag = "HistoryIdProviderImp";

    private Context appContext;

    public HistoryIdProviderImp() {
        this.appContext = ImproveContentProvider.getAppContext();
    }

    @Override
    public String getHistoryId() {
        SharedPreferences sp = appContext.getSharedPreferences("ai.improve", Context.MODE_PRIVATE);
        String historyId = sp.getString(HISTORY_ID_KEY, "");
        if(TextUtils.isEmpty(historyId)) {
            historyId = generateHistoryId();
            sp.edit().putString(HISTORY_ID_KEY, historyId).commit();
        }
        return historyId;
    }

    private String generateHistoryId() {
        byte[] data = UUID.randomUUID().toString().getBytes();
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}