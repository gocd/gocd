/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.service;

import com.google.common.annotations.VisibleForTesting;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.util.Optional;

public class SystemInfo {
    private static final Logger LOG = LoggerFactory.getLogger(SystemInfo.class);
    private static final String OS_COMPLETE_NAME = determineOperatingSystemCompleteName();

    public static String getOperatingSystemCompleteName() {
        return OS_COMPLETE_NAME;
    }

    @VisibleForTesting
    static String determineOperatingSystemCompleteName() {
        try {
            OperatingSystem os = getSystemInfo().getOperatingSystem();
            return String.format("%s %s%s",
                os.getFamily(),
                os.getVersionInfo().getVersion(),
                optionalFrom(os.getVersionInfo().getCodeName()).map(s -> " (" + s + ")").orElse("")
            );
        } catch (Exception e) {
            LOG.warn("Unable to determine OS platform from native, falling back to default", e);
            return new SystemEnvironment().getOperatingSystemFamilyJvmName();
        }
    }

    @VisibleForTesting
    static oshi.SystemInfo getSystemInfo() {
        return new oshi.SystemInfo();
    }

    private static Optional<String> optionalFrom(String systemInfoValue) {
        return Util.isBlankOrUnknown(systemInfoValue) ? Optional.empty() : Optional.of(systemInfoValue);
    }
}
