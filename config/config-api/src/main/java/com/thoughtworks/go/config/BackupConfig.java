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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.ConfigErrors;
import lombok.*;
import lombok.experimental.Accessors;
import org.quartz.CronExpression;

import java.text.ParseException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.NONE)
@ConfigTag("backup")
public class BackupConfig implements Validatable {

    private static final String SCHEDULE = "schedule";
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private final ConfigErrors errors = new ConfigErrors();

    @ConfigAttribute(value = "schedule", allowNull = true)
    private String schedule;

    @ConfigAttribute(value = "postBackupScript", allowNull = true)
    private String postBackupScript;

    @ConfigAttribute(value = "emailOnSuccess")
    private boolean emailOnSuccess;

    @ConfigAttribute(value = "emailOnFailure")
    private boolean emailOnFailure;

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
