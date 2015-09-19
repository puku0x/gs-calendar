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
 * 施設リスト取得タスク
 */
public class GetFacilityListTask extends AsyncTask<FacilityGroupData, Void, List<FacilityData>> {
    private final Context context;
    Exception exception;

    public GetFacilityListTask(Context context) {
        this.context = context;
    }

    @Override
    protected List<FacilityData> doInBackground(FacilityGroupData... params) {
        try {
            // ログイン情報を取得
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String user_name = sp.getString(SettingsActivity.PREFERENCE_USER_NAME, "");
            String password = sp.getString(SettingsActivity.PREFERENCE_PASSWORD, "");
            String server_url = sp.getString(SettingsActivity.PREFERENCE_SERVER_URL, "");
            LoginData login = new LoginData(user_name, password, server_url);

            // 施設リストを取得
            FacilityGroupData group = params[0];
            return new GroupSessionApi(login).getFacilityList(group.id);

        } catch (Exception e) {
            exception = e;
        }
        return null;
    }
}
