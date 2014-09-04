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

package com.thoughtworks.go.server.domain.oauth;

import java.util.Map;

import com.thoughtworks.go.domain.PersistentObject;
import org.apache.commons.lang.StringUtils;

/**
 * @understands baseclass for persistent objects required by oauth plugin
 */
public abstract class OauthDomainEntity<T> extends PersistentObject {
    private static final String PARAM_ID = "id";

    public abstract T getDTO();

    protected void setIdIfAvailable(Map attributes) {
        Object value = attributes.get(PARAM_ID);
        if (StringUtils.isNotBlank(String.valueOf(value))) {
            Long aLong = (Long) attributes.get(PARAM_ID);
            if (aLong != null && aLong > 0) {
                this.id = aLong;
            }
        }
    }

      @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OauthDomainEntity that = (OauthDomainEntity) o;

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
