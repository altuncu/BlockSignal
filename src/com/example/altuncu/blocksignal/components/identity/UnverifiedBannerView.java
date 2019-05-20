package com.example.altuncu.blocksignal.components.identity;


import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.database.IdentityDatabase.IdentityRecord;
import com.example.altuncu.blocksignal.util.ViewUtil;

import java.util.List;

public class UnverifiedBannerView extends LinearLayout {

  private static final String TAG = UnverifiedBannerView.class.getSimpleName();

  private View      container;
  private TextView  text;

  public UnverifiedBannerView(Context context) {
    super(context);
    initialize();
  }

  public UnverifiedBannerView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
  public UnverifiedBannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public UnverifiedBannerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  private void initialize() {
    LayoutInflater.from(getContext()).inflate(R.layout.unverified_banner_view, this, true);
    this.container   = ViewUtil.findById(this, R.id.container);
    this.text        = ViewUtil.findById(this, R.id.unverified_text);
  }

  public void display(@NonNull final String text,
                      @NonNull final List<IdentityRecord> unverifiedIdentities)
  {
    this.text.setText(text);
    setVisibility(View.VISIBLE);
  }

  public void hide() {
    setVisibility(View.GONE);
  }

}
