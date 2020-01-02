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
package com.thoughtworks.go.apiv2.datasharing.reporting.representers

import com.thoughtworks.go.domain.UsageStatisticsReporting
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UsageStatisticsReportingRepresenterTest {
    def dataSharingServerUrl = 'https://datasharing.gocd.org'
    def dataSharingGetEncryptionKeysUrl = 'https://datasharing.gocd.org/encryption_keys'
    @Test
    void "should represent usage statistics reporting"() {
        def metricsReporting = new UsageStatisticsReporting("server-id", new Date())
        def statsSharedAt = new Date()
        metricsReporting.setLastReportedAt(statsSharedAt)
        metricsReporting.setDataSharingServerUrl(dataSharingServerUrl)
        metricsReporting.setDataSharingGetEncryptionKeysUrl(dataSharingGetEncryptionKeysUrl)

        def actualJson = toObjectString({ UsageStatisticsReportingRepresenter.toJSON(it, metricsReporting) })

        def expectedJson = [
                _links     : [
                    self: [href: 'http://test.host/go/api/internal/data_sharing/reporting']
                ],
                "_embedded": [
                  server_id                           : metricsReporting.getServerId(),
                  data_sharing_server_url             : dataSharingServerUrl,
                  data_sharing_get_encryption_keys_url: dataSharingGetEncryptionKeysUrl,
                  last_reported_at                    : metricsReporting.lastReportedAt().getTime(),
                  can_report                          : true
                ]
        ]

        assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void "should represent usage statistics reporting when last_reported_at is unset"() {
        def metricsReporting = new UsageStatisticsReporting("server-id", new Date(0l))
        metricsReporting.setDataSharingServerUrl(dataSharingServerUrl)
        metricsReporting.setDataSharingGetEncryptionKeysUrl(dataSharingGetEncryptionKeysUrl)

        def actualJson = toObjectString({ UsageStatisticsReportingRepresenter.toJSON(it, metricsReporting) })

        def expectedJson = [
                _links     : [
                  self: [href: 'http://test.host/go/api/internal/data_sharing/reporting']
                ],
                "_embedded": [
                  server_id                           : metricsReporting.getServerId(),
                  data_sharing_server_url             : dataSharingServerUrl,
                  data_sharing_get_encryption_keys_url: dataSharingGetEncryptionKeysUrl,
                  last_reported_at                    : 0,
                  can_report                          : true
                ]
        ]

        assertThatJson(actualJson).isEqualTo(expectedJson)
    }
}
