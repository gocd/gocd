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
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

/**
 * @understands updating a specific config element and capturing errors
 */
public interface UpdateConfigFromUI {
    void checkPermission(CruiseConfig cruiseConfig, LocalizedOperationResult result);

    Validatable node(CruiseConfig cruiseConfig);

    Validatable updatedNode(CruiseConfig cruiseConfig);

    void update(Validatable node);

    Validatable subject(Validatable node);

    Validatable updatedSubject(Validatable updatedNode);

}
