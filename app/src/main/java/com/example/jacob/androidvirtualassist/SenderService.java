
package com.example.jacob.androidvirtualassist;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.realvnc.vncsdk.CloudListener;
import com.realvnc.vncsdk.Connection;
import com.realvnc.vncsdk.DataStore;
import com.realvnc.vncsdk.ImmutableDataBuffer;
import com.realvnc.vncsdk.Library;
import com.realvnc.vncsdk.Logger;
import com.realvnc.vncsdk.RsaKey;
import com.realvnc.vncsdk.Server;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.EnumSet;

/**
 * This service starts and runs the server in its own thread,
 * starts a cloud listener and authenticates connections.
 */
public class SenderService extends Service
    implements  CloudListener.Callback,
                Server.SecurityCallback,
                Server.ConnectionCallback,
                RsaKey.Callback {

    private static final String TAG = "SenderService";

    // Constants used as keys in an Intent object used to start this activity.
    public static final String SERVER_CLOUD_ADDRESS = "local_cloud_address";
    public static final String SERVER_CLOUD_PASSWORD = "local_cloud_password";

    public static final String MEDIA_PROJECTION_DATA = "media_projection_data";
    public static final String SERVICE_NOTIFICATION = "service_notification";


    public static final int NOTIFICATION_ID = 100;
    
    private static Thread mThread = null;
    private static Handler mHandler = null;
    private static boolean mRunning = false;

    private Server mServer;

    private CloudListener mCloudListener;
    private Runnable mRetryCloudRunnable = new Runnable() {
            @Override
            public void run() {
                createCloudListener();
            }
        };
    
    private MediaProjection mMediaProjection;
    private PowerManager.WakeLock mWakeLock;
    private int mConnections = 0;

    private String localCloudAddress;
    private String localCloudPassword;

    // Pick a random password for the server
    private final String serverPassword =
        String.format("%04d", (new SecureRandom()).nextInt(10000));

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        // Create a wake lock, this is used to prevent the screen from
        // switching off when a connection is active. Note that the wake
        // lock is reference counted, so we need to acquire whenever
        // a connection starts, and release when a connection ends.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK |
                                   PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        
        // Start the SDK thread if not already running.
        if (mThread == null) {

            final String configFile =
                getFilesDir().getAbsolutePath() + "dataStore";

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runSdkLoop(configFile);
                }
            });
            mThread.start();

            synchronized (mThread) {
                while (mHandler == null) {
                    try {
                        mThread.wait();
                    } catch (InterruptedException e) { }
                }
            }
        }
    }

    /**
     * Start the server
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (mServer != null) {
            Log.e(TAG, "Already running");
            return START_NOT_STICKY;
        }

        if (!mRunning) {
            Log.e(TAG, "Initialisation failed");
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        
        localCloudAddress = intent.getStringExtra(SERVER_CLOUD_ADDRESS);
        localCloudPassword = intent.getStringExtra(SERVER_CLOUD_PASSWORD);

        // mpData is the result of the activity started with
        // MediaProjectionManager.createScreenCaptureIntent(), which is used
        // to control access to the MediaProjection service.
        Intent mpData = intent.getParcelableExtra(MEDIA_PROJECTION_DATA);
        MediaProjectionManager mpm =
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mMediaProjection = mpm.getMediaProjection(Activity.RESULT_OK, mpData);
        if (mMediaProjection == null) {
            Log.e(TAG, "Failed to get MediaProjection");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (localCloudAddress.isEmpty() || localCloudPassword.isEmpty()){
            Log.e(TAG, "No connectivity settings specified");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        // If a notification was supplied, start the service in the foreground
        Notification notification = intent.getParcelableExtra(SERVICE_NOTIFICATION);
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        createServer();

        if (!localCloudAddress.isEmpty()) {
            createCloudListener();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mServer != null) {
            // Stop any cloud listener retries
            mHandler.removeCallbacks(mRetryCloudRunnable);
            
            // Shutdown listeners and disconnect all viewers
            synchronized (mThread) {
                mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCloudListener != null) {
                                mCloudListener.destroy();
                                mCloudListener = null;
                            }
                            mServer.disconnectAll("The server has been stopped",
                                                  EnumSet.of(Server.DisconnectFlags.DISCONNECT_ALERT));
                        }
                    });

                // Wait for all connections to finish disconnecting
                while (mConnections > 0) {
                    try {
                        mThread.wait();
                    } catch (InterruptedException e) { }
                }
            }

            // Destroy the server
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mServer != null) {
                        mServer.destroy();
                        mServer = null;
                    }
                    synchronized (mThread) {
                        mThread.notifyAll();
                    }
                }
            });

            // Wait for the server to shutdown
            synchronized (mThread) {
                while (mServer != null) {
                    try {
                        mThread.wait();
                    } catch (InterruptedException e) { }
                }
            }
        }

        // Stop media projection
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        super.onDestroy();
    }

    /* Binder */

    // Used a static inner class with a weak reference to the
    // service to work around the following Android bug:
    // https://code.google.com/p/android/issues/detail?id=6426
    public static class LocalBinder extends Binder {
        private WeakReference<Service> mServiceRef;
        public LocalBinder(Service service) {
            mServiceRef = new WeakReference<>(service);
        }
        SenderService getService() {
            return (SenderService)mServiceRef.get();
        }
    }

    private final IBinder mBinder = new LocalBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public final String getPassword() {
        return serverPassword;
    }

    /* Cloud callbacks */
    
    @Override
    public void listeningFailed(CloudListener listener, String cloudError,
                                int retryTimeSecs) {
        Log.e(TAG, "Listening Failed: " + cloudError);

        if (retryTimeSecs < 0) {
            stopSelf();

        } else {
            Log.d(TAG, "Will retry in " + retryTimeSecs + " seconds");
            mHandler.removeCallbacks(mRetryCloudRunnable);
            mHandler.postDelayed(mRetryCloudRunnable,
                                 retryTimeSecs * 1000);
        }
    }

    @Override
    public boolean filterConnection(CloudListener listener,
                                    String peerCloudAddress) {
        Log.d(TAG, "Cloud listener filter connection");
        return true;
    }

    @Override
    public void listeningStatusChanged(CloudListener listener,
                                       CloudListener.Status status) {
        if (status == CloudListener.Status.STATUS_SEARCHING) {
            Log.d(TAG, "Cloud listener status: searching");
        } else if (status == CloudListener.Status.STATUS_ONLINE) {
            Log.d(TAG, "Cloud listener status: online");
        }
    }

    /* Security callbacks */
    
    @Override
    public boolean verifyPeer(Server server, Connection connection,
                              String viewerHexFingerprint,
                              ImmutableDataBuffer viewerRsaPublic) {
        return true; // Accept verification
    }

    @Override
    public boolean isUserNameRequired(Server server, Connection connection) {
        return false; // Don't require a username
    }

    @Override
    public boolean isPasswordRequired(Server server, Connection connection) {
        return true; // Require a password
    }

    @Override
    public EnumSet<Server.Permissions>
    authenticateUser(Server server, Connection connection,
                     String username, String password) {
        // Check that the password matches
        if (password.equals(serverPassword)) {
            return EnumSet.of(Server.Permissions.PERM_ALL);
        }
        // Incorrect password, reject connection by returning no permissions
        return EnumSet.noneOf(Server.Permissions.class);
    }

    /* Connection callbacks */

    @Override
    public void connectionStarted(Server server, Connection connection) {
        Log.d(TAG, "Connection started");
        // Acquire wake lock to prevent the screen from switching off
        mWakeLock.acquire();
        synchronized (mThread) { ++mConnections; }
    }

    public void connectionEnded(Server server, Connection connection) {
        Log.d(TAG, "Connection ended");
        // Release wake lock
        mWakeLock.release();

        synchronized (mThread) {
            --mConnections;
            mThread.notifyAll();
        }
    }

    /* RsaKey callback */

    public void detailsReady(ImmutableDataBuffer rsaPublic, String hexFingerprint,
                             String catchphraseFingerprint) {
        Log.d(TAG, "Server id is: " + hexFingerprint);
        Log.d(TAG, "Server catchphrase is: " + catchphraseFingerprint);
    }


    /**
     * Create and setup the server.
     */
    private void createServer() {
        // Runs on the SDK thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mServer != null) {
                    mServer.destroy();
                    mServer = null;
                }
                Log.d(TAG, "Creating server");
                try {
                    mServer = new Server(getApplicationContext(),
                                         mMediaProjection);
                    mServer.setFriendlyName(Build.MODEL);
                    mServer.setSecurityCallback(SenderService.this);
                    mServer.setConnectionCallback(SenderService.this);
                } catch (Library.VncException e) {
                    Log.e(TAG, "Server error", e);
                }
                if (mServer == null) {
                    // Shutdown the service if we failed to create the server
                    stopSelf();
                }
            }
        });
    }

    /**
     * Create and setup the cloud listener
     */
    private void createCloudListener() {
        // Runs on the SDK thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mServer == null) {
                    Log.e(TAG, "Server not created");
                    return;
                }
                // Set up the various callbacks
                if (mCloudListener != null) {
                    mCloudListener.destroy();
                }
                try {                    
                    Log.d(TAG, "Creating cloud listener");
                    mCloudListener =
                        new CloudListener(localCloudAddress,
                                          localCloudPassword,
                                          mServer.getConnectionHandler(),
                                          SenderService.this);
                    Log.d(TAG, "Created cloud listener");
                } catch (Library.VncException e) {
                    Log.e(TAG, "CloudListener error", e);
                }
                if (mCloudListener == null) {
                    // Shutdown the service if we failed to create a
                    // cloud listener.
                    stopSelf();
                }
            }
        });
    }



    /**
     * Run the SDK's main loop.
     * This is executed on the SDK thread.
     */
    private void runSdkLoop(final String configFile) {
        try {
            // Prepare a looper on current thread.
            Looper.prepare();

            // The Handler will automatically bind to the current thread's
            // Looper.
            mHandler = new Handler();

            // Initialise VNC SDK logging
            Logger.createAndroidLogger();

            // Create the filestore with the absolute path to Android's
            // app files directory
            DataStore.createFileStore(configFile);

            // Init the SDK
            Library.init(Library.EventLoopType.ANDROID);

            mRunning = true;
            
            synchronized (mThread) {
                mThread.notifyAll();
            }
            // After the following line the thread will start running the
            // message loop and will not normally exit the loop unless a
            // problem happens or you quit() the looper.
            Looper.loop();
        } catch (Library.VncException e) {
            Log.e(TAG, "Initialisation error: ", e);
        }

        // Something went wrong, but make sure we sent the notification.
        synchronized (mThread) {
            Log.e(TAG, "Sending notification");
            mThread.notifyAll();
        }
    }
}
