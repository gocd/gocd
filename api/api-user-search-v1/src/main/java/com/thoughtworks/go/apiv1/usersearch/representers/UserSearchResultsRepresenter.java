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
package com.thoughtworks.go.apiv1.usersearch.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.spark.Routes;

import java.util.Collection;

public class UserSearchResultsRepresenter {
    public static void toJSON(OutputWriter jsonOutputWriter, String searchTerm, Collection<UserSearchModel> userSearchModels) {
        jsonOutputWriter
                .addLinks(outputLinkWriter -> outputLinkWriter
                        .addAbsoluteLink("doc", Routes.UserSearch.DOC)
                        .addLink("self", Routes.UserSearch.self(searchTerm))
                        .addLink("find", Routes.UserSearch.find()))
                .addChild("_embedded", embeddedWriter -> {
                    embeddedWriter.addChildList("users", usersWriter -> {
                        userSearchModels.forEach(userSearchModel -> usersWriter.addChild(outputWriter -> UserSearchRepresenter.toJSON(outputWriter, userSearchModel)));
                    });
                });
    }
}
