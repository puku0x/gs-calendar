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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.support.v4.view.ViewPager;
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
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vdesmet.lib.calendar.DayAdapter;
import com.vdesmet.lib.calendar.MultiCalendarView;
import com.vdesmet.lib.calendar.OnDayClickListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CalendarActivity extends AppCompatActivity implements OnDayClickListener {
    // プログレスバー
    ProgressBar mProgressBar;

    // ログイン中のユーザの情報
    UserData mLoginUser;

    // 表示するユーザの情報
    UserData mDisplayedUser;

    // 表示する日付
    //Date mDisplayedDate;

    // 表示する日付
    private Date mDisplayedDate, mLastDisplayedDate;
    private Date mStartDate, mEndDate;

    // 現在のカレンダー位置
    private int mViewPagerPosition = 0;

    // 他アクティビティに引数として渡すスケジュール情報
    Schedule mLastSchedule;

    // リストビュー用
    private List<ScheduleData> mScheduleDataList;       // 1月分
    private List<Schedule> mDailyScheduleList;
    private ListView mDailyScheduleListView;
    private ScheduleDataAdapter mDailyScheduleaListAdapter;
    private GetSchedulesTask mScheduleDataTask;

    // カレンダー
    private MultiCalendarView mCalendarView;
    private TextView mSelectedTextView;
    private Typeface mSelectedTypeface;

    // バックキー入力用
    private boolean mBackKeyPressed = false;
    private CountDownTimer mBackKeyTimer;
    private Toast mToast;

    // インテント引数
    private final static String ARGS_LOGIN_USER = "LOGIN_USER";
    private final static String ARGS_DISPLAYED_USER = "DISPLAYED_USER";
    private final static String ARGS_DISPLAYED_DATE = "DISPLAYED_DATE";

    /**
     *  インテントの生成
     */
    public static Intent createIntent(Context context, UserData loginUser, UserData displayedUser, Date displayedDate) {
        Intent intent = new Intent(context, CalendarActivity.class);
        intent.putExtra(ARGS_LOGIN_USER, loginUser);
        intent.putExtra(ARGS_DISPLAYED_USER, displayedUser);
        intent.putExtra(ARGS_DISPLAYED_DATE, displayedDate);
        return intent;
    }

    public class CustomDayAdapter implements DayAdapter {
        public CustomDayAdapter() {
        }

        /**
         *  カテゴリ色の設定
         */
        @Override
        public int[] getCategoryColors(final long dayInMillis) {
            //
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(dayInMillis);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date date = calendar.getTime();

            if (mScheduleDataList != null) {
               for (ScheduleData d : mScheduleDataList) {
                    if (d.date.equals(date)) {
                        //System.out.println("date = " + d.date);
                        List<Integer> colorList = new ArrayList<>();
                        for (Schedule s : d.scheduleList) {
                            int color = GroupSessionApi.title_colors[s.color];
                            if (!colorList.contains(color)) {
                                colorList.add(color);
                            }
                        }

                        // intの配列で返す
                        int[] intArray = new int[colorList.size()];
                        for (int i = 0; i < colorList.size(); i++) {
                            intArray[i] = colorList.get(i);
                        }
                        return intArray;
                    }
                }
            }

            return null;
        }

        /**
         *  有効な日付を設定
         */
        @Override
        public boolean isDayEnabled(final long dayInMillis) {
            return true;
        }

        /**
         *  テキストビューの更新
         */
        @Override
        public void updateTextView(final TextView dateTextView, final long dayInMillis) {
        }

        /**
         *  ヘッダの更新
         */
        @Override
        public void updateHeaderTextView(final TextView header, final int dayOfWeek) {
            switch(dayOfWeek) {
                case Calendar.SATURDAY:
                    header.setTextColor(Color.BLUE);
                    break;
                case Calendar.SUNDAY:
                    header.setTextColor(Color.RED);
                    break;
                default:
                    header.setTextColor(Color.BLACK);
                    break;
            }
        }
    }

    // 表示用
    private class DisplaySchedulesTask extends GetSchedulesTask {
        public DisplaySchedulesTask(Context context, UserData displayedUser, Date displayedDate) {
            super(context, displayedUser);
        }

        /**
         *  読み込み中
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        /**
         *  読み込み終了
         */
        @Override
        protected void onPostExecute(List<ScheduleData> data) {
            super.onPostExecute(data);
            mProgressBar.setVisibility(View.GONE);

            if (data != null) {
                // データ更新
                mScheduleDataList = data;

                // フリックで月移動するときにページャを
                mCalendarView.getIndicator().setOnPageChangeListener(null);
                mCalendarView.getIndicator().setCurrentItem(mViewPagerPosition);
                mCalendarView.getIndicator().setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {
                        // スケジュールリストのクリア
                        mDailyScheduleList.clear();
                        mDailyScheduleaListAdapter.notifyDataSetChanged();

                        // 1か月分読み込み
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis((mCalendarView.getFirstValidDay().getTimeInMillis()));
                        cal.add(Calendar.MONTH, position);
                        mStartDate = cal.getTime();
                        cal.add(Calendar.MONTH, 1);
                        mEndDate = cal.getTime();
                        mViewPagerPosition = position;
                        new DisplaySchedulesTask(CalendarActivity.this, mDisplayedUser, mDisplayedDate).execute(mStartDate, mEndDate);
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });

                try {
                    // スケジュールが複数の日付に設定された場合
                    for (final ScheduleData scheduleData : mScheduleDataList) {
                        for (final Schedule schedule : scheduleData.scheduleList) {
                            SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN);
                            Date start = f.parse(f.format(schedule.start));
                            Date end = f.parse(f.format(schedule.end));

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
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                // カテゴリのカラーインジケータ表示
                for (ScheduleData sd : mScheduleDataList) {
                    long dayInMillis = sd.date.getTime();
                    final LinearLayout categories = mCalendarView.getCategoryViewForDate(dayInMillis);

                    if (categories != null) {
                        // レイアウトをリセット
                        categories.removeAllViews();

                        // インジケータの色を設定
                        CustomDayAdapter tmpAdapter = new CustomDayAdapter();
                        int[] colors = tmpAdapter.getCategoryColors(dayInMillis);
                        for (final int color : colors) {
                            final LayoutInflater inflater = (LayoutInflater) CalendarActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            final View category = inflater.inflate(com.vdesmet.lib.calendar.R.layout.lib_calendar_category, categories, false);
                            category.setBackgroundColor(color);
                            categories.addView(category);
                        }
                    }
                }

                // 表示日付を設定してスケジュール一覧表示
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(mDisplayedDate);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                onDayClick(calendar.getTimeInMillis());
            }
            else {
                Toast.makeText(CalendarActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 表示用アダプタ
    public class ScheduleDataAdapter extends ArrayAdapter<Schedule> {
        private LayoutInflater layoutInflater;

        public ScheduleDataAdapter(Context context, int textViewResourceId, List<Schedule> objects) {
            super(context, textViewResourceId, objects);
            layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 特定の行(position)のデータを得る
            final Schedule schedule = (Schedule)getItem(position);
            convertView = layoutInflater.inflate(R.layout.daily_schedule_row, null);

            // タイトル
            TextView title = (TextView) convertView.findViewById(R.id.tv_schedule_title);
            title.setText(schedule.title);

            // 時間 (HH:mm)
            title = (TextView) convertView.findViewById(R.id.tv_schedule_time_start);
            title.setText(DateFormat.format(getString(R.string.date_hour_minute), schedule.start));
            title = (TextView) convertView.findViewById(R.id.tv_schedule_time_end);
            title.setText(DateFormat.format(getString(R.string.date_hour_minute), schedule.end));

            // スケジュール区分
            ImageView bar = (ImageView) convertView.findViewById(R.id.imageView);
            bar.setBackgroundColor(GroupSessionApi.title_colors[schedule.color]);

            // 同じスケジュールが登録されたユーザ
            String summary = "";
            TextView tv_same_schedule_users = (TextView) convertView.findViewById(R.id.tv_schedule_same_users);
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

            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

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

        // タイトル
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // 他のユーザのスケジュールを閲覧しているときはホームボタン有効
            if (mDisplayedUser.usid != null && !mDisplayedUser.usid.equals(mLoginUser.usid)) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // タイトルを設定
            actionBar.setTitle(mDisplayedUser.name);
            actionBar.setSubtitle(DateFormat.format(getString(R.string.date_year_month_day), mDisplayedDate));
        }

        // Retrieve the CalendarView
        mCalendarView = (MultiCalendarView) findViewById(R.id.calendarView);
        mProgressBar = (ProgressBar) findViewById(R.id.pb_detail);
        //mScheduleLayout = (LinearLayout) findViewById(R.id.scheduleList);

        // カレンダー下のリストビュー
        mScheduleDataList = new ArrayList<>();
        mDailyScheduleList = new ArrayList<>();
        mDailyScheduleaListAdapter = new ScheduleDataAdapter(CalendarActivity.this, 0, mDailyScheduleList);
        mDailyScheduleListView = (ListView)findViewById(R.id.lv_schedule);
        mDailyScheduleListView.setAdapter(mDailyScheduleaListAdapter);
        mDailyScheduleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                Schedule schedule = (Schedule) listView.getItemAtPosition(position);
                // 過去のスケジュールを保存
                mLastSchedule = new Schedule(schedule);

                // 詳細画面に移動
                Intent intent = DetailActivity.createIntent(CalendarActivity.this, mLoginUser, mDisplayedUser, schedule);
                int requestCode = DetailActivity.REQUEST_DETAIL;
                startActivityForResult(intent, requestCode);
            }
        });

        // Set the first valid day
        final Calendar firstValidDay = Calendar.getInstance();
        firstValidDay.add(Calendar.YEAR, -1);
        firstValidDay.set(Calendar.DAY_OF_MONTH, 1);
        firstValidDay.set(Calendar.HOUR_OF_DAY, 0);
        firstValidDay.set(Calendar.MINUTE, 0);
        firstValidDay.set(Calendar.SECOND, 0);
        firstValidDay.set(Calendar.MILLISECOND, 0);
        mCalendarView.setFirstValidDay(firstValidDay);

        // Set the last valid day
        final Calendar lastValidDay = Calendar.getInstance();
        lastValidDay.add(Calendar.YEAR, 1);
        lastValidDay.set(Calendar.HOUR_OF_DAY, 0);
        lastValidDay.set(Calendar.MINUTE, 0);
        lastValidDay.set(Calendar.SECOND, 0);
        lastValidDay.set(Calendar.MILLISECOND, 0);
        mCalendarView.setLastValidDay(lastValidDay);

        // 日~土
        mCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
        mCalendarView.setLastDayOfWeek(Calendar.SATURDAY);

        // Create adapter
        final CustomDayAdapter adapter = new CustomDayAdapter();

        // Set listener and adapter
        mCalendarView.setOnDayClickListener(this);
        mCalendarView.getIndicator().setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                //System.out.println("position:" + position);
                final Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((mCalendarView.getFirstValidDay().getTimeInMillis()));
                cal.add(Calendar.MONTH, position);
                mStartDate = cal.getTime();
                cal.add(Calendar.MONTH, 1);
                mEndDate = cal.getTime();
                mViewPagerPosition = position;
                new DisplaySchedulesTask(CalendarActivity.this, mDisplayedUser, mDisplayedDate).execute(mStartDate, mEndDate);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mCalendarView.setDayAdapter(adapter);

        // カレンダーの最初の表示位置を今月に合わせる
        mCalendarView.setViewPagerPosition(12);
    }

    @Override
    public void onDayClick(final long dayInMillis) {
        // Reset the previously selected TextView to his previous Typeface
        if (mSelectedTextView != null) {
            mSelectedTextView.setTypeface(mSelectedTypeface);
            mSelectedTextView.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        }

        final TextView day = mCalendarView.getTextViewForDate(dayInMillis);
        if (day != null) {
            // Remember the selected TextView and it's font
            mSelectedTypeface = day.getTypeface();
            mSelectedTextView = day;

            // Show the selected TextView as bold
            day.setTypeface(Typeface.DEFAULT_BOLD);
            day.setBackgroundColor(getResources().getColor(R.color.theme_color_transparent));
        }

        // 日付を取得
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dayInMillis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        mDisplayedDate = calendar.getTime();

        // サブタイトル更新
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setTitle(mDisplayedUser.name);
            actionBar.setSubtitle(DateFormat.format(getString(R.string.date_year_month_day), mDisplayedDate));
        }

        // スケジュールリストのレイアウトをリセット
        mDailyScheduleList.clear();

        for (ScheduleData d : mScheduleDataList) {
            if (d.date.equals(mDisplayedDate)) {
                // 日付ラベル
                //TextView tv_date = (TextView) findViewById(R.id.tv_schedule_date);
                //tv_date.setText(DateFormat.format(getString(R.string.date_month_day_week), d.date));
                // スケジュールが登録されてある
                if (!d.scheduleList.isEmpty()) {
                    mDailyScheduleList.addAll(d.scheduleList);
                }
            }
        }
        mDailyScheduleaListAdapter.notifyDataSetChanged();
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
                            new DisplaySchedulesTask(CalendarActivity.this, mDisplayedUser, mDisplayedDate).execute(mStartDate, mEndDate);
                        }
                    }
                }
                break;

            // 追加画面から
            case EditActivity.REQUEST_ADD_SCHEDULE:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // 編集後のスケジュールを取得
                        Bundle bundle = data.getExtras();
                        Schedule newSchedule = (Schedule) bundle.getSerializable(EditActivity.ARGS_NEW_SCHEDULE);
                        if (newSchedule != null) {

                        }

                        // 再描画
                        new DisplaySchedulesTask(CalendarActivity.this, mDisplayedUser, mDisplayedDate).execute(mStartDate, mEndDate);
                    }
                }
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_calendar, menu);

        // 他のユーザのスケジュールを閲覧する際は編集不可に
        if (mDisplayedUser.usid != null && !mDisplayedUser.usid.equals(mLoginUser.usid)) {
            // 追加ボタンを非表示に
            MenuItem menuAdd = menu.findItem(R.id.menu_add);
            menuAdd.setVisible(false);

            // 画面更新
            invalidateOptionsMenu();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // 設定
        if (id == R.id.action_settings) {
            Intent intent = SettingsActivity.createIntent(CalendarActivity.this);
            startActivity(intent);
            return true;
        }
        else if (id == R.id.action_today) {
            // カレンダーを今月に戻す
            mCalendarView.getIndicator().setCurrentItem(12);

            // 今日の日付のスケジュール一覧表示
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            onDayClick(calendar.getTimeInMillis());
        }
        // ユーザ一覧
        else if (id == R.id.action_users) {
            // 自分のスケジュールを見ているときはユーザ一覧画面に移動
            if (mDisplayedUser.usid.equals(mLoginUser.usid)) {
                Intent intent = UserListActivity.createIntent(CalendarActivity.this, mLoginUser, mDisplayedUser, mLastDisplayedDate);
                int requestCode = UserListActivity.REQUEST_GET_USER;
                startActivityForResult(intent, requestCode);
            }
            else {
                finish();
            }
            return true;
        }
        // 更新
        else if (id == R.id.menu_refresh) {
            // 再描画
            new DisplaySchedulesTask(CalendarActivity.this, mDisplayedUser, mDisplayedDate).execute(mStartDate, mEndDate);
            Toast.makeText(CalendarActivity.this, getString(R.string.refresh), Toast.LENGTH_SHORT).show();
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
            Intent intent = EditActivity.createIntent(CalendarActivity.this, mLoginUser, newSchedule);
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
                    mToast = Toast.makeText(CalendarActivity.this, getString(R.string.back_to_exit), Toast.LENGTH_SHORT);
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
