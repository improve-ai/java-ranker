package ai.improve.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import java.util.UUID;

import ai.improve.HistoryIdProvider;

public class HistoryIdProviderImp implements HistoryIdProvider {
    private Context appContext;

    public HistoryIdProviderImp(Context appContext) {
        this.appContext = appContext;
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
