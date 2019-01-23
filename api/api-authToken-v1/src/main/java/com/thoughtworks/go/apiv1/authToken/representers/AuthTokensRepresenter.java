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

import java.util.List;

public class AuthTokensRepresenter {
    public static void toJSON(OutputWriter outputWriter, List<AuthToken> allTokens) {
        outputWriter.addLinks(outputLinkWriter -> outputLinkWriter
                .addLink("self", Routes.AuthToken.BASE)
                .addAbsoluteLink("doc", Routes.AuthToken.DOC))
                .addChild("_embedded", embeddedWriter ->
                        embeddedWriter.addChildList("auth_tokens", authTokenWriter -> {
                            allTokens.forEach(token -> authTokenWriter.addChild(artifactStoreWriter -> AuthTokenRepresenter.toJSON(artifactStoreWriter, token, false)));
                        })
                );
    }
}
