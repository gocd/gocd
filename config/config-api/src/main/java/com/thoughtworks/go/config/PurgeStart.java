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

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@EqualsAndHashCode
@Accessors(chain = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@ConfigTag("purgeStartDiskSpace")
public class PurgeStart {
    @ConfigValue
    private Double purgeStartDiskSpace;

    public PurgeStart(String purgeStartDiskSpace) {
        this.purgeStartDiskSpace = Double.valueOf(purgeStartDiskSpace);
    }

    public void setPurgeStartDiskSpace(String purgeStart) {
        this.purgeStartDiskSpace = Double.valueOf(purgeStart);
    }
}
