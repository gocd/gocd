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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import org.quartz.CronExpression;

import java.text.ParseException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@ConfigTag("backup")
public class BackupConfig implements Validatable {

    public static final String SCHEDULE = "schedule";
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "schedule", allowNull = true)
    private String schedule;

    @ConfigAttribute(value = "postBackupScript", allowNull = true)
    private String postBackupScript;

    @ConfigAttribute(value = "emailOnSuccess")
    private boolean emailOnSuccess;

    @ConfigAttribute(value = "emailOnFailure")
    private boolean emailOnFailure;

    public BackupConfig() {
    }

    public BackupConfig(String schedule, String postBackupScript, boolean emailOnSuccess, boolean emailOnFailure) {
        this.schedule = schedule;
        this.postBackupScript = postBackupScript;
        this.emailOnSuccess = emailOnSuccess;
        this.emailOnFailure = emailOnFailure;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validateTimer();
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getPostBackupScript() {
        return postBackupScript;
    }

    public void setPostBackupScript(String postBackupScript) {
        this.postBackupScript = postBackupScript;
    }

    public boolean isEmailOnSuccess() {
        return emailOnSuccess;
    }

    public void setEmailOnSuccess(boolean emailOnSuccess) {
        this.emailOnSuccess = emailOnSuccess;
    }

    public boolean isEmailOnFailure() {
        return emailOnFailure;
    }

    public void setEmailOnFailure(boolean emailOnFailure) {
        this.emailOnFailure = emailOnFailure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BackupConfig that = (BackupConfig) o;

        if (emailOnSuccess != that.emailOnSuccess) {
            return false;
        }
        if (emailOnFailure != that.emailOnFailure) {
            return false;
        }
        if (schedule != null ? !schedule.equals(that.schedule) : that.schedule != null) {
            return false;
        }
        return postBackupScript != null ? postBackupScript.equals(that.postBackupScript) : that.postBackupScript == null;
    }

    @Override
    public int hashCode() {
        int result = schedule != null ? schedule.hashCode() : 0;
        result = 31 * result + (postBackupScript != null ? postBackupScript.hashCode() : 0);
        result = 31 * result + (emailOnSuccess ? 1 : 0);
        result = 31 * result + (emailOnFailure ? 1 : 0);
        return result;
    }

    private void validateTimer() {
        if (isBlank(schedule)) {
            return;
        }
        try {
            new CronExpression(schedule);
        } catch (ParseException pe) {
            errors.add(SCHEDULE, "Invalid cron syntax for backup configuration at offset " + pe.getErrorOffset() + ": " + pe.getMessage());
        }
    }
}
