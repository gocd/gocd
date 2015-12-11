/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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

package com.thoughtworks.go.listener;

import java.io.IOException;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.server.cache.GoCache;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class BaseUrlChangeListenerTest {

    @Test
    public void shouldFlushCacheWhenBaseUrlConfigChangesAndUpdateTheSiteURLAndSecureSiteURLToTheNewValues() throws IOException {
        GoCache cache = mock(GoCache.class);
        BaseUrlChangeListener listener = new BaseUrlChangeListener(new ServerSiteUrlConfig(""),
                new ServerSiteUrlConfig(""), cache);
        CruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfigWith("http://blah.com", "https://blah.com"));

        listener.onConfigChange(newCruiseConfig);
        listener.onConfigChange(newCruiseConfig);

        verify(cache, times(1)).remove("urls_cache");
    }

    @Test
    public void shouldNotFlushCacheWhenBaseUrlConfigIsNotChanged() {
        GoCache cache = mock(GoCache.class);
        BaseUrlChangeListener listener = new BaseUrlChangeListener(new ServerSiteUrlConfig(""), new ServerSiteUrlConfig(""), cache);
        CruiseConfig newCruiseConfig = new BasicCruiseConfig();
        newCruiseConfig.setServerConfig(serverConfigWith("", ""));

        listener.onConfigChange(newCruiseConfig);
        verifyZeroInteractions(cache);
    }

    private ServerConfig serverConfigWith(String siteUrl, String secureUrl) {
        return new ServerConfig(null, null, new ServerSiteUrlConfig(siteUrl), new ServerSiteUrlConfig(secureUrl));
    }
}
