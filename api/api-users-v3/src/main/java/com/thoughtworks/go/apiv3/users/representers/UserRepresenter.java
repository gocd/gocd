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
package com.thoughtworks.go.apiv3.users.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv3.users.model.UserToRepresent;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.List;

public class UserRepresenter {
    public static void represent(OutputWriter childWriter, UserToRepresent user) {
        childWriter.add("login_name", user.getUsername().getUsername().toString())
                .add("display_name", user.getDisplayName())
                .add("enabled", user.isEnabled())
                .add("email", user.getEmail())
                .add("email_me", user.isEmailMe())
                .add("is_admin", user.isAdmin())
                .addChildList("roles", listWriter -> user.getRoles().forEach(role -> listWriter.addChild(propertyWriter -> RoleRepresenter.toJSON(propertyWriter, role))))
                .addChildList("checkin_aliases", user.getMatchers());
    }

    public static void toJSON(OutputWriter writer, UserToRepresent user) {
        writer.addLinks(linksWriter -> linksWriter
                .addLink("self", String.format("%s/%s", Routes.Users.BASE, user.getUsername().getUsername().toString()))
                .addLink("find", String.format("%s%s", Routes.Users.BASE, Routes.Users.USER_NAME))
                .addAbsoluteLink("doc", Routes.Users.DOC));

        represent(writer, user);
    }

    public static User fromJSON(JsonReader jsonReader, boolean isLoginNameOptional) {
        String optionalUsernameWhilePatch;

        if (isLoginNameOptional) {
            optionalUsernameWhilePatch = jsonReader.optString("login_name").orElse(null);
        } else {
            optionalUsernameWhilePatch = jsonReader.getString("login_name");
        }

        User user = new User(optionalUsernameWhilePatch);
        user.setDisplayName(optionalUsernameWhilePatch);

        user.setEmail(jsonReader.optString("email").orElse(""));
        jsonReader.readBooleanIfPresent("email_me", user::setEmailMe);
        if (!jsonReader.optBoolean("enabled").orElse(true)) {
            user.disable();
        }

        List<String> checkinAliases = jsonReader.readStringArrayIfPresent("checkin_aliases").orElse(Collections.emptyList());
        user.setMatcher(String.join(",", checkinAliases));

        return user;
    }
}
