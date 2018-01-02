/*
 * Copyright 2016 ThoughtWorks, Inc.
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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.Property;
import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.XpathUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;

import static java.lang.String.format;

public class GeneratePropertyCommandExecutor implements BuildCommandExecutor {
    @Override
    public boolean execute(BuildCommand command, BuildSession buildSession) {
        String propertyName = command.getStringArg("name");
        File file = buildSession.resolveRelativeDir(command.getWorkingDirectory(), command.getStringArg("src"));
        String xpath = command.getStringArg("xpath");
        String indent = "             ";
        if (!file.exists()) {
            buildSession.println(format("%sFailed to create property %s. File %s does not exist.", indent, propertyName, file.getAbsolutePath()));
            return true;
        }

        try {
            if (!XpathUtils.nodeExists(file, xpath)) {
                buildSession.println(format("%sFailed to create property %s. Nothing matched xpath \"%s\" in the file: %s.", indent, propertyName, xpath, file.getAbsolutePath()));
            } else {
                String value = XpathUtils.evaluate(file, xpath);
                buildSession.getPublisher().setProperty(new Property(propertyName, value));

                buildSession.println(format("%sProperty %s = %s created." + "\n", indent, propertyName, value));
            }
        } catch (Exception e) {
            String error = (e instanceof XPathExpressionException) ? (format("Illegal xpath: \"%s\"", xpath)) : ExceptionUtils.messageOf(e);
            String message = format("%sFailed to create property %s. %s", indent, propertyName, error);
            buildSession.getPublisher().reportErrorMessage(message, e);
        }

        return true;
    }
}
