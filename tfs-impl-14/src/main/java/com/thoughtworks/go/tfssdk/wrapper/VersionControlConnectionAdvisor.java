/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.tfssdk.wrapper;

import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.DefaultConnectionAdvisor;
import com.microsoft.tfs.core.config.auth.DefaultTransportRequestHandler;
import com.microsoft.tfs.core.config.client.ClientFactory;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.webservice.WebServiceFactory;

import java.util.Locale;
import java.util.TimeZone;

/**
 * A {@link DefaultConnectionAdvisor} that supplies GoCD's minimal client and web service factories, which
 * only support the SDK features whose implementations remain in GoCD's trimmed SDK jar (see trimTfsSdkJar in
 * build.gradle). The SDK's default factories cannot be used with the trimmed jar: initializing them eagerly
 * resolves class literals for every feature the SDK supports, most of which are trimmed away.
 */
public class VersionControlConnectionAdvisor extends DefaultConnectionAdvisor {

    public VersionControlConnectionAdvisor() {
        super(Locale.getDefault(), TimeZone.getDefault());
    }

    @Override
    public ClientFactory getClientFactory(ConnectionInstanceData instanceData) {
        return new VersionControlClientFactory();
    }

    @Override
    public WebServiceFactory getWebServiceFactory(ConnectionInstanceData instanceData) {
        // As DefaultConnectionAdvisor does, but with GoCD's minimal factory
        return new VersionControlClientWebServiceFactory(getLocale(instanceData), new DefaultTransportRequestHandler(instanceData, (ConfigurableHTTPClientFactory) getHTTPClientFactory(instanceData)));
    }
}
