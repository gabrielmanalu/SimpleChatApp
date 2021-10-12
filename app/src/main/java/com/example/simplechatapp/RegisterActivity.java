package com.example.simplechatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.simplechatapp.Common.Common;
import com.example.simplechatapp.Model.UserModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegisterActivity extends AppCompatActivity {

    @BindView(R.id.edit_first_name)
    TextInputEditText edit_first_name;
    @BindView(R.id.edit_last_name)
    TextInputEditText edit_last_name;
    @BindView(R.id.edit_phone_number)
    TextInputEditText edit_phone_number;
    @BindView(R.id.edt_date_of_birth)
    TextInputEditText edt_date_of_birth;
    @BindView(R.id.edit_bio)
    TextInputEditText edit_bio;
    @BindView(R.id.btn_register)
    Button btn_register;

    FirebaseDatabase mDatabase;
    DatabaseReference userRef;

    MaterialDatePicker<Long> mMaterialDatePicker = MaterialDatePicker.Builder.datePicker().build();
    SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("dd-mm-yyyy");
    Calendar mCalendar = Calendar.getInstance();
    boolean isSelectBirthdate = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        init();
        setDefaultDate();
    }

    private void setDefaultDate() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        edit_phone_number.setText(user.getPhoneNumber());
        edit_phone_number.setEnabled(false);

        edt_date_of_birth.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                mMaterialDatePicker.show(getSupportFragmentManager(), mMaterialDatePicker.toString());
            }
        });

        btn_register.setOnClickListener(v -> {
            if(!isSelectBirthdate){
                Toast.makeText(this, "Please enter Birth of Date", Toast.LENGTH_SHORT).show();
                return;
            }

            UserModel userModel = new UserModel();
            //Personal
            userModel.setFirstName(edit_first_name.getText().toString());
            userModel.setLastName(edit_last_name.getText().toString());
            userModel.setBio(edit_bio.getText().toString());
            userModel.setPhone(edit_phone_number.getText().toString());
            userModel.setBirthDate(mCalendar.getTimeInMillis());
            userModel.setUid(FirebaseAuth.getInstance().getCurrentUser().getUid());

            userRef.child(userModel.getUid())
                    .setValue(userModel)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Register Success!", Toast.LENGTH_SHORT).show();
                        Common.currentUser = userModel;
                        startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                        finish();
                    });
        });

    }

    private void init(){
        ButterKnife.bind(this);
        mDatabase = FirebaseDatabase.getInstance();
        userRef = mDatabase.getReference(Common.USER_REFERENCES);
        mMaterialDatePicker.addOnPositiveButtonClickListener(selection -> {
            mCalendar.setTimeInMillis(selection);
            edt_date_of_birth.setText(mSimpleDateFormat.format(selection));
            isSelectBirthdate = true;
        });
    }

}