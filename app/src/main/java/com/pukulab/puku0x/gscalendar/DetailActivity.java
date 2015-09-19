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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * スケジュール詳細表示アクティビティ
 */
public class DetailActivity extends AppCompatActivity {
    // プログレスバー
    ProgressBar mProgressBar;

    // スケジュール (編集中のスケジュール, 元のスケジュール)
    Schedule mSchedule, mLastSchedule;

    // ログイン中のユーザの情報
    UserData mLoginUser;

    // 表示するユーザの情報
    UserData mDisplayedUser;

    // 表示する日付
    //Date mDisplayedDate;

    // タイトル
    TextView mTitleTextView;

    // 区分
    TextView mCategoryTextView;
    ImageView mCategoryImageView;
    int mCategoryIndex = 0;

    // 開始・終了日時設定ボタン
    DatePickerDialog mStartDatePickerDialog, mEndDatePickerDialog;
    TimePickerDialog mStartTimePickerDialog, mEndTimePickerDialog;
    TextView mStartDateTextView, mStartTimeTextView;
    TextView mEndDateTextView, mEndTimeTextView;

    // スケジュールの同時登録
    List<UserData> mSameScheduleUserList;
    TextView mSameScheduleUsersTextView;

    // 予約された施設
    List<FacilityData> mReservedFacilityList;
    TextView mReservedFacilitiesTextView;

    // 内容
    TextView mDetailTextView;

    // 保存ボタン
    MenuItem mSaveMenuItem;

    // インテント引数
    private final static String ARGS_SCHEDULE = "SCHEDULE";               // カレンダーから
    private final static String ARGS_LOGIN_USER = "LOGIN_USER";          //
    private final static String ARGS_DISPLAYED_USER = "DISPLAYED_USER"; //

    // インテント引数 (元画面に返す方)
    public final static int REQUEST_DETAIL = 0x01;
    public final static String ARGS_NEW_SCHEDULE = "NEW_SCHEDULE";
    //public final static String ARGS_LAST_SCHEDULE = "LAST_SCHEDULE";

