package com.example.firebase;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;

public class EnterPhone extends AppCompatActivity {

    private Spinner spinner;
    private EditText mPhoneNumberField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_phone);

        spinner = findViewById(R.id.spinnerCountries);
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CountryData.countryNames));
        mPhoneNumberField = findViewById(R.id.editTextPhone);

        findViewById(R.id.buttonContinue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = CountryData.countryAreaCodes[spinner.getSelectedItemPosition()];
                String number = mPhoneNumberField.getText().toString().trim();

                if (number.isEmpty() || number.length() < 10) {
                    mPhoneNumberField.setError("Valid number is required");
                    mPhoneNumberField.requestFocus();
                    return;
                }
                String phoneNumber = "+" + code + number;
                Intent intent = new Intent(EnterPhone.this, PhoneAuthActivity.class);
                intent.putExtra("phonenumber", phoneNumber);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(this, Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }
    // [END on_start_check_user]

  /*  private boolean validatePhoneNumber(String phonenum) {
         if (phonenum.isEmpty() || phonenum.length() < 10) {
                mPhoneNumberField.setError("Valid number is required");
                mPhoneNumberField.requestFocus();
                return false;
            }
         return true;
        }   */
}


