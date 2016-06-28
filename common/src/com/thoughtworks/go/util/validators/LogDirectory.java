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

package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.ExceptionUtils;
import com.thoughtworks.go.util.OperatingSystem;
import org.apache.commons.io.FileUtils;

import static com.thoughtworks.go.util.FileUtil.readToEnd;
import static com.thoughtworks.go.util.ObjectUtil.nullSafeEquals;
import static com.thoughtworks.go.util.OperatingSystem.LINUX;
import static com.thoughtworks.go.util.OperatingSystem.OSX;
import static com.thoughtworks.go.util.OperatingSystem.SUN_OS;
import static java.util.regex.Matcher.quoteReplacement;

class LogDirectory {
    public static final LogDirectory CURRENT = new LogDirectory();
    private final String logDir;
    private final List<LoggingFileTransformer> transformers;

    public static LogDirectory fromEnvironment(OperatingSystem os) {
        if (LINUX.equals(os)) {
            return new LogDirectory("/var/log/go-server");
        } else if (SUN_OS.equals(os)) {
            return new LogDirectory("/var/log/cruise-server");
        } else if (OSX.equals(os)) {
            return new LogDirectory("/Library/Logs/GoServer");
        } else {
            return LogDirectory.CURRENT;
        }
    }

    public LogDirectory(final String loggingDirectory) {
        this.logDir = loggingDirectory;
        this.transformers = new ArrayList<>();
        this.transformers.add(new LoggingConfUpdateTransformer("/log4j.upgrade.shine.properties"));
        this.transformers.add(new LoggingConfUpdateTransformer("/log4j.upgrade.rails.properties"));
        this.transformers.add(new LoggingFileTransformer() {
            public String transform(String contents) {
                return shouldUpdate() ? contents.replaceAll("File=go-", quoteReplacement("File=" + logDir + "/go-")) : contents;
            }
        });
    }

    private LogDirectory() {
        this(null);
    }

    private boolean shouldUpdate() {
        if (logDir == null) {
            return false;
        }
        return new File(logDir).exists();
    }

    public Validation update(File log4jFile, Validation validation) {
        try {
            String contents = FileUtils.readFileToString(log4jFile);

            for (LoggingFileTransformer transformer : transformers) {
                contents = transformer.transform(contents);
            }

            FileUtils.writeStringToFile(log4jFile, contents);
        } catch (Exception e) {
            return validation.addError(e);
        }
        return Validation.SUCCESS;
    }

    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }
        return equals((LogDirectory) that);
    }

    private boolean equals(LogDirectory that) {
        return nullSafeEquals(this.logDir, that.logDir);
    }

    public int hashCode() {
        return (logDir != null ? logDir.hashCode() : 0);
    }

    public String toString() {
        return "<LogDirectory " + logDir + ">";
    }

    public static interface LoggingFileTransformer {
        String transform(String contents);
    }

    public static class LoggingConfUpdateTransformer implements LoggingFileTransformer {
        private String upgradeContent;
        private String upgradeRequirementIdentifier;
        private static final String UPGRADE_CHECK = "#upgrade_check:";

        public LoggingConfUpdateTransformer(String confUpgradeFileName) {
            try {
                upgradeContent = readToEnd(getClass().getResourceAsStream(confUpgradeFileName));
            } catch (IOException e) {
                ExceptionUtils.bomb(e);
            }
            for (String line : upgradeContent.split("\n")) {
                if (line.startsWith(UPGRADE_CHECK)) {
                    upgradeContent = upgradeContent.replace(line, "");
                    upgradeRequirementIdentifier = line.substring(UPGRADE_CHECK.length()).trim();
                }
            }
        }

        public String transform(String contents) {
            return contents.contains(upgradeRequirementIdentifier) ? contents : contents + "\n" + upgradeContent;
        }
    }
}
