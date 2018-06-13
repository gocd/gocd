/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;

import java.sql.Timestamp;
import java.util.Date;

public class UsageStatisticsReporting extends PersistentObject implements Validatable{
    private final ConfigErrors configErrors = new ConfigErrors();
    private String serverId;
    private Timestamp lastReportedAt = new Timestamp(0);

    public UsageStatisticsReporting() {
    }

    public UsageStatisticsReporting(String serverId, Date lastReportedAt) {
        this.serverId = serverId;
        setLastReportedAt(lastReportedAt);
    }

    public String getServerId() {
        return serverId;
    }

    public void setLastReportedAt(Date lastReportedAt) {
        this.lastReportedAt = new Timestamp(lastReportedAt.getTime());
    }

    public Timestamp lastReportedAt() {
        return lastReportedAt;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (lastReportedAt == null || lastReportedAt.getTime() <= 0) {
            addError("lastReportedAt", "Invalid time");
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}
