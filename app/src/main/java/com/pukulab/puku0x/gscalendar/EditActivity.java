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
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
 * スケジュール編集画面表示アクティビティ
 */
public class EditActivity extends AppCompatActivity {
    // プログレスバー
    ProgressBar mProgressBar;

    // スケジュールデータ
    Schedule mSchedule, mLastSchedule, mNewSchedule;

    // ログイン中のユーザの情報
    UserData mLoginUser;

    // 表示するユーザの情報
    UserData mDisplayedUser;

    // スケジュールタイトル
    EditText mTitleEditText;

    // 文字色
    TextView mCategoryTextView;
    ImageView mCategoryImageView;
    int mCategoryIndex = 0;

    // 開始・終了日時設定ボタン
    DatePickerDialog mStartDatePickerDialog, mEndDatePickerDialog;
    TimePickerDialog mStartTimePickerDialog, mEndTimePickerDialog;
    Button mStartDateButton, mStartTimeButton;
    Button mEndDateButton, mEndTimeButton;
    CheckBox mAlldayCheckBox;

    // スケジュールの同時登録
    List<UserData> mSameScheduleUserList;
    TextView mSameScheduleUsersTextView;

    // 予約された施設
    List<FacilityData> mReservedFacilitiyList;
    TextView mReservedFacilitiesTextView;

    // 内容
    EditText mDetailEditText;

    // インテント引数
    private final static String ARGS_LOGIN_USER = "LOGIN_USER";
    private final static String ARGS_SCHEDULE = "SCHEDULE";

    // インテント引数 (元画面に返す方)
    public final static int REQUEST_EDIT_SCHEDULE = 0x02;
    public final static int REQUEST_ADD_SCHEDULE = 0x04;
    public final static String ARGS_LAST_SCHEDULE = "LAST_SCHEDULE";
    public final static String ARGS_NEW_SCHEDULE = "NEW_SCHEDULE";

    // インテントの生成
    public static Intent createIntent(Context context, UserData loginUser, Schedule schedule) {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra(ARGS_LOGIN_USER, loginUser);
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
                LayoutInflater inflater = (LayoutInflater) EditActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.dialog_same_schedule_user, (ViewGroup) findViewById(R.id.alertdialog_layout));
                mDialogProgressBar = (ProgressBar)layout.findViewById(R.id.pb_same_schedule_users);

                // アダプタ設定
                ArrayAdapter<GroupData> groupListAdapter = new ArrayAdapter<>(EditActivity.this, android.R.layout.simple_spinner_item);
                groupListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // 元スケジュールのチェック状態をコピー
                mSameScheduleUserList = new ArrayList<>(mSchedule.sameScheduleUsers);

