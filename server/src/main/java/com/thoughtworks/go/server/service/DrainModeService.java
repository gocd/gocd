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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.domain.ServerDrainMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DrainModeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrainModeService.class);

    private ServerDrainMode drainMode;

    @Autowired
    public DrainModeService() {
        this.drainMode = new ServerDrainMode();
    }

    public ServerDrainMode get() {
        return drainMode;
    }

    public void update(ServerDrainMode fromRequest) {
        LOGGER.debug("[Drain Mode] Server drain mode state updated to 'isDrained={}' by '{}' at '{}'.", fromRequest.isDrainMode(), fromRequest.updatedBy(), fromRequest.updatedOn());
        this.drainMode = fromRequest;
    }
}
