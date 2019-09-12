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
package com.thoughtworks.go.apiv1.datasharing.settings.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.domain.DataSharingSettings
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.util.TimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import java.sql.Timestamp

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class DataSharingSettingsRepresenterTest {
    @BeforeEach
    void setUp() {
        initMocks(this)
    }

    @Mock
    TimeProvider timeProvider

    @Test
    void "should represent data sharing settings"() {
        def sharingSettings = new DataSharingSettings()
                .setAllowSharing(true)
                .setUpdatedBy("Bob")
                .setUpdatedOn(new Timestamp(new Date().getTime()))

        def actualJson = toObjectString({ DataSharingSettingsRepresenter.toJSON(it, sharingSettings) })

        def expectedJson = [
                _links     : [
                        self: [href: 'http://test.host/go/api/data_sharing/settings'],
                        doc : [href: 'https://api.go.cd/current/#data_sharing_settings']
                ],
                "_embedded": [
                        allow     : sharingSettings.allowSharing,
                        updated_by: sharingSettings.updatedBy,
                        updated_on: jsonDate(sharingSettings.updatedOn)
                ]
        ]

        assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void "should deserialize data sharing settings"() {
        def json = [
                allow: true
        ]

        def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
        def time = 10000000l
        when(timeProvider.currentTimeMillis()).thenReturn(time)
        def deserializedSettings = DataSharingSettingsRepresenter.fromJSON(jsonReader,
                new Username("user"), timeProvider,
                new DataSharingSettings().setUpdatedBy("me").setUpdatedOn(new Timestamp(new Date().getTime())))
        assertThat(deserializedSettings.allowSharing).isEqualTo(true)
        assertThat(deserializedSettings.updatedBy).isEqualTo("user")
        assertThat(deserializedSettings.updatedOn).isEqualTo(new Timestamp(time))
    }

    @Test
    void "should set allow flag from the object from server if the user does not pass it along during deserialization of data sharing settings"() {
        def json = [a: ""]

        def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
        def time = 10000000l
        when(timeProvider.currentTimeMillis()).thenReturn(time)
        def deserializedSettings = DataSharingSettingsRepresenter.fromJSON(jsonReader,
                new Username("user"), timeProvider,
                new DataSharingSettings().setAllowSharing(false).setUpdatedBy("me").setUpdatedOn(new Timestamp(new Date().getTime())))
        assertThat(deserializedSettings.allowSharing).isEqualTo(false)
        assertThat(deserializedSettings.updatedBy).isEqualTo("user")
        assertThat(deserializedSettings.updatedOn).isEqualTo(new Timestamp(time))
    }
}
