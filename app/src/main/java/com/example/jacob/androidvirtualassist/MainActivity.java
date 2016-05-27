
package com.example.jacob.androidvirtualassist;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.firebase.client.Firebase;

/**
 * The activity loaded on app initialization. This allows the service to be
 * started and stopped, and displays the current status and password.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Intent object to initiate Service
    private Intent mServiceIntent;
    //SenderService class object
    private SenderService mService;
    //TextView to display sender service status
    private TextView mStatusText;
    //Toggle to Start/Stop sender service
    private ToggleButton mStartStopBtn;
    //Connecting to viewer cloud address
    private Button mConnectBtn;
    //initializing firebase object
    private Firebase firebase;
    //starting new service connection
    private ServiceConnection mServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            SenderService.LocalBinder binder = (SenderService.LocalBinder)service;
            mService = binder.getService();
            updateStatus(true);
            
        }
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            updateStatus(false);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Creating new Intent for SenderService class
        mServiceIntent = new Intent(this, SenderService.class);
        //passing data strings to that class with cloud addresses
        mServiceIntent.putExtra(SenderService.SERVER_CLOUD_ADDRESS, CloudSettings.getServerCloudAddress());
        mServiceIntent.putExtra(SenderService.SERVER_CLOUD_PASSWORD, CloudSettings.getServerCloudPassword());

        // Create a notification for the service to use
        Intent thisIntent = new Intent(this, MainActivity.class);
        Notification notification = new Notification.Builder(this)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.running))
            .setSmallIcon(R.drawable.ic_stat_a).setColor(00253271)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, thisIntent, 0))
            .build();
        mServiceIntent.putExtra(SenderService.SERVICE_NOTIFICATION, notification);
        
        mStartStopBtn = (ToggleButton) findViewById(R.id.startStopButton);
        mStartStopBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startSenderService();
                } else {
                    stopSenderService();
                }
            }
        });

        mConnectBtn = (Button) findViewById(R.id.button);
        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });

        mStatusText = (TextView) findViewById(R.id.status);

        bindService(mServiceIntent, mServiceConn, 0);
        updateStatus(mService != null);
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConn);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Cannot use media projection");
            return;
        }

        //passing media projection content to a service
        mServiceIntent.putExtra(SenderService.MEDIA_PROJECTION_DATA, data);
        startService(mServiceIntent);
        bindService(mServiceIntent, mServiceConn, 0);
        updateStatus(mService != null);
    }

    protected void startSenderService() {
        Log.d(TAG, "startSenderService");

        //Creating Media Projection object to capture screen content
        MediaProjectionManager mpm =
                (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        //starting Activity
        startActivityForResult(mpm.createScreenCaptureIntent(), 1);
    }

    protected void stopSenderService() {
        Log.d(TAG, "stopSenderService");
        stopService(mServiceIntent);
    }

    protected void updateStatus(boolean running) {
        if (running) {
            String text = getString(R.string.running) + "\n\n" +
                getString(R.string.password) + " " + mService.getPassword();
            mStatusText.setText(text);
            
        } else {
            mStatusText.setText(R.string.not_running);
        }
    }

    /**
     * This method initialises an Intent with the local cloud address and password and the remote
     * cloud address, before launching the {@link ViewerActivity} with that information.
     */
    private void connect() {
        Intent intent = new Intent(this, ViewerActivity.class);
        intent.putExtra(ViewerActivity.VIEWER_CLOUD_ADDRESS, CloudSettings.getViewerCloudAddress());
        intent.putExtra(ViewerActivity.VIEWER_CLOUD_PASSWORD, CloudSettings.getViewerCloudPassword());
        intent.putExtra(ViewerActivity.PEER_CLOUD_ADDRESS, CloudSettings.getPeerCloudAddress());

        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        //Log the user out.
        removeFromFirebase();
        firebase = new Firebase(Constants.FIREBASE_URL);
        firebase.unauth();
        stopSenderService();
        switchToLoginActivity();
    }
    private void switchToLoginActivity() {
        Intent myIntent = new Intent(MainActivity.this, LoginActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopSenderService();
        logout();
    }
    public void removeFromFirebase(){
        firebase = new Firebase(Constants.FIREBASE_URL);
        String uids = firebase.getAuth().getUid();
        firebase = new Firebase(Constants.FIREBASE_URL+"/users/"+uids+"/cloudSettings/");
        System.out.println(firebase);
        firebase.setValue(null);
//        System.out.println("KEY_OUT:"+Constants.KEY);
    }
}
