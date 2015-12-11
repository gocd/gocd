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

package com.thoughtworks.go.listener;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.server.cache.GoCache;
import org.apache.log4j.Logger;

public class BaseUrlChangeListener implements ConfigChangedListener {
    private static final Logger LOGGER = Logger.getLogger(BaseUrlChangeListener.class);

    private ServerSiteUrlConfig siteUrl;
    private ServerSiteUrlConfig secureSiteUrl;
    private GoCache goCache;

    public static final String URLS_CACHE_KEY = "urls_cache";

    public BaseUrlChangeListener(ServerSiteUrlConfig siteUrl, ServerSiteUrlConfig secureSiteUrl,
                                 GoCache goCache) {
        setUrls(siteUrl, secureSiteUrl);
        this.goCache = goCache;
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        ServerConfig newServerConfig = newCruiseConfig.server();
        ServerSiteUrlConfig newSecureSiteUrl = newServerConfig.getSecureSiteUrl();
        ServerSiteUrlConfig newSiteUrl = newServerConfig.getSiteUrl();

        if (!(secureSiteUrl.equals(newSecureSiteUrl) && siteUrl.equals(newSiteUrl))) {
            goCache.remove(URLS_CACHE_KEY);
            LOGGER.info(String.format("[Configuration Changed] Site URL was changed from [%s] to [%s] and "
                    + "Secure Site URL was changed from [%s] to [%s]", siteUrl, newSiteUrl, secureSiteUrl, newSecureSiteUrl));
        }

        setUrls(newSiteUrl, newSecureSiteUrl);
    }

    private void setUrls(ServerSiteUrlConfig siteUrl, ServerSiteUrlConfig secureSiteUrl) {
        this.siteUrl = siteUrl;
        this.secureSiteUrl = secureSiteUrl;
    }

}
