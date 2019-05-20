/*
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.example.altuncu.blocksignal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.altuncu.blocksignal.MessageDetailsRecipientAdapter.RecipientDeliveryStatus;
import com.example.altuncu.blocksignal.color.MaterialColor;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.GroupReceiptDatabase;
import com.example.altuncu.blocksignal.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.example.altuncu.blocksignal.database.MmsDatabase;
import com.example.altuncu.blocksignal.database.MmsSmsDatabase;
import com.example.altuncu.blocksignal.database.SmsDatabase;
import com.example.altuncu.blocksignal.database.loaders.MessageDetailsLoader;
import com.example.altuncu.blocksignal.database.model.MessageRecord;
import com.example.altuncu.blocksignal.mms.GlideApp;
import com.example.altuncu.blocksignal.mms.GlideRequests;
import com.example.altuncu.blocksignal.notifications.MessageNotifier;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.recipients.RecipientModifiedListener;
import com.example.altuncu.blocksignal.util.DateUtils;
import com.example.altuncu.blocksignal.util.DynamicLanguage;
import com.example.altuncu.blocksignal.util.DynamicTheme;
import com.example.altuncu.blocksignal.util.ExpirationUtil;
import com.example.altuncu.blocksignal.util.Util;

import java.lang.ref.WeakReference;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author Jake McGinty
 */
public class MessageDetailsActivity extends PassphraseRequiredActionBarActivity implements LoaderCallbacks<Cursor>, RecipientModifiedListener {
  private final static String TAG = MessageDetailsActivity.class.getSimpleName();

  public final static String MESSAGE_ID_EXTRA     = "message_id";
  public final static String THREAD_ID_EXTRA      = "thread_id";
  public final static String IS_PUSH_GROUP_EXTRA  = "is_push_group";
  public final static String TYPE_EXTRA           = "type";
  public final static String ADDRESS_EXTRA        = "address";

  private GlideRequests    glideRequests;
  private long             threadId;
  private boolean          isPushGroup;
  private ConversationItem conversationItem;
  private ViewGroup        itemParent;
  private View             metadataContainer;
  private View             expiresContainer;
  private TextView         errorText;
  private TextView         sentDate;
  private TextView         receivedDate;
  private TextView         expiresInText;
  private View             receivedContainer;
  private TextView         transport;
  private TextView         toFrom;
  private ListView         recipientsList;
  private LayoutInflater   inflater;

  private DynamicTheme     dynamicTheme    = new DynamicTheme();
  private DynamicLanguage  dynamicLanguage = new DynamicLanguage();

  private boolean running;

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  public void onCreate(Bundle bundle, boolean ready) {
    setContentView(com.example.altuncu.blocksignal.R.layout.message_details_activity);
    running = true;

    initializeResources();
    initializeActionBar();
    getSupportLoaderManager().initLoader(0, null, this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);

    assert getSupportActionBar() != null;
    getSupportActionBar().setTitle(com.example.altuncu.blocksignal.R.string.AndroidManifest__message_details);

    MessageNotifier.setVisibleThread(threadId);
  }

  @Override
  protected void onPause() {
    super.onPause();
    MessageNotifier.setVisibleThread(-1L);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    running = false;
  }

  private void initializeActionBar() {
    assert getSupportActionBar() != null;
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    Recipient recipient = Recipient.from(this, getIntent().getParcelableExtra(ADDRESS_EXTRA), true);
    recipient.addListener(this);

    setActionBarColor(recipient.getColor());
  }

