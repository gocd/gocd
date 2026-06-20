/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.util.SystemTimeClock;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class ServerHealthState {
    static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("MMM-dd HH:mm:ss");

    private final HealthStateLevel healthStateLevel;
    private final HealthStateType type;
    private final @NotNull String message;
    private final @NotNull String description;
    private final @NotNull Date timestamp;
    private final @Nullable Instant expiryTime;

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type) {
        this(healthStateLevel, type, "", "");
    }

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type, String message, String description) {
        this(healthStateLevel, type, message, description, null);
    }

    private ServerHealthState(HealthStateLevel healthStateLevel, HealthStateType type, String message, String description, Duration timeout) {
        this.healthStateLevel = healthStateLevel;
        this.type = type;
        this.message = requireNonNull(message, "message cannot be null");
        this.description = requireNonNull(description, "description cannot be null");
        this.expiryTime = timeout == null || timeout.isZero() ? null : SystemTimeClock.get().timeoutTime(timeout);
        this.timestamp = new Date();
    }

    public static ServerHealthState success(HealthStateType type) {
        return new ServerHealthState(HealthStateLevel.OK, type);
    }

    public static ServerHealthState warning(String message, String description, HealthStateType healthStateType) {
        return new ServerHealthState(HealthStateLevel.WARNING, healthStateType, escapeHtml4(message), escapeHtml4(description));
    }

    public static ServerHealthState warning(String message, String description, HealthStateType healthStateType, Duration timeout) {
        return new ServerHealthState(HealthStateLevel.WARNING, healthStateType, escapeHtml4(message), escapeHtml4(description), timeout);
    }

    /**
     * BE CAREFUL - this does no escaping of HTML before rendering back to UI, so users must ensure there is nothing
     * dynamic; or ensure it is already escaped.
     */
    public static ServerHealthState warningUnsafeHtml(String message, String description, HealthStateType stateType) {
        return new ServerHealthState(HealthStateLevel.WARNING, stateType, message, description);
    }

    /**
     * BE CAREFUL - this does no escaping of HTML before rendering back to UI, so users must ensure there is nothing
     * dynamic; or ensure it is already escaped.
     */
    public static ServerHealthState warningUnsafeHtml(String message, String description, HealthStateType stateType, Duration timeout) {
        return new ServerHealthState(HealthStateLevel.WARNING, stateType, message, description, timeout);
    }

    public static ServerHealthState error(String message, String description, HealthStateType type) {
        return new ServerHealthState(HealthStateLevel.ERROR, type, escapeHtml4(message), escapeHtml4(description));
    }

    public static ServerHealthState error(String message, String description, HealthStateType type, Duration timeout) {
        return new ServerHealthState(HealthStateLevel.ERROR, type, escapeHtml4(message), escapeHtml4(description), timeout);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServerHealthState that = (ServerHealthState) o;
        return healthStateLevel == that.healthStateLevel &&
            Objects.equals(type, that.type) &&
            message.equals(that.message) &&
            description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthStateLevel, type, message, description);
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

    public @NotNull String getMessage() {
        return message;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull Date getTimestamp() {
        return timestamp;
    }

    public String getMessageWithTimestamp() {
        return getMessage() + " [" + TIMESTAMP_FORMAT.format(timestamp) + "]";
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean hasExpired() {
        return expiryTime != null && expiryTime.isBefore(SystemTimeClock.get().currentTime());
    }
}
