package org.thoughtcrime.securesms


import android.Manifest
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dd.CircularProgressButton
import com.soundcloud.android.crop.Crop

import org.thoughtcrime.securesms.components.InputAwareLayout
import org.thoughtcrime.securesms.components.emoji.EmojiDrawer
import org.thoughtcrime.securesms.components.emoji.EmojiToggle
import org.thoughtcrime.securesms.contacts.avatars.ResourceContactPhoto
import org.thoughtcrime.securesms.crypto.ProfileKeyUtil
import org.thoughtcrime.securesms.database.Address
import org.thoughtcrime.securesms.dependencies.InjectableType
import org.thoughtcrime.securesms.jobs.MultiDeviceProfileKeyUpdateJob
import org.thoughtcrime.securesms.mms.GlideApp
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.profiles.AvatarHelper
import org.thoughtcrime.securesms.profiles.ProfileMediaConstraints
import org.thoughtcrime.securesms.profiles.SystemProfileUtil
import org.thoughtcrime.securesms.util.BitmapDecodingException
import org.thoughtcrime.securesms.util.BitmapUtil
import org.thoughtcrime.securesms.util.DynamicLanguage
import org.thoughtcrime.securesms.util.DynamicTheme
import org.thoughtcrime.securesms.util.FileProviderUtil
import org.thoughtcrime.securesms.util.IntentUtils
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.thoughtcrime.securesms.util.Util
import org.thoughtcrime.securesms.util.ViewUtil
import org.thoughtcrime.securesms.util.concurrent.ListenableFuture
import org.whispersystems.signalservice.api.SignalServiceAccountManager
import org.whispersystems.signalservice.api.crypto.ProfileCipher
import org.whispersystems.signalservice.api.util.StreamDetails

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.LinkedList
import java.util.concurrent.ExecutionException

import javax.inject.Inject

import android.provider.MediaStore.EXTRA_OUTPUT

@SuppressLint("StaticFieldLeak")
class CreateProfileActivity : BaseActionBarActivity(), InjectableType {

    private val dynamicTheme = DynamicTheme()
    private val dynamicLanguage = DynamicLanguage()

    @Inject
    internal var accountManager: SignalServiceAccountManager? = null

    private var container: InputAwareLayout? = null
    private var avatar: ImageView? = null
    private var finishButton: CircularProgressButton? = null
    private var name: EditText? = null
    private var emojiToggle: EmojiToggle? = null
    private var emojiDrawer: EmojiDrawer? = null
    private var reveal: View? = null

    private var nextIntent: Intent? = null
    private var avatarBytes: ByteArray? = null
    private var captureFile: File? = null

    public override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        dynamicTheme.onCreate(this)
        dynamicLanguage.onCreate(this)

        setContentView(R.layout.profile_create_activity)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        supportActionBar!!.setTitle(R.string.CreateProfileActivity_your_profile_info)

        initializeResources()
        initializeEmojiInput()
        initializeProfileName(intent.getBooleanExtra(EXCLUDE_SYSTEM, false))
        initializeProfileAvatar(intent.getBooleanExtra(EXCLUDE_SYSTEM, false))

