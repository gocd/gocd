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

package com.thoughtworks.go.domain;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.command.IO;
import org.apache.log4j.Logger;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Handles the Log element, and subelements, of the CruiseControl configuration file. Also represents the Build Log used
 * by the CruiseControl build process.
 */
public class GoControlLog implements Serializable {
    private static final long serialVersionUID = -5727569770074024691L;

    private static final Logger LOG = Logger.getLogger(GoControlLog.class);

    public static final int BEFORE_LENGTH = "logYYYYMMDDhhmmssL".length();

    private transient String logDir;
    private transient Element buildLog;

    /**
     * Log instances created this way must have their projectName set.
     */
    public GoControlLog() {
        reset();
    }

    public GoControlLog(String defaultWorkingFolder) {
        setDir(defaultWorkingFolder);
        reset();
    }

    public void setDir(String logDir) {
        this.logDir = logDir;
    }

    /**
     * Writes the current build log to the appropriate directory and filename.
     */
    public void writeLogFile(Date now) throws IOException {

        String logFilename = decideLogfileName(now);

        // Add the logDir as an info element
        Element logDirElement = new Element("property");
        logDirElement.setAttribute("name", "logdir");
        logDirElement.setAttribute("value", new File(logDir).getAbsolutePath());
        buildLog.getChild("info").addContent(logDirElement);

        // Add the logFile as an info element
        Element logFileElement = new Element("property");
        logFileElement.setAttribute("name", "logfile");
        logFileElement.setAttribute("value", logFilename);
        buildLog.getChild("info").addContent(logFileElement);

        File logfile = new File(logDir, logFilename);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Writing log file [" + logfile.getAbsolutePath() + "]");
        }
        writeLogFile(logfile, buildLog);
    }

    protected void writeLogFile(File file, Element element) throws IOException {
        // Write the log file out, let jdom care about the encoding by using
        // an OutputStream instead of a Writer.
        OutputStream logStream = null;
        try {
            Format format = Format.getPrettyFormat();
            XMLOutputter outputter = new XMLOutputter(format);

            IO.mkdirFor(file);
            file.setWritable(true);

            logStream = new BufferedOutputStream(new FileOutputStream(file));
            outputter.output(new Document(element), logStream);
        }  finally {
            IO.close(logStream);
        }
    }

    public String decideLogfileName(Date now)  {
        return "log.xml";
    }

    public static String formatLogFileName(Date date, String label) {
        StringBuffer logFileName = new StringBuffer();
        logFileName.append("log");
        logFileName.append(DateUtils.getFormattedTime(date));
        if (label != null) {
            logFileName.append("L");
            logFileName.append(label);
        }
        logFileName.append(".xml");

        return logFileName.toString();
    }

    public void addContent(Content newContent) {
        buildLog.addContent(newContent);
    }

    /**
     * Resets the build log. After calling this method a fresh build log will exist, ready for adding new content.
     */
    public void reset() {
        this.buildLog = new Element("cruisecontrol");
    }

}
