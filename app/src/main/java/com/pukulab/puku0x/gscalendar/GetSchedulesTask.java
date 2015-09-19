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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * スケジュール取得タスク
 */
public class GetSchedulesTask extends AsyncTask<Date, Void, List<ScheduleData>> {
    private final Context context;
    private UserData displayedUser;
    Exception exception;

    public GetSchedulesTask(Context context, UserData displayedUser) {
        this.context = context;
        this.displayedUser = displayedUser;
    }

    @Override
    protected List<ScheduleData> doInBackground(Date... params) {
        try {
            // カレンダー準備
            Calendar cal = Calendar.getInstance();
            if (params.length > 0 && params[0] != null) {
                cal.setTime(params[0]);
            }
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dayFrom = cal.getTime();

            // 終了日が設定されている
            if (params.length > 1 && params[1] != null) {
                cal.setTime(params[1]);
            }
//            else {
//                cal.add(Calendar.DATE, Calendar.DAY_OF_WEEK);
//            }
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date dayTo = cal.getTime();

            // ログイン情報を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String user_name = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            LoginData login = new LoginData(user_name, password, server_url);
            return new GroupSessionApi(login).getSchedule(displayedUser.usid, dayFrom, dayTo);

        } catch (Exception e) {
            exception = e;
        }
        return null;
    }
}
