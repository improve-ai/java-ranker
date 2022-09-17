package ai.improve;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import ai.improve.android.AppGivensProvider;
import ai.improve.android.AssetModelLoader;
import ai.improve.android.Logger;
import ai.improve.downloader.ModelDownloader;
import ai.improve.log.IMPLog;
import ai.improve.util.Utils;

/**
 * ImproveContentProvider is declared in the AndroidManifest.xml.
 * The onCreate method is called for all registered content providers on the application main
 * thread at application launch time. So it's a perfect spot to initialize Android only stuff
 * here.
 */
public class ImproveContentProvider extends ContentProvider {
    private static final String METADATA_DEFAULT_TRACK_URL = "ai.improve.DEFAULT_TRACK_URL";

    private Context mContext;

    private static long sSessionStartTime; // millisecond

    @Override
    public boolean onCreate() {
        mContext = getContext();

        // True app launch time
        sSessionStartTime = System.currentTimeMillis();

        AppGivensProvider.setBornTime(mContext);

        IMPLog.setLogger(new Logger());

        setDefaultTrackURL();

        DecisionModel.setDefaultGivensProvider(new AppGivensProvider(mContext));

        DecisionTracker.setPersistenceProvider(new AndroidPersistenceProvider(mContext));

        ModelDownloader.setAssetModelLoader(new AssetModelLoader(mContext));

        return true;
    }

    public static long getSessionStartTime() {
        return sSessionStartTime;
    }

    private void setDefaultTrackURL() {
        String packageName = mContext.getPackageName();
        ApplicationInfo info;
        try {
            info = mContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            // This should never happen
            e.printStackTrace();
            return ;
        }
        if(info.metaData != null) {
            String defaultTrackURL = info.metaData.getString(METADATA_DEFAULT_TRACK_URL);
            if (!TextUtils.isEmpty(defaultTrackURL) && !Utils.isValidURL(defaultTrackURL)) {
                throw new RuntimeException("[" + defaultTrackURL + "], invalid track URL in metadata inside AndroidManifest.xml");
            }
            DecisionModel.setDefaultTrackURL(defaultTrackURL);
        }
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
