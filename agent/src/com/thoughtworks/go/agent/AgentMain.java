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

package com.thoughtworks.go.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.agent.common.util.LoggingHelper;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public final class AgentMain {
    private static final String QUOTE = "\"";

    private AgentMain() {
    }

    public static void main(String... args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException(
                    "Usage: java -jar agent.jar https://<cruise server hostname>:<cruise server port>/go/");
        }
        LoggingHelper.configureLoggerIfNoneExists("go-agent.log", "go-agent-log4j.properties");

        setServiceUrl(args[0]);
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
        ctx.registerShutdownHook();
    }

    static void setServiceUrl(String url) {
        String target = String.format(".*(%s/?)$", GoConstants.OLD_URL_CONTEXT);
        Pattern pattern = Pattern.compile(target);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String context = matcher.group(1);
            url = url.substring(0, url.lastIndexOf(context)) + GoConstants.GO_URL_CONTEXT + "/";
        }
        new SystemEnvironment().setProperty("serviceUrl", url);
    }
}
