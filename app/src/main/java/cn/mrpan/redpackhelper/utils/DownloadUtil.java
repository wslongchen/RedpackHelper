package cn.mrpan.redpackhelper.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import java.net.URI;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DownloadUtil {
    public void enqueue(String url, Context context) {
        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
        r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "redpack.apk");
        r.allowScanningByMediaScanner();
        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(r);
    }
}