        ApplicationContext.getInstance(this).injectDependencies(this)
    }

    public override fun onResume() {
        super.onResume()
        dynamicTheme.onResume(this)
        dynamicLanguage.onResume(this)
    }

    override fun onBackPressed() {
        if (container!!.isInputOpen)
            container!!.hideCurrentInput(name)
        else
            super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (container!!.currentInput === emojiDrawer) {
            container!!.hideAttachedInput(true)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_AVATAR -> if (resultCode == Activity.RESULT_OK) {
                val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
                var inputFile: Uri? = data?.data

                if (inputFile == null && captureFile != null) {
                    inputFile = Uri.fromFile(captureFile)
                }

                if (data != null && data.getBooleanExtra("delete", false)) {
                    avatarBytes = null
                    avatar!!.setImageDrawable(ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, resources.getColor(R.color.grey_400)))
                } else {
                    Crop(inputFile).output(outputFile).asSquare().start(this)
                }
            }
            Crop.REQUEST_CROP -> if (resultCode == Activity.RESULT_OK) {
                object : AsyncTask<Void, Void, ByteArray>() {
                    override fun doInBackground(vararg params: Void): ByteArray? {
                        try {
                            val result = BitmapUtil.createScaledBytes(this@CreateProfileActivity, Crop.getOutput(data!!), ProfileMediaConstraints())
                            return result.bitmap
                        } catch (e: BitmapDecodingException) {
                            Log.w(TAG, e)
                            return null
                        }

                    }

                    override fun onPostExecute(result: ByteArray?) {
                        if (result != null) {
                            avatarBytes = result
                            GlideApp.with(this@CreateProfileActivity)
                                    .load(avatarBytes)
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .circleCrop()
                                    .into(avatar!!)
                        } else {
                            Toast.makeText(this@CreateProfileActivity, R.string.CreateProfileActivity_error_setting_profile_photo, Toast.LENGTH_LONG).show()
                        }
                    }
                }.execute()
            }
        }
    }

    private fun initializeResources() {
        val skipButton = ViewUtil.findById<TextView>(this, R.id.skip_button)
        val informationLabel = ViewUtil.findById<TextView>(this, R.id.information_label)

        this.avatar = ViewUtil.findById(this, R.id.avatar)
        this.name = ViewUtil.findById(this, R.id.name)
        this.emojiToggle = ViewUtil.findById(this, R.id.emoji_toggle)
        this.emojiDrawer = ViewUtil.findById(this, R.id.emoji_drawer)
        this.container = ViewUtil.findById(this, R.id.container)
        this.finishButton = ViewUtil.findById(this, R.id.finish_button)
        this.reveal = ViewUtil.findById(this, R.id.reveal)
        this.nextIntent = intent.getParcelableExtra(NEXT_INTENT)

        this.avatar!!.setImageDrawable(ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, resources.getColor(R.color.grey_400)))

        this.avatar!!.setOnClickListener { view ->
            Permissions.with(this)
                    .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .onAnyResult { this.handleAvatarSelectionWithPermissions() }
                    .execute()
        }

        this.name!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().toByteArray().size > ProfileCipher.NAME_PADDED_LENGTH) {
                    name!!.error = getString(R.string.CreateProfileActivity_too_long)
                    finishButton!!.isEnabled = false
                } else if (name!!.error != null || !finishButton!!.isEnabled) {
                    name!!.error = null
                    finishButton!!.isEnabled = true
                }
            }
        })

        this.finishButton!!.setOnClickListener { view ->
            this.finishButton!!.isIndeterminateProgressMode = true
            this.finishButton!!.progress = 50
            handleUpload()
        }

        skipButton.setOnClickListener { view ->
            if (nextIntent != null) startActivity(nextIntent)
            finish()
        }

        informationLabel.setOnClickListener { view ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://support.signal.org/hc/en-us/articles/115001434171")

            if (packageManager.queryIntentActivities(intent, 0).size > 0) {
                startActivity(intent)
            }
        }
    }

    private fun initializeProfileName(excludeSystem: Boolean) {
        if (!TextUtils.isEmpty(TextSecurePreferences.getProfileName(this))) {
            val profileName = TextSecurePreferences.getProfileName(this)

            name!!.setText(profileName)
            name!!.setSelection(profileName.length, profileName.length)
        } else if (!excludeSystem) {
            SystemProfileUtil.getSystemProfileName(this).addListener(object : ListenableFuture.Listener<String> {
                override fun onSuccess(result: String) {
                    if (!TextUtils.isEmpty(result)) {
                        name!!.setText(result)
                        name!!.setSelection(result.length, result.length)
                    }
                }

                override fun onFailure(e: ExecutionException) {
                    Log.w(TAG, e)
                }
            })
        }
    }

    private fun initializeProfileAvatar(excludeSystem: Boolean) {
        val ourAddress = Address.fromSerialized(TextSecurePreferences.getLocalNumber(this))

        if (AvatarHelper.getAvatarFile(this, ourAddress).exists() && AvatarHelper.getAvatarFile(this, ourAddress).length() > 0) {
            object : AsyncTask<Void, Void, ByteArray>() {
                override fun doInBackground(vararg params: Void): ByteArray? {
                    try {
                        return Util.readFully(AvatarHelper.getInputStreamFor(this@CreateProfileActivity, ourAddress))
                    } catch (e: IOException) {
                        Log.w(TAG, e)
                        return null
                    }

                }

                override fun onPostExecute(result: ByteArray?) {
                    if (result != null) {
                        avatarBytes = result
                        GlideApp.with(this@CreateProfileActivity)
                                .load(result)
                                .circleCrop()
                                .into(avatar!!)
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        } else if (!excludeSystem) {
            SystemProfileUtil.getSystemProfileAvatar(this, ProfileMediaConstraints()).addListener(object : ListenableFuture.Listener<ByteArray> {
                override fun onSuccess(result: ByteArray?) {
                    if (result != null) {
                        avatarBytes = result
                        GlideApp.with(this@CreateProfileActivity)
                                .load(result)
                                .circleCrop()
                                .into(avatar!!)
                    }
                }

                override fun onFailure(e: ExecutionException) {
                    Log.w(TAG, e)
                }
            })
        }
    }

    private fun initializeEmojiInput() {
        this.emojiToggle!!.attach(emojiDrawer)

        this.emojiToggle!!.setOnClickListener { v ->
            if (container!!.currentInput === emojiDrawer) {
                container!!.showSoftkey(name)
            } else {
                container!!.show(name!!, emojiDrawer!!)
            }
        }

        this.emojiDrawer!!.setEmojiEventListener(object : EmojiDrawer.EmojiEventListener {
            override fun onKeyEvent(keyEvent: KeyEvent) {
                name!!.dispatchKeyEvent(keyEvent)
            }

            override fun onEmojiSelected(emoji: String) {
                val start = name!!.selectionStart
                val end = name!!.selectionEnd

                name!!.text.replace(Math.min(start, end), Math.max(start, end), emoji)
                name!!.setSelection(start + emoji.length)
            }
        })

        this.container!!.addOnKeyboardShownListener { emojiToggle!!.setToEmoji() }
        this.name!!.setOnClickListener { v -> container!!.showSoftkey(name) }
    }

    private fun createAvatarSelectionIntent(captureFile: File?, includeClear: Boolean, includeCamera: Boolean): Intent {
        val extraIntents = LinkedList<Intent>()
        var galleryIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        galleryIntent.type = "image/*"

        if (!IntentUtils.isResolvable(this@CreateProfileActivity, galleryIntent)) {
            galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
        }

        if (includeCamera) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            if (captureFile != null && cameraIntent.resolveActivity(packageManager) != null) {
                cameraIntent.putExtra(EXTRA_OUTPUT, FileProviderUtil.getUriFor(this, captureFile))
                extraIntents.add(cameraIntent)
            }
        }

        if (includeClear) {
            extraIntents.add(Intent("org.thoughtcrime.securesms.action.CLEAR_PROFILE_PHOTO"))
        }

        val chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.CreateProfileActivity_profile_photo))

        if (!extraIntents.isEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
        }


        return chooserIntent
    }

    private fun handleAvatarSelectionWithPermissions() {
        val hasCameraPermission = Permissions.hasAll(this, Manifest.permission.CAMERA)

        if (hasCameraPermission) {
            try {
                captureFile = File.createTempFile("capture", "jpg", externalCacheDir)
            } catch (e: IOException) {
                Log.w(TAG, e)
                captureFile = null
            }

        }

        val chooserIntent = createAvatarSelectionIntent(captureFile, avatarBytes != null, hasCameraPermission)
        startActivityForResult(chooserIntent, REQUEST_CODE_AVATAR)
    }

    private fun handleUpload() {
        val name: String?
        val avatar: StreamDetails?

        if (TextUtils.isEmpty(this.name!!.text.toString()))
            name = null
        else
            name = this.name!!.text.toString()

        if (avatarBytes == null || avatarBytes!!.size == 0)
            avatar = null
        else
            avatar = StreamDetails(ByteArrayInputStream(avatarBytes),
                    "image/jpeg", avatarBytes!!.size.toLong())

        object : AsyncTask<Void, Void, Boolean>() {
            override fun doInBackground(vararg params: Void): Boolean? {
                val context = this@CreateProfileActivity
                val profileKey = ProfileKeyUtil.getProfileKey(this@CreateProfileActivity)

                try {
                    accountManager!!.setProfileName(profileKey, name)
                    TextSecurePreferences.setProfileName(context, name)
                } catch (e: IOException) {
                    Log.w(TAG, e)
                    return false
                }

                try {
                    accountManager!!.setProfileAvatar(profileKey, avatar)
                    AvatarHelper.setAvatar(this@CreateProfileActivity, Address.fromSerialized(TextSecurePreferences.getLocalNumber(context)), avatarBytes)
                    TextSecurePreferences.setProfileAvatarId(this@CreateProfileActivity, SecureRandom().nextInt())
                } catch (e: IOException) {
                    Log.w(TAG, e)
                    return false
                }

                ApplicationContext.getInstance(context).jobManager.add(MultiDeviceProfileKeyUpdateJob(context))

                return true
            }

            public override fun onPostExecute(result: Boolean?) {
                super.onPostExecute(result)

                if (result!!) {
                    if (captureFile != null) captureFile!!.delete()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        handleFinishedLollipop()
                    else
                        handleFinishedLegacy()
                } else {
                    Toast.makeText(this@CreateProfileActivity, R.string.CreateProfileActivity_problem_setting_profile, Toast.LENGTH_LONG).show()
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun handleFinishedLegacy() {
        finishButton!!.progress = 0
        if (nextIntent != null) startActivity(nextIntent)
        finish()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun handleFinishedLollipop() {
        val finishButtonLocation = IntArray(2)
        val revealLocation = IntArray(2)

        finishButton!!.getLocationInWindow(finishButtonLocation)
        reveal!!.getLocationInWindow(revealLocation)

        var finishX = finishButtonLocation[0] - revealLocation[0]
        var finishY = finishButtonLocation[1] - revealLocation[1]

        finishX += finishButton!!.width / 2
        finishY += finishButton!!.height / 2

        val animation = ViewAnimationUtils.createCircularReveal(reveal, finishX, finishY, 0f, Math.max(reveal!!.width, reveal!!.height).toFloat())
        animation.duration = 500
        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                finishButton!!.progress = 0
                if (nextIntent != null) startActivity(nextIntent)
                finish()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        reveal!!.visibility = View.VISIBLE
        animation.start()
    }

    companion object {

        private val TAG = CreateProfileActivity::class.java.simpleName

        val NEXT_INTENT = "next_intent"
        val EXCLUDE_SYSTEM = "exclude_system"

        private val REQUEST_CODE_AVATAR = 1
    }
}
