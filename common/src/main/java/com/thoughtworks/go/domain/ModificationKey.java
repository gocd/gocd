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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

public class ModificationKey {
    private String comment;

    private String user;

    private static final int HASH_SEED = 31;

    public ModificationKey(String comment, String user) {
        this.comment = comment;
        this.user = user;
    }

    public String getComment() {
        return comment;
    }

    public String getUser() {
        return user;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModificationKey other = (ModificationKey) o;
        return (StringUtils.equals(comment, other.getComment())) && StringUtils.equals(user, other.getUser());
    }

    public int hashCode() {
        int result;
        result = (comment != null ? comment.hashCode() : 0);
        result = HASH_SEED * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}