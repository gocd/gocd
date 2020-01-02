/*
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public abstract class PersistentObject implements Serializable {

    public static final long NOT_PERSISTED = -1;

    protected long id = NOT_PERSISTED;

    public static final Comparator ORDER_DESCENDING_ID = (o1, o2) -> {
        PersistentObject po1 = (PersistentObject) o1;
        PersistentObject po2 = (PersistentObject) o2;
        long comparison = po2.getId() - po1.getId();
        if (comparison==0) return 0;
        return comparison > 0? 1: -1;
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

    public boolean persisted() {
        return hasId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersistentObject)) return false;
        PersistentObject that = (PersistentObject) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
