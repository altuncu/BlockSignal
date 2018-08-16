package com.example.altuncu.blocksignal.jobs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.annimon.stream.Stream;

import com.example.altuncu.blocksignal.ApplicationContext;
import com.example.altuncu.blocksignal.attachments.Attachment;
import com.example.altuncu.blocksignal.database.Address;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.example.altuncu.blocksignal.database.MmsDatabase;
import com.example.altuncu.blocksignal.database.NoSuchMessageException;
import com.example.altuncu.blocksignal.database.documents.NetworkFailure;
import com.example.altuncu.blocksignal.dependencies.InjectableType;
import com.example.altuncu.blocksignal.jobs.requirements.MasterSecretRequirement;
import com.example.altuncu.blocksignal.mms.MediaConstraints;
import com.example.altuncu.blocksignal.mms.MmsException;
import com.example.altuncu.blocksignal.mms.OutgoingGroupMediaMessage;
import com.example.altuncu.blocksignal.mms.OutgoingMediaMessage;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.recipients.RecipientFormattingException;
import com.example.altuncu.blocksignal.transport.UndeliverableMessageException;
import com.example.altuncu.blocksignal.util.GroupUtil;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Quote;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;
import org.whispersystems.signalservice.api.push.exceptions.NetworkFailureException;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

public class PushGroupSendJob extends PushSendJob implements InjectableType {

  private static final long serialVersionUID = 1L;

  private static final String TAG = PushGroupSendJob.class.getSimpleName();

  @Inject transient SignalServiceMessageSender messageSender;

  private final long   messageId;
  private final long   filterRecipientId; // Deprecated
  private final String filterAddress;

  public PushGroupSendJob(Context context, long messageId, @NonNull Address destination, @Nullable Address filterAddress) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withGroupId(destination.toGroupString())
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withRetryCount(5)
                                .create());

    this.messageId         = messageId;
    this.filterAddress     = filterAddress == null ? null :filterAddress.toPhoneString();
    this.filterRecipientId = -1;
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onPushSend()
      throws MmsException, IOException, NoSuchMessageException
  {
    MmsDatabase          database = DatabaseFactory.getMmsDatabase(context);
    OutgoingMediaMessage message  = database.getOutgoingMessage(messageId);

    try {
      deliver(message, filterAddress == null ? null : Address.fromSerialized(filterAddress));

      database.markAsSent(messageId, true);
      markAttachmentsUploaded(messageId, message.getAttachments());

      if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
        database.markExpireStarted(messageId);
        ApplicationContext.getInstance(context)
                          .getExpiringMessageManager()
                          .scheduleDeletion(messageId, true, message.getExpiresIn());
      }
    } catch (InvalidNumberException | RecipientFormattingException | UndeliverableMessageException e) {
      Log.w(TAG, e);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    } catch (EncapsulatedExceptions e) {
      Log.w(TAG, e);
      List<NetworkFailure> failures = new LinkedList<>();

      for (NetworkFailureException nfe : e.getNetworkExceptions()) {
        failures.add(new NetworkFailure(Address.fromSerialized(nfe.getE164number())));
      }

      for (UntrustedIdentityException uie : e.getUntrustedIdentityExceptions()) {
        database.addMismatchedIdentity(messageId, Address.fromSerialized(uie.getE164Number()), uie.getIdentityKey());
      }

      database.addFailures(messageId, failures);

      if (e.getNetworkExceptions().isEmpty() && e.getUntrustedIdentityExceptions().isEmpty()) {
        database.markAsSent(messageId, true);
        markAttachmentsUploaded(messageId, message.getAttachments());

        if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
          database.markExpireStarted(messageId);
          ApplicationContext.getInstance(context)
                            .getExpiringMessageManager()
                            .scheduleDeletion(messageId, true, message.getExpiresIn());
        }
      } else {
        database.markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
      }
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
  }

  private void deliver(OutgoingMediaMessage message, @Nullable Address filterAddress)
      throws IOException, RecipientFormattingException, InvalidNumberException,
      EncapsulatedExceptions, UndeliverableMessageException
  {
    String                        groupId           = message.getRecipient().getAddress().toGroupString();
    Optional<byte[]>              profileKey        = getProfileKey(message.getRecipient());
    List<Address>                 recipients        = getGroupMessageRecipients(groupId, messageId);
    MediaConstraints              mediaConstraints  = MediaConstraints.getPushMediaConstraints();
    List<Attachment>              scaledAttachments = scaleAndStripExifFromAttachments(mediaConstraints, message.getAttachments());
    List<SignalServiceAttachment> attachmentStreams = getAttachmentsFor(scaledAttachments);
    Optional<Quote>               quote             = getQuoteFor(message);
    List<SharedContact>           sharedContacts    = getSharedContactsFor(message);

    List<SignalServiceAddress>    addresses;

    if (filterAddress != null) addresses = getPushAddresses(filterAddress);
    else                       addresses = getPushAddresses(recipients);

    if (message.isGroup()) {
      OutgoingGroupMediaMessage groupMessage     = (OutgoingGroupMediaMessage) message;
      GroupContext              groupContext     = groupMessage.getGroupContext();
      SignalServiceAttachment   avatar           = attachmentStreams.isEmpty() ? null : attachmentStreams.get(0);
      SignalServiceGroup.Type   type             = groupMessage.isGroupQuit() ? SignalServiceGroup.Type.QUIT : SignalServiceGroup.Type.UPDATE;
      SignalServiceGroup        group            = new SignalServiceGroup(type, GroupUtil.getDecodedId(groupId), groupContext.getName(), groupContext.getMembersList(), avatar);
      SignalServiceDataMessage  groupDataMessage = SignalServiceDataMessage.newBuilder()
                                                                           .withTimestamp(message.getSentTimeMillis())
                                                                           .asGroupMessage(group)
                                                                           .build();

      messageSender.sendMessage(addresses, groupDataMessage);
    } else {
      SignalServiceGroup       group        = new SignalServiceGroup(GroupUtil.getDecodedId(groupId));
      SignalServiceDataMessage groupMessage = SignalServiceDataMessage.newBuilder()
                                                                      .withTimestamp(message.getSentTimeMillis())
                                                                      .asGroupMessage(group)
                                                                      .withAttachments(attachmentStreams)
                                                                      .withBody(message.getBody())
                                                                      .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                      .asExpirationUpdate(message.isExpirationUpdate())
                                                                      .withProfileKey(profileKey.orNull())
                                                                      .withQuote(quote.orNull())
                                                                      .withSharedContacts(sharedContacts)
                                                                      .build();

      messageSender.sendMessage(addresses, groupMessage);
    }
  }

  private List<SignalServiceAddress> getPushAddresses(Address address) {
    List<SignalServiceAddress> addresses = new LinkedList<>();
    addresses.add(getPushAddress(address));
    return addresses;
  }

  private List<SignalServiceAddress> getPushAddresses(List<Address> addresses) {
    return Stream.of(addresses).map(this::getPushAddress).toList();
  }

  private @NonNull List<Address> getGroupMessageRecipients(String groupId, long messageId) {
    List<GroupReceiptInfo> destinations = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageId);
    if (!destinations.isEmpty()) return Stream.of(destinations).map(GroupReceiptInfo::getAddress).toList();

    List<Recipient> members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(groupId, false);
    return Stream.of(members).map(Recipient::getAddress).toList();
  }
}
