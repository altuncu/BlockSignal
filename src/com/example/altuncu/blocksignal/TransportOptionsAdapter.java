package com.example.altuncu.blocksignal;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.altuncu.blocksignal.util.ViewUtil;

import java.util.List;

public class TransportOptionsAdapter extends BaseAdapter {

  private final LayoutInflater inflater;

  private List<TransportOption> enabledTransports;

  public TransportOptionsAdapter(@NonNull Context context,
                                 @NonNull List<TransportOption> enabledTransports)
  {
    super();
    this.inflater          = LayoutInflater.from(context);
    this.enabledTransports = enabledTransports;
  }

  public void setEnabledTransports(List<TransportOption> enabledTransports) {
    this.enabledTransports = enabledTransports;
  }

  @Override
  public int getCount() {
    return enabledTransports.size();
  }

  @Override
  public Object getItem(int position) {
    return enabledTransports.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(com.example.altuncu.blocksignal.R.layout.transport_selection_list_item, parent, false);
    }

    TransportOption transport   = (TransportOption) getItem(position);
    ImageView       imageView   = ViewUtil.findById(convertView, com.example.altuncu.blocksignal.R.id.icon);
    TextView        textView    = ViewUtil.findById(convertView, com.example.altuncu.blocksignal.R.id.text);
    TextView        subtextView = ViewUtil.findById(convertView, com.example.altuncu.blocksignal.R.id.subtext);

    imageView.getBackground().setColorFilter(transport.getBackgroundColor(), Mode.MULTIPLY);
    imageView.setImageResource(transport.getDrawable());
    textView.setText(transport.getDescription());

    if (transport.getSimName().isPresent()) {
      subtextView.setText(transport.getSimName().get());
      subtextView.setVisibility(View.VISIBLE);
    } else {
      subtextView.setVisibility(View.GONE);
    }

    return convertView;
  }
}
