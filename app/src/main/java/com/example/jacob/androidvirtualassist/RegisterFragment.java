package com.example.jacob.androidvirtualassist;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

/**
 * Created by Jacob on 20/04/2016.
 */
public class RegisterFragment extends Fragment {
    public RegisterFragment(){
        // Required empty public constructor
    }
    private EditText mPasswordView;
    private EditText mEmailView;
    private View mRegisterForm;
    private View mProgressSpinner;
    private boolean mRegister;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_register, container, false);
        mEmailView = (EditText) rootView.findViewById(R.id.email_register);
        mPasswordView = (EditText) rootView.findViewById(R.id.password_register);
        mRegisterForm = rootView.findViewById(R.id.register_form);
        mProgressSpinner = rootView.findViewById(R.id.register_progress);
        View registerButton = rootView.findViewById(R.id.register_button);

        mEmailView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_NEXT) {
                    mPasswordView.requestFocus();
                    return true;
                }
                return false;
            }
        });
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_NULL) {
                    createUser();
                    return true;
                }
                return false;
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });
        return rootView;
    }

    private void createUser() {
        if (mRegister) {
            return;
        }

        mEmailView.setError(null);
        mPasswordView.setError(null);

        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancelLogin = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.invalid_password));
            focusView = mPasswordView;
            cancelLogin = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.field_required));
            focusView = mEmailView;
            cancelLogin = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.invalid_email));
            focusView = mEmailView;
            cancelLogin = true;
        }

        if (cancelLogin) {
            // error in login
            focusView.requestFocus();
        } else {
            // show progress spinner, and start background task to login
            showProgress(true);
            mRegister = true;
            addUser(email, password);
            hideKeyboard();
        }
    }

    private void addUser(String email, String password) {
        final Firebase firebase = new Firebase(Constants.FIREBASE_URL);
        firebase.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                Toast.makeText(getActivity(), "Successfully created user account",
                        Toast.LENGTH_LONG).show();

                switchToLoginFragment();

            }
            @Override
            public void onError(FirebaseError firebaseError) {
                Toast.makeText(getActivity(), "Failed to created user account",
                        Toast.LENGTH_LONG).show();

                switchToLoginFragment();
            }
        });
    }

    private void switchToLoginFragment() {
        LoginFragment loginFragment = new LoginFragment();
        this.getFragmentManager().beginTransaction()
                .replace(R.id.fragment, loginFragment,null)
                .addToBackStack(null)
                .commit();
    }


    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEmailView.getWindowToken(), 0);
    }

    private void showProgress(boolean show) {
        mProgressSpinner.setVisibility(show ? View.VISIBLE : View.GONE);
        mRegisterForm.setVisibility(show ? View.GONE : View.VISIBLE);

    }


    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }


}
