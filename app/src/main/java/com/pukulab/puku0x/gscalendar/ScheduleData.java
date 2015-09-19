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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 1日スケジュールデータ
 * date         日付
 * dateString   日付 (yyyy/MM/dd)
 * scheduleList 1日(date)のスケジュールのリスト
 */
public class ScheduleData implements Serializable {
    public Date date = null;
    public String dateString = null;
    public List<Schedule> scheduleList = null;

    public ScheduleData(Date date, List<Schedule> scheduleList) {
        // 日付
        this.date = date;
        SimpleDateFormat f = new SimpleDateFormat("M月dd日 (E)");
        this.dateString = f.format(date);

        // スケジュールのリスト
        this.scheduleList = scheduleList;
    }
}
