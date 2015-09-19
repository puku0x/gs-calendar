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
 * 施設情報
 */
public class FacilityData implements Serializable {
    public String id = null;
    public String name = null;

    public FacilityData() {
        this(null, null);
    }

    public FacilityData(final FacilityData facility) {
        this(facility.id, facility.name);
    }

    public FacilityData(String id, String name) {
        this.id = (id != null) ? id : "";
        this.name = (name != null) ? name : "";
    }

    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FacilityData userData = (FacilityData) o;

        if (!id.equals(userData.id)) return false;
        return name.equals(userData.name);

    }
}