package com.example.altuncu.blocksignal.mms;

import android.content.ContentUris;
import android.net.Uri;

import com.example.altuncu.blocksignal.attachments.AttachmentId;

public class PartUriParser {

  private final Uri uri;

  public PartUriParser(Uri uri) {
    this.uri = uri;
  }

  public AttachmentId getPartId() {
    return new AttachmentId(getId(), getUniqueId());
  }

  private long getId() {
    return ContentUris.parseId(uri);
  }

  private long getUniqueId() {
    return Long.parseLong(uri.getPathSegments().get(1));
  }

}
