package com.example.altuncu.blocksignal.mms;

import com.example.altuncu.blocksignal.attachments.Attachment;
import com.example.altuncu.blocksignal.attachments.PointerAttachment;
import com.example.altuncu.blocksignal.contactshare.Contact;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.util.GroupUtil;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class IncomingMediaMessage {

  private final Address       from;
  private final Address       groupId;
  private final String        body;
  private final boolean       push;
  private final long          sentTimeMillis;
  private final int           subscriptionId;
  private final long          expiresIn;
  private final boolean       expirationUpdate;
  private final QuoteModel    quote;

  private final List<Attachment> attachments    = new LinkedList<>();
  private final List<Contact>    sharedContacts = new LinkedList<>();

  public IncomingMediaMessage(Address from,
                              Optional<Address> groupId,
                              String body,
                              long sentTimeMillis,
                              List<Attachment> attachments,
                              int subscriptionId,
                              long expiresIn,
                              boolean expirationUpdate)
  {
    this.from             = from;
    this.groupId          = groupId.orNull();
    this.sentTimeMillis   = sentTimeMillis;
    this.body             = body;
    this.push             = false;
    this.subscriptionId   = subscriptionId;
    this.expiresIn        = expiresIn;
    this.expirationUpdate = expirationUpdate;
    this.quote            = null;

    this.attachments.addAll(attachments);
  }

  public IncomingMediaMessage(Address from,
                              long sentTimeMillis,
                              int subscriptionId,
                              long expiresIn,
                              boolean expirationUpdate,
                              Optional<String> relay,
                              Optional<String> body,
                              Optional<SignalServiceGroup> group,
                              Optional<List<SignalServiceAttachment>> attachments,
                              Optional<QuoteModel> quote,
                              Optional<List<Contact>> sharedContacts)
  {
    this.push             = true;
    this.from             = from;
    this.sentTimeMillis   = sentTimeMillis;
    this.body             = body.orNull();
    this.subscriptionId   = subscriptionId;
    this.expiresIn        = expiresIn;
    this.expirationUpdate = expirationUpdate;
    this.quote            = quote.orNull();

    if (group.isPresent()) this.groupId = Address.fromSerialized(GroupUtil.getEncodedId(group.get().getGroupId(), false));
    else                   this.groupId = null;

    this.attachments.addAll(PointerAttachment.forPointers(attachments));
    this.sharedContacts.addAll(sharedContacts.or(Collections.emptyList()));
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public String getBody() {
    return body;
  }

  public List<Attachment> getAttachments() {
    return attachments;
  }

  public Address getFrom() {
    return from;
  }

  public Address getGroupId() {
    return groupId;
  }

  public boolean isPushMessage() {
    return push;
  }

  public boolean isExpirationUpdate() {
    return expirationUpdate;
  }

  public long getSentTimeMillis() {
    return sentTimeMillis;
  }

  public long getExpiresIn() {
    return expiresIn;
  }

  public boolean isGroupMessage() {
    return groupId != null;
  }

  public QuoteModel getQuote() {
    return quote;
  }

  public List<Contact> getSharedContacts() {
    return sharedContacts;
  }
}