  private void setActionBarColor(MaterialColor color) {
    assert getSupportActionBar() != null;
    getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(color.toStatusBarColor(this));
    }
  }

  @Override
  public void onModified(final Recipient recipient) {
    Util.runOnMain(() -> setActionBarColor(recipient.getColor()));
  }

  private void initializeResources() {
    inflater       = LayoutInflater.from(this);
    View header = inflater.inflate(com.example.altuncu.blocksignal.R.layout.message_details_header, recipientsList, false);

    threadId          = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);
    isPushGroup       = getIntent().getBooleanExtra(IS_PUSH_GROUP_EXTRA, false);
    glideRequests     = GlideApp.with(this);
    itemParent        = header.findViewById(com.example.altuncu.blocksignal.R.id.item_container);
    recipientsList    = findViewById(com.example.altuncu.blocksignal.R.id.recipients_list);
    metadataContainer = header.findViewById(com.example.altuncu.blocksignal.R.id.metadata_container);
    errorText         = header.findViewById(com.example.altuncu.blocksignal.R.id.error_text);
    sentDate          = header.findViewById(com.example.altuncu.blocksignal.R.id.sent_time);
    receivedContainer = header.findViewById(com.example.altuncu.blocksignal.R.id.received_container);
    receivedDate      = header.findViewById(com.example.altuncu.blocksignal.R.id.received_time);
    transport         = header.findViewById(com.example.altuncu.blocksignal.R.id.transport);
    toFrom            = header.findViewById(com.example.altuncu.blocksignal.R.id.tofrom);
    expiresContainer  = header.findViewById(com.example.altuncu.blocksignal.R.id.expires_container);
    expiresInText     = header.findViewById(com.example.altuncu.blocksignal.R.id.expires_in);
    recipientsList.setHeaderDividersEnabled(false);
    recipientsList.addHeaderView(header, null, false);
  }

  private void updateTransport(MessageRecord messageRecord) {
    final String transportText;
    if (messageRecord.isOutgoing() && messageRecord.isFailed()) {
      transportText = "-";
    } else if (messageRecord.isPending()) {
      transportText = getString(com.example.altuncu.blocksignal.R.string.ConversationFragment_pending);
    } else if (messageRecord.isPush()) {
      transportText = getString(com.example.altuncu.blocksignal.R.string.ConversationFragment_push);
    } else if (messageRecord.isMms()) {
      transportText = getString(com.example.altuncu.blocksignal.R.string.ConversationFragment_mms);
    } else {
      transportText = getString(com.example.altuncu.blocksignal.R.string.ConversationFragment_sms);
    }

    transport.setText(transportText);
  }

  private void updateTime(MessageRecord messageRecord) {
    if (messageRecord.isPending() || messageRecord.isFailed()) {
      sentDate.setText("-");
      receivedContainer.setVisibility(View.GONE);
    } else {
      Locale           dateLocale    = dynamicLanguage.getCurrentLocale();
      SimpleDateFormat dateFormatter = DateUtils.getDetailedDateFormatter(this, dateLocale);
      sentDate.setText(dateFormatter.format(new Date(messageRecord.getDateSent())));

      if (messageRecord.getDateReceived() != messageRecord.getDateSent() && !messageRecord.isOutgoing()) {
        receivedDate.setText(dateFormatter.format(new Date(messageRecord.getDateReceived())));
        receivedContainer.setVisibility(View.VISIBLE);
      } else {
        receivedContainer.setVisibility(View.GONE);
      }
    }
  }

  private void updateExpirationTime(final MessageRecord messageRecord) {
    if (messageRecord.getExpiresIn() <= 0 || messageRecord.getExpireStarted() <= 0) {
      expiresContainer.setVisibility(View.GONE);
      return;
    }

    expiresContainer.setVisibility(View.VISIBLE);
    Util.runOnMain(new Runnable() {
      @Override
      public void run() {
        long elapsed   = System.currentTimeMillis() - messageRecord.getExpireStarted();
        long remaining = messageRecord.getExpiresIn() - elapsed;

        String duration = ExpirationUtil.getExpirationDisplayValue(MessageDetailsActivity.this, Math.max((int)(remaining / 1000), 1));
        expiresInText.setText(duration);

        if (running) {
          Util.runOnMainDelayed(this, 500);
        }
      }
    });
  }

  private void updateRecipients(MessageRecord messageRecord, Recipient recipient, List<RecipientDeliveryStatus> recipients) {
    final int toFromRes;
    if (messageRecord.isMms() && !messageRecord.isPush() && !messageRecord.isOutgoing()) {
      toFromRes = com.example.altuncu.blocksignal.R.string.message_details_header__with;
    } else if (messageRecord.isOutgoing()) {
      toFromRes = com.example.altuncu.blocksignal.R.string.message_details_header__to;
    } else {
      toFromRes = com.example.altuncu.blocksignal.R.string.message_details_header__from;
    }
    toFrom.setText(toFromRes);
    conversationItem.bind(messageRecord, glideRequests, dynamicLanguage.getCurrentLocale(), new HashSet<>(), recipient, false);
    recipientsList.setAdapter(new MessageDetailsRecipientAdapter(this, glideRequests, messageRecord, recipients, isPushGroup));
  }

  private void inflateMessageViewIfAbsent(MessageRecord messageRecord) {
    if (conversationItem == null) {
      if (messageRecord.isGroupAction()) {
        conversationItem = (ConversationItem) inflater.inflate(com.example.altuncu.blocksignal.R.layout.conversation_item_update, itemParent, false);
      } else if (messageRecord.isOutgoing()) {
        conversationItem = (ConversationItem) inflater.inflate(com.example.altuncu.blocksignal.R.layout.conversation_item_sent, itemParent, false);
      } else {
        conversationItem = (ConversationItem) inflater.inflate(com.example.altuncu.blocksignal.R.layout.conversation_item_received, itemParent, false);
      }
      itemParent.addView(conversationItem);
    }
  }

  private @Nullable MessageRecord getMessageRecord(Context context, Cursor cursor, String type) {
    switch (type) {
      case MmsSmsDatabase.SMS_TRANSPORT:
        SmsDatabase        smsDatabase = DatabaseFactory.getSmsDatabase(context);
        SmsDatabase.Reader reader      = smsDatabase.readerFor(cursor);
        return reader.getNext();
      case MmsSmsDatabase.MMS_TRANSPORT:
        MmsDatabase        mmsDatabase = DatabaseFactory.getMmsDatabase(context);
        MmsDatabase.Reader mmsReader   = mmsDatabase.readerFor(cursor);
        return mmsReader.getNext();
      default:
        throw new AssertionError("no valid message type specified");
    }
  }


  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new MessageDetailsLoader(this, getIntent().getStringExtra(TYPE_EXTRA),
                                    getIntent().getLongExtra(MESSAGE_ID_EXTRA, -1));
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
    MessageRecord messageRecord = getMessageRecord(this, cursor, getIntent().getStringExtra(TYPE_EXTRA));

    if (messageRecord == null) {
      finish();
    } else {
      new MessageRecipientAsyncTask(this, messageRecord).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    recipientsList.setAdapter(null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);

    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  @SuppressLint("StaticFieldLeak")
  private class MessageRecipientAsyncTask extends AsyncTask<Void,Void,List<RecipientDeliveryStatus>> {

    private final WeakReference<Context> weakContext;
    private final MessageRecord          messageRecord;

    MessageRecipientAsyncTask(@NonNull Context context, @NonNull MessageRecord messageRecord) {
      this.weakContext   = new WeakReference<>(context);
      this.messageRecord = messageRecord;
    }

    protected Context getContext() {
      return weakContext.get();
    }

    @Override
    public List<RecipientDeliveryStatus> doInBackground(Void... voids) {
      Context context = getContext();

      if (context == null) {
        Log.w(TAG, "associated context is destroyed, finishing early");
        return null;
      }

      List<RecipientDeliveryStatus> recipients = new LinkedList<>();

      if (!messageRecord.getRecipient().isGroupRecipient()) {
        recipients.add(new RecipientDeliveryStatus(messageRecord.getRecipient(), getStatusFor(messageRecord.getDeliveryReceiptCount(), messageRecord.getReadReceiptCount(), messageRecord.isPending()), -1));
      } else {
        List<GroupReceiptInfo> receiptInfoList = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageRecord.getId());

        if (receiptInfoList.isEmpty()) {
          List<Recipient> group = DatabaseFactory.getGroupDatabase(context).getGroupMembers(messageRecord.getRecipient().getAddress().toGroupString(), false);

          for (Recipient recipient : group) {
            recipients.add(new RecipientDeliveryStatus(recipient, RecipientDeliveryStatus.Status.UNKNOWN, -1));
          }
        } else {
          for (GroupReceiptInfo info : receiptInfoList) {
            recipients.add(new RecipientDeliveryStatus(Recipient.from(context, info.getAddress(), true),
                                                       getStatusFor(info.getStatus(), messageRecord.isPending()),
                                                       info.getTimestamp()));
          }
        }
      }

      return recipients;
    }

    @Override
    public void onPostExecute(List<RecipientDeliveryStatus> recipients) {
      if (getContext() == null) {
        Log.w(TAG, "AsyncTask finished with a destroyed context, leaving early.");
        return;
      }

      inflateMessageViewIfAbsent(messageRecord);

      updateRecipients(messageRecord, messageRecord.getRecipient(), recipients);
      if (messageRecord.isFailed()) {
        errorText.setVisibility(View.VISIBLE);
        metadataContainer.setVisibility(View.GONE);
      } else {
        updateTransport(messageRecord);
        updateTime(messageRecord);
        updateExpirationTime(messageRecord);
        errorText.setVisibility(View.GONE);
        metadataContainer.setVisibility(View.VISIBLE);
      }
    }

    private RecipientDeliveryStatus.Status getStatusFor(int deliveryReceiptCount, int readReceiptCount, boolean pending) {
      if      (readReceiptCount > 0)     return RecipientDeliveryStatus.Status.READ;
      else if (deliveryReceiptCount > 0) return RecipientDeliveryStatus.Status.DELIVERED;
      else if (!pending)                 return RecipientDeliveryStatus.Status.SENT;
      else                               return RecipientDeliveryStatus.Status.PENDING;
    }

    private RecipientDeliveryStatus.Status getStatusFor(int groupStatus, boolean pending) {
      if      (groupStatus == GroupReceiptDatabase.STATUS_READ)                    return RecipientDeliveryStatus.Status.READ;
      else if (groupStatus == GroupReceiptDatabase.STATUS_DELIVERED)               return RecipientDeliveryStatus.Status.DELIVERED;
      else if (groupStatus == GroupReceiptDatabase.STATUS_UNDELIVERED && !pending) return RecipientDeliveryStatus.Status.SENT;
      else if (groupStatus == GroupReceiptDatabase.STATUS_UNDELIVERED)             return RecipientDeliveryStatus.Status.PENDING;
      else if (groupStatus == GroupReceiptDatabase.STATUS_UNKNOWN)                 return RecipientDeliveryStatus.Status.UNKNOWN;
      throw new AssertionError();
    }

  }
}
