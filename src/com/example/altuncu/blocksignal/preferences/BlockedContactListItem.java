package com.example.altuncu.blocksignal.preferences;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.components.AvatarImageView;
import com.example.altuncu.blocksignal.mms.GlideRequests;
import com.example.altuncu.blocksignal.recipients.Recipient;
import com.example.altuncu.blocksignal.recipients.RecipientModifiedListener;
import com.example.altuncu.blocksignal.util.Util;

public class BlockedContactListItem extends RelativeLayout implements RecipientModifiedListener {

  private AvatarImageView contactPhotoImage;
  private TextView        nameView;
  private GlideRequests   glideRequests;
  private Recipient       recipient;

  public BlockedContactListItem(Context context) {
    super(context);
  }

  public BlockedContactListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BlockedContactListItem(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void onFinishInflate() {
    super.onFinishInflate();
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.nameView          = findViewById(R.id.name);
  }

  public void set(@NonNull GlideRequests glideRequests, @NonNull Recipient recipients) {
    this.glideRequests = glideRequests;
    this.recipient     = recipients;

    onModified(recipients);
    recipients.addListener(this);
  }

  @Override
  public void onModified(final Recipient recipients) {
    final AvatarImageView contactPhotoImage = this.contactPhotoImage;
    final TextView        nameView          = this.nameView;

    Util.runOnMain(() -> {
      contactPhotoImage.setAvatar(glideRequests, recipients, false);
      nameView.setText(recipients.toShortString());
    });
  }

  public Recipient getRecipient() {
    return recipient;
  }
}
