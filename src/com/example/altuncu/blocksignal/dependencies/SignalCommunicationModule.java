package com.example.altuncu.blocksignal.dependencies;

import android.content.Context;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import com.example.altuncu.blocksignal.BuildConfig;
import com.example.altuncu.blocksignal.CreateProfileActivity;
import com.example.altuncu.blocksignal.DeviceListFragment;
import com.example.altuncu.blocksignal.crypto.storage.SignalProtocolStoreImpl;
import com.example.altuncu.blocksignal.events.ReminderUpdateEvent;
import com.example.altuncu.blocksignal.jobs.AttachmentDownloadJob;
import com.example.altuncu.blocksignal.jobs.AvatarDownloadJob;
import com.example.altuncu.blocksignal.jobs.CleanPreKeysJob;
import com.example.altuncu.blocksignal.jobs.CreateSignedPreKeyJob;
import com.example.altuncu.blocksignal.jobs.GcmRefreshJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceBlockedUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceContactUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceGroupUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceProfileKeyUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceReadReceiptUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceReadUpdateJob;
import com.example.altuncu.blocksignal.jobs.MultiDeviceVerifiedUpdateJob;
import com.example.altuncu.blocksignal.jobs.PushGroupSendJob;
import com.example.altuncu.blocksignal.jobs.PushGroupUpdateJob;
import com.example.altuncu.blocksignal.jobs.PushMediaSendJob;
import com.example.altuncu.blocksignal.jobs.PushNotificationReceiveJob;
import com.example.altuncu.blocksignal.jobs.PushTextSendJob;
import com.example.altuncu.blocksignal.jobs.RefreshAttributesJob;
import com.example.altuncu.blocksignal.jobs.RefreshPreKeysJob;
import com.example.altuncu.blocksignal.jobs.RequestGroupInfoJob;
import com.example.altuncu.blocksignal.jobs.RetrieveProfileAvatarJob;
import com.example.altuncu.blocksignal.jobs.RetrieveProfileJob;
import com.example.altuncu.blocksignal.jobs.RotateSignedPreKeyJob;
import com.example.altuncu.blocksignal.jobs.SendReadReceiptJob;
import com.example.altuncu.blocksignal.preferences.AppProtectionPreferenceFragment;
import com.example.altuncu.blocksignal.preferences.SmsMmsPreferenceFragment;
import com.example.altuncu.blocksignal.push.SecurityEventListener;
import com.example.altuncu.blocksignal.push.SignalServiceNetworkAccess;
import com.example.altuncu.blocksignal.service.MessageRetrievalService;
import com.example.altuncu.blocksignal.service.WebRtcCallService;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.util.CredentialsProvider;
import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
        CreateSignedPreKeyJob.class,
        PushGroupSendJob.class,
        PushTextSendJob.class,
        PushMediaSendJob.class,
        AttachmentDownloadJob.class,
        RefreshPreKeysJob.class,
        MessageRetrievalService.class,
        PushNotificationReceiveJob.class,
        MultiDeviceContactUpdateJob.class,
        MultiDeviceGroupUpdateJob.class,
        MultiDeviceReadUpdateJob.class,
        MultiDeviceBlockedUpdateJob.class,
        DeviceListFragment.class,
        RefreshAttributesJob.class,
        GcmRefreshJob.class,
        RequestGroupInfoJob.class,
        PushGroupUpdateJob.class,
        AvatarDownloadJob.class,
        RotateSignedPreKeyJob.class,
        WebRtcCallService.class,
        RetrieveProfileJob.class,
        MultiDeviceVerifiedUpdateJob.class,
        CreateProfileActivity.class,
        RetrieveProfileAvatarJob.class,
        MultiDeviceProfileKeyUpdateJob.class,
        SendReadReceiptJob.class,
        MultiDeviceReadReceiptUpdateJob.class,
        AppProtectionPreferenceFragment.class})
public class SignalCommunicationModule {

  private static final String TAG = SignalCommunicationModule.class.getSimpleName();

  private final Context                      context;
  private final SignalServiceNetworkAccess   networkAccess;

  private SignalServiceAccountManager  accountManager;
  private SignalServiceMessageSender   messageSender;
  private SignalServiceMessageReceiver messageReceiver;

  public SignalCommunicationModule(Context context, SignalServiceNetworkAccess networkAccess) {
    this.context       = context;
    this.networkAccess = networkAccess;
  }

  @Provides
  synchronized SignalServiceAccountManager provideSignalAccountManager() {
    if (this.accountManager == null) {
      this.accountManager = new SignalServiceAccountManager(networkAccess.getConfiguration(context),
              new DynamicCredentialsProvider(context),
              BuildConfig.USER_AGENT);
    }

    return this.accountManager;
  }

  @Provides
  synchronized SignalServiceMessageSender provideSignalMessageSender() {
    if (this.messageSender == null) {
      this.messageSender = new SignalServiceMessageSender(networkAccess.getConfiguration(context),
              new DynamicCredentialsProvider(context),
              new SignalProtocolStoreImpl(context),
              BuildConfig.USER_AGENT,
              Optional.fromNullable(MessageRetrievalService.getPipe()),
              Optional.of(new SecurityEventListener(context)));
    } else {
      this.messageSender.setMessagePipe(MessageRetrievalService.getPipe());
    }

    return this.messageSender;
  }

  @Provides
  synchronized SignalServiceMessageReceiver provideSignalMessageReceiver() {
    if (this.messageReceiver == null) {
      this.messageReceiver = new SignalServiceMessageReceiver(networkAccess.getConfiguration(context),
              new DynamicCredentialsProvider(context),
              BuildConfig.USER_AGENT,
              new PipeConnectivityListener());
    }

    return this.messageReceiver;
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return TextSecurePreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return TextSecurePreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return TextSecurePreferences.getSignalingKey(context);
    }
  }

  private class PipeConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
      Log.w(TAG, "onConnected()");
    }

    @Override
    public void onConnecting() {
      Log.w(TAG, "onConnecting()");
    }

    @Override
    public void onDisconnected() {
      Log.w(TAG, "onDisconnected()");
    }

    @Override
    public void onAuthenticationFailure() {
      Log.w(TAG, "onAuthenticationFailure()");
      TextSecurePreferences.setUnauthorizedReceived(context, true);
      EventBus.getDefault().post(new ReminderUpdateEvent());
    }

  }

}