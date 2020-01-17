package com.example.mobilemechanics;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverSignUpActivity extends AppCompatActivity {
    private EditText mEmail, mPassword, mEnterPassword, mName, mAdress, mNumber;
    private Button mRegister,mReturnToLogin;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_sign_up);

        mAuth = FirebaseAuth.getInstance();

        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null){
                    Intent intent = new Intent(DriverSignUpActivity.this, DriverMapsActivity.class);
                    startActivity(intent);
                    finish();return;

                }
            }
        };

        mEmail = (EditText)findViewById(R.id.email);
        mName = (EditText)findViewById(R.id.name);
        mNumber = (EditText)findViewById(R.id.number);
        mAdress = (EditText)findViewById(R.id.address);
        mPassword=(EditText)findViewById(R.id.password);
        mEnterPassword =(EditText)findViewById(R.id.enterPassword);
        mRegister=(Button)findViewById(R.id.register);
        mReturnToLogin=(Button)findViewById(R.id.returnlogin);

        mReturnToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverSignUpActivity.this, DriverLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final  String email          =mEmail.getText().toString();
                final  String password       =mPassword.getText().toString();
                final  String name           =mName.getText().toString();
                final  String address        =mAdress.getText().toString();
                final  String number         =mNumber.getText().toString();
                final  String confirmPassword =mEnterPassword.getText().toString();
                if(TextUtils.isEmpty(email)){
                    Toast.makeText(DriverSignUpActivity.this, "enter your email",Toast.LENGTH_SHORT).show();

                }
                else if(TextUtils.isEmpty(password)){
                    Toast.makeText(DriverSignUpActivity.this, "enter a password",Toast.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(confirmPassword)){
                    Toast.makeText(DriverSignUpActivity.this, "confirm your password",Toast.LENGTH_SHORT).show();
                }
                else if(!password.equals(confirmPassword)){
                    Toast.makeText(DriverSignUpActivity.this, "please ensure the password match", Toast.LENGTH_SHORT).show();
                }else{


                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(DriverSignUpActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if(task.isSuccessful()){
                                Toast.makeText(DriverSignUpActivity.this, "you are authenticated successfully", Toast.LENGTH_SHORT).show();

                            }
                            else{
                                String message = task.getException().getMessage();
                                Toast.makeText(DriverSignUpActivity.this, "error occured" + message, Toast.LENGTH_SHORT).show();

                            }
                            User user = new User(
                                    name, address,number,email

                            );
                            //String user_id = mAuth.getCurrentUser().getUid();
                            FirebaseDatabase.getInstance().getReference().child("Users")
                                    .child("mechanics").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user);
                            //current_user_db.setValue(email);
                        }
                    });

                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }
}
