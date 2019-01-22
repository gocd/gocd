/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.authToken.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.spark.Routes;

public class AuthTokenRepresenter {
    public static void toJSON(OutputWriter outputWriter, AuthToken token, boolean includeTokenValue) {
        outputWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.AuthToken.name(token.getName()))
                .addAbsoluteLink("doc", Routes.AuthToken.DOC)
                .addLink("find", Routes.AuthToken.find()));

        outputWriter.add("name", token.getName())
                .add("description", token.getDescription())
                .addChild("_meta", metaWriter -> {
                    metaWriter.add("is_revoked", token.isRevoked())
                            .add("created_at", token.getCreatedAt())
                            .add("last_used_at", token.getLastUsed());
                });

        if (includeTokenValue) {
            outputWriter.add("token", token.getOriginalValue());
        }
    }
}
