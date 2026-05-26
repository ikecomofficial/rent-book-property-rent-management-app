package com.example.rentbook_rentpropertymanager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class TenantDetailsActivity extends AppCompatActivity {

    // 🏠 IDs
    private String user_id, room_id, tenant_id;

    // 👤 Tenant Info
    private String tenant_phone, tenant_profile_url, thumb_tenant_url;

    // 📄 Tenant UI Views
    private TextView tvTenantName, tvTenantPhone, tvTenantAddress, tvTenantStartDate, tvTenantEndDate, tvTenantPropRoom, tvBillingStartDay;
    private CircleImageView cimgTenantProfile;

    // 📊 Billing Info
    private Integer billing_start_day;

    // 📂 Documents (Recycler + Data)
    private RecyclerView rvTenantDocument;
    private FirebaseRecyclerAdapter<Documents, DocumentsViewHolder> documentAdapter;

    // ⬆️ Upload UI
    private AlertDialog uploadDialog;
    private ProgressBar profileUploadBar;
    private TextView tvUploadPercentage, tvUploadDialogSubHeading;
    private boolean is_add_doc = false;

    // 🔗 Firebase References
    private DatabaseReference roomReference, tenantReference, propertyReference, documentReference;

    // ☁️ Storage References
    private StorageReference profileStorageRef, tenantDocumentRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tenant_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.text_toolbar_tenant_details);
        }


        tenant_id = getIntent().getStringExtra("tenant_id");
        room_id = getIntent().getStringExtra("room_id");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        user_id = user.getUid();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        assert tenant_id != null;
        assert room_id != null;
        tenantReference = databaseReference.child("tenants").child(room_id).child(tenant_id);
        documentReference = databaseReference.child("tenant_docs").child(room_id).child(tenant_id);
        roomReference = databaseReference.child("rooms").child(room_id);
        propertyReference = databaseReference.child("properties");

        profileStorageRef = FirebaseStorage.getInstance().getReference().child("tenant_prof_images");
        tenantDocumentRef = FirebaseStorage.getInstance().getReference().child("tenant_doc_images");

        // 📄 Tenant UI Views
        cimgTenantProfile = findViewById(R.id.cimgTenantProfile);
        tvTenantName = findViewById(R.id.tvTenantName);
        tvTenantPhone = findViewById(R.id.tvTenantPhone);
        tvTenantAddress = findViewById(R.id.tvTenantAddress);
        tvTenantStartDate = findViewById(R.id.tvTenantStartDate);
        tvTenantEndDate = findViewById(R.id.tvTenantEndDate);
        tvTenantPropRoom = findViewById(R.id.tvTenantPropRoom);
        tvBillingStartDay = findViewById(R.id.tvBillingStartDay);
        ImageView imgEditStartDay = findViewById(R.id.imgEditStartDay);

        // 🎯 Actions
        MaterialCardView btnChangeTenantProfile = findViewById(R.id.btnChangeTenantProfile);
        LinearLayout btnContactTenant = findViewById(R.id.btnContactTenant);
        TextView btnAddTenantDoc = findViewById(R.id.btnAddTenantDoc);

        // 📂 Tenant Documents UI
        ImageView imgTenantDocHeader = findViewById(R.id.imgTenantDocHeader);
        LinearLayout layoutExpandableHeader = findViewById(R.id.layoutExpandableHeader);
        LinearLayout layoutTenantDocCollapse = findViewById(R.id.layoutTenantDocCollapse);
        View dividerExpandDoc = findViewById(R.id.dividerExpandDoc);
        ImageView imgExpandToggle = findViewById(R.id.imgExpandToggle);

        // Get the background drawable
        Drawable background = imgTenantDocHeader.getBackground();

        // Ensure it's a GradientDrawable (since <shape> creates this type)
        if (background instanceof GradientDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(ContextCompat.getColor(this, R.color.bg_btn_call_blue_faded)); // Example orange color
        }

        rvTenantDocument = findViewById(R.id.rvTenantDocument);


        loadTenantData();
        loadTenantPropertyRoom();
        loadTenantDocuments();

        layoutExpandableHeader.setOnClickListener(v -> {
            if (layoutTenantDocCollapse.getVisibility() == View.VISIBLE) {
                // Collapse
                layoutTenantDocCollapse.setVisibility(View.GONE);
                dividerExpandDoc.setVisibility(View.GONE);
                imgExpandToggle.animate().rotation(0).setDuration(200).start();
            } else {
                // Expand
                layoutTenantDocCollapse.setVisibility(View.VISIBLE);
                dividerExpandDoc.setVisibility(View.VISIBLE);
                imgExpandToggle.animate().rotation(90).setDuration(200).start();
            }
        });

        btnContactTenant.setOnClickListener(view -> showContactBottomSheet());

        btnChangeTenantProfile.setOnClickListener(view -> {
            is_add_doc = false;
            openGallery();
        });

        btnAddTenantDoc.setOnClickListener(view -> {
            is_add_doc = true;
            openGallery();
        });

        cimgTenantProfile.setOnClickListener(v -> {
            // Create dialog
            Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            // Create PhotoView dynamically
            PhotoView photoView = new PhotoView(v.getContext());
            photoView.setBackgroundColor(Color.BLACK);
            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Set PhotoView as dialog content
            dialog.setContentView(photoView);

            if (tenant_profile_url.equals("default")){
                // Load image using Glide
                Glide.with(v.getContext())
                        .load(R.drawable.ic_tenant_profile_default) // your Firebase image URL
                        .into(photoView);
            }else {
                // Load image using Glide
                Glide.with(v.getContext())
                        .load(tenant_profile_url) // your Firebase image URL
                        .placeholder(R.drawable.ic_tenant_profile_default)
                        .into(photoView);
            }


            // Close dialog on tap
            photoView.setOnClickListener(view -> dialog.dismiss());

            dialog.show();
        });

        imgEditStartDay.setOnClickListener(v -> billingStartDaySelector());

    }

    private void billingStartDaySelector(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Container Layout
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(60, 40, 60, 20);
        container.setGravity(Gravity.CENTER);

        // Title
        TextView title = new TextView(this);
        title.setText(R.string.text_sel_billing_start_day);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        // NumberPicker
        NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(31);
        numberPicker.setWrapSelectorWheel(true);
        numberPicker.setValue(billing_start_day);

        // 🔥 Remove divider lines (modern look)
        try {
            Field[] pickerFields = NumberPicker.class.getDeclaredFields();
            for (Field field : pickerFields) {
                if (field.getName().equals("mSelectionDivider")) {
                    field.setAccessible(true);
                    field.set(numberPicker, null);
                    break;
                }
            }
        } catch (Exception ignored) {}

        // Make picker compact width
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(300,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        numberPicker.setLayoutParams(params);

        container.addView(title);
        container.addView(numberPicker);

        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {

            int selectedDay = numberPicker.getValue();

            String selectedBillingDay = addOrdinalSuffix(selectedDay) + " of each month";
            tvBillingStartDay.setText(selectedBillingDay);

            billing_start_day = selectedDay;
            updateBillingStartDayToFirebase(selectedDay);
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void updateBillingStartDayToFirebase(int billingDay){
        tenantReference.child("billing_start_day").setValue(billingDay)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Billing day updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }

    private void openGallery() {

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
                        if (!is_add_doc){
                            cimgTenantProfile.setImageURI(croppedUri);
                        }
                        uploadImage(croppedUri);
                    }
                } else {
                    Exception error = result.getError();
                    Toast.makeText(this, "Crop failed: " + (error != null ? error.getMessage() : ""), Toast.LENGTH_SHORT).show();
                }
            });

    // ✅ Pick image launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // Launch cropper
                        CropImageOptions cropOptions = new CropImageOptions();
                        // Crop Aspect Ratio Based on Btn
                        if (is_add_doc){
                            cropOptions.fixAspectRatio = false;
                        } else {
                            cropOptions.aspectRatioX = 1;
                            cropOptions.aspectRatioY = 1;
                            cropOptions.fixAspectRatio = true;
                        }
                        cropOptions.guidelines = CropImageView.Guidelines.ON;


                        CropImageContractOptions options = new CropImageContractOptions(imageUri, cropOptions);
                        cropImageLauncher.launch(options);
                    }
                }
            });


    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
    private void uploadImage(Uri imageUri) {
        showUploadDialog();

        String randomImageName = randomImageName();
        StorageReference fileRef;
        if (!is_add_doc){
            fileRef = profileStorageRef.child(user_id).child(room_id).child(tenant_id).child("profile_" + randomImageName + ".jpg");
        }else {
            fileRef = tenantDocumentRef.child(user_id).child(room_id).child(tenant_id).child("doc_" + randomImageName + ".jpg");

        }

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
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static String randomImageName() {
        return UUID.randomUUID().toString();
    }

    private void createThumbnailAndUpload(Uri imageUri, String fullUrl, String randomImageName) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

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
            if (!is_add_doc){
                thumbRef = profileStorageRef.child(user_id).child(room_id).child(tenant_id).child("thumb_"+ randomImageName + ".jpg");
            }else {
                thumbRef = tenantDocumentRef.child(user_id).child(room_id).child(tenant_id).child("thumb_doc_"+ randomImageName + ".jpg");
            }
            UploadTask uploadTask = thumbRef.putBytes(thumbData);

            uploadTask
                    .addOnProgressListener(snapshot -> {
                        tvUploadDialogSubHeading.setText(R.string.text_sub_finalizing_upload);  // Optimizing your picture...
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
                                saveUrlsToDatabase(fullUrl, thumbUrl, randomImageName);
                            }))
                    .addOnFailureListener(e -> {
                        //progressDialog.dismiss();
                        dismissUploadDialog();
                        Toast.makeText(this, "Thumbnail upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            dismissUploadDialog();
            Log.e("UploadError", "Error while uploading tenant data", e);
        }
    }

    private void saveUrlsToDatabase(String fullUrl, String thumbUrl, String randomImageName) {

        if (!is_add_doc){
            HashMap<String, Object> tenantProfileUrlMap = new HashMap<>();
            tenantProfileUrlMap.put("tenant_profile_url", fullUrl);
            tenantProfileUrlMap.put("thumb_tenant_url", thumbUrl);

            tenantReference.updateChildren(tenantProfileUrlMap)
                    .addOnSuccessListener(aVoid -> {
                        deleteOldProfileFromFirebase();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } else {
            long timestamp = System.currentTimeMillis();
            String docTimestamp = String.valueOf(timestamp);
            String document_name = "doc_" + randomImageName + ".jpg";

            String doc_id = documentReference.push().getKey();
            HashMap<String, Object> tenantDocumentMap = new HashMap<>();
            tenantDocumentMap.put("doc_name", document_name);
            tenantDocumentMap.put("tenant_doc_url", fullUrl);
            tenantDocumentMap.put("thumb_doc_url", thumbUrl);
            tenantDocumentMap.put("doc_note", "null");
            tenantDocumentMap.put("doc_timestamp", docTimestamp);

            if (doc_id != null){
                documentReference.child(doc_id).setValue(tenantDocumentMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Document Saved..!!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }

    }

    private void deleteOldProfileFromFirebase() {

        // 🔹 Full image
        if (tenant_profile_url != null
                && !tenant_profile_url.isEmpty()
                && !tenant_profile_url.equals("default")) {

            StorageReference oldFullProfileRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(tenant_profile_url);

            oldFullProfileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Full Image Deleted");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        // 🔹 Thumbnail
        if (thumb_tenant_url != null
                && !thumb_tenant_url.isEmpty()
                && !thumb_tenant_url.equals("default")) {

            StorageReference oldThumbProfileRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(thumb_tenant_url);

            oldThumbProfileRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Thumb Image Deleted");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void loadTenantData(){
        tenantReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String tenant_name = snapshot.child("tenant_name").getValue(String.class);
                tenant_phone = snapshot.child("tenant_phone").getValue(String.class);
                String tenant_address = snapshot.child("tenant_address").getValue(String.class);
                thumb_tenant_url = snapshot.child("thumb_tenant_url").getValue(String.class);
                tenant_profile_url = snapshot.child("tenant_profile_url").getValue(String.class);
                String tenant_start_date = snapshot.child("tenant_start_date").getValue(String.class);
                String tenant_end_date = snapshot.child("tenant_end_date").getValue(String.class);

                // Set Tenant Billing Start Day
                billing_start_day = snapshot.child("billing_start_day").getValue(Integer.class);
                String finalBillingStartDay = addOrdinalSuffix(billing_start_day) + " of each month";
                tvBillingStartDay.setText(finalBillingStartDay);

                tvTenantName.setText(tenant_name);
                tvTenantPhone.setText(tenant_phone);
                tvTenantAddress.setText(tenant_address);
                tvTenantStartDate.setText(tenant_start_date);
                assert tenant_end_date != null;
                if (tenant_end_date.equals("null")){
                    tvTenantEndDate.setTextColor(ContextCompat.getColor(TenantDetailsActivity.this, R.color.text_amount));
                    tvTenantEndDate.setTypeface(null, Typeface.BOLD);
                    tvTenantEndDate.setText(R.string.text_active);
                }else {
                    tvTenantEndDate.setText(tenant_end_date);
                }

                if (thumb_tenant_url == null || thumb_tenant_url.trim().isEmpty() || thumb_tenant_url.equals("default")) {
                    // Show only placeholder
                    Glide.with(TenantDetailsActivity.this)
                            .load(R.drawable.ic_tenant_profile_default)
                            .into(cimgTenantProfile);
                } else {
                    Glide.with(TenantDetailsActivity.this)
                            .load(thumb_tenant_url)
                            .placeholder(R.drawable.ic_tenant_profile_default)
                            .into(cimgTenantProfile);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String addOrdinalSuffix(Integer number) {

        if (number >= 11 && number <= 13) {
            return number + "th";
        }

        switch (number % 10) {
            case 1: return number + "st";
            case 2: return number + "nd";
            case 3: return number + "rd";
            default: return number + "th";
        }
    }

    private void loadTenantPropertyRoom(){
        roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String room_name = snapshot.child("room_name").getValue(String.class);
                String pid = snapshot.child("property_id").getValue(String.class);

                assert pid != null;
                propertyReference.child(pid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String property_name = snapshot.child("property_name").getValue(String.class);
                        String propertyRoomName = property_name + ", " + room_name;
                        tvTenantPropRoom.setText(propertyRoomName);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showContactBottomSheet(){

        // Create BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Inflate layout for bottom sheet
        @SuppressLint("InflateParams") View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_contact_tenant, null, false);
        bottomSheetDialog.setContentView(view);

        // Make sure we modify the bottom-sheet container after it is shown
        bottomSheetDialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                // clear default background so your drawable shows through
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                bottomSheet.setClipToPadding(false);
            }
        });

        // Find the option buttons inside the sheet
        LinearLayout btnCall = view.findViewById(R.id.btnCall);
        LinearLayout btnWhatsapp = view.findViewById(R.id.btnWhatsapp);
        LinearLayout btnCopy = view.findViewById(R.id.btnCopy);

        // Set click listeners
        btnCall.setOnClickListener(v -> {
            // handle call action
            String cleanedPhone = cleanTenantPhone(tenant_phone);  // Clean the phone number with 10 Digit Number only.
            if (cleanedPhone != null){
                bottomSheetDialog.dismiss();
                // Create an Intent to open the dialer
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + cleanedPhone));

                // Start the dialer
                startActivity(callIntent);
            }
        });

        btnWhatsapp.setOnClickListener(v -> {

            String cleanedPhone = cleanTenantPhone(tenant_phone);

            // Add country code (India example)
            String phoneWithCountry = "+91" + cleanedPhone;

            // Create WhatsApp chat link
            String url = "https://wa.me/" + phoneWithCountry;

            if (cleanedPhone != null){
                bottomSheetDialog.dismiss();
                Intent intentWhatsApp = new Intent(Intent.ACTION_VIEW);
                intentWhatsApp.setData(Uri.parse(url));
                intentWhatsApp.setPackage("com.whatsapp");
                startActivity(intentWhatsApp);
            }

        });

        btnCopy.setOnClickListener(v -> {
            // handle copy action
            bottomSheetDialog.dismiss();

            String cleanedPhone = cleanTenantPhone(tenant_phone);
            // Get clipboard manager
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            // Create a ClipData object with the text
            ClipData clip = ClipData.newPlainText("Phone Number", cleanedPhone);

            // Set the clip to clipboard
            clipboard.setPrimaryClip(clip);
        });

        // Set the content and show
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();

    }

    private void loadTenantDocuments(){

        // Create options (no query needed)
        FirebaseRecyclerOptions<Documents> options =
                new FirebaseRecyclerOptions.Builder<Documents>()
                        .setQuery(documentReference, Documents.class)
                        .build();
        // Create adapter
        FirebaseRecyclerAdapter<Documents, DocumentsViewHolder> adapter =
                new FirebaseRecyclerAdapter<>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull DocumentsViewHolder holder,
                                                    int position,
                                                    @NonNull Documents model) {

                        holder.setDocumentName(model.getDoc_name());
                        holder.setThumbDocUrl(model.getThumb_doc_url());

                        //String doc_id = getRef(position).getKey();

                        holder.itemView.setOnClickListener(v -> {
                            // Create dialog
                            Dialog dialog = new Dialog(v.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

                            // Create PhotoView dynamically
                            PhotoView photoView = new PhotoView(v.getContext());
                            photoView.setBackgroundColor(Color.BLACK);
                            photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                            // Set PhotoView as dialog content
                            dialog.setContentView(photoView);

                            // Load image using Glide
                            Glide.with(v.getContext())
                                    .load(model.getTenant_doc_url()) // your Firebase image URL
                                    .placeholder(R.drawable.img_doc_placeholder)
                                    .into(photoView);

                            // Close dialog on tap
                            photoView.setOnClickListener(view -> dialog.dismiss());

                            dialog.show();
                        });
                    }

                    @NonNull
                    @Override
                    public DocumentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.single_tenant_document, parent, false);
                        return new DocumentsViewHolder(view);
                    }
                };

// Set layout manager (Horizontal scroll)
        rvTenantDocument.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvTenantDocument.setAdapter(adapter);

        adapter.startListening();
    }

    // ✅ Model class
    public static class Documents {
        private String doc_name, tenant_doc_url, thumb_doc_url;

        public Documents() {} // required for Firebase

        public String getDoc_name() {
            return doc_name;
        }

        public void setDoc_name(String doc_name) {
            this.doc_name = doc_name;
        }

        public String getTenant_doc_url() {
            return tenant_doc_url;
        }

        public void setTenant_doc_url(String tenant_doc_url) {
            this.tenant_doc_url = tenant_doc_url;
        }

        public String getThumb_doc_url() {
            return thumb_doc_url;
        }

        public void setThumb_doc_url(String thumb_doc_url) {
            this.thumb_doc_url = thumb_doc_url;
        }
    }

    public static class DocumentsViewHolder extends RecyclerView.ViewHolder{

        View mView;
        public DocumentsViewHolder(View itemView){
            super(itemView);
            mView = itemView;
        }

        public void setDocumentName(String documentName){
            TextView documentNameView = mView.findViewById(R.id.tvTenantDocName);

            int dotIndex = documentName.lastIndexOf('.');
            String extension = "";
            String baseName = documentName;

            if (dotIndex != -1 && dotIndex < documentName.length() - 1) {
                extension = documentName.substring(dotIndex);
                baseName = documentName.substring(0, dotIndex);
            }

            if (baseName.length() > 11) { // shorter = cleaner UI
                baseName = baseName.substring(0, 8) + "..";
            }
            String shortDocName = baseName + extension;

            documentNameView.setText(shortDocName);
        }

        public void setThumbDocUrl(String thumbDocUrl){
            ImageView thumbDocUrlView = mView.findViewById(R.id.imgTenantDocument);
            Glide.with(mView.getContext())
                    .load(thumbDocUrl)
                    .placeholder(R.drawable.img_doc_placeholder)
                    .into(thumbDocUrlView);
        }
    }

    private String cleanTenantPhone(String phone){
        String cleaned;
        if (phone.length() < 10){
            Toast.makeText(TenantDetailsActivity.this, "Invalid Number", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (phone.length() > 10) {
            cleaned = phone.substring(phone.length() - 10);
            return cleaned;
        }
        return phone;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (documentAdapter != null){
            documentAdapter.startListening();
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (documentAdapter != null){
            documentAdapter.stopListening();
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}