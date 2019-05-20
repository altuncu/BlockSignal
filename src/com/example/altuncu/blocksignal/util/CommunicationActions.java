package com.example.altuncu.blocksignal.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.example.altuncu.blocksignal.ConversationActivity;
import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.WebRtcCallActivity;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.permissions.Permissions;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.service.WebRtcCallService;

public class CommunicationActions {

  public static void startVoiceCall(@NonNull Activity activity, @NonNull Recipient recipient) {
    Permissions.with(activity)
        .request(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        .ifNecessary()
        .withRationaleDialog(activity.getString(R.string.ConversationActivity_to_call_s_signal_needs_access_to_your_microphone_and_camera, recipient.toShortString()),
                             R.drawable.ic_mic_white_48dp,
                             R.drawable.ic_videocam_white_48dp)
        .withPermanentDenialDialog(activity.getString(R.string.ConversationActivity_signal_needs_the_microphone_and_camera_permissions_in_order_to_call_s, recipient.toShortString()))
        .onAllGranted(() -> {
          Intent intent = new Intent(activity, WebRtcCallService.class);
          intent.setAction(WebRtcCallService.ACTION_OUTGOING_CALL);
          intent.putExtra(WebRtcCallService.EXTRA_REMOTE_ADDRESS, recipient.getAddress());
          activity.startService(intent);

          Intent activityIntent = new Intent(activity, WebRtcCallActivity.class);
          activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          activity.startActivity(activityIntent);
        })
        .execute();
  }

  public static void startConversation(@NonNull  Context   context,
                                       @NonNull  Recipient recipient,
                                       @Nullable String    text)
  {
    new AsyncTask<Void, Void, Long>() {
      @Override
      protected Long doInBackground(Void... voids) {
        return DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipient);
      }

      @Override
      protected void onPostExecute(Long threadId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
        intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());

        if (!TextUtils.isEmpty(text)) {
          intent.putExtra(ConversationActivity.TEXT_EXTRA, text);
        }

        context.startActivity(intent);
      }
    }.execute();
  }

  public static void composeSmsThroughDefaultApp(@NonNull Context context, @NonNull Address address, @Nullable String text) {
    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + address.serialize()));
    if (text != null) {
      intent.putExtra("sms_body", text);
    }
    context.startActivity(intent);
  }
}
