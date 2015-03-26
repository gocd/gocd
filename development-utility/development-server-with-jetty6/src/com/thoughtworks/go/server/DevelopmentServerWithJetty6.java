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

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;

/**
 * @understands how to run a local development mode webserver so we can develop live
 * Set the following before running the main method:
 * Working directory: <project-path>/server
 * VM arguments: -Xms512m -Xmx1024m -XX:PermSize=400m -Djava.awt.headless=true
 * classpath: Use classpath of 'development-server'
 */

public class DevelopmentServerWithJetty6 {
    public static void main(String[] args) throws Exception {
        SystemEnvironment systemEnvironment = new SystemEnvironment();
        systemEnvironment.set(SystemEnvironment.APP_SERVER, Jetty6Server.class.getCanonicalName());
        systemEnvironment.set(SystemEnvironment.JETTY_XML_FILE_NAME, "jetty6.xml");
        DevelopmentServer.main(args);
    }
}
