package ai.improve;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import ai.improve.android.AppGivensProvider;

public class ImproveContentProvider extends ContentProvider {
    public static final String Tag = "ImproveContentProvider";

    private static Context sContext;

    @Override
    public boolean onCreate() {
        sContext = getContext();

        DecisionModel.setDefaultGivensProvider(new AppGivensProvider(sContext));

        DecisionTracker.setPersistenceProvider(new AndroidPersistenceProvider(sContext));

        return true;
    }

    public static Context getAppContext() {
        return sContext;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
