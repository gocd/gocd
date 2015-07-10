/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.config;

import com.thoughtworks.go.util.SystemEnvironment;

public class ConfigurableSSLSettings implements SSLConfig {
    private final SystemEnvironment systemEnvironment;

    public ConfigurableSSLSettings(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public String[] getCipherSuitesToBeIncluded() {
        return systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_CIPHERS);
    }

    @Override
    public String[] getCipherSuitesToBeExcluded() {
        return systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_CIPHERS);
    }

    @Override
    public String[] getProtocolsToBeExcluded() {
        return systemEnvironment.get(SystemEnvironment.GO_SSL_EXCLUDE_PROTOCOLS);

    }

    @Override
    public String[] getProtocolsToBeIncluded() {
        return systemEnvironment.get(SystemEnvironment.GO_SSL_INCLUDE_PROTOCOLS);
    }

    @Override
    public boolean isRenegotiationAllowed() {
        return systemEnvironment.get(SystemEnvironment.GO_SSL_RENEGOTIATION_ALLOWED);
    }
}