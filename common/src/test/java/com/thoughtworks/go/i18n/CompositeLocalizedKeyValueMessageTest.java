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

package com.thoughtworks.go.i18n;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompositeLocalizedKeyValueMessageTest {
    @Test
    public void shouldLocalizeCompositeMessages() {
        Localizer localizer = mock(Localizer.class);
        Localizable localizable = LocalizedMessage.composite(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", "staging"), LocalizedMessage.string("CONFIG_MERGED"));
        when(localizer.localize("UPDATE_ENVIRONMENT_SUCCESS", "staging")).thenReturn("Updated staging successfully.");
        when(localizer.localize("CONFIG_MERGED",new Object[]{})).thenReturn("Config Merged Successfully.");

        String message = localizable.localize(localizer);
        assertThat(message, is("Updated staging successfully. Config Merged Successfully."));
    }

    @Test
    public void shouldReturnEmptyMessageWhenEmpty() {
        Localizable composite = LocalizedMessage.composite();
        String localizedMessage = composite.localize(mock(Localizer.class));
        assertThat(localizedMessage, is(""));
    }
}
