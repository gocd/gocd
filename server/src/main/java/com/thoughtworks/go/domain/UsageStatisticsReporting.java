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
