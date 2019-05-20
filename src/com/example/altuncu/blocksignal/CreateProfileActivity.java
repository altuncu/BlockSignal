package com.example.altuncu.blocksignal;


import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.dd.CircularProgressButton;
import com.example.altuncu.blocksignal.providers.PhotoProvider;
import com.soundcloud.android.crop.Crop;

import com.example.altuncu.blocksignal.components.InputAwareLayout;
//import com.example.altuncu.blocksignal.components.emoji.EmojiDrawer;
//import com.example.altuncu.blocksignal.components.emoji.EmojiToggle;
import com.example.altuncu.blocksignal.contacts.avatars.ResourceContactPhoto;
import com.example.altuncu.blocksignal.crypto.ProfileKeyUtil;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.dependencies.InjectableType;
import com.example.altuncu.blocksignal.jobs.MultiDeviceProfileKeyUpdateJob;
import com.example.altuncu.blocksignal.mms.GlideApp;
import com.example.altuncu.blocksignal.permissions.Permissions;
import com.example.altuncu.blocksignal.profiles.AvatarHelper;
import com.example.altuncu.blocksignal.profiles.ProfileMediaConstraints;
import com.example.altuncu.blocksignal.profiles.SystemProfileUtil;
import com.example.altuncu.blocksignal.util.BitmapDecodingException;
import com.example.altuncu.blocksignal.util.BitmapUtil;
import com.example.altuncu.blocksignal.util.DynamicLanguage;
import com.example.altuncu.blocksignal.util.DynamicTheme;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import com.example.altuncu.blocksignal.util.Util;
import com.example.altuncu.blocksignal.util.ViewUtil;
import com.example.altuncu.blocksignal.util.concurrent.ListenableFuture;

import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.util.StreamDetails;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

@SuppressLint("StaticFieldLeak")
public class CreateProfileActivity extends BaseActionBarActivity implements InjectableType {

  private static final String TAG = CreateProfileActivity.class.getSimpleName();

  public static final String NEXT_INTENT    = "next_intent";
  public static final String EXCLUDE_SYSTEM = "exclude_system";

  private static final int REQUEST_CODE_AVATAR = 1;

  private final DynamicTheme    dynamicTheme    = new DynamicTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  @Inject SignalServiceAccountManager accountManager;

  private InputAwareLayout       container;
  private ImageView              avatar;
  private CircularProgressButton finishButton;
  private EditText               name;
 // private EmojiToggle            emojiToggle;
 // private EmojiDrawer            emojiDrawer;
  private View                   reveal;

  private Intent nextIntent;
  private byte[] avatarBytes;
  private File   captureFile;

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);

    setContentView(com.example.altuncu.blocksignal.R.layout.profile_create_activity);

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    getSupportActionBar().setTitle(com.example.altuncu.blocksignal.R.string.CreateProfileActivity_your_profile_info);

    initializeResources();
 //   initializeEmojiInput();
    initializeProfileName(getIntent().getBooleanExtra(EXCLUDE_SYSTEM, false));
    initializeProfileAvatar(getIntent().getBooleanExtra(EXCLUDE_SYSTEM, false));

    ApplicationContext.getInstance(this).injectDependencies(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  public void onBackPressed() {
    if (container.isInputOpen()) container.hideCurrentInput(name);
    else                         super.onBackPressed();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

 /*   if (container.getCurrentInput() == emojiDrawer) {
      container.hideAttachedInput(true);
    }*/
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {
      case REQUEST_CODE_AVATAR:
        if (resultCode == Activity.RESULT_OK) {
          Uri outputFile = PhotoProvider.getPhotoUri(new File(getCacheDir(), "cropped"));
          Uri inputFile  = (data != null ? data.getData() : null);

          if (inputFile == null && captureFile != null) {
            inputFile = PhotoProvider.getPhotoUri(captureFile);
          }

          if (data != null && data.getBooleanExtra("delete", false)) {
            avatarBytes = null;
            avatar.setImageDrawable(new ResourceContactPhoto(com.example.altuncu.blocksignal.R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(com.example.altuncu.blocksignal.R.color.grey_400)));
          } else {
            new Crop(inputFile).output(outputFile).asSquare().start(this);
          }
        }

        break;
      case Crop.REQUEST_CROP:
        if (resultCode == Activity.RESULT_OK) {
          new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... params) {
              try {
                BitmapUtil.ScaleResult result = BitmapUtil.createScaledBytes(CreateProfileActivity.this, Crop.getOutput(data), new ProfileMediaConstraints());
                return result.getBitmap();
              } catch (BitmapDecodingException e) {
                Log.w(TAG, e);
                return null;
              }
            }

            @Override
            protected void onPostExecute(byte[] result) {
              if (result != null) {
                avatarBytes = result;
                GlideApp.with(CreateProfileActivity.this)
                        .load(avatarBytes)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .circleCrop()
                        .into(avatar);
              } else {
                Toast.makeText(CreateProfileActivity.this, com.example.altuncu.blocksignal.R.string.CreateProfileActivity_error_setting_profile_photo, Toast.LENGTH_LONG).show();
              }
            }
          }.execute();
        }
        break;
    }
  }

  private void initializeResources() {
   // TextView skipButton       = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.skip_button);
   // TextView informationLabel = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.information_label);

    this.avatar       = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.avatar);
    this.name         = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.name);
 //   this.emojiToggle  = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.emoji_toggle);
 //   this.emojiDrawer  = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.emoji_drawer);
    this.container    = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.container);
    this.finishButton = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.finish_button);
    this.reveal       = ViewUtil.findById(this, com.example.altuncu.blocksignal.R.id.reveal);
    this.nextIntent   = getIntent().getParcelableExtra(NEXT_INTENT);

    GlideApp.with(CreateProfileActivity.this)
            .load(BlockstackActivity.get_avatar())
            .into(avatar);

    name.setText(BlockstackActivity.get_username());
    name.setEnabled(false);
    name.setFocusable(false);

   /* this.avatar.setOnClickListener(view -> Permissions.with(this)
                                                      .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                      .ifNecessary()
                                                      .onAnyResult(this::handleAvatarSelectionWithPermissions)
                                                      .execute());

    this.name.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void afterTextChanged(Editable s) {
        if (s.toString().getBytes().length > ProfileCipher.NAME_PADDED_LENGTH) {
          name.setError(getString(com.example.altuncu.blocksignal.R.string.CreateProfileActivity_too_long));
          finishButton.setEnabled(false);
        } else if (name.getError() != null || !finishButton.isEnabled()) {
          name.setError(null);
          finishButton.setEnabled(true);
        }
      }
    });
*/
    this.finishButton.setOnClickListener(view -> {
      this.finishButton.setIndeterminateProgressMode(true);
      this.finishButton.setProgress(50);
      handleUpload();
    });
