package com.example.altuncu.blocksignal;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.altuncu.blocksignal.blockstack.VerifyIdentity;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.IdentityDatabase;
import com.example.altuncu.blocksignal.database.IdentityDatabase.IdentityRecord;
import com.example.altuncu.blocksignal.database.IdentityDatabase.VerifiedStatus;
import com.example.altuncu.blocksignal.database.model.MessageRecord;
import com.example.altuncu.blocksignal.mms.GlideRequests;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.recipients.RecipientModifiedListener;
import com.example.altuncu.blocksignal.util.DateUtils;
import com.example.altuncu.blocksignal.util.Dialogs;
import com.example.altuncu.blocksignal.util.GroupUtil;
import com.example.altuncu.blocksignal.util.IdentityUtil;
import com.example.altuncu.blocksignal.util.Util;
import com.example.altuncu.blocksignal.util.concurrent.ListenableFuture;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ConversationUpdateItem extends LinearLayout
    implements RecipientModifiedListener, BindableConversationItem
{
  private static final String TAG = ConversationUpdateItem.class.getSimpleName();

  private Set<MessageRecord> batchSelected;

  private ImageView     icon;
  private TextView      body;
  private TextView      date;
  private Recipient     sender;
  private MessageRecord messageRecord;
  private Locale        locale;

  public ConversationUpdateItem(Context context) {
    super(context);
  }

  public ConversationUpdateItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();

    this.icon = findViewById(com.example.altuncu.blocksignal.R.id.conversation_update_icon);
    this.body = findViewById(com.example.altuncu.blocksignal.R.id.conversation_update_body);
    this.date = findViewById(com.example.altuncu.blocksignal.R.id.conversation_update_date);

    this.setOnClickListener(new InternalClickListener(null));
  }

  @Override
  public void bind(@NonNull MessageRecord      messageRecord,
                   @NonNull GlideRequests      glideRequests,
                   @NonNull Locale             locale,
                   @NonNull Set<MessageRecord> batchSelected,
                   @NonNull Recipient          conversationRecipient,
                            boolean            pulseUpdate)
  {
    this.batchSelected = batchSelected;

    bind(messageRecord, locale);
  }

  @Override
  public void setEventListener(@Nullable EventListener listener) {
    // No events to report yet
  }

  @Override
  public MessageRecord getMessageRecord() {
    return messageRecord;
  }

  private void bind(@NonNull MessageRecord messageRecord, @NonNull Locale locale) {
    this.messageRecord = messageRecord;
    this.sender        = messageRecord.getIndividualRecipient();
    this.locale        = locale;

    this.sender.addListener(this);

    if      (messageRecord.isGroupAction())           setGroupRecord(messageRecord);
    else if (messageRecord.isCallLog())               setCallRecord(messageRecord);
    else if (messageRecord.isJoined())                setJoinedRecord(messageRecord);
    else if (messageRecord.isExpirationTimerUpdate()) setTimerRecord(messageRecord);
    else if (messageRecord.isEndSession())            setEndSessionRecord(messageRecord);
    else if (messageRecord.isIdentityUpdate())        setIdentityRecord(messageRecord);
    else if (messageRecord.isIdentityVerified() ||
             messageRecord.isIdentityDefault())       setIdentityVerifyUpdate(messageRecord);
    else                                              throw new AssertionError("Neither group nor log nor joined.");

    if (batchSelected.contains(messageRecord)) setSelected(true);
    else                                       setSelected(false);
  }

  private void setCallRecord(MessageRecord messageRecord) {
    if      (messageRecord.isIncomingCall()) icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_call_received_grey600_24dp);
    else if (messageRecord.isOutgoingCall()) icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_call_made_grey600_24dp);
    else                                     icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_call_missed_grey600_24dp);

    body.setText(messageRecord.getDisplayBody());
    date.setText(DateUtils.getExtendedRelativeTimeSpanString(getContext(), locale, messageRecord.getDateReceived()));
    date.setVisibility(View.VISIBLE);
  }

  private void setTimerRecord(final MessageRecord messageRecord) {
    if (messageRecord.getExpiresIn() > 0) {
      icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_timer_white_24dp);
      icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    } else {
      icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_timer_off_white_24dp);
      icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    }

    body.setText(messageRecord.getDisplayBody());
    date.setVisibility(View.GONE);
  }

  private void setIdentityRecord(final MessageRecord messageRecord) {
    IdentityDatabase   identityDatabase   = DatabaseFactory.getIdentityDatabase(getContext());

    VerifyIdentity blockstack = new VerifyIdentity();
    boolean isVerified;

    isVerified = blockstack.verifyKeys(messageRecord.getIndividualRecipient());
    if (isVerified) {
      identityDatabase.setVerified(messageRecord.getIndividualRecipient().getAddress(),
                                   identityDatabase.getIdentity(messageRecord.getIndividualRecipient().getAddress()).get().getIdentityKey(),
                                   IdentityDatabase.VerifiedStatus.VERIFIED);
       // Toast.makeText(getContext(), "Verified by Blockstack", Toast.LENGTH_LONG).show();
    }
    else {
      identityDatabase.setVerified(messageRecord.getIndividualRecipient().getAddress(),
              identityDatabase.getIdentity(messageRecord.getIndividualRecipient().getAddress()).get().getIdentityKey(),
              VerifiedStatus.UNVERIFIED);

      Builder dialog = new Builder(getContext());
      dialog.setTitle("Critical Security Issue");
      dialog.setMessage("We detected an attack threatening your security. So, this conversation will be terminated in a few seconds.");
      dialog.setIconAttribute(R.attr.dialog_alert_icon);
      DialogInterface.OnClickListener dialogClickListener = (dif, i) -> {
        Intent intent = new Intent(getContext(), ConversationListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getContext().startActivity(intent);
      };      dialog.setPositiveButton(R.string.ok, dialogClickListener);
      dialog.show();
    }


    body.setText(messageRecord.getDisplayBody());
    date.setVisibility(View.GONE);
  }

  private void setIdentityVerifyUpdate(final MessageRecord messageRecord) {
    IdentityDatabase   identityDatabase   = DatabaseFactory.getIdentityDatabase(getContext());

    VerifyIdentity blockstack = new VerifyIdentity();
    boolean isVerified;

    isVerified = false;
    if (isVerified) {
      identityDatabase.setVerified(messageRecord.getIndividualRecipient().getAddress(),
              identityDatabase.getIdentity(messageRecord.getIndividualRecipient().getAddress()).get().getIdentityKey(),
              IdentityDatabase.VerifiedStatus.VERIFIED);
    }
    else {
      identityDatabase.setVerified(messageRecord.getIndividualRecipient().getAddress(),
              identityDatabase.getIdentity(messageRecord.getIndividualRecipient().getAddress()).get().getIdentityKey(),
              IdentityDatabase.VerifiedStatus.UNVERIFIED);

      AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
      dialog.setTitle("Critical Security Issue");
      dialog.setMessage("We detected an attack threatening your security. So, this conversation will be terminated in a few seconds.");
      dialog.setIconAttribute(R.attr.dialog_alert_icon);
      DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dif, int i) {
          getContext().startActivity(new Intent(getContext(), ConversationListActivity.class));
        }
      };
      dialog.setPositiveButton(R.string.ok, dialogClickListener);
      dialog.show();
    }

    body.setText(messageRecord.getDisplayBody());
    date.setVisibility(View.GONE);
  }

  private void setGroupRecord(MessageRecord messageRecord) {
    icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_group_grey600_24dp);
    icon.clearColorFilter();

    GroupUtil.getDescription(getContext(), messageRecord.getBody()).addListener(this);
    body.setText(messageRecord.getDisplayBody());

    date.setVisibility(View.GONE);
  }

  private void setJoinedRecord(MessageRecord messageRecord) {
    icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_favorite_grey600_24dp);
    icon.clearColorFilter();
    body.setText(messageRecord.getDisplayBody());
    date.setVisibility(View.GONE);
  }

  private void setEndSessionRecord(MessageRecord messageRecord) {
    icon.setImageResource(com.example.altuncu.blocksignal.R.drawable.ic_refresh_white_24dp);
    icon.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#757575"), PorterDuff.Mode.MULTIPLY));
    body.setText(messageRecord.getDisplayBody());
    date.setVisibility(View.GONE);
  }
  
  @Override
  public void onModified(Recipient recipient) {
    Util.runOnMain(() -> bind(messageRecord, locale));
  }

  @Override
  public void setOnClickListener(View.OnClickListener l) {
    super.setOnClickListener(new InternalClickListener(l));
  }

  @Override
  public void unbind() {
    if (sender != null) {
      sender.removeListener(this);
    }
  }

  private class InternalClickListener implements View.OnClickListener {

    @Nullable private final View.OnClickListener parent;

    InternalClickListener(@Nullable View.OnClickListener parent) {
      this.parent = parent;
    }

    @Override
    public void onClick(View v) {
      if ((!messageRecord.isIdentityUpdate()  &&
           !messageRecord.isIdentityDefault() &&
           !messageRecord.isIdentityVerified()) ||
          !batchSelected.isEmpty())
      {
        if (parent != null) parent.onClick(v);
        return;
      }

      final Recipient sender = ConversationUpdateItem.this.sender;

      IdentityUtil.getRemoteIdentityKey(getContext(), sender).addListener(new ListenableFuture.Listener<Optional<IdentityRecord>>() {
        @Override
        public void onSuccess(Optional<IdentityRecord> result) {
          if (result.isPresent()) {
            // Probably Redundant
          }
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
        }
      });
    }
  }

}
