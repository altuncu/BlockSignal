package com.example.altuncu.blocksignal.notifications;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import com.example.altuncu.blocksignal.ConversationListActivity;
import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.database.RecipientDatabase;
import com.example.altuncu.blocksignal.preferences.widgets.NotificationPrivacyPreference;
import com.example.altuncu.blocksignal.util.TextSecurePreferences;

public class PendingMessageNotificationBuilder extends AbstractNotificationBuilder {

  public PendingMessageNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
    super(context, privacy);

    Intent intent = new Intent(context, ConversationListActivity.class);

    setSmallIcon(R.drawable.icon_notification);
    setColor(context.getResources().getColor(R.color.textsecure_primary));
    setPriority(TextSecurePreferences.getNotificationPriority(context));
    setCategory(NotificationCompat.CATEGORY_MESSAGE);

    setContentTitle(context.getString(R.string.MessageNotifier_pending_signal_messages));
    setContentText(context.getString(R.string.MessageNotifier_you_have_pending_signal_messages));
    setTicker(context.getString(R.string.MessageNotifier_you_have_pending_signal_messages));

    setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
    setAutoCancel(true);
    setAlarms(null, RecipientDatabase.VibrateState.DEFAULT);
  }
}
