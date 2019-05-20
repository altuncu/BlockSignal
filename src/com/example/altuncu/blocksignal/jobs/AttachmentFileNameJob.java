package com.example.altuncu.blocksignal.jobs;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.altuncu.blocksignal.attachments.Attachment;
import com.example.altuncu.blocksignal.attachments.AttachmentId;
import com.example.altuncu.blocksignal.attachments.DatabaseAttachment;
import com.example.altuncu.blocksignal.crypto.AsymmetricMasterCipher;
import com.example.altuncu.blocksignal.crypto.AsymmetricMasterSecret;
import com.example.altuncu.blocksignal.crypto.MasterSecret;
import com.example.altuncu.blocksignal.crypto.MasterSecretUtil;
import com.example.altuncu.blocksignal.database.DatabaseFactory;
import com.example.altuncu.blocksignal.jobs.requirements.MasterSecretRequirement;
import com.example.altuncu.blocksignal.mms.IncomingMediaMessage;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.libsignal.InvalidMessageException;

import java.io.IOException;
import java.util.Arrays;

public class AttachmentFileNameJob extends MasterSecretJob {

  private static final long serialVersionUID = 1L;

  private final long   attachmentRowId;
  private final long   attachmentUniqueId;
  private final String encryptedFileName;

  public AttachmentFileNameJob(@NonNull Context context, @NonNull AsymmetricMasterSecret asymmetricMasterSecret,
                               @NonNull DatabaseAttachment attachment, @NonNull IncomingMediaMessage message)
  {
    super(context, new JobParameters.Builder().withPersistence()
                                              .withRequirement(new MasterSecretRequirement(context))
                                              .create());

    this.attachmentRowId    = attachment.getAttachmentId().getRowId();
    this.attachmentUniqueId = attachment.getAttachmentId().getUniqueId();
    this.encryptedFileName  = getEncryptedFileName(asymmetricMasterSecret, attachment, message);
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException, InvalidMessageException {
    if (encryptedFileName == null) return;

    AttachmentId attachmentId      = new AttachmentId(attachmentRowId, attachmentUniqueId);
    String       plaintextFileName = new AsymmetricMasterCipher(MasterSecretUtil.getAsymmetricMasterSecret(context, masterSecret)).decryptBody(encryptedFileName);

    DatabaseFactory.getAttachmentDatabase(context).updateAttachmentFileName(attachmentId, plaintextFileName);
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return false;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onCanceled() {

  }

  private @Nullable String getEncryptedFileName(@NonNull AsymmetricMasterSecret asymmetricMasterSecret,
                                                @NonNull DatabaseAttachment attachment,
                                                @NonNull IncomingMediaMessage mediaMessage)
  {
    for (Attachment messageAttachment : mediaMessage.getAttachments()) {
      if (mediaMessage.getAttachments().size() == 1 ||
          (messageAttachment.getDigest() != null && Arrays.equals(messageAttachment.getDigest(), attachment.getDigest())))
      {
        if (messageAttachment.getFileName() == null) return null;
        else                                         return new AsymmetricMasterCipher(asymmetricMasterSecret).encryptBody(messageAttachment.getFileName());
      }
    }

    return null;
  }


}
