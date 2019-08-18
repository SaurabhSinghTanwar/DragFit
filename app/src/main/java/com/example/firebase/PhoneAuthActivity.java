package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.chaos.view.PinView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentChange.Type;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Query.Direction;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuthActivity";
    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";
    private static String code = null;
    private static FirebaseFirestore db;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private DocumentReference documentReference;

    //private ViewGroup mSignedInViews;

    private TextView mStatusText;
    private TextView mDetailText;
    private PinView verifyCodeET;

    private EditText mVerificationField;

    private Button mVerifyButton;
    private Button mResendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

       //  Restore instance state
          if (savedInstanceState != null) {
             onRestoreInstanceState(savedInstanceState);  }
        // Assign views
        // mSignedInViews = findViewById(R.id.signedInButtons);

        mStatusText = findViewById(R.id.status);
        mDetailText = findViewById(R.id.detail);

//        mPhoneNumberField = findViewById(R.id.fieldPhoneNumber);
   //     mVerificationField = findViewById(R.id.fieldVerificationCode);
        mVerificationField = (PinView) findViewById(R.id.pinView);

        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        db = FirebaseFirestore.getInstance();



        final String phonenumber = getIntent().getStringExtra("phonenumber");
        startPhoneNumberVerification(phonenumber);
        findViewById(R.id.buttonVerifyPhone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                code = mVerificationField.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    mVerificationField.setError("Enter code...");
                    mVerificationField.requestFocus();
                    return;
                }
                verifyPhoneNumberWithCode(code);
            }
        });
        findViewById(R.id.buttonResend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resendVerificationCode(mVerificationField.getText().toString(), mResendToken);
            }
        });
    }
        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]

        private void startPhoneNumberVerification (String phoneNumber){
            // [START start_phone_auth]
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks);        // OnVerificationStateChangedCallbacks
            // [END start_phone_auth]
            mVerificationInProgress = true;
        }

        private void verifyPhoneNumberWithCode (String code){
            // [START verify_with_code]

            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
            // [END verify_with_code]
            signInWithPhoneAuthCredential(credential);
        }

        // [START resend_verification]
        private void resendVerificationCode (String phoneNumber,
                PhoneAuthProvider.ForceResendingToken token){
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,        // Phone number to verify
                    60,                 // Timeout duration
                    TimeUnit.SECONDS,   // Unit of timeout
                    this,               // Activity (for callback binding)
                    mCallbacks,         // OnVerificationStateChangedCallbacks
                    token);             // ForceResendingToken from callbacks
        }
        // [END resend_verification]

        // [START sign_in_with_phone]
        private void signInWithPhoneAuthCredential (PhoneAuthCredential credential){
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "signInWithCredential:success");

                                FirebaseUser user = task.getResult().getUser();
                                // [START_EXCLUDE]
                                updateUI(STATE_SIGNIN_SUCCESS, user);
                                // [END_EXCLUDE]
                            } else {
                                // Sign in failed, display a message and update the UI
                                Log.w(TAG, "signInWithCredential:failure", task.getException());
                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    // The verification code entered was invalid
                                    // [START_EXCLUDE silent]
                                    mVerificationField.setError("Invalid code.");
                                    // [END_EXCLUDE]
                                }
                                // [START_EXCLUDE silent]
                                // Update UI
                                updateUI(STATE_SIGNIN_FAILED);
                                // [END_EXCLUDE]
                            }
                        }
                    });
        }
        // [END sign_in_with_phone]

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks
       mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                // [START_EXCLUDE silent]
                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential);
                // [END_EXCLUDE]
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    mVerificationField.setError("Invalid otp number.");
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // [START_EXCLUDE]
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show();
                    // [END_EXCLUDE]
                }
                // Show a message and update the UI
                // [START_EXCLUDE]
                updateUI(STATE_VERIFY_FAILED);
                // [END_EXCLUDE]
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // [START_EXCLUDE]
                // Update UI
                //updateUI(STATE_CODE_SENT);
                // [END_EXCLUDE]
            }
        };
        // [END phone_auth_callbacks]

    //  @Override
    //  protected void onSaveInstanceState(Bundle outState) {
    //      super.onSaveInstanceState(outState);
    //      outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    //  }

    //  @Override
    //   protected void onRestoreInstanceState(Bundle savedInstanceState) {
    //      super.onRestoreInstanceState(savedInstanceState);
    //      mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    //  }

    private void updateUI(int uiState)                              {  updateUI(uiState, mAuth.getCurrentUser(), null); }
    private void updateUI(int uiState, FirebaseUser user)           {    updateUI(uiState, user, null); }
    private void updateUI(int uiState, PhoneAuthCredential cred)    {    updateUI(uiState, null, cred); }
    private void updateUI(FirebaseUser user)                 {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user);
        } else {
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        switch (uiState) {
            case STATE_INITIALIZED:
                // Initialized state, show only the phone number field and start button
                disableViews(mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(null);
                break;
            case STATE_CODE_SENT:
                // Code sent state, show the verification field, the
              //  enableViews(mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(R.string.status_code_sent);
                verifyPhoneNumberWithCode(code);
                break;
            case STATE_VERIFY_FAILED:
                // Verification has failed, show all options
                //enableViews( mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(R.string.status_verification_failed);

                break;
            case STATE_VERIFY_SUCCESS:
                // Verification has succeeded, proceed to firebase sign in
               // disableViews( mVerifyButton, mResendButton, mVerificationField);
                mDetailText.setText(R.string.status_verification_succeeded);
                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        mVerificationField.setText(cred.getSmsCode());
                    } else {
                        mVerificationField.setText(R.string.instant_validation);
                    }
                }
                afterverification(user,cred);
                break;
            case STATE_SIGNIN_FAILED:
                // No-op, handled by sign-in check
                mDetailText.setText(R.string.status_sign_in_failed);
                Intent intent = new Intent(PhoneAuthActivity.this, EnterPhone.class);
                startActivity(intent);
                break;
            case STATE_SIGNIN_SUCCESS:
                // Np-op, handled by sign-in check
                afterverification(user,cred);
                break;
        }
    }

   // private boolean validatePhoneNumber() {
   //     String phoneNumber = mPhoneNumberField.getText().toString();
   //     if (TextUtils.isEmpty(phoneNumber)) {
   //         mPhoneNumberField.setError("Invalid phone number.");
   //         return false;
   //     }
   //     return true;
   // }

    private void enableViews(View... views) {
        for (View v : views) {
            v.setEnabled(true);
        }
    }

    private void disableViews(View... views) {
        for (View v : views) {
            v.setEnabled(false);
        }
    }

    private void afterverification(FirebaseUser user,PhoneAuthCredential credential) {
        String uid = FirebaseAuth.getInstance().getUid();
        String path = "keys/" + uid ;
        documentReference = FirebaseFirestore.getInstance().document(path);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(documentSnapshot.exists()){
                    if(documentSnapshot.getString("key").equals("1")){
                        Intent intent = new Intent(PhoneAuthActivity.this, Profile.class);
                        startActivity(intent);
                    }
                    else{
                        Intent intent = new Intent(PhoneAuthActivity.this, GetUserRegistered.class);
                        startActivity(intent);
                    }
                }
                else{
                    Map<String, Object> user= new HashMap<String, Object>();
                    user.put("key","0");
                    documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Intent intent = new Intent(PhoneAuthActivity.this, GetUserRegistered.class);
                            startActivity(intent);
                        }
                    });
                }
            }
        });
    }
}
