package com.ajdeguzman.wyup;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.ajdeguzman.wyup.custom.AdjustableLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;

public class MainActivity extends AppCompatActivity {

    final ClarifaiClient client = new ClarifaiBuilder(Credentials.CLARIFAI.API_KEY).buildSync();
    private static final String TAG = MainActivity.class.getSimpleName();

    private String photoPath = null;
    private static final int CODE_PICK = 1;
    private static final int CODE_SHOT = 2;
    private static final int REQUEST_SHOT = 3;
    private static final int CODE_SPEAK = 4;

    private static final int REQUEST_STORAGE = 0;
    private static final int REQUEST_IMAGE_CAPTURE = REQUEST_STORAGE + 1;
    private static final int REQUEST_LOAD_IMAGE = REQUEST_IMAGE_CAPTURE + 1;
    List<String> predictionList = new ArrayList<>();
    private AlertDialog.Builder confirmTextDialog;

    private final List<String> tagsListInitial = new ArrayList<>();

    private TextView mLblResultTags, mLblSelectTag;
    private TextView mLblEmptyState;
    private Uri cameraImageUri = null;

    private ImageView imgResult;
    private ImageView imgEmptyState;
    private LinearLayout mLinearEmpty;
    NetworkConnectivity mNetConn = new NetworkConnectivity(MainActivity.this);

    private Uri imageUri;
    private Bitmap thumbnail;

    public byte[] jpeg;

    String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        imgResult = (ImageView) findViewById(R.id.img_result);
        imgResult = (ImageView) findViewById(R.id.img_result);
        mLblResultTags = (TextView) findViewById(R.id.lbl_result_tag);
        mLblEmptyState = (TextView) findViewById(R.id.lbl_empty_state);
        imgEmptyState = (ImageView) findViewById(R.id.img_empty_state);
        mLblSelectTag = (TextView) findViewById(R.id.lbl_select_tag);

