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

package com.thoughtworks.go.apiv1.securityauthconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;

public class VerifyConnectionResponseRepresenter {
    public static void toJSON(OutputWriter outputWriter, VerifyConnectionResponse verifyConnectionResponse, SecurityAuthConfig securityAuthConfig) {
        outputWriter.add("status", verifyConnectionResponse.getStatus())
                .add("message", verifyConnectionResponse.getMessage())
                .addChild("auth_config", writer -> SecurityAuthConfigRepresenter.toJSON(writer, securityAuthConfig));
    }
}
