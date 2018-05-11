/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.vrbrowser.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mozilla.vrbrowser.R;
import org.mozilla.vrbrowser.SessionStore;

import java.net.URI;
import java.net.URL;

public class NavigationURLBar extends FrameLayout {
    private EditText mURL;
    private String mLatestURL;
    private ImageButton mMicrophoneButton;
    private ImageView mInsecureIcon;
    private RelativeLayout mURLLeftContainer;
    private boolean mIsLoading = false;
    private boolean mIsInsecure = false;
    private int mDefaultURLLeftPadding = 0;
    private int mURLProtocolColor;
    private int mURLWebsiteColor;

    public NavigationURLBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context aContext) {
        inflate(aContext, R.layout.navigation_url, this);
        mURL = findViewById(R.id.urlEditText);
        mURL.setShowSoftInputOnFocus(false);
        mURL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView aTextView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    handleURLEdit(aTextView.getText().toString());
                    return true;
                }
                return false;
            }
        });
        mMicrophoneButton = findViewById(R.id.microphoneButton);
        mURLLeftContainer = findViewById(R.id.urlLeftContainer);
        mInsecureIcon = findViewById(R.id.insecureIcon);
        mDefaultURLLeftPadding = mURL.getPaddingLeft();

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = aContext.getTheme();
        theme.resolveAttribute(R.attr.urlProtocolColor, typedValue, true);
        mURLProtocolColor = typedValue.data;
        theme.resolveAttribute(R.attr.urlWebsiteColor, typedValue, true);
        mURLWebsiteColor = typedValue.data;

        // Prevent the URL TextEdit to get focus when user touches something outside of it
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);
    }

    public void setURL(String aURL) {
        mLatestURL = aURL;
        int index = -1;
        if (aURL != null) {
            index = aURL.indexOf("://");
        }
        if (index > 0) {
            SpannableString spannable = new SpannableString(aURL);
            ForegroundColorSpan color1 = new ForegroundColorSpan(mURLProtocolColor);
            ForegroundColorSpan color2 = new ForegroundColorSpan(mURLWebsiteColor);
            spannable.setSpan(color1, 0, index + 3, 0);
            spannable.setSpan(color2, index + 3, aURL.length(), 0);
            mURL.setText(spannable);
        } else {
            mURL.setText(aURL);
        }
    }

    public void setIsInsecure(boolean aIsInsecure) {
        if (mIsInsecure != aIsInsecure) {
            mIsInsecure = aIsInsecure;
            syncViews();
        }
    }

    private void syncViews() {
        boolean showContainer = mIsInsecure;
        int leftPadding = mDefaultURLLeftPadding;
        if (showContainer) {
            mURLLeftContainer.setVisibility(View.VISIBLE);
            mURLLeftContainer.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            leftPadding += mURLLeftContainer.getMeasuredWidth();
        }
        else {
            mURLLeftContainer.setVisibility(View.GONE);
        }

        mURL.setPadding(leftPadding, mURL.getPaddingTop(), mURL.getPaddingRight(), mURL.getPaddingBottom());
    }

    private void handleURLEdit(String text) {
        URI uri = null;
        try {
            URL url = new URL(text);
            uri = url.toURI();
        }
        catch (Exception ex) {
        }

        String url;
        if (uri != null) {
            url = uri.toString();
        } else  if (text.startsWith("resource://")) {
            url = text;
        } else {
            url = "https://www.google.com/search?q=" + text;
        }

        if (SessionStore.get().getCurrentUri() != url) {
            SessionStore.get().loadUri(url);
        }
    }
}