/*
    skipButton.setOnClickListener(view -> {
      if (nextIntent != null) startActivity(nextIntent);
      finish();
    });
*/
  /*  informationLabel.setOnClickListener(view -> {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("https://support.signal.org/hc/en-us/articles/115001434171"));

      if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
        startActivity(intent);
      }
    });*/
  }

  private void initializeProfileName(boolean excludeSystem) {
    if (!TextUtils.isEmpty(TextSecurePreferences.getProfileName(this))) {
      String profileName = TextSecurePreferences.getProfileName(this);

      name.setText(profileName);
      name.setSelection(profileName.length(), profileName.length());
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileName(this).addListener(new ListenableFuture.Listener<String>() {
        @Override
        public void onSuccess(String result) {
          if (!TextUtils.isEmpty(result)) {
            name.setText(result);
            name.setSelection(result.length(), result.length());
          }
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
        }
      });
    }
  }

  private void initializeProfileAvatar(boolean excludeSystem) {
    Address ourAddress = Address.fromSerialized(TextSecurePreferences.getLocalNumber(this));

    if (AvatarHelper.getAvatarFile(this, ourAddress).exists() && AvatarHelper.getAvatarFile(this, ourAddress).length() > 0) {
      new AsyncTask<Void, Void, byte[]>() {
        @Override
        protected byte[] doInBackground(Void... params) {
          try {
            return Util.readFully(AvatarHelper.getInputStreamFor(CreateProfileActivity.this, ourAddress));
          } catch (IOException e) {
            Log.w(TAG, e);
            return null;
          }
        }

        @Override
        protected void onPostExecute(byte[] result) {
          if (result != null) {
            avatarBytes = result;
            GlideApp.with(CreateProfileActivity.this)
                    .load(result)
                    .circleCrop()
                    .into(avatar);
          }
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileAvatar(this, new ProfileMediaConstraints()).addListener(new ListenableFuture.Listener<byte[]>() {
        @Override
        public void onSuccess(byte[] result) {
          if (result != null) {
            avatarBytes = result;
            GlideApp.with(CreateProfileActivity.this)
                    .load(result)
                    .circleCrop()
                    .into(avatar);
          }
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
        }
      });
    }
  }
/*
  private void initializeEmojiInput() {
    this.emojiToggle.attach(emojiDrawer);

    this.emojiToggle.setOnClickListener(v -> {
      if (container.getCurrentInput() == emojiDrawer) {
        container.showSoftkey(name);
      } else {
        container.show(name, emojiDrawer);
      }
    });

    this.emojiDrawer.setEmojiEventListener(new EmojiDrawer.EmojiEventListener() {
      @Override
      public void onKeyEvent(KeyEvent keyEvent) {
        name.dispatchKeyEvent(keyEvent);
      }

      @Override
      public void onEmojiSelected(String emoji) {
        final int start = name.getSelectionStart();
        final int end   = name.getSelectionEnd();

        name.getText().replace(Math.min(start, end), Math.max(start, end), emoji);
        name.setSelection(start + emoji.length());
      }
    });

    this.container.addOnKeyboardShownListener(() -> emojiToggle.setToEmoji());
    this.name.setOnClickListener(v -> container.showSoftkey(name));
  }*/

 /* private Intent createAvatarSelectionIntent(@Nullable File captureFile, boolean includeClear, boolean includeCamera) {
    List<Intent> extraIntents  = new LinkedList<>();
    Intent       galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
    galleryIntent.setType("image/*");

    if (!IntentUtils.isResolvable(CreateProfileActivity.this, galleryIntent)) {
      galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
      galleryIntent.setType("image/*");
    }

    if (includeCamera) {
      Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

      if (captureFile != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
        cameraIntent.putExtra(EXTRA_OUTPUT, FileProviderUtil.getUriFor(this, captureFile));
        extraIntents.add(cameraIntent);
      }
    }

    if (includeClear) {
      extraIntents.add(new Intent("com.example.altuncu.blocksignal.action.CLEAR_PROFILE_PHOTO"));
    }

    Intent chooserIntent = Intent.createChooser(galleryIntent, getString(com.example.altuncu.blocksignal.R.string.CreateProfileActivity_profile_photo));

    if (!extraIntents.isEmpty()) {
      chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Intent[0]));
    }


    return chooserIntent;
  }
*/
/*  private void handleAvatarSelectionWithPermissions() {
    boolean hasCameraPermission = Permissions.hasAll(this, Manifest.permission.CAMERA);

    if (hasCameraPermission) {
      try {
        captureFile = File.createTempFile("capture", "jpg", getExternalCacheDir());
      } catch (IOException e) {
        Log.w(TAG, e);
        captureFile = null;
      }
    }

    Intent chooserIntent = createAvatarSelectionIntent(captureFile, avatarBytes != null, hasCameraPermission);
    startActivityForResult(chooserIntent, REQUEST_CODE_AVATAR);
  }
*/
  private void handleUpload() {
    final String        name;
    final StreamDetails avatar;

    if (TextUtils.isEmpty(this.name.getText().toString())) name = null;
    else                                                   name = this.name.getText().toString();

    if (avatarBytes == null || avatarBytes.length == 0) avatar = null;
    else                                                avatar = new StreamDetails(new ByteArrayInputStream(avatarBytes),
                                                                                   "image/jpeg", avatarBytes.length);

    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        Context context    = CreateProfileActivity.this;
        byte[]  profileKey = ProfileKeyUtil.getProfileKey(CreateProfileActivity.this);

        try {
          accountManager.setProfileName(profileKey, name);
          TextSecurePreferences.setProfileName(context, name);
        } catch (IOException e) {
          Log.w(TAG, e);
          return false;
        }

        try {
          accountManager.setProfileAvatar(profileKey, avatar);
          AvatarHelper.setAvatar(CreateProfileActivity.this, Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)), avatarBytes);
          TextSecurePreferences.setProfileAvatarId(CreateProfileActivity.this, new SecureRandom().nextInt());
        } catch (IOException e) {
          Log.w(TAG, e);
          return false;
        }

        ApplicationContext.getInstance(context).getJobManager().add(new MultiDeviceProfileKeyUpdateJob(context));

        return true;
      }

      @Override
      public void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        if (result) {
          if (captureFile != null) captureFile.delete();
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) handleFinishedLollipop();
          else                                                       handleFinishedLegacy();
        } else        {
          Toast.makeText(CreateProfileActivity.this, com.example.altuncu.blocksignal.R.string.CreateProfileActivity_problem_setting_profile, Toast.LENGTH_LONG).show();
        }
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void handleFinishedLegacy() {
    finishButton.setProgress(0);
    if (nextIntent != null) startActivity(nextIntent);
    finish();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void handleFinishedLollipop() {
    int[] finishButtonLocation = new int[2];
    int[] revealLocation       = new int[2];

    finishButton.getLocationInWindow(finishButtonLocation);
    reveal.getLocationInWindow(revealLocation);

    int finishX = finishButtonLocation[0] - revealLocation[0];
    int finishY = finishButtonLocation[1] - revealLocation[1];

    finishX += finishButton.getWidth() / 2;
    finishY += finishButton.getHeight() / 2;

    Animator animation = ViewAnimationUtils.createCircularReveal(reveal, finishX, finishY, 0f, (float) Math.max(reveal.getWidth(), reveal.getHeight()));
    animation.setDuration(500);
    animation.addListener(new Animator.AnimatorListener() {
      @Override
      public void onAnimationStart(Animator animation) {}

      @Override
      public void onAnimationEnd(Animator animation) {
        finishButton.setProgress(0);
        if (nextIntent != null)  startActivity(nextIntent);
        finish();
      }

      @Override
      public void onAnimationCancel(Animator animation) {}
      @Override
      public void onAnimationRepeat(Animator animation) {}
    });

    reveal.setVisibility(View.VISIBLE);
    animation.start();
  }
}
