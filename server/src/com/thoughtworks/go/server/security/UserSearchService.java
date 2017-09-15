/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @understands searching for users(from authentication sources)
 */
@Service
public class UserSearchService {
    private final AuthorizationMetadataStore store;
    private final AuthorizationExtension authorizationExtension;
    private GoConfigService goConfigService;
    private AuthenticationPluginRegistry authenticationPluginRegistry;
    private AuthenticationExtension authenticationExtension;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSearchService.class);
    private static final int MINIMUM_SEARCH_STRING_LENGTH = 2;

    @Autowired
    public UserSearchService(AuthorizationExtension authorizationExtension, GoConfigService goConfigService,
                             AuthenticationPluginRegistry authenticationPluginRegistry, AuthenticationExtension authenticationExtension) {
        this.store = AuthorizationMetadataStore.instance();
        this.authorizationExtension = authorizationExtension;
        this.goConfigService = goConfigService;
        this.authenticationPluginRegistry = authenticationPluginRegistry;
        this.authenticationExtension = authenticationExtension;
    }

    public List<UserSearchModel> search(String searchText, HttpLocalizedOperationResult result) {
        List<UserSearchModel> userSearchModels = new ArrayList<>();
        if (isInputValid(searchText, result)) {
            return userSearchModels;
        }

        searchUsingPlugins(searchText, userSearchModels);

        if (userSearchModels.size() == 0 && !result.hasMessage()) {
            result.setMessage(LocalizedMessage.string("NO_SEARCH_RESULTS_ERROR"));
        }
        return userSearchModels;
    }

    private void searchUsingPlugins(String searchText, List<UserSearchModel> userSearchModels) {
        List<User> searchResults = new ArrayList<>();
        for (final String pluginId : getAuthorizationAndAuthenticationPlugins()) {
            try {
                List<com.thoughtworks.go.plugin.access.authentication.models.User> users = getUsersConfiguredViaPlugin(pluginId, searchText);
                if (users != null && !users.isEmpty()) {
                    for (com.thoughtworks.go.plugin.access.authentication.models.User user : users) {
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

    private List<com.thoughtworks.go.plugin.access.authentication.models.User> getUsersConfiguredViaPlugin(String pluginId, String searchTerm) {
        List<com.thoughtworks.go.plugin.access.authentication.models.User> users = new ArrayList<>();
        if (authorizationExtension.canHandlePlugin(pluginId)) {
            List<SecurityAuthConfig> authConfigs = goConfigService.security().securityAuthConfigs().findByPluginId(pluginId);
            users.addAll(authorizationExtension.searchUsers(pluginId, searchTerm, authConfigs));
        }
        if (authenticationExtension.canHandlePlugin(pluginId)) {
            users.addAll(authenticationExtension.searchUser(pluginId, searchTerm));
        }
        return users;
    }

    private Set<String> getAuthorizationAndAuthenticationPlugins() {
        Set<String> authPlugins = new HashSet<>();
        Set<String> pluginsThatSupportsUserSearch = store.getPluginsThatSupportsUserSearch();
        Set<String> authenticationPlugins = authenticationPluginRegistry.getAuthenticationPlugins();
        authPlugins.addAll(pluginsThatSupportsUserSearch);
        authPlugins.addAll(authenticationPlugins);
        return authPlugins;
    }

    private boolean isInputValid(String searchText, HttpLocalizedOperationResult result) {
        if (searchText.trim().length() < MINIMUM_SEARCH_STRING_LENGTH) {
            result.badRequest(LocalizedMessage.string("SEARCH_STRING_TOO_SMALL"));
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
