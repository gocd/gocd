/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.user.representers;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;


public class UserRepresenter {

    public static Map<String, Object> toJSON(User user, RequestContext requestContext) {
        return UserSummaryRepresenter.getJsonWriter(user.getName(), requestContext)
                .add("display_name", user.getDisplayName())
                .add("enabled", user.isEnabled())
                .add("email", user.getEmail())
                .add("email_me", user.isEmailMe())
                .add("checkin_aliases", user.getMatchers())
                .getAsMap();
    }

}
