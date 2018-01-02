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

package com.thoughtworks.go.server.service.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

import static com.thoughtworks.go.logging.LogHelper.LOGGER_CONTEXT;

@Component
public class FileLocationProvider implements ServerInfoProvider {

    private final SystemEnvironment systemEnvironment;

    @Autowired
    public FileLocationProvider(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public double priority() {
        return 3.0;
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("loc.config.dir", systemEnvironment.configDir().getAbsolutePath());

        List<Logger> loggers = LOGGER_CONTEXT.getLoggerList();

        Appender[] appenders = getAppenders(loggers);

        for (int i = 0; i < appenders.length; i++) {
            Appender appender = appenders[i];
            if (!isFileAppender(appender)) {
                continue;
            }
            FileAppender fileAppender = (FileAppender) appender;
            File logFile = new File(fileAppender.rawFileProperty());
            json.put("loc.log.root." + i, new File(logFile.getAbsolutePath()).getParent());
            json.put("loc.log.basename." + i, logFile.getName());
        }

        return json;
    }

    private Appender[] getAppenders(List<Logger> loggers) {
        LinkedHashSet<Appender<ILoggingEvent>> appenders = new LinkedHashSet<>();

        for (Logger logger : loggers) {
            Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
            while (appenderIterator.hasNext()) {
                Appender<ILoggingEvent> appender = appenderIterator.next();
                appenders.add(appender);
            }
        }
        return appenders.toArray(new Appender[0]);
    }

    @Override
    public String name() {
        return "Config file locations";
    }

    private boolean isFileAppender(Appender appender) {
        return appender instanceof FileAppender;
    }
}
