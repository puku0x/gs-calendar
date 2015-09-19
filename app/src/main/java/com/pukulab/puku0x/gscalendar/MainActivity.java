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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Calendar;

/**
 * メインアクティビティ
 */
public class MainActivity extends AppCompatActivity {
    // プログレスバー
    private ProgressBar progress;

    // ビュー
    EditText mUserIdEditText;
    EditText mPasswordEditText;
    EditText mServerUrlEditText;
    CheckBox mAutoLoginCheckBox;

    // 表示用
    private class CheckingLoginTask extends LoginTask {
        public CheckingLoginTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(UserData user) {
            super.onPostExecute(user);
            progress.setVisibility(View.GONE);

            if (user != null) {
                // 今日の日付
                Calendar cal = Calendar.getInstance();
                final int year = cal.get(Calendar.YEAR);                 // 年
                final int monthOfYear = cal.get(Calendar.MONTH);         // 月
                final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);  // 日
                cal.clear();
                cal.set(year, monthOfYear, dayOfMonth);

                // スケジュール一覧へ移動
                Intent intent = null;
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String default_schedule_display_mode = sp.getString(SettingsActivity.PREFERENCE_DEFAULT_SCHEDULE_DISPLAY_MODE, "0");
                if (default_schedule_display_mode != null && default_schedule_display_mode.equals("0")) {
                    intent = CalendarActivity.createIntent(getApplicationContext(), user, user, cal.getTime());
                }
                else {
                    intent = ScheduleActivity.createIntent(getApplicationContext(), user, user, cal.getTime());
                }
                startActivity(intent);
                finish();
            }
            else {
                Toast.makeText(getApplicationContext(), R.string.login_error, Toast.LENGTH_LONG).show();
            }

            if (exception != null) {
                Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                exception.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //new GetPluginTask(MainActivity.this).execute("");
        try {
            //戻るボタンの追加
            ActionBar actionBar = getSupportActionBar();
            actionBar.hide();

            // 設定情報を取得
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            // ログイン情報取得
            String user_id = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            boolean auto_login = sp.getBoolean(SettingsActivity.PREFERENCE_AUTO_LOGIN, false);

            // プログレスバー
            progress = (ProgressBar) findViewById(R.id.progressBar);

            // テキストボックスたち
            mUserIdEditText = (EditText)findViewById(R.id.et_login_id);
            mPasswordEditText = (EditText)findViewById(R.id.et_password);
            mServerUrlEditText = (EditText)findViewById(R.id.et_server_url);
            mAutoLoginCheckBox = (CheckBox)findViewById(R.id.checkBox);

            // 設定値があれば読み込む
            mUserIdEditText.setText(user_id);
            mPasswordEditText.setText(password);
            mServerUrlEditText.setText(server_url);
            mAutoLoginCheckBox.setChecked(auto_login);

            // ログインボタン
            Button btn_login = (Button) findViewById(R.id.btn_login);
            btn_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // ログイン情報を保存する
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(SettingsActivity.PREFERENCE_USER_NAME, mUserIdEditText.getText().toString());
                    editor.putString(SettingsActivity.PREFERENCE_PASSWORD, mPasswordEditText.getText().toString());
                    editor.putString(SettingsActivity.PREFERENCE_SERVER_URL, mServerUrlEditText.getText().toString());
                    editor.putBoolean(SettingsActivity.PREFERENCE_AUTO_LOGIN, mAutoLoginCheckBox.isChecked());
                    editor.commit();
                    new CheckingLoginTask(MainActivity.this).execute("");
                }
            });

            if (auto_login) {
                new CheckingLoginTask(this).execute("");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = SettingsActivity.createIntent(getApplicationContext());
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}