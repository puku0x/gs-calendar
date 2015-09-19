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
 *  スケジュール削除タスク
 */
public class DeleteScheduleTask extends AsyncTask<Schedule, Void, Boolean> {
    private final Context context;
    Exception exception;

    public DeleteScheduleTask(Context context) {
        this.context = context;
    }

    /**
     * params[0] = 編集元のスケジュール
     */
    @Override
    protected Boolean doInBackground(Schedule... params) {
        try {
            // 削除されるスケジュール
            Schedule schedule = params[0];

            // 設定情報を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            // ログイン情報取得
            String user_name = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            LoginData login = new LoginData(user_name, password, server_url);

            // 削除実行
            GroupSessionApi api = new GroupSessionApi(login);
            return api.deleteSchedule(schedule);
        } catch (Exception e) {
            exception = e;
        }
        return Boolean.FALSE;
    }
}
