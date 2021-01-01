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
package com.thoughtworks.go.util.command;

import org.apache.commons.lang3.StringUtils;

public class ExecScript implements Script, StreamConsumer {
    private String errorStr;
    private int exitCode;
    private boolean foundError = false;

    public ExecScript(String errorString) {
        this.errorStr = errorString;
    }

    /**
     * Ugly parsing of Exec output into some Elements. Gets called from StreamPumper.
     *
     * @param line the line of output to parse
     */
    @Override
    public synchronized void consumeLine(final String line) {

        // check if the output contains the error string
        if (StringUtils.isNotEmpty(errorStr)) {
            // YES: set error flag
            if (StringUtils.equalsIgnoreCase(line.trim(), errorStr)) {
                foundError = true;
            }
        }
    } // consumeLine

    /**
     * @return returns the exitcode of the command
     */
    @Override
    public int getExitCode() {
        return exitCode;
    } // getExitCode

    /**
     * @param exitCode the exit code value to set.
     */
    @Override
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    } // setExitCode

    /**
     * @return true if error occurred, else false
     */
    public boolean foundError() {
        return this.foundError;
    } // wasError

} // ExecScript