    /**
     * インテントの生成
     */
    public static Intent createIntent(Context context, UserData loginUser, UserData displayedUser, Schedule schedule) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(ARGS_LOGIN_USER, loginUser);
        intent.putExtra(ARGS_DISPLAYED_USER, displayedUser);
        intent.putExtra(ARGS_SCHEDULE, schedule);
        return intent;
    }

    /**
     * 同時登録ユーザを表示するタスク
     */
    private class DisplaySameScheduleUserListDialog extends GetGroupListTask {
        private ProgressBar mDialogProgressBar;

        public DisplaySameScheduleUserListDialog(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<GroupData> data) {
            super.onPostExecute(data);
            mProgressBar.setVisibility(View.GONE);

            if (data != null) {
                LayoutInflater inflater = (LayoutInflater) DetailActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.dialog_same_schedule_user, (ViewGroup) findViewById(R.id.alertdialog_layout));
                mDialogProgressBar = (ProgressBar)layout.findViewById(R.id.pb_same_schedule_users);

                // アダプタ設定
                ArrayAdapter<GroupData> groupListAdapter = new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_spinner_item);
                groupListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // 元スケジュールのチェック状態をコピー
                mSameScheduleUserList = new ArrayList<>(mSchedule.sameScheduleUsers);

                // ユーザ一覧表示用のアダプタ
                final ArrayAdapter<UserData> userDataAdapter = new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_multiple_choice);
                final ListView lv_same_schedule_users = (ListView)layout.findViewById(R.id.lv_same_schedule_users);
                lv_same_schedule_users.setAdapter(userDataAdapter);
                lv_same_schedule_users.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ListView listView = (ListView) parent;
                        UserData data = userDataAdapter.getItem(position);
                        // チェックが入っている
                        if (listView.isItemChecked(position)) {
                            // 未登録なら追加
                            if (!mSameScheduleUserList.contains(data)) {
                                mSameScheduleUserList.add(data);
                            }
                        }
                        else {
                            // 登録済みなら削除
                            if (mSameScheduleUserList.contains(data)) {
                                mSameScheduleUserList.remove(data);
                            }
                        }
                    }
                });

                // アダプタに追加
                for (GroupData group: data) {
                    groupListAdapter.add(group);
                }

                // スピナーにアダプタを設定
                Spinner spinner = (Spinner) layout.findViewById(R.id.spinner_group);
                spinner.setAdapter(groupListAdapter);

                // 最後に選択したグループに切り替え
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
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
                        final GroupData group = (GroupData) spinner.getSelectedItem();

                        // 最後に選択したグループIDを保存
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(SettingsActivity.PREFERENCE_DEFAULT_USER_GROUP_ID, group.id);
                        editor.apply();

                        // ユーザの一覧を取得
                        GetUserListTask task = new GetUserListTask(DetailActivity.this) {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                mDialogProgressBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected void onPostExecute(List<UserData> result) {
                                super.onPostExecute(result);
                                mDialogProgressBar.setVisibility(View.GONE);

                                // 一旦チェックを全て外す
                                final int list_len = lv_same_schedule_users.getCount();
                                for (int i = 0; i < list_len; i++) {
                                    lv_same_schedule_users.setItemChecked(i, false);
                                }

                                // アダプタをリセット
                                userDataAdapter.clear();
                                for (final UserData user : result) {
                                    if (!user.usid.equals(mLoginUser.usid)) {
                                        userDataAdapter.add(user);
                                    }
                                }
                                userDataAdapter.notifyDataSetChanged();

                                // 登録済みのユーザにチェックを入れる
                                for (int i = 0; i < userDataAdapter.getCount(); i++) {
                                    UserData user = userDataAdapter.getItem(i);
                                    if (mSameScheduleUserList.contains(user)) {
                                        lv_same_schedule_users.setItemChecked(i, true);
                                    }
                                }

                                // 再描画
                                lv_same_schedule_users.invalidateViews();
                            }
                        };
                        task.execute(group);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                // ユーザ選択ダイアログ
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle(getString(R.string.dialog_title_select_user));
                builder.setView(layout);
                builder.setPositiveButton(getString(R.string.ok), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        TextView tv_same_schedule_users = (TextView) findViewById(R.id.tv_detail_same_schedule_users);

                        // データ入れ替え
                        mSchedule.sameScheduleUsers = new ArrayList<>(mSameScheduleUserList);

                        // 表示テキストに反映
                        String users = "";
                        for (UserData user : mSchedule.sameScheduleUsers) {
                            if (!users.isEmpty()) users += ",  ";
                            users += user.name;
                        }
                        tv_same_schedule_users.setText(users);

                        // 変更があれば保存ボタンを表示
                        if (!mSchedule.sameScheduleUsers.equals(mLastSchedule.sameScheduleUsers)) {
                            mSaveMenuItem.setVisible(true);
                        }
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 何もしない
                    }
                });

                // 表示
                builder.create().show();
            }
        }
    }

    /**
     * 予約施設を表示するタスク
     */
    private class DisplayReservedFacilitiesListDialog extends GetFacilityGroupListTask {
        private ProgressBar mDialogProgressBar;

        public DisplayReservedFacilitiesListDialog(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<FacilityGroupData> data) {
            super.onPostExecute(data);
            mProgressBar.setVisibility(View.GONE);

            if (data != null) {
                LayoutInflater inflater = (LayoutInflater) DetailActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.dialog_facility_reservation, (ViewGroup) findViewById(R.id.dialog_facility_reservation));
                mDialogProgressBar = (ProgressBar)layout.findViewById(R.id.pb_facility_reservation);

                // アダプタ設定
                ArrayAdapter<FacilityGroupData> groupListAdapter = new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_spinner_item);
                groupListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // 元スケジュールのチェック状態をコピー
                mReservedFacilityList = new ArrayList<>(mSchedule.reservedFacilities);

                // 施設覧表示用のアダプタ
                final ArrayAdapter<FacilityData> facilityDataAdapter = new ArrayAdapter<>(DetailActivity.this, android.R.layout.simple_list_item_multiple_choice);
                final ListView lv_facilities = (ListView)layout.findViewById(R.id.lv_facility_reservation);
                lv_facilities.setAdapter(facilityDataAdapter);
                lv_facilities.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ListView listView = (ListView) parent;
                        FacilityData data = facilityDataAdapter.getItem(position);
                        // チェックが入っている
                        if (listView.isItemChecked(position)) {
                            // 未登録なら追加
                            if (!mReservedFacilityList.contains(data)) {
                                mReservedFacilityList.add(data);
                            }
                        }
                        else {
                            // 登録済みなら削除
                            if (mReservedFacilityList.contains(data)) {
                                mReservedFacilityList.remove(data);
                            }
                        }
                    }
                });

                // アダプタに追加
                for (FacilityGroupData group: data) {
                    groupListAdapter.add(group);
                }

                // スピナーにアダプタを設定
                Spinner spinner = (Spinner) layout.findViewById(R.id.spinner_group);
                spinner.setAdapter(groupListAdapter);

                // 最後に選択したグループに切り替え
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String defaultGroup = sp.getString(SettingsActivity.PREFERENCE_DEFAULT_FACILITY_GROUP_ID, "");
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
                        final FacilityGroupData group = (FacilityGroupData) spinner.getSelectedItem();

                        // 最後に選択したグループIDを保存
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(SettingsActivity.PREFERENCE_DEFAULT_FACILITY_GROUP_ID, group.id);
                        editor.apply();

                        // ユーザの一覧を取得
                        GetFacilityListTask task = new GetFacilityListTask(DetailActivity.this) {
                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                mDialogProgressBar.setVisibility(View.VISIBLE);
                            }

                            @Override
                            protected void onPostExecute(List<FacilityData> result) {
                                super.onPostExecute(result);
                                mDialogProgressBar.setVisibility(View.GONE);

                                // 一旦チェックを全て外す
                                final int list_len = lv_facilities.getCount();
                                for (int i = 0; i < list_len; i++) {
                                    lv_facilities.setItemChecked(i, false);
                                }

                                // アダプタをリセット
                                facilityDataAdapter.clear();
                                for (final FacilityData facility : result) {
                                    facilityDataAdapter.add(facility);
                                }
                                facilityDataAdapter.notifyDataSetChanged();

                                // 登録済みのユーザにチェックを入れる
                                for (int i = 0; i < facilityDataAdapter.getCount(); i++) {
                                    FacilityData facility = facilityDataAdapter.getItem(i);
                                    if (mReservedFacilityList.contains(facility)) {
                                        lv_facilities.setItemChecked(i, true);
                                    }
                                }

                                // 再描画
                                lv_facilities.invalidateViews();
                            }
                        };
                        task.execute(group);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                // 施設選択ダイアログ
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setTitle(getString(R.string.dialog_title_select_facility));
                builder.setView(layout);
                builder.setPositiveButton(getString(R.string.ok), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // データ入れ替え
                        mSchedule.reservedFacilities = new ArrayList<>(mReservedFacilityList);

                        // 表示テキストに反映
                        String facilities = "";
                        for (FacilityData facility : mSchedule.reservedFacilities) {
                            if (!facilities.isEmpty()) facilities += ",  ";
                            facilities += facility.name;
                        }
                        mReservedFacilitiesTextView.setTextColor(Color.BLACK);
                        mReservedFacilitiesTextView.setText(facilities);

                        // 変更があれば保存ボタンを表示
                        if (!mSchedule.reservedFacilities.equals(mLastSchedule.reservedFacilities)) {
                            mSaveMenuItem.setVisible(true);
                        }
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // 何もしない
                    }
                });

                // 表示
                builder.create().show();
            }
        }
    }

    /**
     * スケジュールを保存するタスク
     */
    private class SaveScheduleTask extends EditScheduleTask {
        public SaveScheduleTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Schedule result) {
            super.onPostExecute(result);
            mProgressBar.setVisibility(View.GONE);

            if (result == null) {
                Toast.makeText(DetailActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            }
            else if (result.id.equals("-1")) {
                Toast.makeText(DetailActivity.this, result.detail, Toast.LENGTH_SHORT).show();
                mReservedFacilitiesTextView.setTextColor(Color.RED);
            }
            else {
                // 新しいスケジュール
                mSchedule = new Schedule(result);
                mLastSchedule = new Schedule(mSchedule);
            }
        }
    }

    // スケジュール(1件)表示
    private void showSchedule(boolean editable) {
        // スケジュールのタイトル
        mTitleTextView.setText(mSchedule.title);
        if (editable) {
            mTitleTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // テキスト入力を受け付けるビューを作成
                    final EditText editView = new EditText(DetailActivity.this);
                    editView.setText(mSchedule.title);
                    editView.setInputType(InputType.TYPE_CLASS_TEXT);

                    // テキストボックス付きのダイアログを生成
                    new AlertDialog.Builder(DetailActivity.this)
                            .setTitle(getString(R.string.dialog_title_input_title))
                            .setView(editView)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // 詳細画面に反映
                                    mSchedule.title = editView.getText().toString();
                                    mTitleTextView.setText(mSchedule.title);

                                    // 変更があれば保存ボタンを表示
                                    if (!mSchedule.title.equals(mLastSchedule.title)) {
                                        mSaveMenuItem.setVisible(true);
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
            });
        }


        // 予定区分 (タイトル文字色)
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
        final String [] category_keys = {SettingsActivity.PREFERENCE_CATEGORY_BLUE, SettingsActivity.PREFERENCE_CATEGORY_RED, SettingsActivity.PREFERENCE_CATEGORY_GREEN, SettingsActivity.PREFERENCE_CATEGORY_YELLOW, SettingsActivity.PREFERENCE_CATEGORY_BLACK};
        final List<String> category_list = new ArrayList<>();
        for (int i = 0; i < category_keys.length; i++) {
            String category = sp.getString(category_keys[i], GroupSessionApi.title_items[i]);
            category_list.add(category);
        }
        final String [] categories =(String[])category_list.toArray(new String[0]);
        mCategoryTextView.setText(categories[mSchedule.color]);
        mCategoryImageView.setBackgroundColor(GroupSessionApi.title_colors[mSchedule.color]);
        RelativeLayout layoutCategory = (RelativeLayout)findViewById(R.id.rl_category);
        if (editable) {
            layoutCategory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 初期値
                    mCategoryIndex = mSchedule.color;

                    // ラジオボタン付きのダイアログを生成
                    new AlertDialog.Builder(DetailActivity.this)
                            .setTitle(getString(R.string.dialog_title_select_category))
                            .setSingleChoiceItems(categories, mCategoryIndex,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            mCategoryIndex = which;
                                        }
                                    })
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mSchedule.color = mCategoryIndex;
                                    mCategoryTextView.setText(categories[mCategoryIndex]);
                                    mCategoryImageView.setBackgroundColor(GroupSessionApi.title_colors[mSchedule.color]);

                                    // 変更があれば保存ボタンを表示
                                    if (mSchedule.color != mLastSchedule.color) {
                                        mSaveMenuItem.setVisible(true);
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
            });
        }

        // 開始日時ボタンイベント
        if (editable) {
            mStartDateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 現在の設定値を反映
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(mSchedule.start);
                    final int year = cal.get(Calendar.YEAR);                 // 年
                    final int monthOfYear = cal.get(Calendar.MONTH);         // 月
                    final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);  // 日

                    // 開始日設定(yyyy:MM:dd)ダイアログの作成・リスナの登録
                    mStartDatePickerDialog = new DatePickerDialog(DetailActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(android.widget.DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                            Calendar cal = Calendar.getInstance();
                            cal.clear();
                            cal.setTime(mSchedule.start);
                            cal.set(Calendar.YEAR, year);
                            cal.set(Calendar.MONTH, monthOfYear);
                            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            Date startDate = cal.getTime();

                            // 日付設定エラーのチェック(終了日よりも後だった場合)
                            if (startDate.after(mSchedule.end)) {
                                // 終了日をずらす
                                cal.clear();
                                cal.setTime(mSchedule.end);
                                cal.set(Calendar.YEAR, year);
                                cal.set(Calendar.MONTH, monthOfYear);
                                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                mSchedule.end = cal.getTime();

                                // 日付表示の更新
                                mEndDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));

                                // 時間設定エラーのチェック(終了時間よりも後だった場合)
                                if (startDate.equals(mSchedule.end) || startDate.after(mSchedule.end)) {
                                    mSchedule.end = mSchedule.start;
                                    mEndTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                                }

                                // 読み直し
                                cal.clear();
                                cal.setTime(startDate);
                            }

                            // ボタンのキャプションに反映
                            mSchedule.start = cal.getTime();
                            mStartDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));

                            // 変更があれば保存ボタンを表示
                            if (!mSchedule.start.equals(mLastSchedule.start)) {
                                mSaveMenuItem.setVisible(true);
                            }
                        }
                    }, year, monthOfYear, dayOfMonth);
                    mStartDatePickerDialog.setTitle(getString(R.string.dialog_title_select_date));
                    mStartDatePickerDialog.show();
                }
            });
        }


        if (editable) {
            // 開始時間設定ボタン
            mStartTimeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 現在の設定値を反映
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(mSchedule.start);
                    final int hour = cal.get(Calendar.HOUR_OF_DAY); // 時間
                    final int minute = cal.get(Calendar.MINUTE);     // 分

                    // 開始時間設定(HH:mm)ダイアログの作成・リスナの登録
                    mStartTimePickerDialog = new TimePickerDialog(DetailActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            // 5分間隔かチェック
                            int remainder = minute % 5;
                            if (remainder != 0) {
                                Toast.makeText(getApplicationContext(), getString(R.string.edit_error_interval_five_minutes), Toast.LENGTH_SHORT).show();
                                minute = ((minute - remainder) + 5 * Math.round(remainder / 5.0f));
                                if (minute >= 55) minute = 55;
                            }

                            // 開始時間を更新
                            Calendar cal = Calendar.getInstance();
                            cal.clear();
                            cal.setTime(mSchedule.start);
                            cal.set(Calendar.MILLISECOND, 0);           //
                            cal.set(Calendar.SECOND, 0);           //
                            cal.set(Calendar.MINUTE, minute);           // 分
                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);  // 時
                            mSchedule.start = cal.getTime();
                            System.out.println("start:" + mSchedule.start);
                            mStartTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                            mStartTimeTextView.setTextColor(Color.BLACK);

                            // 時間設定エラーのチェック(終了時間よりも後だった場合)
                            if (mSchedule.start.equals(mSchedule.end) || mSchedule.start.after(mSchedule.end)) {
                                mSchedule.end = mSchedule.start;
                                mEndTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                                mEndTimeTextView.setTextColor(Color.RED);
                            } else {
                                mStartTimeTextView.setTextColor(Color.BLACK);
                                mEndTimeTextView.setTextColor(Color.BLACK);
                            }

                            // 変更があれば保存ボタンを表示
                            if (!mSchedule.start.equals(mLastSchedule.start)) {
                                mSaveMenuItem.setVisible(true);
                            }
                        }
                    }, hour, minute, true);
                    mStartTimePickerDialog.setTitle(getString(R.string.dialog_title_select_time));
                    mStartTimePickerDialog.show();
                }
            });
        }

        if (editable) {
            // 終了日時ボタンイベント
            mEndDateTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 現在の設定値を反映
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(mSchedule.end);
                    final int year = cal.get(Calendar.YEAR);                 // 年
                    final int monthOfYear = cal.get(Calendar.MONTH);         // 月
                    final int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);  // 日

                    // 終了日設定(yyyy:MM:dd)ダイアログの作成・リスナの登録
                    mEndDatePickerDialog = new DatePickerDialog(DetailActivity.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(android.widget.DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                            Calendar cal = Calendar.getInstance();
                            cal.clear();
                            cal.setTime(mSchedule.end);
                            cal.set(Calendar.YEAR, year);
                            cal.set(Calendar.MONTH, monthOfYear);
                            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            Date endDate = cal.getTime();

                            // 日付設定エラーのチェック(開始日よりも前だった場合)
                            if (endDate.before(mSchedule.start)) {
                                // 開始日をずらす
                                cal.setTime(mSchedule.start);
                                cal.set(Calendar.YEAR, year);
                                cal.set(Calendar.MONTH, monthOfYear);
                                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                mSchedule.start = cal.getTime();

                                // 日付表示の更新
                                mStartDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));

                                // 時間設定エラーのチェック(開始時間よりも前だった場合)
                                if (endDate.equals(mSchedule.start) || endDate.before(mSchedule.start)) {
                                    mSchedule.start = mSchedule.end;
                                    mStartTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                                }

                                // 読み直し
                                cal.clear();
                                cal.setTime(endDate);
                            }

                            // ボタンのキャプションに反映
                            mSchedule.end = cal.getTime();
                            mEndDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));

                            // 変更があれば保存ボタンを表示
                            if (!mSchedule.end.equals(mLastSchedule.end)) {
                                mSaveMenuItem.setVisible(true);
                            }
                        }
                    }, year, monthOfYear, dayOfMonth);
                    mEndDatePickerDialog.setTitle(getString(R.string.dialog_title_select_date));
                    mEndDatePickerDialog.show();
                }
            });
        }

        if (editable) {
            // 終了時間設定ボタン
            mEndTimeTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 現在の設定値を反映
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(mSchedule.end);
                    final int hour = cal.get(Calendar.HOUR_OF_DAY); // 時間
                    final int minute = cal.get(Calendar.MINUTE);     // 分

                    // 終了時間設定(HH:mm)ダイアログの作成・リスナの登録
                    mEndTimePickerDialog = new TimePickerDialog(DetailActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            // 5分間隔かチェック
                            int remainder = minute % 5;
                            if (remainder != 0) {
                                Toast.makeText(getApplicationContext(), getString(R.string.edit_error_interval_five_minutes), Toast.LENGTH_SHORT).show();
                                minute = ((minute - remainder) + 5 * Math.round(remainder / 5.0f));
                                if (minute >= 55) minute = 55;
                            }

                            // 終了時間を更新
                            Calendar cal = Calendar.getInstance();
                            cal.clear();
                            cal.setTime(mSchedule.end);
                            cal.set(Calendar.HOUR_OF_DAY, hourOfDay);  // 時
                            cal.set(Calendar.MINUTE, minute);           // 分
                            mSchedule.end = cal.getTime();
                            mEndTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                            mEndTimeTextView.setTextColor(Color.BLACK);

                            // 時間設定エラーのチェック(開始時間よりも前だった場合)
                            if (mSchedule.end.equals(mSchedule.start) || mSchedule.end.before(mSchedule.start)) {
                                mSchedule.start = mSchedule.end;
                                mStartTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                                mStartTimeTextView.setTextColor(Color.RED);
                            } else {
                                mStartTimeTextView.setTextColor(Color.BLACK);
                                mEndTimeTextView.setTextColor(Color.BLACK);
                            }

                            // 変更があれば保存ボタンを表示
                            if (!mSchedule.end.equals(mLastSchedule.end)) {
                                mSaveMenuItem.setVisible(true);
                            }
                        }
                    }, hour, minute, true);
                    mEndTimePickerDialog.setTitle(getString(R.string.dialog_title_select_time));
                    mEndTimePickerDialog.show();
                }
            });
        }

        // 日付をボタンのテキストに反映
        mStartDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));
        mStartTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
        mEndDateTextView.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));
        mEndTimeTextView.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));

        // 同じスケジュールが登録されたユーザ (ユーザ1, ユーザ2,ユーザ3, ...)
        String users = "";
        for (UserData user : mSchedule.sameScheduleUsers) {
            if (!users.isEmpty()) users += ",  ";
            users += user.name;
        }
        mSameScheduleUsersTextView.setText(users);
        if (editable) {
            mSameScheduleUsersTextView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    new DisplaySameScheduleUserListDialog(DetailActivity.this).execute();
                }
            });
        }

        // 予約された施設
        String facilities = "";
        for (FacilityData facility : mSchedule.reservedFacilities) {
            if (!facilities.isEmpty()) facilities += ",  ";
            facilities += facility.name;
        }
        mReservedFacilitiesTextView.setText(facilities);
        if (editable) {
            mReservedFacilitiesTextView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    new DisplayReservedFacilitiesListDialog(DetailActivity.this).execute();
                }
            });
        }

        // 内容
        mDetailTextView.setText(mSchedule.detail);
        if (editable) {
            mDetailTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //テキスト入力を受け付けるビューを作成します。
                    final EditText editView = new EditText(DetailActivity.this);
                    editView.setText(mSchedule.detail);
                    editView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                    new AlertDialog.Builder(DetailActivity.this)
                            .setTitle(getString(R.string.dialog_title_input_detail))
                            .setView(editView)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mSchedule.detail = editView.getText().toString();
                                    mDetailTextView.setText(mSchedule.detail);

                                    // 変更があれば保存ボタンを表示
                                    if (!mSchedule.detail.equals(mLastSchedule.detail)) {
                                        mSaveMenuItem.setVisible(true);
                                    }
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            })
                            .show();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

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

        // 表示スケジュール
        if (mSchedule == null) {
            mSchedule = (Schedule) extras.getSerializable(ARGS_SCHEDULE);
            mLastSchedule = new Schedule(mSchedule);
            //mNewSchedule = new Schedule();
        }

        //戻るボタンの追加
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(mDisplayedUser.name);

            if (mDisplayedUser.usid.equals(mLoginUser.usid)) {
                actionBar.setSubtitle(R.string.title_activity_detail);
            }
            else {
                actionBar.setSubtitle(getString(R.string.title_activity_detail) + " （※ 編集不可）");
            }
        }

        // ビューの生成
        mProgressBar = (ProgressBar) findViewById(R.id.pb_detail);
        mTitleTextView = (TextView) findViewById(R.id.tv_detail_title);
        mCategoryTextView = (TextView) findViewById(R.id.tv_category);
        mCategoryImageView = (ImageView) findViewById(R.id.iv_category);
        mStartDateTextView = (TextView) findViewById(R.id.tv_start_date);
        mStartTimeTextView = (TextView) findViewById(R.id.tv_start_time);
        mEndDateTextView = (TextView) findViewById(R.id.tv_end_date);
        mEndTimeTextView = (TextView) findViewById(R.id.tv_end_time);
        mSameScheduleUsersTextView = (TextView) findViewById(R.id.tv_detail_same_schedule_users);
        mReservedFacilitiesTextView = (TextView) findViewById(R.id.tv_detail_reserved_facilities);
        mDetailTextView = (TextView) findViewById(R.id.tv_detail_detail);

        // 表示 (表示ユーザとログインユーザが同じなら編集可能に)
        boolean editable = false;
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
        if (sp.getBoolean(SettingsActivity.PREFERENCE_QUICK_EDIT, false)) {
            editable = mDisplayedUser.usid.equals(mLoginUser.usid);
        }
        showSchedule(editable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);

        // 保存ボタン
        mSaveMenuItem = menu.findItem(R.id.menu_save);
        mSaveMenuItem.setVisible(false);

        // 他のユーザのスケジュールを閲覧する際は編集不可に
        if (!mDisplayedUser.usid.equals(mLoginUser.usid)) {
            // 追加ボタンを非表示に
            MenuItem itemEdit = menu.findItem(R.id.menu_edit);
            itemEdit.setVisible(false);

            // 画面更新
            invalidateOptionsMenu();
        }


        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EditActivity.REQUEST_EDIT_SCHEDULE:
                switch (resultCode) {
                    // 何か変更があった
                    case RESULT_OK:
                        if (data != null) {
                            // 保存ボタンを非表示に戻す
                            mSaveMenuItem.setVisible(false);

                            // 編集後のスケジュールを取得
                            Bundle bundle = data.getExtras();
                            mSchedule = (Schedule) bundle.getSerializable(EditActivity.ARGS_NEW_SCHEDULE);
                            mLastSchedule = new Schedule(mSchedule);

                            // 再描画 (表示ユーザとログインユーザが同じなら編集可能に)
                            boolean editable = false;
                            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(DetailActivity.this);
                            if (sp.getBoolean(SettingsActivity.PREFERENCE_QUICK_EDIT, false)) {
                                editable = mDisplayedUser.usid.equals(mLoginUser.usid);
                            }
                            showSchedule(editable);
                        }
                        break;

                    // キャンセルが実行された
                    case RESULT_CANCELED:
                        break;

                    // その他(削除が実行された)
                    default:
                        // そのまま元画面に空のスケジュールを返す
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable(ARGS_NEW_SCHEDULE, new Schedule());
                        intent.putExtras(bundle);
                        setResult(RESULT_OK, intent);
                        finish();
                        break;
                }
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // 設定
        if (id == R.id.menu_edit) {
            Intent intent = EditActivity.createIntent(DetailActivity.this, mLoginUser, mSchedule);
            int requestCode = EditActivity.REQUEST_EDIT_SCHEDULE;
            startActivityForResult(intent, requestCode);
            return true;
        }
        else if (id == R.id.menu_save) {
            // 開始と終了時刻が同じ
            if (mSchedule.start.equals(mSchedule.end)) {
                Toast.makeText(DetailActivity.this, R.string.edit_error_same_start_end, Toast.LENGTH_SHORT).show();
                mStartTimeTextView.setTextColor(Color.RED);
                mEndTimeTextView.setTextColor(Color.RED);
            }
            else if (mSchedule.start.after(mSchedule.end)) {
                Toast.makeText(DetailActivity.this, R.string.edit_error_start_after_end, Toast.LENGTH_SHORT).show();
            }
            else if (mSchedule.end.before(mSchedule.start)) {
                Toast.makeText(DetailActivity.this, R.string.edit_error_end_before_start, Toast.LENGTH_SHORT).show();
            }
            else {
                // タイトルが空
                if (mSchedule.title.isEmpty()) {
                    Toast.makeText(DetailActivity.this, R.string.edit_error_no_title, Toast.LENGTH_SHORT).show();
                    mTitleTextView.setHintTextColor(Color.RED);
                }
                else {
                    // 保存実行
                    SaveScheduleTask task = new SaveScheduleTask(DetailActivity.this) {
                        @Override
                        protected void onPostExecute(Schedule result) {
                            super.onPostExecute(result);
                            if (result != null && !result.id.equals("-1")) {
                                // 保存ボタンを非表示に戻す
                                mSaveMenuItem.setVisible(false);
                            }
                        }
                    };
                    task.execute(mSchedule);
                }
            }
        }
        // 戻る
        else if (id == android.R.id.home) {
            // 何か変更があった
            if (!mSchedule.equals(mLastSchedule)) {
                // 保存確認ダイアログ表示
                new AlertDialog.Builder(DetailActivity.this)
                        .setTitle(R.string.ask_update)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // 開始と終了時刻が同じ
                                if (mSchedule.start.equals(mSchedule.end)) {
                                    Toast.makeText(DetailActivity.this, R.string.edit_error_same_start_end, Toast.LENGTH_SHORT).show();
                                    mStartTimeTextView.setTextColor(Color.RED);
                                    mEndTimeTextView.setTextColor(Color.RED);
                                }
                                else if (mSchedule.start.after(mSchedule.end)) {
                                    Toast.makeText(DetailActivity.this, R.string.edit_error_start_after_end, Toast.LENGTH_SHORT).show();
                                }
                                else if (mSchedule.end.before(mSchedule.start)) {
                                    Toast.makeText(DetailActivity.this, R.string.edit_error_end_before_start, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    // タイトルが空
                                    if (mSchedule.title.isEmpty()) {
                                        Toast.makeText(DetailActivity.this, R.string.edit_error_no_title, Toast.LENGTH_SHORT).show();
                                        mTitleTextView.setHintTextColor(Color.RED);
                                    }
                                    else {
                                        // 保存実行
                                        SaveScheduleTask task = new SaveScheduleTask(DetailActivity.this) {
                                            @Override
                                            protected void onPostExecute(Schedule result) {
                                                super.onPostExecute(result);

                                                if (result != null && !result.id.equals("-1")) {
                                                    // 元画面に編集後のスケジュール情報を返す
                                                    Intent intent = new Intent();
                                                    Bundle bundle = new Bundle();
                                                    bundle.putSerializable(ARGS_NEW_SCHEDULE, result);
                                                    intent.putExtras(bundle);
                                                    setResult(RESULT_OK, intent);
                                                    finish();
                                                }
                                            }
                                        };
                                        task.execute(mSchedule);
                                    }

                                    // ダイアログを閉じる
                                    dialog.cancel();
                                }
                            }
                        })
                        .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // キャンセルを渡す
                                Intent intent = new Intent();
                                setResult(RESULT_CANCELED, intent);
                                finish();
                            }
                        })
                        .show();
            }
            else {
                // 元画面に編集後のスケジュール情報を返す
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable(ARGS_NEW_SCHEDULE, mSchedule);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
