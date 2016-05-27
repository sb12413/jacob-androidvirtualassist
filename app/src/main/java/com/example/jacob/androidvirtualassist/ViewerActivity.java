
package com.example.jacob.androidvirtualassist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import com.example.jacob.androidvirtualassist.input.TouchEventAdapter;
import com.example.jacob.androidvirtualassist.ui.FrameBufferView;
import com.realvnc.vncsdk.CloudConnector;
import com.realvnc.vncsdk.ImmutableDataBuffer;
import com.realvnc.vncsdk.Library;
import com.realvnc.vncsdk.Viewer;

import java.util.EnumSet;

/**
 * This class represents the main screen used to show the sender's framebuffer.
 */
public class ViewerActivity extends AppCompatActivity implements TouchEventAdapter.Callback,
        Viewer.AuthenticationCallback, Viewer.PeerVerificationCallback,
        ViewerThread.Callback {

    private static final String TAG = "ViewerActivity";

    // Constants used as keys in an Intent object used to start this activity.

    public static final String VIEWER_CLOUD_ADDRESS = "local_cloud_address";
    public static final String VIEWER_CLOUD_PASSWORD = "local_cloud_password";
    public static final String PEER_CLOUD_ADDRESS = "peer_cloud_address";


    private Viewer mViewer;
    private CloudConnector mCloudConnector;

    private FrameBufferView mFrameBufferView;

    private AlertDialog mAuthDialog;
    private ProgressDialog mProgressDialog;
    private AlertDialog mDisconnectDlg;
    private AlertDialog mAlertDialog;
    private AlertDialog mPeerDialog;

    // region dialog builders

    /**
     * Helper method to build a disconnect confirmation dialog
     */
    private AlertDialog buildDisconnectAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        return builder.setMessage(R.string.confirm_disconnect)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.disconnect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                        ViewerThread.getInstance().post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mViewer.disconnect();
                                        } catch (Library.VncException e) {
                                            Log.e(TAG, "Disconnect error:", e);
                                        }
                                    }
                                }
                        );

                    }
                })
                .create();
    }

    /**
     * Builds the disconnected dialog.
     */
    private AlertDialog buildAlertDialog(String disconnectedReason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        return builder.setMessage(disconnectedReason)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cleanup();
                    }
                })
                .setCancelable(false)
                .create();
    }

    /**
     * Build the authentication dialog.
     */
    private AlertDialog buildAuthDialog(boolean needUser, boolean needPassword) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View authDialogView = getLayoutInflater().inflate(R.layout.auth_dialog, null);
        if (!needUser) {
            authDialogView.findViewById(R.id.auth_username).setVisibility(View.GONE);
        }
        if (!needPassword) {
            authDialogView.findViewById(R.id.auth_password).setVisibility(View.GONE);
        }

        return builder.setView(authDialogView)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ViewerThread.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mViewer.sendAuthenticationResponse(false, "", "");
                                    mViewer.disconnect();
                                } catch (Library.VncException e) {
                                    Log.w(TAG, "userPasswdResult", e);
                                    displayMessage(R.string.sending_authentication_response_failed, e.getMessage());
                                }
                            }
                        });
                        dismissDialogs();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String username = ((EditText) authDialogView.findViewById(R.id.auth_username)).getText()
                                .toString();
                        final String password = ((EditText) authDialogView.findViewById(R.id.auth_password)).getText()
                                .toString();
                        ViewerThread.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mViewer.sendAuthenticationResponse(true, username, password);
                                } catch (Library.VncException e) {
                                    Log.w(TAG, "userPasswdResult", e);
                                    displayMessage(R.string.sending_authentication_response_failed, e.getMessage());
                                }
                            }
                        });
                    }
                })
                .setCancelable(false)
                .create();
    }

    /**
     * Build the peer verification dialog.
     */
    private AlertDialog buildPeerVerificationDialog(String hexFingerprint,
                                                    String catchphraseFingerprint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.peer_verification_title);

        String message;
        try {
            String address = mViewer.getPeerAddress();
            message = getResources().getString(R.string.peer_verification_message,
                    address, hexFingerprint, catchphraseFingerprint);
        } catch (Library.VncException e) {
            Log.w(TAG, "Unable to get peer address");
            message = getResources().getString(R.string.peer_verification_message,
                    "Unknown", hexFingerprint, catchphraseFingerprint);
        }

        builder.setMessage(message);

        return builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ViewerThread.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        sendPeerIdentityResponse(false);
                    }
                });
                dismissDialogs();
            }
        })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ViewerThread.getInstance().post(new Runnable() {
                            @Override
                            public void run() {
                                sendPeerIdentityResponse(true);
                            }
                        });
                        dismissDialogs();
                    }
                })
                .setCancelable(false)
                .create();
    }
    // endregion

    // region Activity overrides
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.activity_viewer);

        mFrameBufferView = (FrameBufferView) findViewById(R.id.frameBufferView);

        // Set up touch event handling
        mFrameBufferView.setTouchEventAdapter(new TouchEventAdapter(this, this));

        // Keep device awake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onPause() {
        if (ViewerThread.getInstance().initComplete()) {
            ViewerThread.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    if (mViewer != null && mViewer.getConnectionStatus() == Viewer.ConnectionStatus.CONNECTED) {
                        try {
                            // Disconnect when the activity is hidden.
                            mViewer.disconnect();
                        } catch (Library.VncException e) {
                            Log.e(TAG, "Disconnect error:", e);
                        }
                    }
                }
            });
        }

        dismissDialogs();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        init();
    }

    @Override
    public void onBackPressed() {
        if (mAuthDialog != null && mAuthDialog.isShowing()) {
            mAuthDialog.cancel();
        } else if (mViewer.getConnectionStatus() == Viewer.ConnectionStatus.CONNECTED ||
                mViewer.getConnectionStatus() == Viewer.ConnectionStatus.CONNECTING) {
            mDisconnectDlg = buildDisconnectAlertDialog();
            mDisconnectDlg.show();
        } else {
            cleanup();
        }
    }
    //endregion

    // region helper methods
    /**
     * Initialization
     */
    private void init() {

        Intent intent = getIntent();

        //final boolean useCloudConnectivity = intent.getBooleanExtra(USE_CLOUD_CONNECTIVITY, true);

        final String localCloudAddress = intent.getStringExtra(VIEWER_CLOUD_ADDRESS);
        final String localCloudPassword = intent.getStringExtra(VIEWER_CLOUD_PASSWORD);
        final String peerCloudAddress = intent.getStringExtra(PEER_CLOUD_ADDRESS);

        mProgressDialog = ProgressDialog.show(ViewerActivity.this,
                "", getString(R.string.connecting_dlg), true);

        ViewerThread.getInstance().init(getFilesDir().getAbsolutePath() + "dataStore", this);

        ViewerThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                // Set up the various callbacks
                if (mCloudConnector != null) {
                    mCloudConnector.destroy();
                }
                if (mViewer != null) {
                    mViewer.destroy();
                }

                try {
                    mViewer = new Viewer();
                } catch (Library.VncException e) {
                    displayMessage(R.string.failed_to_create_viewer, e.getMessage());
                    return;
                }

                try {
                    /*
                    These callbacks will all be run on the SDK thread, so any UI updates must be
                    done through calls to {@link #runOnUiThread}.
                     */
                    mViewer.setConnectionCallback(new Viewer.ConnectionCallback() {
                        @Override
                        public void connecting(Viewer viewer) {
                        }

                        @Override
                        public void connected(Viewer viewer) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.hide();
                                    //mExtensionKeyboardView.setVisibility(View.VISIBLE);
                                }
                            });
                        }

                        @Override
                        public void disconnected(Viewer viewer,
                                                 final String reason,
                                                 final EnumSet<Viewer.DisconnectFlags> disconnectFlags) {

                            final String disconnectMsg = mViewer.getDisconnectMessage() == null ?
                                    mViewer.getDisconnectReason() :
                                    String.format("%s, %s", mViewer.getDisconnectReason(), mViewer.getDisconnectMessage());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressDialog.hide();
                                    String message = getString(R.string.disconnected) +
                                            disconnectMsg;
                                    if (mAuthDialog != null && mAuthDialog.isShowing()) {
                                        message = getString(R.string.disconnected_authenticating) +
                                                mViewer.getDisconnectReason();
                                    }
                                    mAlertDialog = buildAlertDialog(message);

                                    if (disconnectFlags.contains(Viewer.DisconnectFlags.ALERT_USER)) {
                                        mAlertDialog.show();
                                    } else {
                                        cleanup();
                                    }
                                }
                            });
                        }
                    });
                    mViewer.setFramebufferCallback(mFrameBufferView);

                    // Set the framebuffer to a sensible default. We will be notified of the actual
                    // size before rendering anything.
                    mFrameBufferView.serverFbSizeChanged(mViewer, 1024, 768);
                    mViewer.setAuthenticationCallback(ViewerActivity.this);

                    mCloudConnector = new CloudConnector(localCloudAddress, localCloudPassword);
                    mCloudConnector.connect(peerCloudAddress, mViewer.getConnectionHandler());

                } catch (Library.VncException e) {
                    Log.e(TAG, "Connection error", e);
                    displayMessage(R.string.failed_to_make_vnc_cloud_connection, e.getMessage());
                }
            }
        });
    }

    private void dismissDialogs() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mAuthDialog != null && mAuthDialog.isShowing()) {
            mAuthDialog.dismiss();
        }
        if (mDisconnectDlg != null && mDisconnectDlg.isShowing()) {
            mDisconnectDlg.dismiss();
        }
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
        if (mPeerDialog != null && mPeerDialog.isShowing()) {
            mPeerDialog.dismiss();
        }
    }

    private void cleanup() {
        if (ViewerThread.getInstance().initComplete()) {
            ViewerThread.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    if (mCloudConnector != null) {
                        mCloudConnector.destroy();
                        mCloudConnector = null;
                    }
                    if (mViewer != null) {
                        mViewer.destroy();
                        mViewer = null;
                    }
                }
            });
        }
        finish();
    }

    // endregion

    @Override
    public void onPointerEvent(int x, int y, EnumSet<Viewer.MouseButton> mouseButtons) {
        mFrameBufferView.sendPointerEvent(mViewer, x, y, mouseButtons, false);
    }

    @Override
    public void onScaleChanged(float scale) {
        mFrameBufferView.setScaleFactor(scale);
    }

    @Override
    public void onScroll(float x, float y) {
        mFrameBufferView.setOffset(x, y);
    }

    // region Authentication callbacks
    @Override
    public void requestUserCredentials(Viewer viewer, final boolean needUser, final boolean needPassword) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAuthDialog = buildAuthDialog(needUser, needPassword);

                // Show the keyboard when the auth dialog appears.
                mAuthDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
