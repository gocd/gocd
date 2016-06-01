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

import javax.net.ssl.SSLSocketFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeakSSLConfig implements SSLConfig {
    private final SSLSocketFactory socketFactory;
    private List<String> cipherSuitesSupportedByGo = Arrays.asList("SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_WITH_RC4_128_MD5");

    public WeakSSLConfig(SSLSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    @Override
    public String[] getCipherSuitesToBeIncluded() {
        String[] supportedCipherSuites = socketFactory.getSupportedCipherSuites();

        ArrayList<String> suitesToBeIncluded = new ArrayList<>();
        for (String suite : supportedCipherSuites) {
            if (cipherSuitesSupportedByGo.contains(suite)) {
                suitesToBeIncluded.add(suite);
            }
        }

        if (suitesToBeIncluded.size() == 0) {
            return supportedCipherSuites;
        }
        return cipherSuitesSupportedByGo.toArray(new String[cipherSuitesSupportedByGo.size()]);
    }

    @Override
    public String[] getCipherSuitesToBeExcluded() {
        return null;
    }

    @Override
    public String[] getProtocolsToBeExcluded() {
        return null;
    }

    @Override
    public String[] getProtocolsToBeIncluded() {
        return null;
    }

    @Override
    public boolean isRenegotiationAllowed() {
        return true;
    }
}
