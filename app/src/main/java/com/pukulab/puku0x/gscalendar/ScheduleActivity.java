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
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * スケジュール一覧表示アクティビティ
 */
public class ScheduleActivity extends AppCompatActivity {
    // ログイン中のユーザの情報
    UserData mLoginUser;

    // 表示するユーザの情報
    UserData mDisplayedUser;

    // 表示する日付
    private Date mDisplayedDate, mLastDisplayedDate;

    // 他アクティビティに引数として渡すスケジュール情報
    Schedule mLastSchedule;

    // リストビュー用
    private List<ScheduleData> mScheduleDataList;
    private ListView mScheduleDataListView;
    private ScheduleDataAdapter mScheduleDataAdapter;
    private GetSchedulesTask mScheduleDataTask;

    // バックキー入力用
    private boolean mBackKeyPressed = false;
    private CountDownTimer mBackKeyTimer;
    private Toast mToast;

    // インテント引数
    private final static String ARGS_LOGIN_USER = "LOGIN_USER";
    private final static String ARGS_DISPLAYED_USER = "DISPLAYED_USER";
    private final static String ARGS_DISPLAYED_DATE = "DISPLAYED_DATE";

    // インテントの生成
    public static Intent createIntent(Context context, UserData loginUser, UserData displayedUser, Date displayedDate) {
        Intent intent = new Intent(context, ScheduleActivity.class);
        intent.putExtra(ARGS_LOGIN_USER, loginUser);
        intent.putExtra(ARGS_DISPLAYED_USER, displayedUser);
        intent.putExtra(ARGS_DISPLAYED_DATE, displayedDate);
        return intent;
    }

    // 表示用アダプタ
    public class ScheduleDataAdapter extends ArrayAdapter<ScheduleData> {
        private LayoutInflater layoutInflater;

        public ScheduleDataAdapter(Context context, int textViewResourceId, List<ScheduleData> objects) {
            super(context, textViewResourceId, objects);
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 特定の行(position)のデータを得る
            final ScheduleData d = (ScheduleData)getItem(position);
            convertView = layoutInflater.inflate(R.layout.weekly_schedule_row, null);

            // 日付ラベル
            TextView tv_date = (TextView)convertView.findViewById(R.id.tv_schedule_date);
            tv_date.setText(DateFormat.format(getString(R.string.date_month_day_week), d.date));

            // スケジュールが登録されてある
            if (!d.scheduleList.isEmpty()) {
                // スケジュールの内容を描画
                for (final Schedule schedule : d.scheduleList) {
                    // タイトル
                    View row_daily = View.inflate(ScheduleActivity.this, R.layout.daily_schedule_row, null);
                    TextView title = (TextView) row_daily.findViewById(R.id.tv_schedule_title);
                    title.setText(schedule.title);
                    row_daily.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            // 過去のスケジュールを保存
                            mLastSchedule = new Schedule(schedule);

                            // 詳細画面に移動
                            Intent intent = DetailActivity.createIntent(ScheduleActivity.this, mLoginUser, mDisplayedUser, schedule);
                            int requestCode = DetailActivity.REQUEST_DETAIL;
                            startActivityForResult(intent, requestCode);
                        }
                    });

                    // 時間 (HH:mm)
                    title = (TextView) row_daily.findViewById(R.id.tv_schedule_time_start);
                    title.setText(DateFormat.format(getString(R.string.date_hour_minute), schedule.start));
                    title = (TextView) row_daily.findViewById(R.id.tv_schedule_time_end);
                    title.setText(DateFormat.format(getString(R.string.date_hour_minute), schedule.end));

                    // スケジュール区分
                    ImageView bar = (ImageView) row_daily.findViewById(R.id.imageView);
                    bar.setBackgroundColor(GroupSessionApi.title_colors[schedule.color]);

                    // 同じスケジュールが登録されたユーザ
                    String summary = "";
                    TextView tv_same_schedule_users = (TextView) row_daily.findViewById(R.id.tv_schedule_same_users);
                    if (!schedule.sameScheduleUsers.isEmpty()) {
                        for (UserData user : schedule.sameScheduleUsers) {
                            if (!summary.isEmpty()) summary += ",  ";
                            summary += user.name;
                        }
                        tv_same_schedule_users.setText(summary);
                    }

                    // 予約済みの施設
                    if (!schedule.reservedFacilities.isEmpty()) {
                        for (FacilityData facility : schedule.reservedFacilities) {
                            if (!summary.isEmpty()) summary += ",  ";
                            summary += facility.name;
                        }
                        tv_same_schedule_users.setText(summary);
                    }

