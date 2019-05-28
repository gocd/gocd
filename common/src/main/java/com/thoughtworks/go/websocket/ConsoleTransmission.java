/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.websocket;

import com.google.gson.annotations.Expose;
import com.thoughtworks.go.domain.JobIdentifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;

public class ConsoleTransmission implements Serializable, Transmission {
    @Expose
    private JobIdentifier jobIdentifier;
    @Expose
    private String tag;
    @Expose
    private String line;
    @Expose
    private String buildId;
    @Expose
    private String timestamp;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public ConsoleTransmission(String tag, String line, JobIdentifier jobIdentifier) {
        this.tag = tag;
        this.line = line;
        // Dates don't serialize well (or at least with enough precision), so bake the timestamp as a String
        this.timestamp = DATE_FORMAT.format(new Date());
        this.jobIdentifier = jobIdentifier;
    }

    public ConsoleTransmission(String tag, String line, String buildId) {
        this.tag = tag;
        this.line = line;
        // Dates don't serialize well (or at least with enough precision), so bake the timestamp as a String
        this.timestamp = DATE_FORMAT.format(new Date());
        this.buildId = buildId;
    }

    public InputStream getLineAsStream() {
        return new ByteArrayInputStream(getLine().getBytes());
    }

    public String getLine() {
        String prepend = format("%s|%s", getTag(), timestamp);
        String multilineJoin = "\n" + prepend + " ";
        return format("%s %s", prepend, line).replaceAll("\n", multilineJoin) + "\n";
    }

    public String getTag() {
        return null == tag ? "  " : tag;
    }

    @Override
    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    @Override
    public String getBuildId() {
        return buildId;
    }
}
