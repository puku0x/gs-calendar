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

/**
 * ユーザ情報
 */
public class UserData implements Serializable {
    public String usid = null;
    public String name = null;
    public String number = null;

    public UserData() {
        this(null, null, null);
    }

    public UserData(String usid, String name, String number) {
        this.usid = usid;
        this.name = name;
        this.number = number;
    }

    public UserData(final UserData user) {
        this(user.usid, user.name, user.number);
    }

    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserData userData = (UserData) o;

        if (!usid.equals(userData.usid)) return false;
        return name.equals(userData.name);

    }
}