/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @understands searching for users(from authentication sources)
 */
@Service
public class UserSearchService {
    private final AuthorizationMetadataStore store;
    private final AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSearchService.class);
    private static final int MINIMUM_SEARCH_STRING_LENGTH = 2;

    @Autowired
    public UserSearchService(AuthorizationExtension authorizationExtension, GoConfigService goConfigService) {
        this.store = AuthorizationMetadataStore.instance();
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
    }

    public List<UserSearchModel> search(String searchText, HttpLocalizedOperationResult result) {
        List<UserSearchModel> userSearchModels = new ArrayList<>();
        if (isInputValid(searchText, result)) {
            return userSearchModels;
        }

        searchUsingPlugins(searchText, userSearchModels);

        if (userSearchModels.size() == 0 && !result.hasMessage()) {
            result.setMessage("No results found.");
        }
        return userSearchModels;
    }

    private void searchUsingPlugins(String searchText, List<UserSearchModel> userSearchModels) {
        List<User> searchResults = new ArrayList<>();
        for (final String pluginId : store.getPluginsThatSupportsUserSearch()) {
            try {
                List<com.thoughtworks.go.plugin.domain.authorization.User> users = getUsersConfiguredViaPlugin(pluginId, searchText);
                if (users != null && !users.isEmpty()) {
                    for (com.thoughtworks.go.plugin.domain.authorization.User user : users) {
                        String displayName = user.getDisplayName() == null ? "" : user.getDisplayName();
                        String emailId = user.getEmailId() == null ? "" : user.getEmailId();
                        searchResults.add(new User(user.getUsername(), displayName, emailId));
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Error occurred while performing user search using plugin: {}", pluginId, e);
            }
        }
        userSearchModels.addAll(convertUsersToUserSearchModel(searchResults, UserSourceType.PLUGIN));
    }

    private List<com.thoughtworks.go.plugin.domain.authorization.User> getUsersConfiguredViaPlugin(String pluginId, String searchTerm) {
        List<com.thoughtworks.go.plugin.domain.authorization.User> users = new ArrayList<>();
        if (authorizationExtension.canHandlePlugin(pluginId)) {
            List<SecurityAuthConfig> authConfigs = goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
            users.addAll(authorizationExtension.searchUsers(pluginId, searchTerm, authConfigs));
        }
        return users;
    }

    private boolean isInputValid(String searchText, HttpLocalizedOperationResult result) {
        if (searchText.trim().length() < MINIMUM_SEARCH_STRING_LENGTH) {
            result.badRequest("Please use a search string that has at least two (2) letters.");
            return true;
        }
        return false;
    }

    private List<UserSearchModel> convertUsersToUserSearchModel(List<User> users, UserSourceType source) {
        List<UserSearchModel> userSearchModels = new ArrayList<>();
        for (User user : users) {
            userSearchModels.add(new UserSearchModel(user, source));
        }
        return userSearchModels;
    }
}
