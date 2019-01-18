package com.example.altuncu.blocksignal.components.identity;


import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import org.whispersystems.libsignal.logging.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.altuncu.blocksignal.R;
import com.example.altuncu.blocksignal.database.IdentityDatabase.IdentityRecord;
import com.example.altuncu.blocksignal.util.ViewUtil;

import java.util.List;

public class VerifiedBannerView extends LinearLayout {

    private static final String TAG = VerifiedBannerView.class.getSimpleName();

    private View      container;
    private TextView  text;

    public VerifiedBannerView(Context context) {
        super(context);
        initialize();
    }

    public VerifiedBannerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public VerifiedBannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VerifiedBannerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        LayoutInflater.from(getContext()).inflate(R.layout.verified_banner_view, this, true);
        this.container   = ViewUtil.findById(this, R.id.container);
        this.text        = ViewUtil.findById(this, R.id.verified_text);
    }

    public void display(@NonNull final String text,
                        @NonNull final List<IdentityRecord> unverifiedIdentities)
    {
        this.text.setText(text);
        this.postDelayed(new Runnable() {
            public void run() {
                setVisibility(View.GONE);
            }
        }, 3000);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

}
