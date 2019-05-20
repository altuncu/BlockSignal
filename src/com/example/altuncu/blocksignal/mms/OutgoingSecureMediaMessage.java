package com.example.altuncu.blocksignal.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.altuncu.blocksignal.attachments.Attachment;
import com.example.altuncu.blocksignal.contactshare.Contact;
import com.example.altuncu.blocksignal.recipients.Recipient;

import java.util.List;

public class OutgoingSecureMediaMessage extends OutgoingMediaMessage {

  public OutgoingSecureMediaMessage(Recipient recipient, String body,
                                    List<Attachment> attachments,
                                    long sentTimeMillis,
                                    int distributionType,
                                    long expiresIn,
                                    @Nullable QuoteModel quote,
                                    @NonNull List<Contact> contacts)
  {
    super(recipient, body, attachments, sentTimeMillis, -1, expiresIn, distributionType, quote, contacts);
  }

  public OutgoingSecureMediaMessage(OutgoingMediaMessage base) {
    super(base);
  }

  @Override
  public boolean isSecure() {
    return true;
  }
}
