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
package com.thoughtworks.go.server.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class DataSharingSettingsTest {
    @Test
    public void shouldCopyDataSharingAttributesFromProvidedObject() {
        Date currentUpdatedOn = new Date();
        currentUpdatedOn.setTime(currentUpdatedOn.getTime() - 10000);

        DataSharingSettings current = new DataSharingSettings(false, "default", currentUpdatedOn);
        DataSharingSettings latest = new DataSharingSettings(true, "Bob", new Date());

        assertThat(current.allowSharing(), is(not(latest.allowSharing())));
        assertThat(current.updatedBy(), is(not(latest.updatedBy())));
        assertThat(current.updatedOn().getTime(), is(not(latest.updatedOn().getTime())));

        current.copyFrom(latest);

        assertThat(current.allowSharing(), is(latest.allowSharing()));
        assertThat(current.updatedBy(), is(latest.updatedBy()));
        assertThat(current.updatedOn().getTime(), is(latest.updatedOn().getTime()));
    }

    @Test
    public void shouldNotCopyPersistentObjectIdWhileCopying() {
        DataSharingSettings current = new DataSharingSettings();
        current.setId(1);
        DataSharingSettings latest = new DataSharingSettings();
        latest.setId(2);

        assertThat(current.getId(), is(1L));
        current.copyFrom(latest);
        assertThat(current.getId(), is(1L));
    }
}
