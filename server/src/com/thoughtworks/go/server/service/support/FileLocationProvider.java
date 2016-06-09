/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service.support;

import java.io.File;
import java.util.*;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileLocationProvider implements ServerInfoProvider {

    private final SystemEnvironment systemEnvironment;

    @Autowired
    public FileLocationProvider(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public double priority() {
        return 2.0;
    }

    @Override
    public void appendInformation(InformationStringBuilder infoCollector) {
        infoCollector.addSection("Config file locations");
        infoCollector.append("loc.config.dir: ").append(systemEnvironment.configDir().getAbsolutePath()).append("\n");
        populateLogFileInfo(infoCollector);
    }

    @Override
    public Map<String, Object> asJson() {
        LinkedHashMap<String, Object> json = new LinkedHashMap<>();
        json.put("loc.config.dir", systemEnvironment.configDir().getAbsolutePath());

        List<Logger> loggers = new ArrayList<>();
        Logger rootLogger = Logger.getRootLogger();
        loggers.add(rootLogger);
        Enumeration currentLoggers = rootLogger.getLoggerRepository().getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            loggers.add((Logger) currentLoggers.nextElement());
        }

        int index = 0;
        for (Logger logger : loggers) {
            Enumeration appenders = logger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = (Appender) appenders.nextElement();
                if (!isFileAppender(appender)) {
                    continue;
                }
                FileAppender fileAppender = (FileAppender) appender;
                File logFile = new File(fileAppender.getFile());
                json.put("loc.log.root." + index, new File(logFile.getAbsolutePath()).getParent());
                json.put("loc.log.basename." + index, logFile.getName());
                ++index;
            }
        }
        return json;
    }

    @Override
    public String name() {
        return "Config file locations";
    }

    private void populateLogFileInfo(InformationStringBuilder infoCollector) {
        List<Logger> loggers = new ArrayList<>();
        Logger rootLogger = Logger.getRootLogger();
        loggers.add(rootLogger);
        Enumeration currentLoggers = rootLogger.getLoggerRepository().getCurrentLoggers();
        while (currentLoggers.hasMoreElements()) {
            loggers.add((Logger) currentLoggers.nextElement());
        }

        int index = 0;
        for (Logger logger : loggers) {
            Enumeration appenders = logger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                Appender appender = (Appender) appenders.nextElement();
                if (!isFileAppender(appender)) {
                    continue;
                }
                FileAppender fileAppender = (FileAppender) appender;
                File logFile = new File(fileAppender.getFile());
                infoCollector.append("loc.log.root.").append(index).append(": ").append(new File(logFile.getAbsolutePath()).getParent()).append("\n");
                infoCollector.append("loc.log.basename.").append(index).append(": ").append(logFile.getName()).append("\n");
                ++index;
            }
        }
    }

    private boolean isFileAppender(Appender appender) {
        return appender instanceof FileAppender;
    }
}
