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

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.util.Set;

import static java.lang.String.format;

public class ExportCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        String name = command.getStringArg("name");

        if (!command.hasArg("value")) {
            String displayValue = buildSession.getEnvs().get(name);
            buildSession.printlnSafely(format("[%s] setting environment variable '%s' to value '%s'",
                    GoConstants.PRODUCT_NAME, name, displayValue));
            return true;
        }

        String value = command.getStringArg("value");
        boolean secure = command.getBooleanArg("secure");
        String displayValue = secure ? EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE : value;
        Set<String> processLevelEnvs = ProcessManager.getInstance().environmentVariableNames();

        if (buildSession.getEnvs().containsKey(name) || processLevelEnvs.contains(name)) {
            buildSession.printlnSafely(format("[%s] overriding environment variable '%s' with value '%s'",
                    GoConstants.PRODUCT_NAME, name, displayValue));
        } else {
            buildSession.printlnSafely(format("[%s] setting environment variable '%s' to value '%s'",
                    GoConstants.PRODUCT_NAME, name, displayValue));
        }
        buildSession.setEnv(name, value);
        return true;
    }
}
