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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.NoOverwriteUpdateConfigCommand;

public class FullConfigUpdateCommand implements NoOverwriteUpdateConfigCommand{
    private final CruiseConfig configForEdit;
    private final String unmodifiedMd5;

    public FullConfigUpdateCommand(CruiseConfig configForEdit, String unmodifiedMd5) {
        this.configForEdit = configForEdit;
        this.unmodifiedMd5 = unmodifiedMd5;
    }

    @Override
    public String unmodifiedMd5() {
        return this.unmodifiedMd5;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) {
        return configForEdit;
    }

    public CruiseConfig configForEdit() {
        return this.configForEdit;
    }
}
