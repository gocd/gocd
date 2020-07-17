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

package com.thoughtworks.go.addon.businesscontinuity;

import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@Component
public class AuthToken {
    private static final Logger LOG = LoggerFactory.getLogger(AuthToken.class);

    private final File tokenFile;

    @Autowired
    public AuthToken(SystemEnvironment systemEnvironment) {
        this.tokenFile = new File(systemEnvironment.configDir(), "business-continuity-token");
    }

    public boolean isValid() {
        try {
            read();
            return true;
        } catch (Exception e) {
            LOG.error("[Business-Continuity] Unable to read authentication token file", e);
            return false;
        }
    }

    public UsernamePassword toUsernamePassword() {
        return new UsernamePassword(getUsername(), getPassword());
    }

    private String getUsername() {
        return ((String) read().getKey()).trim();
    }

    private String getPassword() {
        return ((String) read().getValue()).trim();
    }

    private Map.Entry<Object, Object> read() {
        Properties properties = new Properties();
        if (tokenFile.exists()) {
            try (InputStream is = new FileInputStream(tokenFile)) {
                properties.load(is);
            } catch (IOException e) {
                throw new AuthTokenException("Unable to read auth token", e);
            }
        } else {
            throw new AuthTokenException("The auth token file " + tokenFile + " does not exist!");
        }

        if (properties.size() != 1) {
            throw new AuthTokenException("The `business-continuity-token` file should contain exactly one credential.");
        }

        return new TreeMap<>(properties).firstEntry();
    }

    public String forHttp() {
        return getUsername() + ":" + getPassword();
    }
}