                // ユーザ一覧表示用のアダプタ
                final ArrayAdapter<UserData> userDataAdapter = new ArrayAdapter<>(EditActivity.this, android.R.layout.simple_list_item_multiple_choice);
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
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(EditActivity.this);
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
                        GetUserListTask task = new GetUserListTask(EditActivity.this) {
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
                AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
                builder.setTitle(getString(R.string.dialog_title_select_user));
                builder.setView(layout);
                builder.setPositiveButton(getString(R.string.ok), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // データ入れ替え
                        mSchedule.sameScheduleUsers = new ArrayList<>(mSameScheduleUserList);

                        // 表示テキストに反映
                        String users = "";
                        for (UserData user : mSchedule.sameScheduleUsers) {
                            if (!users.isEmpty()) users += ",  ";
                            users += user.name;
                        }
                        mSameScheduleUsersTextView.setText(users);
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
                LayoutInflater inflater = (LayoutInflater) EditActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                final View layout = inflater.inflate(R.layout.dialog_facility_reservation, (ViewGroup) findViewById(R.id.dialog_facility_reservation));
                mDialogProgressBar = (ProgressBar)layout.findViewById(R.id.pb_facility_reservation);

                // アダプタ設定
                ArrayAdapter<FacilityGroupData> groupListAdapter = new ArrayAdapter<>(EditActivity.this, android.R.layout.simple_spinner_item);
                groupListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // 元スケジュールのチェック状態をコピー
                mReservedFacilitiyList = new ArrayList<>(mSchedule.reservedFacilities);

                // 施設一覧表示用のアダプタ
                final ArrayAdapter<FacilityData> facilityDataAdapter = new ArrayAdapter<>(EditActivity.this, android.R.layout.simple_list_item_multiple_choice);
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
                            if (!mReservedFacilitiyList.contains(data)) {
                                mReservedFacilitiyList.add(data);
                            }
                        } else {
                            // 登録済みなら削除
                            if (mReservedFacilitiyList.contains(data)) {
                                mReservedFacilitiyList.remove(data);
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
                        GetFacilityListTask task = new GetFacilityListTask(EditActivity.this) {
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
                                    if (mReservedFacilitiyList.contains(facility)) {
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
                AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
                builder.setTitle(getString(R.string.dialog_title_select_facility));
                builder.setView(layout);
                builder.setPositiveButton(getString(R.string.ok), new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // データ入れ替え
                        mSchedule.reservedFacilities = new ArrayList<>(mReservedFacilitiyList);

                        // 表示テキストに反映
                        String facilities = "";
                        for (FacilityData facility : mSchedule.reservedFacilities) {
                            if (!facilities.isEmpty()) facilities += ",  ";
                            facilities += facility.name;
                        }
                        mReservedFacilitiesTextView.setTextColor(Color.BLACK);
                        mReservedFacilitiesTextView.setText(facilities);
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

    // 表示用
    private class SaveEditScheduleTask extends EditScheduleTask {
        public SaveEditScheduleTask(Context context) {
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
                Toast.makeText(EditActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            }
            else if (result.id.equals("-1")) {
                Toast.makeText(EditActivity.this, result.detail, Toast.LENGTH_SHORT).show();
                mReservedFacilitiesTextView.setTextColor(Color.RED);
            }
            else {
                // 元画面に編集後のスケジュール情報を返す
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable(ARGS_NEW_SCHEDULE, result);
                bundle.putSerializable(ARGS_LAST_SCHEDULE, mLastSchedule);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // 引数取得
        Bundle extras = getIntent().getExtras();

        // ログイン中のユーザ
        if (mLoginUser == null) {
            mLoginUser = (UserData) extras.getSerializable(ARGS_LOGIN_USER);
        }

        // 表示スケジュール
        if (mSchedule == null) {
            mLastSchedule = (Schedule) extras.getSerializable(ARGS_SCHEDULE);
            mSchedule = new Schedule(mLastSchedule);
            mNewSchedule = new Schedule();
        }

        // ビューの生成
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mTitleEditText = (EditText)findViewById(R.id.et_title);
        mCategoryTextView = (TextView) findViewById(R.id.tv_category);
        mCategoryImageView = (ImageView) findViewById(R.id.iv_category);
        mStartDateButton = (Button) findViewById(R.id.btn_start_date);
        mStartTimeButton = (Button) findViewById(R.id.btn_start_time);
        mEndDateButton = (Button) findViewById(R.id.btn_end_date);
        mEndTimeButton = (Button) findViewById(R.id.btn_end_time);
        mAlldayCheckBox = (CheckBox) findViewById(R.id.checkbox_allday);
        mSameScheduleUsersTextView = (TextView) findViewById(R.id.tv_edit_same_schedule_users);
        mReservedFacilitiesTextView = (TextView) findViewById(R.id.tv_edit_reserved_facilities);
        mDetailEditText = (EditText)findViewById(R.id.et_detail);

        //戻るボタンの追加
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // メニューのタイトル
        if (mSchedule.id.equals(Schedule.DEFAULT_SCHEDULE_ID)) {
            // タイトルを追加に変更
            actionBar.setTitle(R.string.title_activity_add);

            // 追加時は削除ボタンを見せない
            LinearLayout btn_delete = (LinearLayout)findViewById(R.id.btn_delete_schedule);
            btn_delete.setVisibility(View.GONE);
        }
        else {
            actionBar.setTitle(R.string.title_activity_edit);
        }

        // スケジュールのタイトル
        mTitleEditText.setText(mSchedule.title);

        // 予定区分 (タイトル文字色)
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(EditActivity.this);
        final String [] category_keys = {SettingsActivity.PREFERENCE_CATEGORY_BLUE, SettingsActivity.PREFERENCE_CATEGORY_RED, SettingsActivity.PREFERENCE_CATEGORY_GREEN, SettingsActivity.PREFERENCE_CATEGORY_YELLOW, SettingsActivity.PREFERENCE_CATEGORY_BLACK};
        final List<String> category_list = new ArrayList<>();
        for (int i = 0; i < category_keys.length; i++) {
            String category = sp.getString(category_keys[i], GroupSessionApi.title_items[i]);
            category_list.add(category);
        }
        final String [] categories =(String[])category_list.toArray(new String[0]);
        mCategoryTextView.setText(categories[mSchedule.color]);
        mCategoryImageView.setBackgroundColor(GroupSessionApi.title_colors[mSchedule.color]);
        RelativeLayout rl_category = (RelativeLayout)findViewById(R.id.rl_category);
        rl_category.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 初期値
                mCategoryIndex = mSchedule.color;

                // ラジオボタン付きのダイアログを生成
                new AlertDialog.Builder(EditActivity.this)
                        .setTitle(getString(R.string.dialog_title_select_category))
                        .setSingleChoiceItems(categories, mCategoryIndex,
                                new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog, int which) {
                                        mCategoryIndex = which;
                                    }
                                })
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mSchedule.color = mCategoryIndex;
                                mCategoryTextView.setText(categories[mCategoryIndex]);
                                mCategoryImageView.setBackgroundColor(GroupSessionApi.title_colors[mSchedule.color]);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                        .show();
            }
        });

        // 開始日時ボタンイベント
        mStartDateButton.setOnClickListener(new View.OnClickListener() {
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
                mStartDatePickerDialog = new DatePickerDialog(EditActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                        Calendar cal = Calendar.getInstance();
                        cal.clear();
                        cal.setTime(mSchedule.start);
                        cal.set(Calendar.YEAR, year);
                        cal.set(Calendar.MONTH, monthOfYear);
                        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        Date startDate = cal.getTime();

                        // 終日設定の場合
                        if (mAlldayCheckBox.isChecked()) {
                            // 終了日を同日に設定
                            cal.clear();
                            cal.setTime(mSchedule.end);
                            cal.set(Calendar.YEAR, year);
                            cal.set(Calendar.MONTH, monthOfYear);
                            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            mSchedule.end = cal.getTime();
                            mEndDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));
                            mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));

                            // 読み直し
                            cal.clear();
                            cal.setTime(startDate);
                        }
                        else {
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
                                mEndDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));

                                // 時間設定エラーのチェック(終了時間よりも後だった場合)
                                if (startDate.equals(mSchedule.end) || startDate.after(mSchedule.end)) {
                                    mSchedule.end = mSchedule.start;
                                    mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                                }

                                // 読み直し
                                cal.clear();
                                cal.setTime(startDate);
                            }
                        }

