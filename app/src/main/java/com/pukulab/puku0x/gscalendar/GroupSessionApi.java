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

import android.graphics.Color;
import android.text.Html;
import android.util.Base64;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * GroupSession WebAPI
 */
public class GroupSessionApi {
    private String address = null;
    private String user = null;
    private String password = null;


    public static final int max_schedule_num = 1000;
    public static final String[] title_items = { "既定", "来客", "出張", "勤怠", "その他"};
    public static final int title_colors[] = { Color.rgb(52, 152, 219),
                                                     Color.rgb(231, 76, 60),
                                                     Color.rgb(46, 204, 113),
                                                     Color.rgb(241, 196, 15),
                                                     Color.rgb(52, 73, 94)};

    public GroupSessionApi(LoginData login) {
        this.address = login.server_url;
        this.user = login.user;
        this.password = login.password;
    }

    /**
     *
     * @param usid      ユーザのID
     * @param dayFrom   開始日
     * @param dayTo     終了日
     * @return          開始日~終了日までのスケジュール
     */
    public List<ScheduleData> getSchedule(String usid, Date dayFrom, Date dayTo) {
        Date tmpFrom = (Date)dayFrom;
        List<ScheduleData> scheduleList = new ArrayList<ScheduleData>();

        while (true) {
            int count = 0;
            List<ScheduleData> tmpList = getSchedule_(usid, tmpFrom, dayTo);

            ScheduleData lastSchedule = null;
            for (ScheduleData s : tmpList) {
                if (s.scheduleList.size() > 0) {
                    count += s.scheduleList.size();
                    lastSchedule = s;
                }
            }

            // 取得可能な数以上登録されている場合は追加読み込み
            if (count < max_schedule_num)  {
                scheduleList.addAll(tmpList);
                break;
            }
            else {
                for (ScheduleData s : tmpList) {
                    if (s.date.before(lastSchedule.date)) {
                        scheduleList.add(s);
                    }
                }
                tmpFrom = (Date)lastSchedule.date;
            }
        }

        return scheduleList;
    }

