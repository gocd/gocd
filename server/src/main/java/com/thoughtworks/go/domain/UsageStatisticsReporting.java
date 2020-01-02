/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import java.sql.Timestamp;
import java.util.Date;

public class UsageStatisticsReporting extends PersistentObject {
    private String serverId;
    private String dataSharingServerUrl = null;
    private String dataSharingGetEncryptionKeysUrl = null;
    private Timestamp lastReportedAt = new Timestamp(0);
    private boolean canReport = true;

    public UsageStatisticsReporting() {
    }

    public UsageStatisticsReporting(String serverId, Date lastReportedAt) {
        this.serverId = serverId;
        setLastReportedAt(lastReportedAt);
    }

    public String getDataSharingServerUrl() {
        return dataSharingServerUrl;
    }

    public void setDataSharingServerUrl(String dataSharingServerUrl) {
        this.dataSharingServerUrl = dataSharingServerUrl;
    }

    public String getDataSharingGetEncryptionKeysUrl() {
        return dataSharingGetEncryptionKeysUrl;
    }

    public void setDataSharingGetEncryptionKeysUrl(String dataSharingGetEncryptionKeysUrl) {
        this.dataSharingGetEncryptionKeysUrl = dataSharingGetEncryptionKeysUrl;
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

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public boolean canReport() {
        return canReport;
    }

    public void canReport(boolean canReport) {
        this.canReport = canReport;
    }
}
