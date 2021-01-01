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
package com.thoughtworks.go.server.newsecurity.filters;

import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/* API requests should not start long-lived session. */
@Component
public class AgentSessionReduceIdleTimeoutFilter extends AbstractSessionReduceIdleTimeoutFilter {

    @Autowired
    public AgentSessionReduceIdleTimeoutFilter(SystemEnvironment systemEnvironment) {
        super(systemEnvironment.get(SystemEnvironment.AGENT_REQUEST_IDLE_TIMEOUT_IN_SECONDS));
    }

}
