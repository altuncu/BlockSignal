package com.example.altuncu.blocksignal.database.model;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.altuncu.blocksignal.contactshare.Contact;
import com.example.altuncu.blocksignal.database.documents.IdentityKeyMismatch;
import com.example.altuncu.blocksignal.database.documents.NetworkFailure;
import com.example.altuncu.blocksignal.mms.Slide;
import com.example.altuncu.blocksignal.mms.SlideDeck;
import com.example.altuncu.blocksignal.recipients.Recipient;

import java.util.LinkedList;
import java.util.List;

public abstract class MmsMessageRecord extends MessageRecord {

  private final @NonNull  SlideDeck     slideDeck;
  private final @Nullable Quote         quote;
  private final @NonNull  List<Contact> contacts = new LinkedList<>();

  MmsMessageRecord(Context context, long id, String body, Recipient conversationRecipient,
                   Recipient individualRecipient, int recipientDeviceId, long dateSent,
                   long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                   long type, List<IdentityKeyMismatch> mismatches,
                   List<NetworkFailure> networkFailures, int subscriptionId, long expiresIn,
                   long expireStarted, @NonNull SlideDeck slideDeck, int readReceiptCount,
                   @Nullable Quote quote, @NonNull List<Contact> contacts)
  {
    super(context, id, body, conversationRecipient, individualRecipient, recipientDeviceId, dateSent, dateReceived, threadId, deliveryStatus, deliveryReceiptCount, type, mismatches, networkFailures, subscriptionId, expiresIn, expireStarted, readReceiptCount);

    this.slideDeck = slideDeck;
    this.quote     = quote;

    this.contacts.addAll(contacts);
  }

  @Override
  public boolean isMms() {
    return true;
  }

  @NonNull
  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @Override
  public boolean isMediaPending() {
    for (Slide slide : getSlideDeck().getSlides()) {
      if (slide.isInProgress() || slide.isPendingDownload()) {
        return true;
      }
    }

    return false;
  }

  public boolean containsMediaSlide() {
    return slideDeck.containsMediaSlide();
  }

  public @Nullable Quote getQuote() {
    return quote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }
}
