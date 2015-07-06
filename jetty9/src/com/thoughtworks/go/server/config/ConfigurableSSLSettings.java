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

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConfigurableSSLSettings implements SSLConfig {
    private final Config config;

    public ConfigurableSSLSettings(SystemEnvironment systemEnvironment) {
        try {
            String systemConfiguredSslConfigFile = systemEnvironment.get(SystemEnvironment.GO_SSL_CONFIG_FILE_PATH);
            InputStream sslConfigStream = getClass().getResourceAsStream(systemConfiguredSslConfigFile);
            String json = IOUtils.toString(sslConfigStream);
            config = new Gson().fromJson(json, Config.class).setDefaults();

            if (!StringUtil.isBlank(systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH))) {
                File userConfiguredSslConfigFile = new File(systemEnvironment.configDir(), systemEnvironment.get(SystemEnvironment.USER_CONFIGURED_SSL_CONFIG_FILE_PATH));
                if (userConfiguredSslConfigFile.exists()) {
                    Config userDefinedConfig = new Gson().fromJson(IOUtils.toString(new FileInputStream(userConfiguredSslConfigFile)), Config.class);
                    config.overrideWith(userDefinedConfig).setDefaults();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String[] getCipherSuitesToBeIncluded() {
        return config.getIncludedCiphers();
    }

    @Override
    public String[] getCipherSuitesToBeExcluded() {
        return config.getExcludedCiphers();
    }

    @Override
    public String[] getProtocolsToBeExcluded() {
        return config.getExcludedProtocols();
    }

    @Override
    public String[] getProtocolsToBeIncluded() {
        return config.getIncludedProtocols();
    }

    @Override
    public boolean isRenegotiationAllowed() {
        return config.isRenegotiationAllowed();
    }

    public class Config {
        @Expose()
        @SerializedName("ciphers")
        private Ciphers ciphers;
        @Expose()
        @SerializedName("protocols")
        private Protocols protocols;
        @Expose()
        @SerializedName("renegotiationAllowed")
        private String renegotiationAllowed;

        private Config setDefaults() {
            if (ciphers == null) {
                ciphers = new Ciphers();
                ciphers.setDefaults();
            }
            if (protocols == null) {
                protocols = new Protocols();
                protocols.setDefaults();
            }
            return this;
        }

        private String[] getIncludedCiphers() {
            return ciphers.includes;
        }

        private String[] getExcludedCiphers() {
            return ciphers.excludes;
        }

        private String[] getExcludedProtocols() {
            return protocols.excludes;
        }

        private String[] getIncludedProtocols() {
            return protocols.includes;
        }

        private boolean isRenegotiationAllowed() {
            return "true".equalsIgnoreCase(renegotiationAllowed);
        }

        public Config overrideWith(Config overriddenValue) {
            if (overriddenValue.ciphers != null) {
                this.ciphers.overrideWith(overriddenValue.ciphers);
            }
            if (overriddenValue.protocols != null) {
                this.protocols.overrideWith(overriddenValue.protocols);
            }
            if (overriddenValue.renegotiationAllowed != null) {
                this.renegotiationAllowed = overriddenValue.renegotiationAllowed;
            }
            return this;
        }


        private class Ciphers {
            @Expose()
            @SerializedName("includes")
            private String[] includes;
            @Expose()
            @SerializedName("excludes")
            private String[] excludes;

            public void setDefaults() {
                if (includes == null) includes = new String[0];
                if (excludes == null) excludes = new String[0];
            }

            public void overrideWith(Ciphers ciphers) {
                if (ciphers.includes != null) {
                    this.includes = ciphers.includes;
                }
                if (ciphers.excludes != null) {
                    this.excludes = ciphers.excludes;
                }
            }
        }

        private class Protocols {
            @Expose()
            @SerializedName("excludes")
            private String[] excludes;
            @Expose()
            @SerializedName("includes")
            private String[] includes;

            public void setDefaults() {
                if (excludes == null)
                    excludes = new String[0];
                if (includes == null)
                    includes = new String[0];
            }

            public void overrideWith(Protocols protocols) {
                if (protocols.excludes != null) {
                    this.excludes = protocols.excludes;
                }
                if (protocols.includes != null) {
                    this.includes = protocols.includes;
                }
            }
        }


    }


}