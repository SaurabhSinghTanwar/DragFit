package com.example.firebase;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.chaos.view.PinView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.Objects;

public class GetUserRegistered extends AppCompatActivity {

    private RadioButton gender_male;
    private RadioButton gender_female;
    private EditText fullname;
    private EditText age_;
    private DocumentReference documentReference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        gender_male = findViewById(R.id.male);
        gender_female = findViewById(R.id.female);

        fullname = findViewById(R.id.name);
        age_ =  findViewById(R.id.age);


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_user_registered);


        findViewById(R.id.create_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uid = FirebaseAuth.getInstance().getUid();
                String path ="Users"+uid;
                String name = fullname.getText().toString() ;
                String sex = "";


                if(findViewById(R.id.male).isSelected()){
                    sex = "M";
                }
                else if(findViewById(R.id.female).isSelected()){
                    sex = "F";
                }
                String Age = age_.getText().toString().trim();
                int age = Integer.parseInt(Age);
                if(name.isEmpty()){
                    fullname.setError("Enter a valid name");
                    fullname.requestFocus();
                    return;
                }
                if(age<13 && age>70){
                    age_.setError("Enter Age Between 13-70");
                    age_.requestFocus();
                    return;
                }
                documentReference = FirebaseFirestore.getInstance().document(path);
                final Map<String, Object> user= new HashMap<String, Object>();
                user.put("name", name);
                user.put("sex",sex);
                user.put("age",Age);
                documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        String uid = FirebaseAuth.getInstance().getUid();
                        final DocumentReference keydoc = FirebaseFirestore.getInstance().document("keys/"+uid);
                        keydoc.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if(documentSnapshot.exists()){
                                    final Map<String, Object> users= new HashMap<String, Object>();
                                    users.put("key","1");
                                    keydoc.set(users).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Intent intent = new Intent(GetUserRegistered.this, Profile.class);
                                            startActivity(intent);
                                        }
                                    });
                                }
                            }
                        });

                    }
                });

            }
        });
    }
}
