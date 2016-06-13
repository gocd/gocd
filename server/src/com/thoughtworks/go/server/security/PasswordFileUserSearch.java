/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.server.service.GoConfigService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Understands searching users in a password file specified in cruise config secruity section
 */
@Service
public class PasswordFileUserSearch {

    private final GoConfigService goConfigService;

    @Autowired
    public PasswordFileUserSearch(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public List<User> search(String searchText) throws IOException {
        final Properties properties = loadPasswordFile();
        List<User> users = findUserNameContaining(searchText, properties);
        return users;
    }

    private List<User> findUserNameContaining(String searchText, Properties properties) {
        List<User> users = new ArrayList<>();
        for (Object o : properties.keySet()) {
            String username = (String) o;
            if (username.toLowerCase().contains(searchText.toLowerCase())) {
                users.add(new User(username));
            }
        }
        return users;
    }

    private Properties loadPasswordFile() throws IOException {
        final String passwordFilePath = goConfigService.security().passwordFileConfig().path();
        final Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(passwordFilePath);
            properties.load(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return properties;
    }
}