        mLinearEmpty = (LinearLayout) findViewById(R.id.layout_empty_state);
        confirmTextDialog = new AlertDialog.Builder(this);
        setSupportActionBar(toolbar);
        grantPermissions();
        FloatingActionButton fab = findViewById(R.id.fabCamera);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraShot();
            }
        });
    }

    public void cameraShot() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile;
                try {
                    File storageDir = getFilesDir();
                    photoFile = File.createTempFile(
                            "SNAPSHOT",
                            ".jpg",
                            storageDir
                    );

                    photoPath = photoFile.getAbsolutePath();
                } catch (IOException ex) {
                    return;
                }

                 imageUri = FileProvider.getUriForFile(this,
                        "com.ajdeguzman.wyup.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, CODE_SHOT);
            }
        } else {
            requestPermission();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mNetConn.isConnectedToInternet()) {
            if (requestCode == CODE_PICK && resultCode == RESULT_OK) {
                final Bitmap bitmap = loadBitmapFromUri(data.getData());
                if (bitmap != null) {
                    mLinearEmpty.setVisibility(View.GONE);
                    imgResult.setImageBitmap(bitmap);
                    callClarifai();
                } else {
                    mLblResultTags.setText(R.string.err_msg_unable_to_load_image);
                }
            } else if (requestCode == CODE_SHOT && resultCode == RESULT_OK) {
                try {
                    mLinearEmpty.setVisibility(View.GONE);
                    thumbnail = MediaStore.Images.Media.getBitmap(
                            getContentResolver(), imageUri);
                    imgResult.setImageBitmap(thumbnail);
                    callClarifai();
                } catch (Exception e) {
                    mLblResultTags.setText(R.string.err_unable_load_image);
                    e.printStackTrace();
                }

            } else if (requestCode == CODE_SPEAK && resultCode == RESULT_OK && null != data) {
                final ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                confirmTextDialog.setTitle(R.string.confirm_msg_correct)
                        .setMessage(result.get(0))
                        .setPositiveButton(R.string.action_submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                             /*   Intent i = new Intent(getApplicationContext(), ResultActivity.class);
                                i.putExtra("str_tag", result.get(0));
                                i.putExtra("type", 1);
                                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);*/
                            }
                        })
                        .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton(R.string.action_retry, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                               // speechText();
                            }
                        })
                        .show();

            }
        } else if (resultCode == RESULT_CANCELED) {
            mLinearEmpty.setVisibility(View.VISIBLE);
        } else {
            showNoConnectionState();
        }

        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = null;
            if (requestCode == REQUEST_LOAD_IMAGE && data != null) {
                selectedImage = data.getData();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                selectedImage = cameraImageUri;
            }

            if (selectedImage != null) {
                    sendSelectedImage(selectedImage);
            }
        }
    }

    void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_SHOT);
    }

    void grantPermissions() {
        if (!hasPermissions(getApplicationContext(), PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    void showNoConnectionState() {
        imgEmptyState.setImageResource(R.drawable.empty_state_onion_connection);
        mLblEmptyState.setText(R.string.msg_no_connection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void callClarifai() {
        mLblResultTags.setText(R.string.info_msg_loading);
        if (photoPath != null) {
            new ClarifaiTask().execute(new File(photoPath));
        }
    }
    private class ClarifaiTask extends AsyncTask<File, Integer, List<String>> {


        protected List<String> doInBackground(File... images) {
            predictionList.clear();
            List<ClarifaiOutput<Concept>> predictionResults;
            for (File image : images) {
                predictionResults = client.getDefaultModels().foodModel().predict()
                        .withInputs(ClarifaiInput.forImage(image))
                        .withMaxConcepts(10)
                        .selectConcepts()
                        .executeSync()
                        .get();

                for (ClarifaiOutput<Concept> result : predictionResults) {
                    Log.i("TEST",result.data().size() + "");
                    for (Concept datum : result.data()) {
                        predictionList.add(datum.name());
                    }
                }
            }

            return predictionList;
        }

        protected void onPostExecute(List<String> predictionList) {
            // Delete photo
            (new File(photoPath)).delete();
            photoPath = null;

            if (predictionList != null) {
                updateUIForResult(predictionList);
            } //else info.setText("Try again...");
        }
    }
    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
            int sampleSize = 1;
            while (opts.outWidth / (2 * sampleSize) >= imgResult.getWidth() && opts.outHeight / (2 * sampleSize) >= imgResult.getHeight()) {
                sampleSize *= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + uri, e);
        }
        return null;
    }

   /* private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {

            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                    320 * bitmap.getHeight() / bitmap.getWidth(), true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            jpeg = out.toByteArray();

            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            return null;
        }
    }
*/
    private void updateUIForResult(List<String> result) {
        tagsListInitial.clear();

        if (result != null) {
                StringBuilder b = new StringBuilder();
                for(int i=0; i < result.size(); i++)
                    tagsListInitial.add(result.get(i));
                mLblResultTags.setVisibility(View.GONE);
                mLblSelectTag.setVisibility(View.VISIBLE);
                addChipsViewFinal(tagsListInitial);
            } else {
                mLblResultTags.setText(R.string.err_msg_unrecognized_image);
            }

    }

    void submitTag(String tag) {
        /*Intent i = new Intent(getApplicationContext(), ResultActivity.class);
        i.putExtra("str_tag", tag);
        i.putExtra("type", 1);
        i.putExtra("byteArray", jpeg);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);*/
    }

    private void addChipsViewFinal(List<String> tagList) {
        AdjustableLayout adjustableLayout = (AdjustableLayout) findViewById(R.id.container);
        adjustableLayout.removeAllViews();
        for (int i = 0; i < tagList.size(); i++) {
            @SuppressLint("InflateParams") final View newView = LayoutInflater.from(this).inflate(R.layout.layout_view_chip_text, null);
            LinearLayout linearChipTag = (LinearLayout) newView.findViewById(R.id.linear_chip_tag);
            final TextView txtChipTag = (TextView) newView.findViewById(R.id.txt_chip_content);

            linearChipTag.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("WrongConstant")
                @Override
                public void onClick(View view) {

                    final String tempTags = txtChipTag.getText().toString();
                    if (mNetConn.isConnectedToInternet()) {
                        submitTag(tempTags);
                    } else {

                    }
                }
            });
            txtChipTag.setText(tagList.get(i));
            adjustableLayout.addingMultipleView(newView);
        }
        adjustableLayout.invalidateView();
    }

    private void sendSelectedImage(Uri selectedImageUri) {
        Log.d("uri", selectedImageUri + "");
        imgResult.setImageDrawable(null);
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Drawable d = new BitmapDrawable(getResources(), bitmap);
        imgResult.setImageDrawable(d);
        callClarifai();
        mLinearEmpty.setVisibility(View.GONE);
    }

}