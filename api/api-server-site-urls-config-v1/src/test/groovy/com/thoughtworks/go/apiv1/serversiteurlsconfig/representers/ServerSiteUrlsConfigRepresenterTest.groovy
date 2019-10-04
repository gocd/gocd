/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.serversiteurlsconfig.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.config.SiteUrls
import com.thoughtworks.go.domain.SecureSiteUrl
import com.thoughtworks.go.domain.ServerSiteUrlConfig
import com.thoughtworks.go.domain.SiteUrl
import org.junit.jupiter.api.Test
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import com.thoughtworks.go.api.util.GsonTransformer
import static org.assertj.core.api.Assertions.assertThat

class ServerSiteUrlsConfigRepresenterTest {

  @Test
  void 'should represent site urls'() {
    def siteUrls = new SiteUrls(new SiteUrl("http://foo"), new SecureSiteUrl("https://bar"))
    def json = toObjectString({ ServerSiteUrlsConfigRepresenter.toJSON(it, siteUrls) })
    assertThatJson(json).isEqualTo(["_links"         :
                                      ["doc" : ["href": apiDocsUrl("#create-site-url")],
                                       "self": ["href": "http://test.host/go/api/admin/config/server/site_urls"]],
                                    "site_url"       : "http://foo",
                                    "secure_site_url": "https://bar"])
  }

  @Test
  void 'should deserialize site urls configuration'() {
    def json = ["site_url"       : "http://foo",
                "secure_site_url": "https://foo"]
    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(json)
    def config = ServerSiteUrlsConfigRepresenter.fromJson(jsonReader)

    def serverSiteUrlsConfig = new SiteUrls(new SiteUrl("http://foo"), new SecureSiteUrl("https://foo"))

    assertThat(config).isEqualTo(serverSiteUrlsConfig)
  }
}
