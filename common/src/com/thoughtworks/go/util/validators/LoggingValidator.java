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

package com.thoughtworks.go.util.validators;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;

public class LoggingValidator implements Validator {
    private final Validator log4jPropertiesValidator;
    private final Log4jConfigReloader log4jConfigReloader;
    private final File log4jFile;

    public LoggingValidator(SystemEnvironment systemEnvironment) {
        this(new File(systemEnvironment.getConfigDir(), "log4j.properties"),
                FileValidator.configFile("log4j.properties", systemEnvironment),
                new Log4jConfigReloader());
    }

    LoggingValidator(File log4jFile, Validator log4jPropsValidator,
                     Log4jConfigReloader log4jConfigReloader) {
        this.log4jFile = log4jFile;
        this.log4jPropertiesValidator = log4jPropsValidator;
        this.log4jConfigReloader = log4jConfigReloader;
    }

    public Validation validate(Validation validation) {
        Validation propFileValidation = log4jPropertiesValidator.validate(validation);
        if (!propFileValidation.isSuccessful()) {
            return propFileValidation;
        }
        log4jConfigReloader.reload(log4jFile);
        return Validation.SUCCESS;
    }

    public static class Log4jConfigReloader {
        public void reload(File log4jFile) {
            PropertyConfigurator.configureAndWatch(log4jFile.getAbsolutePath());
        }
    }

    Validator getLog4jPropertiesValidator() {
        return log4jPropertiesValidator;
    }

    File getLog4jFile() {
        return log4jFile;
    }
}
