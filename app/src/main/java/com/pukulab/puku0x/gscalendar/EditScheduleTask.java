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

/**
 * スケジュール編集タスク
 */
public class EditScheduleTask extends AsyncTask<Schedule, Void, Schedule> {
    private final Context context;
    Exception exception;

    public EditScheduleTask(Context context) {
        this.context = context;
    }

    /**
     * params[0] = 編集元のスケジュール
     */
    @Override
    protected Schedule doInBackground(Schedule... params) {
        try {
            // 編集元スケジュール
            Schedule schedule = params[0];

            // 設定情報を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            // 文字列取得
            String user_name = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            LoginData login = new LoginData(user_name, password, server_url);

            // スケジュール編集を実行
            return new GroupSessionApi(login).editSchedule(schedule);

        } catch (Exception e) {
            exception = e;
        }
        return null;
    }
}
