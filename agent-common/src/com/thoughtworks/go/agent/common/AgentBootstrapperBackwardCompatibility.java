/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.common;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.util.SslVerificationMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class AgentBootstrapperBackwardCompatibility {
    private static final Log LOG = LogFactory.getLog(AgentBootstrapperBackwardCompatibility.class);

    private final Map context;

    public AgentBootstrapperBackwardCompatibility(Map context) {
        this.context = context;
    }

    public File rootCertFile() {
        return rootCertFileAsString() != null ? new File(rootCertFileAsString()) : null;
    }

    public String rootCertFileAsString() {
        return context.containsKey(AgentBootstrapperArgs.ROOT_CERT_FILE) ? (String) context.get(AgentBootstrapperArgs.ROOT_CERT_FILE) : null;
    }

    public SslVerificationMode sslVerificationMode() {
        try {
            return SslVerificationMode.valueOf((String) context.get(AgentBootstrapperArgs.SSL_VERIFICATION_MODE));
        } catch (Exception e) {
            LOG.warn("SslVerificationMode could not parsed. Disabling SSL verification.");
            return SslVerificationMode.NONE;
        }
    }

    public ServerUrlGenerator getUrlGenerator() throws MalformedURLException {
        return new UrlConstructor(serverUrl());
    }

    // Backward compatibility
    // Bootstrappers <= 16.4 are invoked using java -jar agent-bootstrapper.jar 1.2.3.4 [8153]
    // and they stuff "hostname" and "port" in the context, the new bootstrapper stuffs the (ssl) serverUrl directly
    private String serverUrl() {
        if (context.containsKey(AgentBootstrapperArgs.SERVER_URL)) {
            return (String) context.get(AgentBootstrapperArgs.SERVER_URL);
        } else {
            return "http://" + context.get("hostname") + ":" + context.get("port") + "/go";
        }
    }

    public String sslServerUrl(String sslPort) {
        String serverUrl = serverUrl();

        try {
            // backward compatibility, since the agent.jar requires an ssl url, but the old bootstrapper does not have one.
            URIBuilder url = new URIBuilder(serverUrl);
            if (url.getScheme().equals("http")) {
                url.setPort(Integer.valueOf(sslPort));
                url.setScheme("https");
            }
            return url.toString();
        } catch (URISyntaxException e) {
            throw bomb(e);
        }

    }
}
