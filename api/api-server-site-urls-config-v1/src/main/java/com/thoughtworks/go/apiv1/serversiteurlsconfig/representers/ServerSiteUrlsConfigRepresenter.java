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
package com.thoughtworks.go.apiv1.serversiteurlsconfig.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.SiteUrls;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class ServerSiteUrlsConfigRepresenter {

    public static void toJSON(OutputWriter writer, SiteUrls siteUrls) {
        writer.addLinks(outputLinkWriter -> outputLinkWriter
                .addAbsoluteLink("doc", apiDocsUrl("#siteurls-config"))
                .addLink("self", Routes.ServerSiteUrlsConfig.BASE))
                .add("site_url", siteUrls.getSiteUrl().toString())
                .add("secure_site_url", siteUrls.getSecureSiteUrl().toString());

        if (!siteUrls.getSiteUrl().errors().isEmpty() || !siteUrls.getSecureSiteUrl().errors().isEmpty()) {
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("siteUrl", "site_url");
            fieldMapping.put("secureSiteUrl", "secure_site_url");
            writer.addChild("errors", errorWriter -> {
                ErrorGetter errorGetter = new ErrorGetter(fieldMapping);
                errorGetter.toJSON(errorWriter, siteUrls.getSiteUrl());
                errorGetter.toJSON(errorWriter, siteUrls.getSecureSiteUrl());
            });
        }
    }

    public static SiteUrls fromJson(JsonReader jsonReader) {
        String siteUrl = jsonReader.getStringOrDefault("site_url", null);
        String secureSiteUrl = jsonReader.getStringOrDefault("secure_site_url", null);
        return new SiteUrls(new SiteUrl(siteUrl), new SecureSiteUrl(secureSiteUrl));
    }
}
