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
package com.thoughtworks.go.server.service;

import org.apache.commons.lang3.EnumUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

public enum AccessTokenFilter {
    active(Restrictions.eq("revoked", false)),
    all(),
    revoked(Restrictions.eq("revoked", true)),
    ;

    public static final Map<String, AccessTokenFilter> ALL_ENUMS = EnumUtils.getEnumMap(AccessTokenFilter.class);
    private final SimpleExpression[] expressions;

    AccessTokenFilter(SimpleExpression... expressions) {
        this.expressions = expressions;
    }

    public static AccessTokenFilter fromString(String filter) {
        if (isBlank(filter)) {
            return active;
        } else {
            return ALL_ENUMS.get(filter);
        }
    }

    public Criteria applyTo(Criteria criteria) {
        for (SimpleExpression expression : expressions) {
            criteria.add(expression);
        }
        return criteria;
    }
}
