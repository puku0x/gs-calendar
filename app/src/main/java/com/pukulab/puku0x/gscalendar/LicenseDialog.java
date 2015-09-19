/*
 * Copyright (C) 2015 puku0x
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pukulab.puku0x.gscalendar;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

public class LicenseDialog extends DialogPreference {
    Context mContext;

    // コンストラクタは通常のカスタムViewと同じ
    public LicenseDialog(Context context) {
        this(context, null);
        setDialogLayoutResource(R.layout.dialog_license);
    }

    public LicenseDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        setDialogLayoutResource(R.layout.dialog_license);
    }

    public LicenseDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        setDialogLayoutResource(R.layout.dialog_license);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        WebView myWebView = (WebView) view.findViewById(R.id.webview_license);
        myWebView.loadUrl("file:///android_asset/license.html");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        // このやり方だとPOSITIVE,NEGATIVEでしかとれないっぽい
        if (DialogInterface.BUTTON_POSITIVE == which) {
            // emailのインテント作成
            //LaunchEmailUtil.launchEmailToIntent(mContext);
        }
    }
}