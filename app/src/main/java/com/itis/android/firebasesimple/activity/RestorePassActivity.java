package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.itis.android.firebasesimple.R;

public class RestorePassActivity extends AppCompatActivity {

    private EditText resetPassEditText;
    private Button resetPassButton;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_pass);

        resetPassEditText = findViewById(R.id.edt_reset_pass);
        resetPassButton = findViewById(R.id.btn_reset_pass);
        firebaseAuth = FirebaseAuth.getInstance();

        resetPassButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {

                String email = resetPassEditText.getText().toString().trim();

                if(TextUtils.isEmpty(email)){
                    resetPassEditText.setError("Please enter your email");
                }
                else {
                    firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                        if(task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.reset_password_complete_msg), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RestorePassActivity.this, SignInActivity.class));
                            finish();
                        }
                        else {
                            Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }
}
