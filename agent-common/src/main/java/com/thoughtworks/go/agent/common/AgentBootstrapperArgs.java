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
package com.thoughtworks.go.agent.common;

import com.beust.jcommander.Parameter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@ToString
public class AgentBootstrapperArgs {

    public enum SslMode {
        FULL, NONE, NO_VERIFY_HOST
    }

    public static final String SERVER_URL = "serverUrl";
    public static final String SSL_VERIFICATION_MODE = "sslVerificationMode";
    public static final String ROOT_CERT_FILE = "rootCertFile";
    public static final String PRIVATE_KEY = "sslPrivateKey";
    public static final String PRIVATE_KEY_PASSPHRASE_FILE = "sslPrivateKeyPassphraseFile";
    public static final String SSL_CERTIFICATE = "sslCertificate";

    @Parameter(names = "-serverUrl", description = "The GoCD server URL. Example: http://gocd.example.com:8153/go", required = true, validateWith = ServerUrlValidator.class)
    private URL serverUrl;

    @Parameter(names = "-rootCertFile", description = "The root certificate from the certificate chain of the GoCD server (in PEM format)", validateWith = CertificateFileValidator.class)
    private File rootCertFile;

    @Parameter(names = "-sslVerificationMode", description = "The SSL verification mode.")
    private SslMode sslVerificationMode = SslMode.FULL;

    @Parameter(names = "-sslPrivateKey", description = "The private key for mutual TLS.")
    private File sslPrivateKey;

    @Parameter(names = "-sslPrivateKeyPassphraseFile", description = "The file containing the passphrase for decoding the SSL private key.")
    private File sslPrivateKeyPassphraseFile;

    @Parameter(names = "-sslCertificate", description = "The X509 certificate for mutual TLS.")
    private File sslCertificate;

    @Parameter(names = "-help", help = true, description = "Print this help")
    boolean help;

    public AgentBootstrapperArgs() {
    }

    public Map<String, String> toProperties() {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(SERVER_URL, serverUrl.toString());
        properties.put(SSL_VERIFICATION_MODE, sslVerificationMode.name());

        if (sslPrivateKey != null) {
            properties.put(PRIVATE_KEY, sslPrivateKey.getPath());
        }

        if (sslPrivateKeyPassphraseFile != null) {
            properties.put(PRIVATE_KEY_PASSPHRASE_FILE, sslPrivateKeyPassphraseFile.getPath());
        }

        if (sslCertificate != null) {
            properties.put(SSL_CERTIFICATE, sslCertificate.getPath());
        }

        if (rootCertFile != null) {
            properties.put(ROOT_CERT_FILE, rootCertFile.getPath());
        }

        return properties;
    }

    public static AgentBootstrapperArgs fromProperties(Map<String, String> properties) {
        try {
            AgentBootstrapperArgs agentBootstrapperArgs = new AgentBootstrapperArgs();

            agentBootstrapperArgs.setServerUrl(new URL(properties.get(SERVER_URL)));
            agentBootstrapperArgs.setSslVerificationMode(SslMode.valueOf(properties.get(SSL_VERIFICATION_MODE)));

            if (properties.containsKey(ROOT_CERT_FILE)) {
                agentBootstrapperArgs.setRootCertFile(new File(properties.get(ROOT_CERT_FILE)));
            }

            if (properties.containsKey(PRIVATE_KEY)) {
                agentBootstrapperArgs.setSslPrivateKey(new File(properties.get(PRIVATE_KEY)));
            }

            if (properties.containsKey(PRIVATE_KEY_PASSPHRASE_FILE)) {
                agentBootstrapperArgs.setSslPrivateKeyPassphraseFile(new File(properties.get(PRIVATE_KEY_PASSPHRASE_FILE)));
            }

            if (properties.containsKey(SSL_CERTIFICATE)) {
                agentBootstrapperArgs.setSslCertificate(new File(properties.get(SSL_CERTIFICATE)));
            }
            return agentBootstrapperArgs;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