                        // ボタンのキャプションに反映
                        mSchedule.start = cal.getTime();
                        mStartDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));
                    }
                }, year, monthOfYear, dayOfMonth);
                mStartDatePickerDialog.setTitle(getString(R.string.dialog_title_select_date));
                mStartDatePickerDialog.show();
            }
        });

        // 開始時間設定ボタン
        mStartTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 現在の設定値を反映
                Calendar cal = Calendar.getInstance();
                cal.clear();
                cal.setTime(mSchedule.start);
                final int hour = cal.get(Calendar.HOUR_OF_DAY); // 時間
                final int minute = cal.get(Calendar.MINUTE);     // 分

                // 開始時間設定(HH:mm)ダイアログの作成・リスナの登録
                mStartTimePickerDialog = new TimePickerDialog(EditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // 5分間隔かチェック
                        int remainder = minute % 5;
                        if (remainder != 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.edit_error_interval_five_minutes), Toast.LENGTH_SHORT).show();
                            minute = ((minute - remainder) + 5 * Math.round(remainder / 5.0f));
                        }

                        // 開始時間を更新
                        Calendar cal = Calendar.getInstance();
                        cal.clear();
                        cal.setTime(mSchedule.start);
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);  // 時
                        cal.set(Calendar.MINUTE, minute);           // 分
                        mSchedule.start = cal.getTime();
                        mStartTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                        mStartTimeButton.setTextColor(Color.BLACK);

                        // 時間設定エラーのチェック(終了時間よりも後だった場合)
                        if (mSchedule.start.equals(mSchedule.end) || mSchedule.start.after(mSchedule.end)) {
                            mSchedule.end = mSchedule.start;
                            mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                            mEndTimeButton.setTextColor(Color.RED);
                        } else {
                            mStartTimeButton.setTextColor(Color.BLACK);
                            mEndTimeButton.setTextColor(Color.BLACK);
                        }
                    }
                }, hour, minute, true);
                mStartTimePickerDialog.setTitle(getString(R.string.dialog_title_select_time));
                mStartTimePickerDialog.show();
            }
        });

        // 終了日時ボタンイベント
        mEndDateButton.setOnClickListener(new View.OnClickListener() {
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
                mEndDatePickerDialog = new DatePickerDialog(EditActivity.this, new DatePickerDialog.OnDateSetListener() {
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
                            mStartDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));

                            // 時間設定エラーのチェック(開始時間よりも前だった場合)
                            if (endDate.equals(mSchedule.start) || endDate.before(mSchedule.start)) {
                                mSchedule.start = mSchedule.end;
                                mStartTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                            }

                            // 読み直し
                            cal.clear();
                            cal.setTime(endDate);
                        }

                        // ボタンのキャプションに反映
                        mSchedule.end = cal.getTime();
                        mEndDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));
                    }
                }, year, monthOfYear, dayOfMonth);
                mEndDatePickerDialog.setTitle(getString(R.string.dialog_title_select_date));
                mEndDatePickerDialog.show();
            }
        });

        // 終了時間設定ボタン
        mEndTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 現在の設定値を反映
                Calendar cal = Calendar.getInstance();
                cal.clear();
                cal.setTime(mSchedule.end);
                final int hour = cal.get(Calendar.HOUR_OF_DAY); // 時間
                final int minute = cal.get(Calendar.MINUTE);     // 分

                // 終了時間設定(HH:mm)ダイアログの作成・リスナの登録
                mEndTimePickerDialog = new TimePickerDialog(EditActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        // 5分間隔かチェック
                        int remainder = minute % 5;
                        if (remainder != 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.edit_error_interval_five_minutes), Toast.LENGTH_SHORT).show();
                            minute = ((minute - remainder) + 5 * Math.round(remainder / 5.0f));
                        }

                        // 終了時間を更新
                        Calendar cal = Calendar.getInstance();
                        cal.clear();
                        cal.setTime(mSchedule.end);
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);  // 時
                        cal.set(Calendar.MINUTE, minute);           // 分
                        mSchedule.end = cal.getTime();
                        mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                        mEndTimeButton.setTextColor(Color.BLACK);

                        // 時間設定エラーのチェック(開始時間よりも前だった場合)
                        if (mSchedule.end.equals(mSchedule.start) || mSchedule.end.before(mSchedule.start)) {
                            mSchedule.start = mSchedule.end;
                            mStartTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                            mStartTimeButton.setTextColor(Color.RED);
                        } else {
                            mStartTimeButton.setTextColor(Color.BLACK);
                            mEndTimeButton.setTextColor(Color.BLACK);
                        }
                    }
                }, hour, minute, true);
                mEndTimePickerDialog.setTitle(getString(R.string.dialog_title_select_time));
                mEndTimePickerDialog.show();
            }
        });

        // 終日チェックボックス
        mAlldayCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    // 編集不可に変更
                    mStartTimeButton.setEnabled(false);
                    mStartTimeButton.setTextColor(Color.LTGRAY);
                    mEndDateButton.setEnabled(false);
                    mEndTimeButton.setEnabled(false);
                    mEndTimeButton.setTextColor(Color.LTGRAY);

                    // 開始時間を00:00
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(mSchedule.start);
                    cal.set(Calendar.HOUR_OF_DAY, 0);   // 時
                    cal.set(Calendar.MINUTE, 0);         // 分
                    mSchedule.start = cal.getTime();
                    mSchedule.end = cal.getTime();      // 同じ日付にする

                    // 終了時間を00:00
                    cal.clear();
                    cal.setTime(mSchedule.start);
                    cal.set(Calendar.HOUR_OF_DAY, 23);   // 時
                    cal.set(Calendar.MINUTE, 59);         // 分
                    mSchedule.end = cal.getTime();

                    // 日付をボタンのテキストに反映
                    mStartDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));
                    mStartTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
                    mEndDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));
                    mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));
                } else {
                    mStartTimeButton.setEnabled(true);
                    mStartTimeButton.setTextColor(Color.BLACK);
                    mEndDateButton.setEnabled(true);
                    mEndTimeButton.setEnabled(true);
                    mEndTimeButton.setTextColor(Color.BLACK);
                }
            }
        });

        // 既に終日登録されているか?
        if (!mAlldayCheckBox.isChecked()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(mSchedule.start);
            final int start_year = cal.get(Calendar.YEAR);
            final int start_month = cal.get(Calendar.MONTH);
            final int start_day = cal.get(Calendar.DAY_OF_MONTH);
            final int start_hour = cal.get(Calendar.HOUR_OF_DAY);
            final int start_minute = cal.get(Calendar.MINUTE);
            cal.setTime(mSchedule.end);
            final int end_year = cal.get(Calendar.YEAR);
            final int end_month = cal.get(Calendar.MONTH);
            final int end_day = cal.get(Calendar.DAY_OF_MONTH);
            final int end_hour = cal.get(Calendar.HOUR_OF_DAY);
            final int end_minute = cal.get(Calendar.MINUTE);

            // 終日判定
            if (start_year == end_year && start_month == end_month && start_day == end_day) {
                if (start_hour == 0 && start_minute == 0 && end_hour == 23 && end_minute == 59) {
                    // 編集不可に変更
                    mStartTimeButton.setEnabled(false);
                    mEndDateButton.setEnabled(false);
                    mEndTimeButton.setEnabled(false);
                    mAlldayCheckBox.setChecked(true);
                }
            }
        }

        // 日付をボタンのテキストに反映
        mStartDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.start));
        mStartTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.start));
        mEndDateButton.setText(DateFormat.format(getString(R.string.date_year_month_day_week), mSchedule.end));
        mEndTimeButton.setText(DateFormat.format(getString(R.string.date_hour_minute), mSchedule.end));

        // 同じスケジュールが登録されたユーザ (ユーザ1, ユーザ2,ユーザ3, ...)
        String users = "";
        for (UserData user : mSchedule.sameScheduleUsers) {
            if (!users.isEmpty()) users += ",  ";
            users += user.name;
        }
        mSameScheduleUsersTextView.setText(users);
        mSameScheduleUsersTextView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                new DisplaySameScheduleUserListDialog(EditActivity.this).execute();
            }
        });

        // 予約された施設
        String facilities = "";
        for (FacilityData facility : mSchedule.reservedFacilities) {
            if (!facilities.isEmpty()) facilities += ",  ";
            facilities += facility.name;
        }
        mReservedFacilitiesTextView.setText(facilities);
        mReservedFacilitiesTextView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                new DisplayReservedFacilitiesListDialog(EditActivity.this).execute();
            }
        });

        // 内容
        if (!mSchedule.detail.isEmpty()) {
            mDetailEditText.setText(mSchedule.detail);
        }

        // 削除ボタン
        LinearLayout btn_delete = (LinearLayout)findViewById(R.id.btn_delete_schedule);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(EditActivity.this)
                        .setTitle(R.string.ask_delete)
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // 削除してダイアログを閉じる
                                DeleteScheduleTask task = new DeleteScheduleTask(EditActivity.this) {
                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        mProgressBar.setVisibility(View.VISIBLE);
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean result) {
                                        super.onPostExecute(result);
                                        mProgressBar.setVisibility(View.GONE);

                                        if (result != null) {
                                            // 元画面に移動
                                            Intent intent = new Intent();
                                            Bundle bundle = new Bundle();
                                            bundle.putSerializable(ARGS_NEW_SCHEDULE, new Schedule());
                                            bundle.putSerializable(ARGS_LAST_SCHEDULE, mSchedule);
                                            intent.putExtras(bundle);
                                            setResult(RESULT_FIRST_USER, intent);
                                            finish();
                                        }
                                        else {
                                            Toast.makeText(EditActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                };
                                task.execute(mSchedule);

                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // 保存ボタンを押したときの処理
        if (id == R.id.menu_save) {
            if (mSchedule.start.equals(mSchedule.end)) {
                Toast.makeText(EditActivity.this, R.string.edit_error_same_start_end, Toast.LENGTH_SHORT).show();
                mStartTimeButton.setTextColor(Color.RED);
                mEndTimeButton.setTextColor(Color.RED);
            }
            else if (mSchedule.start.after(mSchedule.end)) {
                Toast.makeText(EditActivity.this, R.string.edit_error_start_after_end, Toast.LENGTH_SHORT).show();
            }
            else if (mSchedule.end.before(mSchedule.start)) {
                Toast.makeText(EditActivity.this, R.string.edit_error_end_before_start, Toast.LENGTH_SHORT).show();
            }
            else {
                // タイトル
                mSchedule.title = mTitleEditText.getText().toString();

                // タイトルが空
                if (mSchedule.title.isEmpty()) {
                    Toast.makeText(EditActivity.this, R.string.edit_error_no_title, Toast.LENGTH_SHORT).show();
                    mTitleEditText.setHintTextColor(Color.RED);
                    return false;
                }

                // 内容
                mSchedule.detail = mDetailEditText.getText().toString();

                // 大丈夫そうなら保存実行
                new SaveEditScheduleTask(EditActivity.this).execute(mSchedule);
                return true;
            }
            return false;
        }
        //アクションバーの戻るを押したときの処理
        else if(id == android.R.id.home){
            // 何か変更があった
            mSchedule.title = mTitleEditText.getText().toString();
            mSchedule.detail = mDetailEditText.getText().toString();
            if (!mSchedule.equals(mLastSchedule)) {
                // 保存確認ダイアログ表示
                new AlertDialog.Builder(EditActivity.this)
                        .setTitle(R.string.ask_update)
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // 開始と終了時刻が同じ
                                if (mSchedule.start.equals(mSchedule.end)) {
                                    Toast.makeText(EditActivity.this, R.string.edit_error_same_start_end, Toast.LENGTH_SHORT).show();
                                    mStartTimeButton.setTextColor(Color.RED);
                                    mEndTimeButton.setTextColor(Color.RED);
                                }
                                else if (mSchedule.start.after(mSchedule.end)) {
                                    Toast.makeText(EditActivity.this, R.string.edit_error_start_after_end, Toast.LENGTH_SHORT).show();
                                }
                                else if (mSchedule.end.before(mSchedule.start)) {
                                    Toast.makeText(EditActivity.this, R.string.edit_error_end_before_start, Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    // タイトルが空
                                    if (mSchedule.title.isEmpty()) {
                                        Toast.makeText(EditActivity.this, R.string.edit_error_no_title, Toast.LENGTH_SHORT).show();
                                        mTitleEditText.setHintTextColor(Color.RED);
                                    }
                                    else {
                                        // 大丈夫そうなら保存実行
                                        new SaveEditScheduleTask(EditActivity.this).execute(mSchedule);
                                    }

                                    // ダイアログを閉じる
                                    dialog.cancel();
                                }
                            }
                        })
                        .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // ダイアログを閉じる
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // ダイアログを閉じる
                                dialog.cancel();

                                // キャンセルを渡す
                                Intent intent = new Intent();
                                setResult(RESULT_CANCELED, intent);
                                finish();
                            }
                        })
                        .show();
            }
            else {
                // キャンセルを渡す
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
