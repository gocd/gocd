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
package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.IOException;

import static java.text.MessageFormat.format;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class JettyWorkDirValidator implements Validator {

    private SystemEnvironment systemEnvironment;

    public JettyWorkDirValidator() {
        this(new SystemEnvironment());
    }

    protected JettyWorkDirValidator(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public Validation validate(Validation val) {
        if (StringUtils.isBlank(systemEnvironment.getPropertyImpl("jetty.home"))) {
            systemEnvironment.setProperty("jetty.home", systemEnvironment.getPropertyImpl("user.dir"));
        }
        systemEnvironment.setProperty("jetty.base", systemEnvironment.getPropertyImpl("jetty.home"));

        File home = new File(systemEnvironment.getPropertyImpl("jetty.home"));
        File work = new File(systemEnvironment.getPropertyImpl("jetty.home"), "work");
        if (home.exists()) {
            if (work.exists()) {
                try {
                    FileUtils.deleteDirectory(work);
                } catch (IOException e) {
                    String message = format("Error trying to remove Jetty working directory {0}: {1}",
                            work.getAbsolutePath(), e);
                    return val.addError(new RuntimeException(message));
                }
            }
            work.mkdir();
        }
        return Validation.SUCCESS;
    }
}
