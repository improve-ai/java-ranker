package ai.improve;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import ai.improve.android.AssetModelLoader;
import ai.improve.android.Logger;
import ai.improve.downloader.ModelDownloader;
import ai.improve.log.IMPLog;

/**
 * ImproveContentProvider is declared in the AndroidManifest.xml.
 * The onCreate method is called for all registered content providers on the application main
 * thread at application launch time. So it's a perfect spot to initialize Android only stuff
 * here.
 */
public class ImproveContentProvider extends ContentProvider {

    private Context mContext;

    @Override
    public boolean onCreate() {
        mContext = getContext();

        IMPLog.setLogger(new Logger());

        ModelDownloader.setAssetModelLoader(new AssetModelLoader(mContext));

        return true;
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
