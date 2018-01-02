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

package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.agent.common.AgentCLI;
import com.thoughtworks.go.logging.LogConfigurator;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static com.thoughtworks.go.utils.AssertJava8.assertVMVersion;


public final class AgentMain {
    private static final String DEFAULT_LOGBACK_CONFIGURATION_FILE = "agent-logback.xml";

    public static void main(String... argv) throws Exception {
        assertVMVersion();
        AgentBootstrapperArgs args = new AgentCLI().parse(argv);
        LogConfigurator logConfigurator = new LogConfigurator(DEFAULT_LOGBACK_CONFIGURATION_FILE);
        logConfigurator.initialize();

        new SystemEnvironment().setProperty("go.process.type", "agent");
        new SystemEnvironment().setProperty(SystemEnvironment.SERVICE_URL, args.getServerUrl().toString());
        new SystemEnvironment().setProperty(SystemEnvironment.AGENT_SSL_VERIFICATION_MODE, args.getSslMode().toString());

        if (args.getRootCertFile() != null) {
            new SystemEnvironment().setProperty(SystemEnvironment.AGENT_ROOT_CERT_FILE, args.getRootCertFile().toString());
        }

        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        ctx.registerShutdownHook();
    }
}
