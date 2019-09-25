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
import lombok.*;
import lombok.experimental.Accessors;

import static com.thoughtworks.go.config.ServerConfig.PURGE_START;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.NONE)
@ConfigTag("purgeSettings")
public class PurgeSettings implements Validatable {
    @ConfigSubtag
    private PurgeStart purgeStart;
    @ConfigSubtag
    private PurgeUpto purgeUpto;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    private ConfigErrors errors = new ConfigErrors();

    @Override
    public void validate(ValidationContext validationContext) {
        if (!(purgeStart == null && purgeUpto == null)) {
            if (purgeUpto != null && (purgeStart == null || purgeStart.getPurgeStartDiskSpace() == 0)) {
                errors().add(PURGE_START, "Error in artifact cleanup values. The trigger value is has to be specified when a goal is set");
            } else if (purgeStart.getPurgeStartDiskSpace() > purgeUpto.getPurgeUptoDiskSpace()) {
                errors().add(PURGE_START, String.format("Error in artifact cleanup values. The trigger value (%sGB) should be less than the goal (%sGB)", purgeStart, purgeUpto));
            }
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
