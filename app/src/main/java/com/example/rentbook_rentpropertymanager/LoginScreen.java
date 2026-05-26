package com.example.rentbook_rentpropertymanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginScreen extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "GoogleSignIn";
    private long backPressedTime = 0;
    private EditText etPhoneNumber, etOtpCode;
    private MaterialCardView btnSendOtp;
    private LinearLayout layoutOTPCode;
    private TextView tvBtnSendOtp;
    private ProgressBar pbSendOtp;
    private boolean is_send_otp = true;
    private String verificationId;  // To store OTP verification id
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_login_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialCardView btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // Phone Login Ids
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etOtpCode = findViewById(R.id.etOtpCode);
        pbSendOtp = findViewById(R.id.progressOtpLogin);
        btnSendOtp = findViewById(R.id.btnRequestOtp);
        tvBtnSendOtp = findViewById(R.id.tvBtnSendOtp);
        layoutOTPCode = findViewById(R.id.layoutOTPCode);

        mAuth = FirebaseAuth.getInstance();

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInWithGoogle();
            }
        });

        // Phone Login Button Clicks
        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (is_send_otp){
                    sendOTP();
                }else {
                    verifyOTP();
                }
            }
        });

    }

    private void signInWithGoogle(){

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void goToMainActivity() {
        Intent intent = new Intent(LoginScreen.this, MainActivity.class);
        startActivity(intent);
        finish(); // Finish LoginScreen so user can't press back to it
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in on start. If they are, go to MainActivity.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            String uid = user.getUid();

                            // Taking url of Google Signed-In user Profile Pic
                            String originalProfileUrl = Objects.requireNonNull(user.getPhotoUrl()).toString();

                            // Replace the size suffix (e.g., =s96-c) with your own
                            String profileUrl = originalProfileUrl.replaceAll("=s\\d+-c", "=s256-c");
                            String thumbProfileUrl = originalProfileUrl.replaceAll("=s\\d+-c", "=s200-c");  // 200x200 full image

                            Toast.makeText(LoginScreen.this, "Signed In As " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                            mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(uid);
                            HashMap<String, String> userMap = new HashMap<>();
                            userMap.put("name", user.getDisplayName());
                            userMap.put("email", user.getEmail());
                            userMap.put("role", "Owner");
                            userMap.put("profile_url", profileUrl);
                            userMap.put("thumb_profile_url", thumbProfileUrl);
                            userMap.put("created_at", String.valueOf(System.currentTimeMillis()));
                            userMap.put("is_verified", String.valueOf(user.isEmailVerified()));
                            if (user.getPhoneNumber() != null){
                                userMap.put("phone", user.getPhoneNumber());
                            }else {
                                userMap.put("phone", "null");
                            }

                            mDatabaseReference.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        goToMainActivity();
                                    }
                                }
                            });

                        } else {
                            Log.w(TAG, "signInWithCredential failed", task.getException());
                            Toast.makeText(LoginScreen.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Phone Login
    private void sendOTP() {


        String phone = etPhoneNumber.getText().toString().trim();

        if (phone.isEmpty() || phone.length() < 10) {
            etPhoneNumber.setError("Enter valid phone number");
            etPhoneNumber.requestFocus();
            return;
        }

        // UI updates first
        tvBtnSendOtp.setText("");
        pbSendOtp.bringToFront();
        pbSendOtp.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);

        // Add country code if not included
        if (!phone.startsWith("+91")) {
            phone = "+91" + phone;
        }

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();

        // 👇 Force UI to refresh before Firebase call
        pbSendOtp.post(() -> PhoneAuthProvider.verifyPhoneNumber(options));
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-retrieval or instant verification
                    String code = credential.getSmsCode();
                    if (code != null) {
                        etOtpCode.setText(code);
                        verifyCode(code);
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(LoginScreen.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    pbSendOtp.setVisibility(View.GONE);
                    tvBtnSendOtp.setText(R.string.text_btn_send_otp);
                    btnSendOtp.setEnabled(true);
                    is_send_otp = true;
                }

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    super.onCodeSent(s, token);
                    verificationId = s;
                    pbSendOtp.setVisibility(View.GONE);
                    tvBtnSendOtp.setText(R.string.text_btn_verify_otp);
                    btnSendOtp.setEnabled(true);
                    is_send_otp = false;
                    layoutOTPCode.setVisibility(View.VISIBLE);

                    Toast.makeText(LoginScreen.this, "OTP Sent!", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyOTP() {
        String code = etOtpCode.getText().toString().trim();
        if (code.isEmpty()) {
            etOtpCode.setError("Enter OTP");
            etOtpCode.requestFocus();
            return;
        }
        tvBtnSendOtp.setText("");
        pbSendOtp.bringToFront();
        pbSendOtp.setVisibility(View.VISIBLE);
        btnSendOtp.setEnabled(false);
        verifyCode(code);
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pbSendOtp.setVisibility(View.GONE);
                        FirebaseUser user = task.getResult().getUser();
                        assert user != null;
                        Toast.makeText(LoginScreen.this, "Login Success: " + user.getPhoneNumber(), Toast.LENGTH_LONG).show();

                        // Save user in Realtime Database if first login
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
                        String uid = user.getUid();

                        ref.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()){
                                    HashMap<String, Object> userMap = new HashMap<>();

                                    userMap.put("name", "User");
                                    userMap.put("email", "null");
                                    userMap.put("role", "Owner");
                                    userMap.put("profile_url", "default");
                                    userMap.put("thumb_profile_url", "default");
                                    userMap.put("created_at", String.valueOf(System.currentTimeMillis()));
                                    userMap.put("is_verified", String.valueOf(user.isEmailVerified()));
                                    userMap.put("phone", user.getPhoneNumber());

                                    ref.child(uid).updateChildren(userMap);

                                    // Move to Home Screen
                                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                                    finish();
                                }else {
                                    // Move to Home Screen
                                    startActivity(new Intent(LoginScreen.this, MainActivity.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });



                    } else {
                        Toast.makeText(LoginScreen.this, "Login Failed", Toast.LENGTH_SHORT).show();
                        btnSendOtp.setEnabled(true);
                    }
                });
    }

    @Override
    public void onBackPressed(){
        if (SystemClock.elapsedRealtime() - backPressedTime < 2000){
            super.onBackPressed();
            return;
        }else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = SystemClock.elapsedRealtime();
    }

}