
package com.example.jacob.androidvirtualassist;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.realvnc.vncsdk.DataStore;
import com.realvnc.vncsdk.Library;
import com.realvnc.vncsdk.Logger;

/**
 * ViewerThread is a singleton that will run the VNC SDK on its own thread. All calls to the SDK should
 * be posted on the SDK message queue using the post method.
 */
public class ViewerThread {
    private static final ViewerThread instance = new ViewerThread();
    private static final String TAG = "ViewerThread";
    private Thread mThread;
    private Handler mHandler;

    private ViewerThread() {}

    /**
     * Callback interface for displaying messages such as errors from the SDK.
     */
    public interface Callback {
        void displayMessage(final int msgId, String error);
    }

    public void init(final String configFile, final Callback callback) {
        synchronized (this) {
            if (initComplete()) {
                return;
            }

            try {
                mThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        initSdk(configFile, callback);
                    }
                });
                mThread.start();
                synchronized (mThread) {
                    while (!initComplete()) {
                        mThread.wait();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Connection error", e);
            }
        }
    }

    private void initSdk(final String configFile, final Callback callback) {
        try {
            // Prepare a looper on current thread.
            Looper.prepare();

            // The Handler will automatically bind to the current thread's Looper.
            mHandler = new Handler();

            // Initialise VNC SDK logging
            Logger.createAndroidLogger();

            try {
                // Create the filestore with the absolute path to Android's app files directory
                DataStore.createFileStore(configFile);
            } catch (Library.VncException e) {
                callback.displayMessage(R.string.failed_to_create_datastore, e.getMessage());
            }

            // Init the SDK
            Library.init(Library.EventLoopType.ANDROID);

            synchronized (mThread) {
                mThread.notifyAll();
            }
            // After the following line the thread will start running the message loop and will not
            // normally exit the loop unless a problem happens or you quit() the looper.
            Looper.loop();
        } catch (Library.VncException e) {
            Log.e(TAG, "Initialisation error: ", e);
            callback.displayMessage(R.string.failed_to_initialise_vnc_sdk, e.getMessage());
        }
    }

    /**
     * Posts a runnable the SDK thread. {@link #init} must be called first.
     * @param runnable The Runnable to run on the SDK thread.
     */
    public void post(Runnable runnable) {
        if (!initComplete()) {
            throw new RuntimeException("init() must be called first");
        }
        mHandler.post(runnable);
    }

    /**
     * Helper method to determine whether or not we need to init the VNC SDK.
     */
    public boolean initComplete() {
        return mHandler != null;
    }

    public static ViewerThread getInstance() {
        return instance;
    }
}
