/*
 * Copyright 2015 ThoughtWorks, Inc.
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


import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.domain.exception.VersionFormatException;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VersionInfoService {
    private ServerVersionInfoManager manager;

    @Autowired
    public VersionInfoService(ServerVersionInfoManager manager) {
        this.manager = manager;
    }

    public VersionInfo getStaleVersionInfo() {
        return manager.versionInfoForUpdate();
    }

    public VersionInfo updateServerLatestVersion(String latestVersion, LocalizedOperationResult result) {
        VersionInfo versionInfo = null;
        try {
            versionInfo = manager.updateLatestVersion(latestVersion);
        } catch (VersionFormatException e) {
            result.badRequest(LocalizedMessage.string("INVALID_VERSION_STRING_FORMAT", e.getMessage()));
        }
        return versionInfo;
    }

    public String getGoUpdate() {
        return this.manager.getGoUpdate();
    }

    public boolean isGOUpdateCheckEnabled() { return manager.isUpdateCheckEnabled(); }
}