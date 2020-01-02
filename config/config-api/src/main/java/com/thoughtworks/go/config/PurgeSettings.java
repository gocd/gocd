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

import static com.thoughtworks.go.config.ServerConfig.PURGE_START;
import static com.thoughtworks.go.config.ServerConfig.PURGE_UPTO;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.NONE)
@ConfigTag("purgeSettings")
public class PurgeSettings implements Validatable {
    @ConfigSubtag
    private PurgeStart purgeStart = new PurgeStart();
    @ConfigSubtag
    private PurgeUpto purgeUpto = new PurgeUpto();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private ConfigErrors errors = new ConfigErrors();

    @Override
    public void validate(ValidationContext validationContext) {
        Double purgeUptoDiskSpace = purgeUpto.getPurgeUptoDiskSpace();
        Double purgeStartDiskSpace = purgeStart.getPurgeStartDiskSpace();

        if (purgeUptoDiskSpace == null && purgeStartDiskSpace == null) {
            return;
        }

        if (purgeUptoDiskSpace != null && (purgeStartDiskSpace == null || purgeStartDiskSpace == 0)) {
            errors().add(PURGE_START, "Error in artifact cleanup values. The trigger value has to be specified when a goal is set");
        } else if (purgeUptoDiskSpace != null && purgeStartDiskSpace > purgeUptoDiskSpace) {
            errors().add(PURGE_START, String.format("Error in artifact cleanup values. The trigger value (%sGB) should be less than the goal (%sGB)", purgeStartDiskSpace, purgeUptoDiskSpace));
        } else if (purgeUptoDiskSpace == null) {
            errors().add(PURGE_UPTO, "Error in artifact cleanup values. Please specify goal value");
        }
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }
}
