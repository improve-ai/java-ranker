package ai.improve;

import static ai.improve.android.Constants.Improve_SP_File_Name;

import android.content.Context;
import android.content.SharedPreferences;

import ai.improve.provider.PersistenceProvider;

public class AndroidPersistenceProvider implements PersistenceProvider {
    private final Context context;

    public AndroidPersistenceProvider(Context context) {
        this.context = context;
    }

    @Override
    public void write(String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(Improve_SP_File_Name, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).apply();
    }
}
