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

package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemTimeClock;
import com.thoughtworks.go.utils.Timeout;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class ServerHealthState {
    private final HealthStateLevel healthStateLevel;
    private final HealthStateType type;
    private final String message;
    private final String description;
    static Clock clock = new SystemTimeClock();
    private DateTime expiryTime;
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MMM-dd HH:mm:ss");
    private Date timestamp;

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type) {
        this(healthStateLevel, type, "", "");
    }

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type, String message, String description) {
        this(healthStateLevel, type, message, description, Timeout.NEVER);
    }

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type, String message, String description, Timeout timeout) {
        bombIfNull(description, "description cannot be null");
        bombIfNull(message, "message cannot be null");
        this.healthStateLevel = healthStateLevel;
        this.type = type;
        this.message = message;
        this.description = description;
        setTimeout(timeout);
        this.timestamp = new Date();
    }

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType healthStateType, String message, String description, long milliSeconds) {
        bombIfNull(description, "description cannot be null");
        bombIfNull(message, "message cannot be null");
        this.healthStateLevel = healthStateLevel;
        this.type = healthStateType;
        this.message = message;
        this.description = description;
        setTimeout(milliSeconds);
        this.timestamp = new Date();
    }

    private void setTimeout(long milliSeconds) {
        this.expiryTime = milliSeconds == Timeout.NEVER.inMillis() ? null : clock.timeoutTime(milliSeconds);
    }

    public void setTimeout(Timeout timeout) {
        this.expiryTime = timeout == Timeout.NEVER ? null : clock.timeoutTime(timeout);
    }

    public static ServerHealthState success(HealthStateType type) {
        return new ServerHealthState(HealthStateLevel.OK, type);
    }

    public static ServerHealthState warning(String message, String description, HealthStateType healthStateType) {
        return new ServerHealthState(HealthStateLevel.WARNING, healthStateType, escapeHtml(message), escapeHtml(description));
    }

    public static ServerHealthState error(String message, String description, HealthStateType type) {
        return new ServerHealthState(HealthStateLevel.ERROR, type, escapeHtml(message), escapeHtml(description));
    }

    public static ServerHealthState warning(String message, String description, HealthStateType healthStateType, Timeout timeout) {
        return new ServerHealthState(HealthStateLevel.WARNING, healthStateType, message, description, timeout);
    }

    public static ServerHealthState warning(String message, String description, HealthStateType healthStateType, long milliSeconds) {
        return new ServerHealthState(HealthStateLevel.WARNING, healthStateType, escapeHtml(message), escapeHtml(description), milliSeconds);
    }

    public static ServerHealthState warningWithHtml(String message, String description, HealthStateType stateType) {
        return new ServerHealthState(HealthStateLevel.WARNING, stateType, message, description);
    }

    public static ServerHealthState warningWithHtml(String message, String description, HealthStateType stateType, long milliSeconds) {
        return new ServerHealthState(HealthStateLevel.WARNING, stateType, message, description, milliSeconds);
    }

    public static ServerHealthState failToScheduling(HealthStateType healthStateType, String pipelineName, String description) {
        String message = String.format("Failed to trigger pipeline [%s]", pipelineName);
        return new ServerHealthState(HealthStateLevel.ERROR, healthStateType, message, description);
    }

    public static ServerHealthState failedToScheduleStage(HealthStateType healthStateType, String pipelineName, String stageName, String description) {
        String message = String.format("Failed to trigger stage [%s] pipeline [%s]", stageName, pipelineName);
        return new ServerHealthState(HealthStateLevel.ERROR, healthStateType, message, description, Timeout.TWO_MINUTES);
    }

    public HealthStateType getType() {
        return type;
    }

    boolean isWarning() {
        return this.healthStateLevel.equals(HealthStateLevel.WARNING);
    }

    public boolean isRealSuccess() {
        return this.healthStateLevel.equals(HealthStateLevel.OK);
    }


    public boolean isSuccess() {
        return isRealSuccess() || isWarning();
    }

    public ServerHealthState trump(ServerHealthState otherServerHealthState) {
        int result = healthStateLevel.compareTo(otherServerHealthState.healthStateLevel);
        return result > 0 ? this : otherServerHealthState;
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
        return equals((ServerHealthState) that);
    }

    private boolean equals(ServerHealthState that) {
        if (!this.healthStateLevel.equals(that.healthStateLevel)) {
            return false;
        }
        if (!this.type.equals(that.type)) {
            return false;
        }
        if (!this.description.equals(that.description)) {
            return false;
        }
        if (!this.message.equals(that.message)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = healthStateLevel.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    public Map<String, String> asJson() {
        Map<String, String> json = new LinkedHashMap<>();
        json.put("message", getMessage());
        json.put("detail", getDescription());
        json.put("level", healthStateLevel.toString());
        return json;
    }

    public HealthStateLevel getLogLevel() {
        return healthStateLevel;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpCode() {
        return getType().getHttpCode();
    }


    public String getDescription() {
        return description;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMessageWithTimestamp() {
        return getMessage() + " [" + TIMESTAMP_FORMAT.format(timestamp) + "]";
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean hasExpired() {
        return expiryTime != null && expiryTime.isBefore(clock.currentDateTime());
    }

    public Set<String> getPipelineNames(CruiseConfig config) {
        return type.getPipelineNames(config);
    }
}
