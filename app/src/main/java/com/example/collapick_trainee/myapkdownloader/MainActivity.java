package com.example.collapick_trainee.myapkdownloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final boolean SHOW_DOWNLOAD_TOAST = true; // Enables toast in DownloadStatus
    private static String APK_URL = "http://home.tamk.fi/~e5tjokin/BricoliniPizza.apk"; // Download URL for APK

    private boolean isDownloading = false; // SavedInstanceState

    private DownloadManager downloadManager; // Download Manager, used to request and query.
    private BroadcastReceiver downloadReceiver; // Broadcast receiver to "hear" when download is complete.
    private long apkDownloadReferenceID; // Store the download ID in this variable.
    private Uri apkUri;

    Button btnDownloadStatus;
    Button btnCancelDownload;
    Button btnStartDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDownloadStatus = (Button) findViewById(R.id.DownloadStatus);
        btnCancelDownload = (Button) findViewById(R.id.CancelDownload);
        btnStartDownload = (Button) findViewById(R.id.DownloadStart);

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Checking if the broadcast message was our enqueued download.
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                if (referenceId == apkDownloadReferenceID){
                    // APK Download found, check that the download

                    // Send the query to download manager which returns a cursor.
                    Cursor cursor = getDownloadIdCursor(apkDownloadReferenceID);

                    if(cursor.moveToFirst()){
                        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
                        int status = cursor.getInt(columnIndex);

                        // Check that the download is successful.
                        if (status == DownloadManager.STATUS_SUCCESSFUL && downloadLocalUri != null){

                            Toast toast = Toast.makeText(MainActivity.this,
                                    R.string.text_downloadComplete, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.TOP, 25, 400);
                            toast.show();

                            // Downloading done, disable buttons.
                            btnDownloadStatus.setEnabled(false);
                            btnCancelDownload.setEnabled(false);
                            btnStartDownload.setEnabled(true);
                            isDownloading = false;

                            // TODO: Start installation intent.
                            openApkInstallation(context, Uri.parse(downloadLocalUri), downloadMimeType);
                        }
                        else{
                            // Possible error in download.
                            Toast toast = Toast.makeText(MainActivity.this,
                                    R.string.text_apkDownloadStatus + "\n" + DownloadStatus(cursor, apkDownloadReferenceID),
                                    Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.TOP, 25, 400);
                            toast.show();
                        }
                    }
                    else {
                        // Download was cancelled.
                        Toast toast = Toast.makeText(MainActivity.this,
                                R.string.text_downloadCancelled,
                                Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.TOP, 25, 400);
                        toast.show();
                    }

                    cursor.close(); // Close cursor.

                }
            }
        };

        apkUri = Uri.parse(APK_URL);
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
    }

    public void onClickDownload(View view){
        // Starts downloading, disable button to prevent multiple instances of download.


        // Save the Download Request ID into apkDownloadReferenceID
        apkDownloadReferenceID = DownloadAPK(apkUri);
    }
    public void onClickCancel(View view){
        cancelApkDownload(apkDownloadReferenceID);
        btnDownloadStatus.setEnabled(false);
        btnCancelDownload.setEnabled(false);
        btnStartDownload.setEnabled(true);

    }

    public void onClickStatus(View view){
        Check_APK_Status(apkDownloadReferenceID);
    }

    /*
        Takes in an ID of a download request and cancels it.
     */
    private void cancelApkDownload(long downloadID){
        if (downloadManager != null)
            downloadManager.remove(downloadID);
    }


    /*
        Queries Download Manager with the given request ID and returns Cursor pointing at such ID.
     */
    private Cursor getDownloadIdCursor(long APK_DownloadId){
        // Create a query DownloadManager's requests, filtered by the given ID.
        DownloadManager.Query APKDownloadQuery = new DownloadManager.Query();
        APKDownloadQuery.setFilterById(APK_DownloadId);

        // Send the query to download manager which returns a cursor.
        return downloadManager.query(APKDownloadQuery);
    }

    /*
        Creates a request for Download Manager to download the given file from the uri.
        Returns the id of the requested download.
     */
    private long DownloadAPK(Uri uri){
        long downloadReference; // Unique ID for the download

        // Creating a request for android Download Manager. Could be created already in onCreate()
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);


        // Setting a title and a short description for the request
        request.setTitle("Update download");
        String name = getApplicationName(this);
        if (name != null)
            request.setDescription("Download an update for " + name + " application");
        else
            request.setDescription("Download an update for " + getPackageName() + " application");

        // Set a local destination for the downloaded APK file to a path.
        // Uses URL's last part to get file name. ( /myAPKname.apk)
        request.setDestinationInExternalFilesDir(MainActivity.this,
                Environment.DIRECTORY_DOWNLOADS, APK_URL.substring(APK_URL.lastIndexOf("/") + 1, APK_URL.length()));

        // Enqueue our APK download request and save referenceId
        downloadReference = downloadManager.enqueue(request);

        // Enable buttons for controlling download.
        btnDownloadStatus.setEnabled(true);
        btnCancelDownload.setEnabled(true);
        btnStartDownload.setEnabled(false);
        isDownloading = true;

        return downloadReference;
    }

    /*
        Requires long Download Manager request's ID.
        Toast's download status of the given ID.
     */
    private void Check_APK_Status(long APK_DownloadId) {

        // Default error message.
        String message = getString(R.string.STATUS_FAILED) +"\n"+ getString(R.string.ERROR_UNKNOWN);
        Cursor cursor = getDownloadIdCursor(APK_DownloadId);

        if(cursor.moveToFirst()){
            // Call a DownloadStatus function
            message = DownloadStatus(cursor, APK_DownloadId);
        }

        if(SHOW_DOWNLOAD_TOAST) {
            Toast toast = Toast.makeText(MainActivity.this,
                    R.string.text_apkDownloadStatus + "\n" + message,
                    Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 25, 400);
            toast.show();
        }

        cursor.close(); // Close cursor.
    }


    /*
        Goes through the Columns and looks for the given DownloadId's status.
        Takes in cursor and the DownloadId which was requested.
        Returns a string message (status + reason text).
    */
    private String DownloadStatus(Cursor cursor, long DownloadId){

        // Find the column index of the STATUS column, shows download status.
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);
        // Find the column index of the REASON column, shows code if download failed or is paused.
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);
        // Gets the downloaded file name from the column.
        int filenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
        String filename = cursor.getString(filenameIndex);

        String statusText = "";
        String reasonText = "";

        // Switch case, message for each reason codes.
        switch(status){
            case DownloadManager.STATUS_FAILED:
                statusText = getString(R.string.STATUS_FAILED);
                switch(reason){
                    case DownloadManager.ERROR_CANNOT_RESUME:
                        reasonText = getString(R.string.ERROR_CANNOT_RESUME);
                        break;
                    case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                        reasonText = getString(R.string.ERROR_DEVICE_NOT_FOUND);
                        break;
                    case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                        reasonText = getString(R.string.ERROR_FILE_ALREADY_EXISTS);
                        break;
                    case DownloadManager.ERROR_FILE_ERROR:
                        reasonText = getString(R.string.ERROR_FILE_ERROR);
                        break;
                    case DownloadManager.ERROR_HTTP_DATA_ERROR:
                        reasonText = getString(R.string.ERROR_HTTP_DATA_ERROR);
                        break;
                    case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                        reasonText = getString(R.string.ERROR_INSUFFICIENT_SPACE);
                        break;
                    case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                        reasonText = getString(R.string.ERROR_TOO_MANY_REDIRECTS);
                        break;
                    case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                        reasonText = getString(R.string.ERROR_UNHANDLED_HTTP_CODE);
                        break;
                    case DownloadManager.ERROR_UNKNOWN:
                        reasonText = getString(R.string.ERROR_UNKNOWN);
                        break;
                }
                break;
            case DownloadManager.STATUS_PAUSED:
                statusText = getString(R.string.STATUS_PAUSED);
                switch(reason){
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        reasonText = getString(R.string.PAUSED_QUEUED_FOR_WIFI);
                        break;
                    case DownloadManager.PAUSED_UNKNOWN:
                        reasonText = getString(R.string.PAUSED_UNKNOWN);
                        break;
                    case DownloadManager.PAUSED_WAITING_FOR_NETWORK:
                        reasonText = getString(R.string.PAUSED_WAITING_FOR_NETWORK);
                        break;
                    case DownloadManager.PAUSED_WAITING_TO_RETRY:
                        reasonText = getString(R.string.PAUSED_WAITING_TO_RETRY);
                        break;
                }
                break;
            case DownloadManager.STATUS_PENDING:
                statusText = getString(R.string.STATUS_PENDING);
                break;
            case DownloadManager.STATUS_RUNNING:
                statusText = getString(R.string.STATUS_RUNNING);
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                statusText = getString(R.string.STATUS_SUCCESSFUL);
                reasonText = getString(R.string.text_Filename) + "\n" + filename;
                break;
        }

        return statusText + "\n" + reasonText;
    }


    private void openApkInstallation(Context context, Uri downloadedUri, String mimeType){
        Intent installationIntent = new Intent(Intent.ACTION_VIEW);
        installationIntent = installationIntent.setDataAndType(downloadedUri,"application/vnd.android.package-archive");

        installationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(installationIntent);
    }

    // Returns a string with the application name
    public static String getApplicationName(Context context) {

        String name = null;

        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            int stringId = applicationInfo.labelRes;
            name =  stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
        } catch (Exception e) {
            // Resources not found
            e.printStackTrace();
        }

        return name;
    }



    // This callback is called only when there is a saved instance previously saved using
    // onSaveInstanceState(). We restore some state in onCreate() while we can optionally restore
    // other state here, possibly usable after onStart() has completed.
    // The savedInstanceState Bundle is same as the one used in onCreate().
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null){
            isDownloading = savedInstanceState.getBoolean("isDownloading");
            apkDownloadReferenceID = savedInstanceState.getLong("apkDownloadReferenceID");
        }


        if (isDownloading){
            btnDownloadStatus.setEnabled(true);
            btnCancelDownload.setEnabled(true);
            btnStartDownload.setEnabled(false);

            // Recreate downloadManager, could be created in onCreate() to avoid this.
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        }
        else{
            btnDownloadStatus.setEnabled(false);
            btnCancelDownload.setEnabled(false);
            btnStartDownload.setEnabled(true);
        }


    }

    // Invoked when the activity may be temporarily destroyed, save the instance state here
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store DownloadManager ID
        outState.putLong("apkDownloadReferenceID", apkDownloadReferenceID);

        // Store download status
        outState.putBoolean("isDownloading", isDownloading);


        // Call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }
}
