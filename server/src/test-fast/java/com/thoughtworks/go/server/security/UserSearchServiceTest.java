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


import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.presentation.UserSearchModel;
import com.thoughtworks.go.presentation.UserSourceType;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ClearSingleton.class)
public class UserSearchServiceTest  {
    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private GoConfigService goConfigService;
    private UserSearchService userSearchService;

    @BeforeEach
    public void setUp() {
        userSearchService = new UserSearchService(authorizationExtension, goConfigService);
    }

    @Test
    public void shouldSearchUserUsingPlugins() throws Exception {
        final String searchTerm = "foo";
        List<String> pluginIds = asList("plugin-id-1", "plugin-id-2", "plugin-id-3", "plugin-id-4");

        addPluginSupportingUserSearch(pluginIds.get(0));
        addPluginSupportingUserSearch(pluginIds.get(1));
        addPluginSupportingUserSearch(pluginIds.get(2));
        addPluginSupportingUserSearch(pluginIds.get(3));
        when(authorizationExtension.canHandlePlugin(anyString())).thenReturn(true);
        when(goConfigService.security()).thenReturn(new SecurityConfig());
        when(authorizationExtension.searchUsers("plugin-id-1", searchTerm, Collections.emptyList())).thenReturn(asList(getPluginUser(1)));
        when(authorizationExtension.searchUsers("plugin-id-2", searchTerm, Collections.emptyList())).thenReturn(asList(getPluginUser(2), getPluginUser(3)));
        when(authorizationExtension.searchUsers("plugin-id-3", searchTerm, Collections.emptyList())).thenReturn(new ArrayList<>());
        when(authorizationExtension.searchUsers("plugin-id-4", searchTerm, Collections.emptyList())).thenReturn(asList(new User("username-" + 4, null, null)));

        List<UserSearchModel> models = userSearchService.search(searchTerm, new HttpLocalizedOperationResult());

        assertThat(models, Matchers.containsInAnyOrder(
                new UserSearchModel(getUser(1), UserSourceType.PLUGIN),
                new UserSearchModel(getUser(2), UserSourceType.PLUGIN),
                new UserSearchModel(getUser(3), UserSourceType.PLUGIN),
                new UserSearchModel(new com.thoughtworks.go.domain.User ("username-" + 4, "", ""), UserSourceType.PLUGIN)
        ));
    }

    @Test
    public void shouldReturnWarningMessageWhenSearchReturnsNoResults() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userSearchService.search("foo", result);
        assertThat(result.message(), is("No results found."));
    }

    @Test
    public void shouldNotInvokeSearchWhenUserSearchTextIsTooSmall() throws Exception {
        String smallSearchText = "f";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userSearchService.search(smallSearchText, result);

        verifyNoInteractions(authorizationExtension);
        assertThat(result.message(), is("Please use a search string that has at least two (2) letters."));
    }

    @Test
    public void shouldNotInvokeSearchWhenUserSearchTextIsTooSmallAfterTrimming() throws Exception {
        String smallSearchText = "a ";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        userSearchService.search(smallSearchText, result);

        verifyNoInteractions(authorizationExtension);
        assertThat(result.message(), is("Please use a search string that has at least two (2) letters."));
    }

    private com.thoughtworks.go.domain.User  getUser(Integer userId) {
        return new com.thoughtworks.go.domain.User ("username-" + userId, "display-name-" + userId, "test" + userId + "@test.com");
    }

    private User getPluginUser(Integer userId) {
        return new User("username-" + userId, "display-name-" + userId, "test" + userId + "@test.com");
    }

    private void addPluginSupportingUserSearch(String pluginId) {
        AuthorizationPluginInfo pluginInfo = new AuthorizationPluginInfo(
                GoPluginDescriptor.builder().id(pluginId).build(), null, null, null,
                new Capabilities(SupportedAuthType.Password, true, true, false));
        AuthorizationMetadataStore.instance().setPluginInfo(pluginInfo);
    }
}
