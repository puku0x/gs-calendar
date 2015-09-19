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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class UserListActivity extends AppCompatActivity {
    // ログイン中のユーザの情報
    UserData mLoginUser;

    // 表示するユーザの情報
    UserData mDisplayedUser;

    // 表示する日付
    Date mDisplayedDate;

    // インテント引数
    //private final static String ARGS_SCHEDULE = "SCHEDULE";

    // インテント引数 (元画面に返す方)
    public final static int REQUEST_GET_USER = 0x08;
    public final static String ARGS_NEW_USER = "NEW_USER";

    // インテント引数
    private final static String ARGS_LOGIN_USER = "LOGIN_USER";
    private final static String ARGS_DISPLAYED_USER = "DISPLAYED_USER";
    private final static String ARGS_DISPLAYED_DATE = "DISPLAYED_DATE";

    // プログレスバー
    private ProgressBar mProgressBar;

    // ユーザリスト
    List<UserData> mUserList;
    UserListAdapter mListAdapter;
    private ListView mListView;

    // インテント
    public static Intent createIntent(Context context, UserData loginUser, UserData displayedUser, Date displayedDate) {
        Intent intent = new Intent(context, UserListActivity.class);
        intent.putExtra(ARGS_LOGIN_USER, loginUser);
        intent.putExtra(ARGS_DISPLAYED_USER, displayedUser);
        intent.putExtra(ARGS_DISPLAYED_DATE, displayedDate);
        return intent;
    }

    // 表示用アダプタ
    public class UserListAdapter extends ArrayAdapter<UserData> {
        private LayoutInflater layoutInflater;

        public UserListAdapter(Context context, int textViewResourceId, List<UserData> objects) {
            super(context, textViewResourceId, objects);
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 特定の行(position)のデータを得る
            final UserData user = (UserData)getItem(position);
            convertView = layoutInflater.inflate(R.layout.users_row, null);
            TextView text = (TextView) convertView.findViewById(R.id.tv_users);
            text.setText(user.name);
            // ログイン中のユーザはクリック不可・灰色
            if (user.usid.equals(mLoginUser.usid)) {
                text.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            final UserData user = (UserData)getItem(position);
            return (!user.usid.equals(mLoginUser.usid));
        }
    }

    // 表示用
    private class DisplayUserListTask extends GetGroupListTask {
        public DisplayUserListTask(Context context) {
            super(context);
        }

        // 処理中
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        // 処理後
        @Override
        protected void onPostExecute(List<GroupData> data) {
            super.onPostExecute(data);
            mProgressBar.setVisibility(View.GONE);

            if (data != null) {
                // アダプタ設定
                ArrayAdapter<GroupData> groupListAdapter = new ArrayAdapter<>(UserListActivity.this, android.R.layout.simple_spinner_item);
                groupListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // アダプタに追加
                for (GroupData group: data) {
                    groupListAdapter.add(group);
                }

                // スピナーにアダプタを設定
                Spinner spinner = (Spinner)findViewById(R.id.spinner_group);
                spinner.setAdapter(groupListAdapter);

                // 最後に選択したグループに切り替え
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String defaultGroup = sp.getString(SettingsActivity.PREFERENCE_DEFAULT_USER_GROUP_ID, "");
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).id.equals(defaultGroup)) {
                        spinner.setSelection(i);
                        break;
                    }
                }

                // スピナークリック時の動き
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Spinner spinner = (Spinner) parent;
                        GroupData group = (GroupData) spinner.getSelectedItem();

                        // 最後に選択したグループIDを保存
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(SettingsActivity.PREFERENCE_DEFAULT_USER_GROUP_ID, group.id);
                        editor.apply();

                        // ユーザの一覧を取得
                        GetUserListTask task = new GetUserListTask(UserListActivity.this) {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                mProgressBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected void onPostExecute(List<UserData> result) {
                                super.onPostExecute(result);
                                mProgressBar.setVisibility(View.GONE);

                                if (result != null) {
                                    mUserList.clear();
                                    mUserList.addAll(result);
                                    mListAdapter.notifyDataSetChanged();
                                }
                                else {
                                    Toast.makeText(UserListActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                                }
                            }
                        };
                        task.execute(group);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
            else {
                Toast.makeText(UserListActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        //戻るボタンの追加
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("ユーザ 一覧");

        // プログレスバー
        mProgressBar = (ProgressBar) findViewById(R.id.pb_user_list);

        // 引数取得
        Bundle extras = getIntent().getExtras();

        // ログイン中のユーザ
        if (mLoginUser == null) {
            mLoginUser = (UserData) extras.getSerializable(ARGS_LOGIN_USER);
        }

        // 表示ユーザ
        if (mDisplayedUser == null) {
            mDisplayedUser = (UserData) extras.getSerializable(ARGS_DISPLAYED_USER);
        }

        // 表示する日付
        if (mDisplayedDate == null) {
            mDisplayedDate = (Date) extras.getSerializable(ARGS_DISPLAYED_DATE);
        }

        // ユーザーリスト
        mUserList = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.lv_users);
        mListAdapter = new UserListAdapter(UserListActivity.this, 0, mUserList);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                UserData user = (UserData) listView.getItemAtPosition(position);

                // 今日の日付
                Calendar cal = Calendar.getInstance();
                final int year = cal.get(Calendar.YEAR);                 // 年
                final int monthOfYear = cal.get(Calendar.MONTH);         // 月
                final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);  // 日
                cal.clear();
                cal.set(year, monthOfYear, dayOfMonth);

                // スケジュール一覧へ移動
                Intent intent = CalendarActivity.createIntent(UserListActivity.this, mLoginUser, user, cal.getTime());
                startActivity(intent);
            }
        });

        // ユーザ 一覧表示
        new DisplayUserListTask(this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //アクションバーの戻るを押したときの処理
        if(id == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}