/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain;

import java.util.Comparator;
import java.io.Serializable;

public abstract class PersistentObject implements Serializable {

    public static final long NOT_PERSISTED = -1;

    protected long id = NOT_PERSISTED;

    public static final Comparator<PersistentObject> ORDER_DESCENDING_ID = (left, right) -> {
        long comparison = right.getId() - left.getId();
        if (comparison == 0) {
            return 0;
        }
        return comparison > 0 ? 1 : -1;
    };

    public long getId() {
        return id;
    }

    public final void setId(long id) {
        this.id = id;
    }

    public final boolean hasId() {
        return id != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PersistentObject that = (PersistentObject) o;

        if (id != that.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
