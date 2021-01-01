/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.util.command.UrlArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @understands: Factory to give appropriate TFS Command
 */
public class TfsCommandFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(TfsCommandFactory.class);

    public TfsCommand create(SubprocessExecutionContext subprocessExecutionContext, UrlArgument url, String domain, String userName, String password, String fingerprint,
                             String projectPath) {
        return getTfsSdkCommand(subprocessExecutionContext, url, domain, userName, password, fingerprint, projectPath);
    }

    private TfsCommand getTfsSdkCommand(SubprocessExecutionContext subprocessExecutionContext, UrlArgument url, String domain, String userName, String password, String fingerprint,
                                        String projectPath) {
        LOGGER.debug("[TFS SDK] Creating TFS SDK Client");
        return getSDKBuilder().buildTFSSDKCommand(fingerprint, url, domain, userName, password, subprocessExecutionContext.getProcessNamespace(fingerprint), projectPath);
    }

    TfsSDKCommandBuilder getSDKBuilder() {
        try {
            return TfsSDKCommandBuilder.getBuilder();
        } catch (Exception e) {
            String message = "[TFS SDK] Could not create TFS SDK Builder";
            LOGGER.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

}
