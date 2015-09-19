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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * テスト用
 */
public class GetPluginTask extends AsyncTask<String, Void, List<PluginData>> {
    private final Context context;
    Exception exception;

    public GetPluginTask(Context context) {
        this.context = context;
    }

    @Override
    protected List<PluginData> doInBackground(String... params) {
        try {
            // ログイン情報を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String user_name = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            LoginData login = new LoginData(user_name, password, server_url);

            return new GroupSessionApi(login).getPluginInfo();
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

//    @Override
//    protected void onPostExecute(String data) {
//        super.onPostExecute(data);
//        System.out.println(data);
//    }

//    // 表示用
//    private class DisplayGroupSessionTask extends GetGroupSessionPluginTask {
//        public DisplayGroupSessionTask(Context context) {
//            super(context);
//        }
//
//        @Override
//        protected void onPostExecute(List<PluginData> data) {
//            super.onPostExecute(data);
//
////            // プログレスバー消去
////            ProgressBar progress = (ProgressBar)findViewById(R.id.progress);
////            progress.setVisibility(View.GONE);
//
//            if (data != null) {
//                TextView title = (TextView)findViewById(R.id.tv_title);
//                title.setText("有効なプラグイン");
//
//                for (PluginData pd : data) {
//                    if (pd.getAvailable() == true) {
//                        // テキスト書き込み
//                        View row = View.inflate(MainActivity.this, R.layout.plugin_row, null);
//                        TextView name = (TextView) row.findViewById(R.id.tv_plugin_name);
//                        name.setText(pd.getName());
//
//                        // 予報アイコン
//                        ImageView imageView = (ImageView)row.findViewById(R.id.iv_icon);
//                        imageView.setImageBitmap(pd.getBitmap());
//
//                        // レイアウトに追加
//                        LinearLayout forecastLayout = (LinearLayout) findViewById(R.id.ll_plugins);
//                        forecastLayout.addView(row);
//                    }
//                }
//            }
//            else {
//                if (exception != null) {
//                    Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//        }
//    }
}
