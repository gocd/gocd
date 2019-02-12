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

package com.thoughtworks.go.apiv1.accessToken.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;

public class AccessTokenRepresenter {
    public static void toJSON(OutputWriter outputWriter, AccessToken token) {
        outputWriter.addLinks(linksWriter -> linksWriter
                .addLink("self", Routes.AccessToken.find(token.getId()))
                .addAbsoluteLink("doc", Routes.AccessToken.DOC)
                .addLink("find", Routes.AccessToken.find()));

        outputWriter.add("id", token.getId())
                .add("description", token.getDescription())
                .add("auth_config_id", token.getAuthConfigId())
                .addChild("_meta", metaWriter -> {
                    metaWriter.add("is_revoked", token.isRevoked())
                            .add("revoked_at", token.getRevokedAt())
                            .add("created_at", token.getCreatedAt())
                            .add("last_used_at", token.getLastUsed());
                });

        if (token instanceof AccessToken.AccessTokenWithDisplayValue) {
            outputWriter.addIfNotNull("token", ((AccessToken.AccessTokenWithDisplayValue) token).getDisplayValue());
        }

        if (!token.errors().isEmpty()) {
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(Collections.emptyMap()).toJSON(errorWriter, token));
        }
    }
}
