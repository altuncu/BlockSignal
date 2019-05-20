package com.example.altuncu.blocksignal.contactshare;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.components.AvatarImageView;
import com.example.altuncu.blocksignal.mms.GlideRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.altuncu.blocksignal.contactshare.Contact.*;

public class ContactShareEditAdapter extends RecyclerView.Adapter<ContactShareEditAdapter.ContactEditViewHolder> {

  private final GlideRequests glideRequests;
  private final Locale        locale;
  private final EventListener eventListener;
  private final List<Contact> contacts;

  ContactShareEditAdapter(@NonNull GlideRequests glideRequests, @NonNull Locale locale, @NonNull EventListener eventListener) {
    this.glideRequests = glideRequests;
    this.locale        = locale;
    this.eventListener = eventListener;
    this.contacts      = new ArrayList<>();
  }

  @Override
  public ContactEditViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ContactEditViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_editable_contact, parent, false), locale);
  }

  @Override
  public void onBindViewHolder(ContactEditViewHolder holder, int position) {
    holder.bind(position, contacts.get(position), glideRequests, eventListener);
  }

  @Override
  public int getItemCount() {
    return contacts.size();
  }

  void setContacts(@Nullable List<Contact> contacts) {
    this.contacts.clear();

    if (contacts != null) {
      this.contacts.addAll(contacts);
    }

    notifyDataSetChanged();
  }

  static class ContactEditViewHolder extends RecyclerView.ViewHolder {

    private final AvatarImageView     avatar;
    private final TextView            name;
    private final View                nameEditButton;
    private final ContactFieldAdapter fieldAdapter;

    ContactEditViewHolder(View itemView, @NonNull Locale locale) {
      super(itemView);

      this.avatar         = itemView.findViewById(R.id.editable_contact_avatar);
      this.name           = itemView.findViewById(R.id.editable_contact_name);
      this.nameEditButton = itemView.findViewById(R.id.editable_contact_name_edit_button);
      this.fieldAdapter   = new ContactFieldAdapter(locale, true);

      RecyclerView fields = itemView.findViewById(R.id.editable_contact_fields);
      fields.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
      fields.getLayoutManager().setAutoMeasureEnabled(true);
      fields.setAdapter(fieldAdapter);
    }

    void bind(int position, @NonNull Contact contact, @NonNull GlideRequests glideRequests, @NonNull EventListener eventListener) {
      Context context = itemView.getContext();

      if (contact.getAvatarAttachment() != null && contact.getAvatarAttachment().getDataUri() != null) {
        glideRequests.load(contact.getAvatarAttachment().getDataUri())
                     .fallback(R.drawable.ic_contact_picture)
                     .circleCrop()
                     .diskCacheStrategy(DiskCacheStrategy.ALL)
                     .into(avatar);
      } else {
        glideRequests.load(R.drawable.ic_contact_picture)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(avatar);
      }

      name.setText(ContactUtil.getDisplayName(contact));
      nameEditButton.setOnClickListener(v -> eventListener.onNameEditClicked(position, contact.getName()));
      fieldAdapter.setFields(context, contact.getPhoneNumbers(), contact.getEmails(), contact.getPostalAddresses());
    }
  }

  interface EventListener {
    void onNameEditClicked(int position, @NonNull Name name);
  }
}
