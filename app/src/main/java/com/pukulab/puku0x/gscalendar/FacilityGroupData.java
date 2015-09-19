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

import java.util.ArrayList;
import java.util.List;

/**
 * 施設グループ情報
 * id     グループSID
 * name   グループ名
 * facilities    グループに属する施設
 */
public class FacilityGroupData {
    public String id;
    public String name;
    public List<FacilityData> facilities;

    FacilityGroupData() {
        this(null, null, null);
    }
    public FacilityGroupData(String id, String name, List<FacilityData> facilities) {
        // グループID
        this.id = (id != null) ? id : "";

        // グループ名
        this.name = (name != null) ? name : "";

        // 施設リスト
        if (facilities != null) {
            this.facilities = facilities;
        }
        else {
            this.facilities = new ArrayList<>();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
