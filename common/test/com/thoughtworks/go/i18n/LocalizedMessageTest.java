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

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import org.joda.time.Duration;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalizedMessageTest {
    private Localizer localizer;

    @Test
    public void shouldUseTheProvidedLocalizerToLocalize() {
        when(localizer.localize(anyString(), anyVararg())).thenReturn("whateva");
        assertThat(LocalizedMessage.string("me").localize(localizer), is("whateva"));
    }

    @Test
    public void shouldUseTheProvidedLocalizerToLocalizeWithArgs() {
        when(localizer.localize("ME", "for-real")).thenReturn("whateva");
        assertThat(LocalizedMessage.string("me", Arrays.asList("for-real")).localize(localizer), is("whateva"));
    }

    @Before public void setUp() throws Exception {
        localizer = mock(Localizer.class);
    }

    private enum Enum {
        X,
        yyy
    }

    @Test public void shouldLocalizeEnums() throws Exception {
        when(localizer.localize(anyString(), anyVararg())).thenReturn("ರಘುನಂದನ");
        assertThat(LocalizedMessage.messageFor(Enum.X).localize(localizer), is("ರಘುನಂದನ"));
        when(localizer.localize(anyString(), anyVararg())).thenReturn("ಪವನ");
        assertThat(LocalizedMessage.messageFor(Enum.yyy).localize(localizer), is("ಪವನ"));
    }

    @Test public void shouldLocalizeDurations() throws Exception {
        Duration theDuration = new Duration(1000);
        String expected = "it took a long time";
        when(localizer.localize(theDuration)).thenReturn(expected);
        assertThat(LocalizedMessage.localizeDuration(theDuration).localize(localizer), is(expected));
    }

    @Test public void shouldUseCapitalizedNoSpaceKeyToFindStringLocalization() throws Exception {
        when(localizer.localize(anyString(), anyVararg())).thenReturn("helped!");
        String s = "Help me";
        assertThat(LocalizedMessage.string(s).localize(localizer), is("helped!"));
    }
}
