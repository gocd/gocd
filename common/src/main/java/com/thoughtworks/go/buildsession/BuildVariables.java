/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.Clock;
import org.apache.commons.lang.text.StrLookup;

import java.text.SimpleDateFormat;

import static com.thoughtworks.go.util.MapBuilder.map;

public class BuildVariables extends StrLookup {
    private final StrLookup staticLookup;
    private final Clock clock;

    public BuildVariables(AgentRuntimeInfo agentRuntimeInfo, Clock clock) {
        this.clock = clock;
        this.staticLookup = StrLookup.mapLookup(map(
                "agent.location", agentRuntimeInfo.getLocation(),
                "agent.hostname", agentRuntimeInfo.getHostName()
        ));
    }

    @Override
    public String lookup(String key) {
        String staticResult = staticLookup.lookup(key);
        if(staticResult != null) {
            return staticResult;
        }

        if(key.equals("date")) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(clock.currentTime());
        }

        return null;
    }

}