                    // レイアウトに追加
                    LinearLayout layout_daily = (LinearLayout) convertView.findViewById(R.id.ll_daily_schedule);
                    layout_daily.addView(row_daily);
                }
            }
            // 予定なし
            else {
                TextView none = (TextView) convertView.findViewById(R.id.tv_schedule_none);
                none.setText(R.string.no_schedule);
                if (mDisplayedUser.usid.equals(mLoginUser.usid)) {
                    none.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            // 予定なしのラベルクリックで予定追加
                            Schedule newSchedule = new Schedule();
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(d.date);
                            cal.set(Calendar.HOUR_OF_DAY, Schedule.DEFAULT_START_HOUR);
                            cal.set(Calendar.MINUTE, Schedule.DEFAULT_START_MINUTE);
                            newSchedule.start = cal.getTime();
                            cal.set(Calendar.HOUR_OF_DAY, Schedule.DEFAULT_END_HOUR);
                            cal.set(Calendar.MINUTE, Schedule.DEFAULT_END_MINUTE);
                            newSchedule.end = cal.getTime();
                            Intent intent = EditActivity.createIntent(ScheduleActivity.this, mLoginUser, newSchedule);
                            int requestCode = EditActivity.REQUEST_ADD_SCHEDULE;
                            startActivityForResult(intent, requestCode);
                        }
                    });
                }
            }
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // 終了用のタイマ
        mBackKeyTimer = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                mBackKeyPressed = false;
            }
        };

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
            mLastDisplayedDate = mDisplayedDate;
        }

        // サブタイトルのフォーマット
        //mDisplayedDateFormat = new SimpleDateFormat("yyyy年 MM月dd日", Locale.JAPAN);

        //戻るボタンの追加
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 他のユーザのスケジュールを閲覧しているときはホームボタン有効
            if (!mDisplayedUser.usid.equals(mLoginUser.usid)) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // タイトルを設定
            actionBar.setTitle(mDisplayedUser.name);
            actionBar.setSubtitle(DateFormat.format(getString(R.string.date_year_month_day), mDisplayedDate));
        }

        // リストビュー
        mScheduleDataList = new ArrayList<>();
        mScheduleDataAdapter = new ScheduleDataAdapter(ScheduleActivity.this, 0, mScheduleDataList);
        mScheduleDataListView = (ListView)findViewById(R.id.lv_weekly_schedule);
        //mScheduleDataListView.addHeaderView(getLayoutInflater().inflate(R.layout.schdule_footer, null));
        mScheduleDataListView.addFooterView(getLayoutInflater().inflate(R.layout.schdule_footer, null));
        mScheduleDataListView.setAdapter(mScheduleDataAdapter);
        //mScheduleDataListView.setSelection(1);
        mScheduleDataListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //System.out.println("fv:" + firstVisibleItem + ", vc:" + visibleItemCount + " ic:" + totalItemCount);

                // タイトルバーの名前変更
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    if (mScheduleDataList.size() > 0) {
                        mDisplayedDate = mScheduleDataList.get(firstVisibleItem).date;
                        actionBar.setSubtitle(DateFormat.format(getString(R.string.date_year_month_day), mDisplayedDate));
                    }
                }

                if (totalItemCount <= firstVisibleItem + visibleItemCount + Calendar.DAY_OF_WEEK*2) {
//                if (totalItemCount <= firstVisibleItem + visibleItemCount) {
                    // 読み込み中ならスキップ
                    if (mScheduleDataTask != null && mScheduleDataTask.getStatus() == AsyncTask.Status.RUNNING) {
                        return;
                    }
                    // 追加読み込み
                    else {
                        mScheduleDataTask = new GetSchedulesTask(getApplicationContext(), mDisplayedUser) {
                            @Override
                            protected void onPostExecute(List<ScheduleData> data) {
                                super.onPostExecute(data);
                                if (data != null) {
                                    try {
                                        //int position = mScheduleDataListView.getFirstVisiblePosition();
                                        //int y = mScheduleDataListView.getChildAt(0).getTop();
                                        // データを追加して再描画
                                        mScheduleDataList.addAll(data);

                                        // スケジュールが複数の日付に設定された場合
                                        for (final ScheduleData scheduleData : mScheduleDataList) {
                                            for (final Schedule schedule : scheduleData.scheduleList) {
                                                SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN);
                                                Date start = f.parse(f.format(schedule.start));       // 開始日
                                                Date end = f.parse(f.format(schedule.end));           // 終了日

                                                // スケジュールが日をまたいでいる
                                                if (end.after(start)) {
                                                    // 他の日のスケジュールを見る
                                                    for (final ScheduleData scheduleData2 : mScheduleDataList) {
                                                        Date date = f.parse(f.format(scheduleData2.date));

                                                        // 開始日後~終了日以前にコピー
                                                        if (date.after(start) && (date.before(end) || date.equals(end))) {
                                                            // スケジュール未登録じゃね？
                                                            if (!scheduleData2.scheduleList.contains(schedule)) {
                                                                scheduleData2.scheduleList.add(schedule);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // データ更新を知らせる
                                        mScheduleDataAdapter.notifyDataSetChanged();
                                        mScheduleDataListView.invalidateViews();
                                        //mScheduleDataListView.setSelectionFromTop(position, y);
                                        //mScheduleDataListView.setSelection(7);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                }
                                else {
                                    Toast.makeText(ScheduleActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                                }
                            }

                        };

                        // 日付を更新してスケジュール取得
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(mLastDisplayedDate);
                        cal.add(Calendar.DATE, mScheduleDataList.size());
                        Date dayFrom = cal.getTime();
                        cal.add(Calendar.DATE, Calendar.DAY_OF_WEEK);
                        Date dayTo = cal.getTime();
                        mScheduleDataTask.execute(dayFrom, dayTo);
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_schedule, menu);

        // 他のユーザのスケジュールを閲覧する際は編集不可に
        if (!mDisplayedUser.usid.equals(mLoginUser.usid)) {
            // 追加ボタンを非表示に
            MenuItem menuAdd = menu.findItem(R.id.menu_add);
            menuAdd.setVisible(false);

            // 画面更新
            invalidateOptionsMenu();
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // 詳細画面から
            case DetailActivity.REQUEST_DETAIL:
                // 何か変更された
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // 編集後のスケジュールを取得
                        Bundle bundle = data.getExtras();
                        Schedule newSchedule = (Schedule) bundle.getSerializable(EditActivity.ARGS_NEW_SCHEDULE);

                        // スケジュールが更新されていればカレンダー再描画
                        if (!newSchedule.equals(mLastSchedule)) {
                            // 再描画
                            mScheduleDataAdapter.clear();
                            mScheduleDataAdapter.notifyDataSetChanged();
                        }
                    }
                }
                break;

            // 追加画面から
            case EditActivity.REQUEST_ADD_SCHEDULE:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // 再描画
                        mScheduleDataAdapter.clear();
                        mScheduleDataAdapter.notifyDataSetChanged();
                    }
                }
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // 設定
        if (id == R.id.action_settings) {
            Intent intent = SettingsActivity.createIntent(ScheduleActivity.this);
            startActivity(intent);
            return true;
        }
        // 更新
        else if (id == R.id.action_refresh) {
            // 再描画
            mScheduleDataAdapter.clear();
            mScheduleDataAdapter.notifyDataSetChanged();
            return true;
        }
        else if (id == R.id.action_today) {
            mScheduleDataListView.setSelection(0);
        }
        // カレンダー表示切り替え
        else if (id == R.id.action_calendar) {
            Intent intent = CalendarActivity.createIntent(ScheduleActivity.this, mLoginUser, mDisplayedUser, mLastDisplayedDate);
            startActivity(intent);
            finish();
        }
        // ユーザ一覧
        else if (id == R.id.action_users) {
            // 自分のスケジュールを見ているときはユーザ一覧画面に移動
            if (mDisplayedUser.usid.equals(mLoginUser.usid)) {
                Intent intent = UserListActivity.createIntent(ScheduleActivity.this, mLoginUser, mDisplayedUser, mLastDisplayedDate);
                int requestCode = UserListActivity.REQUEST_GET_USER;
                startActivityForResult(intent, requestCode);
            }
            else {
                finish();
            }
            return true;
        }
        // 追加
        else if (id == R.id.menu_add) {
            // 新規追加用スケジュール
            Schedule newSchedule = new Schedule();
            Calendar cal = Calendar.getInstance();
            cal.setTime(mDisplayedDate);
            cal.set(Calendar.HOUR_OF_DAY, Schedule.DEFAULT_START_HOUR);
            cal.set(Calendar.MINUTE, Schedule.DEFAULT_START_MINUTE);
            newSchedule.start = cal.getTime();
            cal.set(Calendar.HOUR_OF_DAY, Schedule.DEFAULT_END_HOUR);
            cal.set(Calendar.MINUTE, Schedule.DEFAULT_END_MINUTE);
            newSchedule.end = cal.getTime();

            // 追加画面に移動
            Intent intent = EditActivity.createIntent(ScheduleActivity.this, mLoginUser, newSchedule);
            int requestCode = EditActivity.REQUEST_ADD_SCHEDULE;
            startActivityForResult(intent, requestCode);
            return true;
        }
        //アクションバーの戻るを押したときの処理
        else if(id == android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Backボタン検知
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (mDisplayedUser.usid.equals(mLoginUser.usid)) {
                if (!mBackKeyPressed) {
                    // Timerを開始
                    //mBackKeyPressed.cancel(); // いらない？
                    mBackKeyTimer.start();

                    // 終了する場合, もう一度タップするようにメッセージを出力する
                    mToast = Toast.makeText(ScheduleActivity.this, getString(R.string.back_to_exit), Toast.LENGTH_SHORT);
                    mToast.show();
                    mBackKeyPressed = true;
                    return false;
                }
            }

            // pressed=trueの時、通常のBackボタンで終了処理.
            return super.dispatchKeyEvent(event);
        }

        // Backボタンに関わらないボタンが押された場合は、通常処理.
        return super.dispatchKeyEvent(event);
    }


    @Override
    protected void onDestroy() {
        // トーストが表示されていれば消す
        if (mToast != null) {
            mToast.cancel();
        }
        super.onDestroy();
    }
}