/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.domain.common;

import java.util.Map;

public class MetadataWithPartOfIdentity extends Metadata {

    private final boolean partOfIdentity;

    public MetadataWithPartOfIdentity(boolean required, boolean secure, boolean partOfIdentity) {
        super(required, secure);
        this.partOfIdentity = partOfIdentity;
    }

    public boolean isPartOfIdentity() {
        return partOfIdentity;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();

        map.put("part_of_identity", isPartOfIdentity());

        return map;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MetadataWithPartOfIdentity that = (MetadataWithPartOfIdentity) o;

        return partOfIdentity == that.partOfIdentity;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (partOfIdentity ? 1 : 0);
        return result;
    }
}
