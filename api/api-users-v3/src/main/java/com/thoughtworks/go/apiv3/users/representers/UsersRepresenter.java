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
package com.thoughtworks.go.apiv3.users.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv3.users.model.UserToRepresent;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;

public class UsersRepresenter {
    public static void toJSON(OutputWriter writer, Collection<UserToRepresent> users) {
        writer.addLinks(linksWriter -> linksWriter.addLink("self", Routes.Users.BASE).addAbsoluteLink("doc", Routes.Users.DOC))
                .addChild("_embedded", childWriter -> {
                    childWriter.addChildList("users", userWriter -> {
                        users.forEach(user -> {
                            userWriter.addChild(innerChildWriter -> {
                                UserRepresenter.represent(innerChildWriter, user);
                            });
                        });
                    });
                });
    }
}
