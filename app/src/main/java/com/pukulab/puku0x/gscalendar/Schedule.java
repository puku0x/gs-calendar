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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * スケジュール
 * id     スケジュールID(編集に必要)
 * title  タイトル
 * detail 内容
 * start 開始日時刻
 * end 終了日時刻
 *
 */
public class Schedule implements Serializable {
    public final static String DEFAULT_SCHEDULE_ID = "-1";
    public final static int DEFAULT_START_HOUR = 9;
    public final static int DEFAULT_START_MINUTE = 0;
    public final static int DEFAULT_END_HOUR = 18;
    public final static int DEFAULT_END_MINUTE = 0;

    // メンバ変数
    public String id;
    public String title;
    public String detail;
    public Date   start;
    public Date   end;
    public int   color;
    public List<UserData> sameScheduleUsers;
    public List<FacilityData> reservedFacilities;

    // デフォルトコンストラクタ
    public Schedule() {
        this(null, null, null, null, null, 0, null, null);
    }

    // コピーコンストラクタ
    public Schedule(final Schedule s) {
        this(s.id, s.title, s.detail, s.start, s.end, s.color, s.sameScheduleUsers, s.reservedFacilities);
    }

    // コピーコンストラクタ
    public Schedule(String id, String title, String detail, Date startDateTime, Date endDateTime, int color, List<UserData> sameScheduleUsers, List<FacilityData> reservedFacilities) {
        super();

        // スケジュールID
        this.id = (id != null) ? id : DEFAULT_SCHEDULE_ID;

        //　タイトル
        this.title = (title != null) ? title : "";

        // 内容
        this.detail = (detail != null) ? detail : "";

        // 日付
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.HOUR_OF_DAY,  DEFAULT_START_HOUR);  // 時
        cal.set(Calendar.MINUTE, DEFAULT_START_MINUTE);      // 分
        this.start = (startDateTime != null) ? startDateTime : cal.getTime();
        cal.set(Calendar.HOUR_OF_DAY, DEFAULT_END_HOUR);
        cal.set(Calendar.MINUTE, DEFAULT_END_MINUTE);
        this.end = (endDateTime != null) ? endDateTime : cal.getTime();

        // 色
        this.color = color;

        // 同じスケジュールを登録されたユーザたち
        if (sameScheduleUsers == null) {
            this.sameScheduleUsers = new ArrayList<>();
        }
        else {
            this.sameScheduleUsers = sameScheduleUsers;
        }

        // 予約された施設たち
        if (reservedFacilities == null) {
            this.reservedFacilities = new ArrayList<>();
        }
        else {
            this.reservedFacilities = reservedFacilities;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Schedule schedule = (Schedule) o;

        if (color != schedule.color) return false;
        if (!id.equals(schedule.id)) return false;
        if (!title.equals(schedule.title)) return false;
        if (!detail.equals(schedule.detail)) return false;
        if (!start.equals(schedule.start)) return false;
        if (!end.equals(schedule.end)) return false;
        if (!sameScheduleUsers.equals(schedule.sameScheduleUsers)) return false;
        return reservedFacilities.equals(schedule.reservedFacilities);

    }
}