//                        View usernameView = mAuthDialog.findViewById(R.id.auth_username);
//                        View passwordView = mAuthDialog.findViewById(R.id.auth_password);
//                        View focusView = null;
//                        if (usernameView.getVisibility() == View.VISIBLE) {
//                            focusView = usernameView;
//                        } else if (passwordView.getVisibility() == View.VISIBLE) {
//                            focusView = passwordView;
//                        }
//                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//
//                        if (imm != null) {
//                            imm.showSoftInput(focusView, 0);
//                        }
                    }
                });
                mAuthDialog.show();
            }
        });
    }

    @Override
    public void cancelUserCredentialsRequest(Viewer viewer) {
        displayMessage(R.string.authentication_check_cancelled, "");
    }

    // endregion

    // region Peer Verification callbacks
    @Override
    public void verifyPeer(Viewer viewer, final String hexFingerprint,
                           final String catchphraseFingerprint, ImmutableDataBuffer serverRsaPublic) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPeerDialog = buildPeerVerificationDialog(hexFingerprint, catchphraseFingerprint);
                mPeerDialog.show();
            }
        });
    }

    private void sendPeerIdentityResponse(final boolean accept) {
        ViewerThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                try {
                    mViewer.sendPeerVerificationResponse(accept);
                } catch (Library.VncException e) {
                    displayMessage(R.string.peer_verification_failed, e.getMessage());
                }
            }
        });
    }

    @Override
    public void cancelPeerVerification(Viewer viewer) {
        displayMessage(R.string.peer_verification_cancelled, "");
    }

    @Override
    public void displayMessage(final int msgId, final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog = buildAlertDialog(getString(msgId) + message);
                mAlertDialog.show();
            }
        });
    }
}
