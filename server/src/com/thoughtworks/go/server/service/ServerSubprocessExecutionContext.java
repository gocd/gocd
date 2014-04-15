/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.CachedDigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerSubprocessExecutionContext implements SubprocessExecutionContext {
    private final GoConfigService goConfigService;

    @Autowired
    public ServerSubprocessExecutionContext(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public String getProcessNamespace(String fingerprint) {
        return CachedDigestUtils.sha256Hex(goConfigService.getServerId() + fingerprint);//memoize if necessary(for gc reasons) -jj
    }
}
