/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.util;

import com.jezhumble.javasysmon.JavaSysMon;
import com.thoughtworks.go.CurrentGoCDVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @understands the version of go server
 */
@Component
public class ServerVersion implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(ServerVersion.class);

    public String version() {
        return CurrentGoCDVersion.getInstance().formatted();
    }

    public void afterPropertiesSet() throws Exception {
        LOG.info("[Startup] Go Version: {}", version());
        LOG.info("[Startup] PID: {}", new JavaSysMon().currentPid());
        LOG.info("[Startup] JVM properties: {}", System.getProperties());
        LOG.info("[Startup] Environment Variables: {}", System.getenv());
    }

}
