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

import javax.net.ssl.SSLSocketFactory;

public class GoSSLConfig implements SSLConfig {
    private final SSLConfig config;

    public GoSSLConfig(SSLSocketFactory socketFactory, SystemEnvironment systemEnvironment) {
        if (systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_ALLOW)) {
            config = new ConfigurableSSLSettings(systemEnvironment);
        } else {
            config = new WeakSSLConfig(socketFactory);
        }
    }

    @Override
    public String[] getCipherSuitesToBeIncluded() {
        return config.getCipherSuitesToBeIncluded();
    }

    @Override
    public String[] getCipherSuitesToBeExcluded() {
        return config.getCipherSuitesToBeExcluded();
    }

    @Override
    public String[] getProtocolsToBeExcluded() {
        return config.getProtocolsToBeExcluded();
    }

    @Override
    public String[] getProtocolsToBeIncluded() {
        return config.getProtocolsToBeIncluded();
    }

    @Override
    public boolean isRenegotiationAllowed() {
        return config.isRenegotiationAllowed();
    }
}

