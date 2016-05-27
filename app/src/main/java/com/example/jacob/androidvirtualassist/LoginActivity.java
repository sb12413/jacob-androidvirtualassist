package com.example.jacob.androidvirtualassist;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;

/**
 * Created by Jacob on 19/04/2016.
 */
public class LoginActivity extends AppCompatActivity implements LoginFragment.OnLoginListener, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_GOOGLE_LOGIN = 1;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "LoginActivity";
    private Firebase firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        if (savedInstanceState == null) {
            Firebase.setAndroidContext(this);
        }
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        firebase = new Firebase(Constants.FIREBASE_URL);
        if (firebase.getAuth() == null || isExpired(firebase.getAuth())) {
            switchToLoginFragment();
        } else {

            addToFirabase();

            switchToMainActivity();
        }
    }

    private boolean isExpired(AuthData authData) {
        return (System.currentTimeMillis() / 1000) >= authData.getExpires();
    }

    @Override
    public void onLogin(String email, String password) {
        //Log user in with username & password
        firebase = new Firebase(Constants.FIREBASE_URL);
        firebase.authWithPassword(email, password, new MyAuthResultHandler());
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage());
    }

    class MyAuthResultHandler implements Firebase.AuthResultHandler {
        @Override
        public void onAuthenticated(AuthData authData) {
            addToFirabase();
            switchToMainActivity();
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            Log.e(TAG, "onAuthenticationError:" + firebaseError.getMessage());
        }
    }

    @Override
    public void onGoogleLogin() {
        //Log user in with Google Account
        Intent intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(intent, REQUEST_CODE_GOOGLE_LOGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_GOOGLE_LOGIN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()){
                GoogleSignInAccount account = result.getSignInAccount();
                String emailAddress = account.getEmail();
                getGoogleOAuthToken(emailAddress);
            }
        }
    }
    private void getGoogleOAuthToken(final String emailAddress) {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;

            @Override
            protected String doInBackground(Void... params) {
                String token = null;
                try {
                    String scope = "oauth2:profile email";
                    token = GoogleAuthUtil.getToken(LoginActivity.this, emailAddress, scope);
                } catch (IOException transientEx) {
                /* Network or server error */
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                /* We probably need to ask for permissions, so start the intent if there is none pending */
                    Intent recover = e.getIntent();
                    startActivityForResult(recover, LoginActivity.REQUEST_CODE_GOOGLE_LOGIN);
                } catch (GoogleAuthException authEx) {
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }
                return token;
            }
            @Override
            protected void onPostExecute(String token) {
                Log.d(TAG, "onPostExecute, token" + token);
                if (token != null){
                    onGoogleLoginWithToken(token);
                }else {
                    showLoginError(errorMessage);
                }
            }
        };
        task.execute();
    }

    private void onGoogleLoginWithToken(String oAuthToken) {
        //Log user in with Google OAuth Token
        firebase = new Firebase(Constants.FIREBASE_URL);
        firebase.authWithOAuthToken("google", oAuthToken, new MyAuthResultHandler());
    }

    public void switchToLoginFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, new LoginFragment(), "Login");
        ft.commit();
    }


    private void switchToMainActivity() {
        Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
        LoginActivity.this.startActivity(myIntent);
    }

    private void showLoginError(String message) {
        LoginFragment loginFragment = (LoginFragment) getSupportFragmentManager().findFragmentByTag("Login");
        loginFragment.onLoginError(message);
    }
    public void addToFirabase(){
        FirebaseCloudSettings cl = new FirebaseCloudSettings();
        String uids = firebase.getAuth().getUid();

        firebase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                FirebaseCloudSettings cl = dataSnapshot.getValue(FirebaseCloudSettings.class);
//                Constants.KEY = (dataSnapshot.getKey());
//                System.out.println("KEY:"+dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
        //firebase = new Firebase(Constants.FIREBASE_URL+"/users/" + uids + "/cloudSettings");
        //firebase.child("users").child(uids).child("cloudSettings").push().setValue(cl);
    }
}
