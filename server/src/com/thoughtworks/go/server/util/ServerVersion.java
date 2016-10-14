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

package com.thoughtworks.go.server.util;

import java.util.Locale;

import com.jezhumble.javasysmon.JavaSysMon;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.server.web.GoVelocityView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ViewResolver;

/**
 * @understands the version of go server
 */
@Component
public class ServerVersion implements InitializingBean {

    private static final Logger LOG = Logger.getLogger(ServerVersion.class);

    public String version() {
        return CurrentGoCDVersion.getInstance().formatted();
    }

    public void afterPropertiesSet() throws Exception {
        LOG.info(String.format("[Startup] Go Version: %s", version()));
        LOG.info(String.format("[Startup] PID: %s", new JavaSysMon().currentPid()));
        LOG.info(String.format("[Startup] JVM properties: %s", System.getProperties()));
        LOG.info(String.format("[Startup] Environment Variables: %s", System.getenv()));
    }

}
