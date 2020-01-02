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
package com.thoughtworks.go.config;

import java.util.Map;

import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @understands MQL criteria for a mingle config
 */
@ConfigTag("mqlGroupingConditions")
public class MqlCriteria implements ParamsAttributeAware, Validatable {
    @ConfigValue
    private String mql;

    public static final String MQL = "mql";

    private final ConfigErrors configErrors = new ConfigErrors();

    public MqlCriteria() {
    }

    public MqlCriteria(String mql) {
        this.mql = mql;
    }

    @Override
    public void validate(ValidationContext validationContext) {
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MqlCriteria that = (MqlCriteria) o;

        if (mql != null ? !mql.equals(that.mql) : that.mql != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return mql != null ? mql.hashCode() : 0;
    }

    @Override public String toString() {
        return new ToStringBuilder(this).
                append("mql", mql).
                toString();
    }

    public String getMql() {
        return mql;
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        if (attributes == null) {
            return;
        }
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(MQL)) {
            mql = (String) attributesMap.get(MQL);
        }
    }

    public static boolean isEmpty(Object attributes) {
        return attributes == null || StringUtils.isEmpty((String) ((Map)attributes).get(MQL));
    }
}
