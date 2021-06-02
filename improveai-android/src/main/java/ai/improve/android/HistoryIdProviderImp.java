package ai.improve.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.UUID;

import ai.improve.HistoryIdProvider;

public class HistoryIdProviderImp implements HistoryIdProvider {

    private Context appContext;

    private String historyId;

    public HistoryIdProviderImp(Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public String getHistoryId() {
        SharedPreferences preferences = appContext.getSharedPreferences("ai.improve", Context.MODE_PRIVATE);
        if (preferences.contains(HISTORY_ID_KEY)) {
            this.historyId = preferences.getString(HISTORY_ID_KEY, "");
        } else {
            this.historyId = generateHistoryId();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(HISTORY_ID_KEY, this.historyId);
            editor.commit();
        }
        return this.historyId;
    }

    private String generateHistoryId() {
        byte[] data = UUID.randomUUID().toString().getBytes();
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
