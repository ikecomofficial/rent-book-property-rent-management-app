package com.example.rentbook_rentpropertymanager.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.rentbook_rentpropertymanager.LoginScreen;
import com.example.rentbook_rentpropertymanager.MainActivity;
import com.example.rentbook_rentpropertymanager.R;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountFragment extends Fragment {

    // 👤 User Info
    private String user_id, profile_url, thumb_profile_url;

    // 📄 User UI Views
    private TextView tvUserName, tvUserPhoneNumber, tvUserEmail, tvUserRole;
    private CircleImageView cimgUserProfile;

    // ✏️ Edit User
    private TextInputEditText etUpdatedUserName;
    private LinearLayout layoutEditName;
    private ImageView imgUserNameEdit;

    // 📂 Upload UI
    private AlertDialog uploadDialog;
    private ProgressBar profileUploadBar;
    private TextView tvUploadPercentage, tvUploadDialogSubHeading;

    // 📊 User Info Cards
    private MaterialCardView layoutUserMobile, layoutUserEmail;

    // 🔐 Authentication
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    // 🔗 Firebase References
    private DatabaseReference userReference;

    // ☁️ Storage Reference
    private StorageReference userProfileRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);


        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhoneNumber = view.findViewById(R.id.tvUserPhoneNumber);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        cimgUserProfile = view.findViewById(R.id.cimgUserProfile);
        MaterialCardView btnUserProfileEdit = view.findViewById(R.id.btnUserProfileEdit);
        MaterialCardView layoutUserSignOut = view.findViewById(R.id.layoutUserSignOut);

        layoutUserMobile = view.findViewById(R.id.layoutUserMobile);
        layoutUserEmail = view.findViewById(R.id.layoutUserEmail);

        // Edit Name Views
        layoutEditName = view.findViewById(R.id.layoutEditName);
        MaterialCardView btnCancelEditName = view.findViewById(R.id.btnCancelEditName);
        MaterialCardView btnUpdateUserName = view.findViewById(R.id.btnUpdateUserName);
        etUpdatedUserName = view.findViewById(R.id.etUpdatedUserName);
        imgUserNameEdit = view.findViewById(R.id.imgUserNameEdit);

        userProfileRef = FirebaseStorage.getInstance().getReference().child("user_prof_images");

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();


        if (user != null){
            String providerId = user.getProviderId();
            if (providerId.equals("google.com")){

                user_id = user.getUid();

                // Configure Google Sign In to get the client for sign-out
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .setAccountName(null)
                        .build();
                mGoogleSignInClient = GoogleSignIn.getClient(getContext(), gso);

                tvUserName.setText(user.getDisplayName());
                tvUserEmail.setText(user.getEmail());

                // Taking url of Google Signed In user Profile Pic
                String originalProfileUrl = Objects.requireNonNull(user.getPhotoUrl()).toString();

                // Replace the size suffix (e.g., =s96-c) with your own
                String thumbProfileUrl = originalProfileUrl.replaceAll("=s\\d+-c", "=s200-c");
                Glide.with(this)
                        .load(thumbProfileUrl)
                        .placeholder(R.drawable.ic_tenant_profile_default)
                        .into(cimgUserProfile);
                layoutUserSignOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        googleSignOut();
                    }
                });
            } else {
                user_id = user.getUid();
                userReference = FirebaseDatabase.getInstance().getReference().child("users").child(user_id);
                userReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String user_name = snapshot.child("name").getValue(String.class);
                        String user_email = snapshot.child("email").getValue(String.class);
                        String user_role = snapshot.child("role").getValue(String.class);
                        profile_url = snapshot.child("profile_url").getValue(String.class);
                        thumb_profile_url = snapshot.child("thumb_profile_url").getValue(String.class);

                        tvUserName.setText(user_name);
                        tvUserRole.setText(user_role);
                        if (Objects.equals(user_email, "null")){
                            tvUserPhoneNumber.setText(user.getPhoneNumber());
                            layoutUserEmail.setVisibility(View.GONE);
                            layoutUserMobile.setVisibility(View.VISIBLE);

                        }else {
                            tvUserEmail.setText(user.getEmail());
                            layoutUserEmail.setVisibility(View.VISIBLE);
                            layoutUserMobile.setVisibility(View.GONE);
                        }

                        if (thumb_profile_url == null || thumb_profile_url.trim().isEmpty() || thumb_profile_url.equals("default")) {
                            // Show only placeholder
                            Glide.with(requireContext())
                                    .load(R.drawable.ic_tenant_profile_default)
                                    .into(cimgUserProfile);
                        } else {
                            Glide.with(requireContext())
                                    .load(thumb_profile_url)
                                    .placeholder(R.drawable.ic_tenant_profile_default)
                                    .into(cimgUserProfile);
                        }

                        assert user_name != null;
                        if (user_name.equals("User")){
                            imgUserNameEdit.setVisibility(View.VISIBLE);
                        }else {
                            imgUserNameEdit.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                layoutUserSignOut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        emailSignOut();
                    }
                });
            }
        }

        imgUserNameEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutEditName.setVisibility(View.VISIBLE);

            }
        });

        cimgUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create dialog
                Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                // Create PhotoView dynamically
                PhotoView photoView = new PhotoView(v.getContext());
                photoView.setBackgroundColor(Color.BLACK);
                photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                // Set PhotoView as dialog content
                dialog.setContentView(photoView);

                if (profile_url.equals("default")){
                    // Load image using Glide
                    Glide.with(v.getContext())
                            .load(R.drawable.ic_tenant_profile_default) // your Firebase image URL
                            .into(photoView);
                }else {
                    // Load image using Glide
                    Glide.with(v.getContext())
                            .load(profile_url) // your Firebase image URL
                            .placeholder(R.drawable.ic_tenant_profile_default)
                            .into(photoView);
                }


                // Close dialog on tap
                photoView.setOnClickListener(view -> dialog.dismiss());

                dialog.show();
            }
        });

        btnUserProfileEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        btnCancelEditName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutEditName.setVisibility(View.GONE);
            }
        });

        btnUpdateUserName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String new_name = Objects.requireNonNull(etUpdatedUserName.getText()).toString().trim();
                if (new_name.isEmpty()){
                    etUpdatedUserName.setError("Enter Name");
                    return;
                }
                userReference.child("name").setValue(new_name)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                            layoutEditName.setVisibility(View.GONE);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            }
        });


        return view;
    }

    private void openGallery() {
        //checkAndRequestPermissions();

        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ Photo Picker
            intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
        } else {
            // Older Android Gallery Picker
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        pickImageLauncher.launch(intent);

    }

    // ✅ Crop Image launcher (new API)
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    if (croppedUri != null) {
                        cimgUserProfile.setImageURI(croppedUri);
                        uploadImage(croppedUri);
                    }
                } else {
                    Exception error = result.getError();
                    Toast.makeText(getContext(), "Crop failed: " + (error != null ? error.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            });

    // ✅ Pick image launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // Launch cropper
                        CropImageOptions cropOptions = new CropImageOptions();

                        cropOptions.aspectRatioX = 1;
                        cropOptions.aspectRatioY = 1;

                        cropOptions.fixAspectRatio = true;
                        cropOptions.guidelines = CropImageView.Guidelines.ON;


                        CropImageContractOptions options = new CropImageContractOptions(imageUri, cropOptions);
                        cropImageLauncher.launch(options);
                    }
                }
            });


    public static String randomImageName() {
        return UUID.randomUUID().toString();
    }
    private void uploadImage(Uri imageUri) {
        showUploadDialog();

        String randomImageName = randomImageName();
        StorageReference fileRef;
        fileRef = userProfileRef.child(user_id).child("user_" + randomImageName + ".jpg");

        fileRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    long bytesTransferred = snapshot.getBytesTransferred();
                    long totalBytes = snapshot.getTotalByteCount();
                    //int progress = (int) ((bytesTransferred * 100) / totalBytes);
                    updateUploadProgress(bytesTransferred, totalBytes);
                })
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String fullUrl = uri.toString();

                            createThumbnailAndUpload(imageUri, fullUrl, randomImageName);
                        }))
                .addOnFailureListener(e -> {
                    //progressDialog.dismiss();
                    dismissUploadDialog();
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createThumbnailAndUpload(Uri imageUri, String fullUrl, String randomImageName) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

            // Create thumbnail (resize + compress)
            int maxSize = 400;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = (float) maxSize / Math.max(width, height);
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] thumbData = byteArrayOutputStream.toByteArray();

            StorageReference thumbRef;
            thumbRef = userProfileRef.child(user_id).child("thumb_user_"+ randomImageName + ".jpg");
            UploadTask uploadTask = thumbRef.putBytes(thumbData);

            uploadTask
                    .addOnProgressListener(snapshot -> {
                        tvUploadDialogSubHeading.setText(R.string.text_sub_finalizing_upload);  // Finalizing Your Upload...
                        long bytesTransferred = snapshot.getBytesTransferred();
                        long totalBytes = snapshot.getTotalByteCount();
                        //int progress = (int) ((bytesTransferred * 100) / totalBytes);
                        updateUploadProgress(bytesTransferred, totalBytes);
                    })
                    .addOnSuccessListener(taskSnapshot ->

                            thumbRef.getDownloadUrl().addOnSuccessListener(uri -> {

                                //progressDialog.dismiss();
                                dismissUploadDialog();
                                String thumbUrl = uri.toString();
                                saveUrlsToDatabase(fullUrl, thumbUrl);
                            }))
                    .addOnFailureListener(e -> {
                        dismissUploadDialog();
                        Toast.makeText(getContext(), "Thumbnail upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            dismissUploadDialog();
            Log.e("UploadError", "Error uploading profile image", e);
        }
    }

    private void saveUrlsToDatabase(String fullUrl, String thumbUrl) {

            HashMap<String, Object> userProfileUrlMap = new HashMap<>();
        userProfileUrlMap.put("profile_url", fullUrl);
        userProfileUrlMap.put("thumb_profile_url", thumbUrl);

            userReference.updateChildren(userProfileUrlMap)
                    .addOnSuccessListener(aVoid -> {
                        deleteOldProfileFromFirebase();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());


    }

    private void deleteOldProfileFromFirebase(){
        StorageReference oldFullProfileRef = FirebaseStorage.getInstance().getReferenceFromUrl(profile_url);
        StorageReference oldThumbProfileRef = FirebaseStorage.getInstance().getReferenceFromUrl(thumb_profile_url);

        if (profile_url != null && !profile_url.isEmpty() && !profile_url.equals("default")){
            oldFullProfileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase Database:", "Full Image Deleted");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        }
        if (thumb_profile_url != null && !thumb_profile_url.isEmpty() && !thumb_profile_url.equals("default")){
            oldThumbProfileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase Database:", "Full Image Deleted");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_upload_progress, null);
        builder.setView(view);
        builder.setCancelable(false);

        profileUploadBar = view.findViewById(R.id.profileUploadBar);
        tvUploadPercentage = view.findViewById(R.id.tvUploadPercentage);
        tvUploadDialogSubHeading = view.findViewById(R.id.tvUploadDialogSubHeading);

        uploadDialog = builder.create();
        Objects.requireNonNull(uploadDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        uploadDialog.show();
    }

    private void updateUploadProgress(long bytesTransferred, long totalBytes) {
        int progress = (int) ((bytesTransferred * 100) / totalBytes);
        profileUploadBar.setProgress(progress);
        String progPercent = progress + "%";
        tvUploadPercentage.setText(progPercent);
    }

    private void dismissUploadDialog() {
        if (uploadDialog != null && uploadDialog.isShowing()) {
            uploadDialog.dismiss();
        }
    }


    private void googleSignOut(){
        // First, sign out from Firebase

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sign Out?");
        builder.setMessage("Want to Sign Out from This Account?");

        // Positive button -> Yes
        builder.setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform sign out here
                mAuth.signOut();

                // Next, sign out from Google
                mGoogleSignInClient.revokeAccess().addOnCompleteListener(requireActivity(), task -> {
                    Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                    goToLoginScreen(); // Redirect to the login screen after successful sign out
                });
            }
        });

        // Negative button -> No
        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, just dismiss
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void emailSignOut(){
        // First, sign out from Firebase

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sign Out?");
        builder.setMessage("Want to Sign Out from This Account?");

        // Positive button -> Yes
        builder.setPositiveButton("Sign Out", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mAuth.signOut();
                // Redirect to login screen
                Intent intent = new Intent(getContext(), LoginScreen.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                goToLoginScreen();
                requireActivity().finish();
            }
        });

        // Negative button -> No
        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, just dismiss
            }
        });

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void goToLoginScreen() {
        Intent intent = new Intent(getContext(), LoginScreen.class);
        startActivity(intent);
        requireActivity().finish(); // End MainActivity so the user can't press back to it
    }

}

