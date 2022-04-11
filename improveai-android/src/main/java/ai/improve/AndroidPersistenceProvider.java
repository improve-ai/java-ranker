package ai.improve;

import static ai.improve.android.Constants.Improve_SP_File_Name;

import android.content.Context;
import android.content.SharedPreferences;

import ai.improve.android.AppGivensProviderUtils;
import ai.improve.provider.PersistenceProvider;

public class AndroidPersistenceProvider implements PersistenceProvider {
    private static final String Key_Last_Decision_Id = "ai.improve.last_decision-%s";

    private Context context;

    public AndroidPersistenceProvider(Context context) {
        this.context = context;
    }

    @Override
    public void persistDecisionIdForModel(String modelName, String decisionId) {
        String key = String.format(Key_Last_Decision_Id, modelName);
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        sp.edit().putString(key, decisionId).apply();
    }

    @Override
    public String lastDecisionIdForModel(String modelName) {
        String key = String.format(Key_Last_Decision_Id, modelName);
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        return sp.getString(key, "");
    }

    @Override
    public void addRewardForModel(String modelName, double reward) {
        AppGivensProviderUtils.addRewardForModel(modelName, reward);
    }
}