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

import com.beust.jcommander.Parameter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class AgentBootstrapperArgs {

    public enum SslMode {
        FULL, NONE, NO_VERIFY_HOST
    }

    public static String SERVER_URL = "serverUrl";
    public static String SSL_VERIFICATION_MODE = "sslVerificationMode";
    public static String ROOT_CERT_FILE = "rootCertFile";

    @Parameter(names = "-serverUrl", description = "The GoCD server URL. Must begin with `https://`, and end with `/go`", required = true, validateWith = ServerUrlValidator.class)
    private URL serverUrl;

    @Parameter(names = "-rootCertFile", description = "The root certificate from the certificate chain of the GoCD server (in PEM format)", validateWith = CertificateFileValidator.class)
    private File rootCertFile;

    @Parameter(names = "-sslVerificationMode", description = "The SSL verification mode.")
    private SslMode sslVerificationMode = SslMode.NONE;

    @Parameter(names = "-help", help = true, description = "Print this help")
    boolean help;

    public AgentBootstrapperArgs() {
    }

    public AgentBootstrapperArgs(URL serverUrl, File rootCertFile, SslMode sslVerificationMode) {
        this.serverUrl = serverUrl;
        this.rootCertFile = rootCertFile;
        this.sslVerificationMode = sslVerificationMode;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.put(SERVER_URL, serverUrl.toString());
        properties.put(SSL_VERIFICATION_MODE, sslVerificationMode.name());

        if (rootCertFile != null) {
            properties.put(ROOT_CERT_FILE, rootCertFile.getAbsoluteFile().toString());
        }

        return properties;
    }

    public static AgentBootstrapperArgs fromProperties(Properties properties) {
        try {
            URL serverUrl = new URL(properties.getProperty(SERVER_URL));
            File rootCertFile = null;

            SslMode sslVerificationMode = SslMode.valueOf(properties.getProperty(SSL_VERIFICATION_MODE));

            if (properties.containsKey(ROOT_CERT_FILE)) {
                rootCertFile = new File(properties.getProperty(ROOT_CERT_FILE));
            }

            return new AgentBootstrapperArgs(serverUrl, rootCertFile, sslVerificationMode);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "AgentBootstrapperArgs{" +
                "serverUrl=" + serverUrl +
                ", rootCertFile=" + rootCertFile +
                ", sslVerificationMode=" + sslVerificationMode +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AgentBootstrapperArgs that = (AgentBootstrapperArgs) o;

        if (serverUrl != null ? !serverUrl.equals(that.serverUrl) : that.serverUrl != null) return false;
        if (rootCertFile != null ? !rootCertFile.equals(that.rootCertFile) : that.rootCertFile != null) return false;
        return sslVerificationMode == that.sslVerificationMode;
    }


    public URL getServerUrl() {
        return serverUrl;
    }

    public File getRootCertFile() {
        return rootCertFile;
    }

    public SslMode getSslMode() {
        return sslVerificationMode;
    }
}