    /**
     *
     * @param usid      ユーザのID
     * @param dayFrom   開始日
     * @param dayTo     終了日
     * @return          開始日~終了日までのスケジュール
     */
    private List<ScheduleData> getSchedule_(String usid, Date dayFrom, Date dayTo) {
        HttpURLConnection connection = null;
        List<ScheduleData> result = null;

        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat f2 = new SimpleDateFormat("yyyy/MM/dd HH:mm");

            // URLリクエスト
            URL url = new URL(address + "api/schedule/search.do?" + "usid=" + usid + "&startFrom=" + f.format(dayFrom) + "&startTo=" + f.format(dayTo) + "&endFrom=" + f.format(dayFrom) + "&endTo=" + f.format(dayTo) + "&sameInputFlg=1" + "&results=" + max_schedule_num);

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            Schedule schedule = null;
            int total_count = 0, user_count = 0, facility_count = 0;
            UserData user = null;
            FacilityData facility = null;
            String tag = "", value = "";
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                // 全スケジュール数
                                total_count = Integer.parseInt(parser.getAttributeValue(0));

                                // スケジュールデータを作る
                                result = new ArrayList<>();

                                // カレンダー初期化
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(dayFrom);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                // スケジュール情報の初期化
                                do {
                                    List<Schedule> scheduleList = new ArrayList<>();
                                    result.add(new ScheduleData(calendar.getTime(), scheduleList));
                                    calendar.add(Calendar.DATE, 1);
                                } while (!calendar.getTime().equals(dayTo));
                                break;
                            case "Result":
                                schedule = new Schedule();
                                break;
                            case "SameScheduleUserSet":
                                user_count = Integer.parseInt(parser.getAttributeValue(0));
                                if (user_count > 0) {
                                    schedule.sameScheduleUsers = new ArrayList<>(user_count);
                                }
                                break;
                            case "User":
                                user = new UserData();
                                break;
                            case "ReserveSet":
                                facility_count = Integer.parseInt(parser.getAttributeValue(0));
                                if (facility_count > 0) {
                                    schedule.reservedFacilities = new ArrayList<>(facility_count);
                                }
                                break;
                            case "Reserve":
                                facility = new FacilityData();
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            // ID
                            case "Schsid":
                                schedule.id = value;
                                break;
                            // タイトル
                            case "Title":
                                schedule.title = value;
                                break;
                            // 内容 (Naiyoってなんだよ...
                            case "Naiyo":
                                String strDetail = parser.getText();
                                if (strDetail != null) {
                                    CharSequence cs = Html.fromHtml(strDetail);
                                    schedule.detail = cs.toString();
                                }
                                break;
                            // 開始日時
                            case "StartDateTime":
                                schedule.start = f2.parse(parser.getText());
                                break;
                            // 終了日時
                            case "EndDateTime":
                                schedule.end = f2.parse(parser.getText());
                                break;
                            // フォント色 [0～4]
                            case "ColorKbn":
                                schedule.color = Integer.parseInt(parser.getText()) - 1;
                                break;
                            // ユーザID
                            case "UsrSid":
                                user.usid = value;
                                break;
                            // 施設ID
                            case "RsdSid":
                                facility.id = value;
                                break;
                            // 名前
                            case "Name":
                                if (user_count > 0) {
                                    user.name = value;
                                }
                                if (facility_count > 0) {
                                    facility.name = value;
                                }
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;

                            case "Result":
                                // 一致した日付にスケジュールを追加
                                Date d1 = f.parse(f.format(schedule.start));
                                for (ScheduleData s : result) {
                                    Date d2 = f.parse(f.format(s.date));
                                    if (d1.equals(d2)) {
                                        s.scheduleList.add(schedule);
                                    }
                                }
                                break;

                            case "SameScheduleUserSet":
                                user_count = 0;
                                break;

                            case "User":
                                schedule.sameScheduleUsers.add(user);
                                break;

                            case "ReserveSet":
                                facility_count = 0;
                                break;

                            case "Reserve":
                                schedule.reservedFacilities.add(facility);
                                break;
                        }
                        tag = "";
                        break;
                    default:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }

        return result;
    }

    /**
     * @param usid  ユーザID
     * @param day   スケジュールを取得する日
     * @return      指定された日のスケジュール
     */
    public List<Schedule> getSchedule(String usid, Date day) {
        List<ScheduleData> data = getSchedule(usid, day, day);
        return data.get(0).scheduleList;
    }

    private static void ignoreValidateCertification(
            HttpsURLConnection httpsconnection)
            throws NoSuchAlgorithmException, KeyManagementException {
        KeyManager[] km = null;
        TrustManager[] tm = { new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                    throws CertificateException {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };
        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(km, tm, new SecureRandom());
        httpsconnection.setSSLSocketFactory(sslcontext.getSocketFactory());
    }

    //
    public UserData getWhoami()  {

        //HttpsURLConnection connection = null;
        HttpURLConnection connection = null;
        UserData data = null;

        try {
            // URLリクエスト
            URL url = new URL(address + "api/user/whoami.do?");

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");
            int type = parser.getEventType();

            // ドキュメントの最後まで読み込み
            String usid = null;
            String sei = null;
            String mei = null;
            String number = null;
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    String name = parser.getName();

                    // ユーザID取得
                    if (name.equals("Usid")) {
                        type = parser.next();
                        usid = parser.getText();
                    }

                    // 姓
                    if (name.equals("NameSei")) {
                        type = parser.next();
                        sei = parser.getText();
                    }

                    // 名
                    if (name.equals("NameMei")) {
                        type = parser.next();
                        mei = parser.getText();
                    }

                    // 社員番号
                    if (name.equals("SyainNo")) {
                        type = parser.next();
                        number = parser.getText();
                    }
                }

                // 次の要素へ
                type = parser.next();
            }

            // データ代入(姓と名は半角スペースを挟んで繋ぐ)
            if (usid != null) {
                data = new UserData(usid, sei + " " + mei, number);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // さようなら
            if (connection != null) {
                connection.disconnect();
            }
        }

        return data;
    }

    //
    public List<UserData> getUserList(String groupID)  {

        HttpURLConnection connection = null;
        List<UserData> userList = new ArrayList<>();

        try {
            // URLリクエスト
            URL url = new URL(address + "api/user/belong.do?grpSid=" + groupID);

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");
            int type = parser.getEventType();

            // ドキュメントの最後まで読み込み
            while (type != XmlPullParser.END_DOCUMENT) {
                int tag_count = 0;
                if (type == XmlPullParser.START_TAG) {
                    tag_count++;

                    String name = parser.getName();
                    //System.out.println("name: " + name);

                    if (name.equals("Result")) {
                        String data_usid = null;
                        String data_name_sei = null;
                        String data_name_mei = null;
                        String data_number = null;

                        while (tag_count != 0) {
                            type = parser.next();
                            name = parser.getName();
                            //System.out.println("name: " + name);

                            if (type == XmlPullParser.START_TAG) {
                                tag_count++;

                                // ユーザID
                                if (name.equals("Usid") || name.equals("Usrsid")) {
                                    type = parser.next();
                                    data_usid = parser.getText();

                                }

                                // 姓
                                if (name.equals("NameSei") || name.equals("Usisei")) {
                                    type = parser.next();
                                    data_name_sei = parser.getText();
                                }

                                // 名
                                if (name.equals("NameMei") || name.equals("Usimei")) {
                                    type = parser.next();
                                    data_name_mei = parser.getText();
                                }

                                // 社員番号
                                if (name.equals("SyainNo")) {
                                    type = parser.next();
                                    data_number = parser.getText();
                                }
                            }

                            if (type == XmlPullParser.END_TAG) {
                                tag_count--;
                            }
                        }

                        // データ追加
                        UserData data = new UserData(data_usid, data_name_sei + " " + data_name_mei, data_number);
                        userList.add(data);
                    }
                }

                // 次の要素へ
                type = parser.next();
            }

        } catch (XmlPullParserException | IOException | NullPointerException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }

        return userList;
    }

    //
    public List<GroupData> getGroupList() throws Exception {

        HttpURLConnection connection = null;
        List<GroupData> result = null;

        try {
            // URLリクエスト
            URL url = new URL(address + "api/user/groupl.do?");

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            GroupData group = null;
            String tag = "", value = "";
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                // スケジュールデータを作る
                                result = new ArrayList<>();
                                break;
                            case "Result":
                                group = new GroupData();
                                break;

                        }
                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            // ID
                            case "Grpsid":
                                group.id = value;
                                break;

                            // グループ名
                            case "Grpname":
                                group.name = value;
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;

                            case "Result":
                                // データ追加
                                result.add(group);
                                break;
                        }
                        tag = "";
                        break;

                    default:
                        break;
                }
            }
        } catch (XmlPullParserException | NullPointerException | IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    /**
     * @param schedule 編集するスケジュール (id = -1で追加)
     * @return 編集後のスケジュール 失敗時はid=-1でエラーメッセージがdetailに保存される)
     */
    public Schedule editSchedule(final Schedule schedule)
    {
        Schedule result = null;
        HttpURLConnection connection = null;

        try {
            // URLリクエスト
            SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd%20HH:mm");
            String strURL = address + "api/schedule/edit.do?schSid=" + schedule.id + "&colorKbn=" + (schedule.color + 1) + "&from=" + f.format(schedule.start) + "&to=" + f.format(schedule.end);

            // タイトル
            if (schedule.title != null) {
                strURL += "&title=" + URLEncoder.encode(schedule.title, "UTF-8");
            }

            // 内容
            if (schedule.detail != null) {
                strURL += "&naiyo=" + URLEncoder.encode(schedule.detail, "UTF-8");
            }

            // 同じスケジュールが登録されたユーザ
            if (schedule.sameScheduleUsers != null) {
                for (UserData user : schedule.sameScheduleUsers) {
                    strURL += "&sameScheduledUser=" + user.usid;
                }
            }

            // 予約された施設
            if (schedule.reservedFacilities != null) {
                for (FacilityData facility : schedule.reservedFacilities) {
                    strURL += "&reserves=" + facility.id;
                }
            }

            // アクセス先URL
            URL url = new URL(strURL);

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

//            // 画面表示用
//            StringBuilder buf = new StringBuilder();
//            BufferedReader br = new BufferedReader(new InputStreamReader(is));
//            String line;
//            while ((line = br.readLine()) != null) {
//                buf.append(line);
//            }
//            System.out.println(buf);

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            String message = null;
            String tag = "", value = "";
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();

                        // nullだったら生成
                        if (result == null) {
                            result = new Schedule(schedule);
                        }

                        switch (tag) {
                            case "SchSid":
                                break;
                            case "Errors":
                                result.id = "-1";
                                message = "";
                                break;
                            case "Message":
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            // ID
                            case "SchSid":
                                result.id = value;
                                break;

                            // エラーメッセージ
                            case "Message":
                                message += parser.getText() + "\n";
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "SchSid":
                                break;
                            case "Errors":
                                result.detail = message;
                                break;
                            case "Message":
                                break;
                        }
                        tag = "";
                        break;

                    default:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }

        return result;
    }

    public Boolean deleteSchedule(Schedule schedule) throws Exception {
        Boolean result = null;
        HttpURLConnection connection = null;

        try {
            // URLリクエスト
            URL url = new URL(address + "api/schedule/delete.do?schSid=" + schedule.id);

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            //StringBuilder buf = new StringBuilder();
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");
            int type = parser.getEventType();

            // ドキュメントの最後まで読み込み
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if (name.equals("Result")) {
                        type = parser.next();
                        String text = parser.getText();
                        if (text.equals("OK")) {
                            result = Boolean.TRUE;
                        }
                        else {
                            result = Boolean.FALSE;
                        }
                    }
                }

                // 次の要素へ
                type = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }

        return result;
    }

    public List<FacilityData> getFacilityList(String groupId) {
        HttpURLConnection connection = null;
        List<FacilityData> result = new ArrayList<>();

        try {
            // URLリクエスト
            URL url = new URL(address + "api/reserve/list.do?" + "rsgSid=" + groupId);

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            FacilityData facility = null;
            String tag = "", value = "";
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;
                            case "Result":
                                facility = new FacilityData();
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            // ID
                            case "RsdSid":
                                facility.id = value;
                                break;
                            case "RsdName":
                                facility.name = value;
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;

                            case "Result":
                                result.add(facility);
                                break;
                        }
                        tag = "";
                        break;
                    default:
                        break;
                }
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }

    public List<FacilityGroupData> getFacilityGroupList() {
        HttpURLConnection connection = null;
        List<FacilityGroupData> result = new ArrayList<>();

        try {
            // URLリクエスト
            URL url = new URL(address + "api/reserve/group.do?");

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            FacilityGroupData group = null;
            String tag = "", value = "";
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;
                            case "Result":
                                group = new FacilityGroupData();
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            // ID
                            case "RsgSid":
                                group.id = value;
                                break;
                            case "RsgName":
                                group.name = value;
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();
                        switch (tag) {
                            case "ResultSet":
                                break;

                            case "Result":
                                result.add(group);
                                break;
                        }
                        tag = "";
                        break;
                    default:
                        break;
                }
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }
        return result;
    }

    //
    public List<PluginData> getPluginInfo() {
        // 結果
        List<PluginData> result = null;
        HttpURLConnection connection = null;

        try {
            // URLリクエスト
            URL url = new URL(address + "api/main/plugin.do?");

            // 接続 (Basic認証)
            connection = (HttpURLConnection) url.openConnection();
            String credentials = user + ":" + password;
            String base64EncodedCredentials = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", "Basic " + base64EncodedCredentials);
            connection.setRequestMethod("GET");
            connection.connect();

            // InputStream から String に変換
            InputStream is = new BufferedInputStream(connection.getInputStream());

            // XML読み込み
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            // タグ名と値
            String tag = "", value = "";
            PluginData plugin = null;
            for (int type = parser.getEventType(); type != XmlPullParser.END_DOCUMENT; type = parser.next()) {
                switch (type) {
                    case XmlPullParser.START_TAG: // 開始タグ
                        tag = parser.getName();

                        if (tag.equals("ResultSet")) {
                            result = new ArrayList<>();
                        } else if (tag.equals("Result")) {
                            plugin = new PluginData();
                        }

                        break;
                    case XmlPullParser.TEXT: // タグの内容
                        value = parser.getText();
                        switch (tag) {
                            case "Plgid":
                                plugin.setId(value);
                                break;
                            case "PlgName":
                                plugin.setName(value);
                                break;
                            case "PlgKbn":
                                plugin.setCategory(value);
                                break;
                        }
                        break;
                    case XmlPullParser.END_TAG: // 終了タグ
                        tag = parser.getName();

                        switch (tag) {
                            case "ResultSet":
                                for (PluginData p: result) {
                                    System.out.println("plugin:" + p.getName());
                                }
                                break;
                            case "Result":
                                result.add(plugin);
                                break;
                        }
                        tag = "";
                        break;

                    default:
                        break;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // さようなら
            connection.disconnect();
        }

        return result;
    }




//        // 画面表示用
//        StringBuilder buf = new StringBuilder();
//        BufferedReader br = new BufferedReader(new InputStreamReader(is));
//        String line;
//        while ((line = br.readLine()) != null) {
//            buf.append(line);
//        }
//        System.out.println(buf);

        //Persister persister = new Persister();
        //ResultSet r = persister.read(ResultSet.class, is);

//        for (Result res : r.getResult()) {
//            System.out.println("name = " + res.name);
//
//        }

//        for (PluginData.Plugin plugin : result.results) {
//            System.out.println("name = " + plugin.name);
//        }
//
//        return result;
//    }
//        // プラグイン情報
//        List<PluginData> pluginList = null;
//
//        // URLリクエスト
//        URL url = new URL(address + "api/main/plugin.do?");
//
//        // 接続
//        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//        //HttpsURLConnection connection = MyHttpsURLConnection.make(url);
//        //HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
//
//        // BASIC認証
////        Authenticator authenticator = new BasicAuthenticator(user, password);
////        Authenticator.setDefault(authenticator);
//        connection.setRequestMethod("POST");
//        connection.connect();
//
//        // レスポンス
//        //int responseCode = connection.getResponseCode();
//        //System.out.println("responseCode = " + responseCode);
//
//        // InputStream から String に変換
//        StringBuilder buf = new StringBuilder();
//        try {
//            InputStream is = new BufferedInputStream(connection.getInputStream());
//
////            // 画面表示用
////            BufferedReader br = new BufferedReader(new InputStreamReader(is));
////            String line;
////            while ((line = br.readLine()) != null) {
////                buf.append(line);
////            }
//
//            // XML読み込み
//            XmlPullParser parser = Xml.newPullParser();
//            parser.setInput(is, "UTF-8");
//            int type = parser.getEventType();
//
//            // ドキュメントの最後まで読み込み
//            int count = 0;
//            while (type != XmlPullParser.END_DOCUMENT) {
//                if (type == XmlPullParser.START_TAG) {
//                    String name = parser.getName();
//                    if (name.equals("Result")) {
//                        if (pluginList == null) {
//                            pluginList = new ArrayList<PluginData>();
//                        }
//
//                        // ID取得
//                        type = parser.next();
//                        name = parser.getName();
//                        type = parser.next();
//                        String data_id = parser.getText();
//
//                        // 名前取得
//                        type = parser.next();
//                        type = parser.next();
//                        name = parser.getName();
//                        type = parser.next();
//                        String data_name = parser.getText();
//
//                        // 有効かな？
//                        type = parser.next();
//                        type = parser.next();
//                        name = parser.getName();
//                        type = parser.next();
//                        boolean data_available = parser.getText().equals("1");
//
////                        // アイコンの読み込み
////                        URL iconURL = new URL(address + data_id + "/images/menu_icon_single.gif");
////                        HttpURLConnection connection_bitmap = (HttpURLConnection)iconURL.openConnection();
//                        Bitmap bitmap = null;
////                        try {
////                            bitmap = BitmapFactory.decodeStream(connection_bitmap.getInputStream());
////                        } catch (IOException e) {
////                            e.printStackTrace();    // アイコンが見つからない
////                        } finally {
////                            connection_bitmap.disconnect();
////                        }
//
//                        // データ追加
//                        PluginData data = new PluginData(data_id, data_name, data_available, bitmap);
//                        pluginList.add(data);
//
//                        // 表示用のバッファに追加
//                        //buf.append("ID: " + data.getID() + ", NAME: " + data.getName() + ", VALUE: " + data.getAvailable() + "\n");
//                    }
//                }
//
//                // 次の要素へ
//                type = parser.next();
//            }
//        } catch (XmlPullParserException e) {
//            e.printStackTrace();
//        } finally {
//            // さようなら
//            connection.disconnect();
//        }
//
//        return pluginList;
//    }
}
