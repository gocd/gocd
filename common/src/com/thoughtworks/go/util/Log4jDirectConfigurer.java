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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.Loader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

public class Log4jDirectConfigurer implements InitializingBean {
    private static final int refreshInterval = 60000;
    private String log4jLocation;

    public Log4jDirectConfigurer() {
        this.log4jLocation = "log4j.properties";
    }

    public Log4jDirectConfigurer(String log4jLocation) {
        this.log4jLocation = log4jLocation;
    }

    public void afterPropertiesSet() throws Exception {
        LogManager.getLogger("httpclient.wire").setLevel(Level.INFO);
        if (!isLog4jExist()) {
            File log4j = new File(log4jLocation);
            FileUtils.writeStringToFile(log4j, DEFAULT_LOG4J);
            Log4jConfigurer.initLogging(log4j.getAbsolutePath(), refreshInterval);
            Logger.getLogger(Log4jDirectConfigurer.class).info(
                    "created log4j file at [" + log4j.getAbsolutePath() + "]");
        }
    }

    private boolean isLog4jExist() throws IOException {
        File file = ResourceUtils.getFile(log4jLocation);
        if (log4jExistInPath(file)) {
            doExistAction(file.getAbsolutePath());
            return true;
        }
        URL resource = Loader.getResource(log4jLocation);
        if (log4JExistInClassPath(resource)) {
            doExistAction(resource.toString());
            return true;
        }
        return false;
    }

    private boolean log4JExistInClassPath(URL resource) {
        return resource != null && resource.getProtocol().equalsIgnoreCase("file");
    }

    private boolean log4jExistInPath(File file) {
        return file != null && file.exists();
    }

    private void doExistAction(String path) throws FileNotFoundException {
        Logger.getLogger(Log4jDirectConfigurer.class).info("Using log4j file [" + path + "]");
        //make the log4j file can be dynamically changed.
        Log4jConfigurer.initLogging(path, refreshInterval);
    }

    public static final String DEFAULT_LOG4J = "log4j.rootCategory=INFO,FILE\n"
            + "\n"
            + "log4j.logger.net.sourceforge.cruisecontrol=INFO\n"
            + "log4j.logger.com.thoughtworks.go=INFO\n"
            + "log4j.logger.org.springframework.context.support=INFO\n"
            + "log4j.logger.httpclient.wire=INFO\n"
            + "\n"
            + "## FILE is file logger with rotation\n"
            + "log4j.appender.FILE=org.apache.log4j.RollingFileAppender\n"
            + "log4j.appender.FILE.layout=org.apache.log4j.PatternLayout\n"
            + "log4j.appender.FILE.layout.ConversionPattern=%d{ISO8601} [%-9t] %-5p %-16c{4}:%L %x- %m%n\n"
            + "log4j.appender.FILE.File=go-agent.log\n"
            + "log4j.appender.FILE.MaxFileSize=5000KB\n"
            + "log4j.appender.FILE.MaxBackupIndex=4";
}
