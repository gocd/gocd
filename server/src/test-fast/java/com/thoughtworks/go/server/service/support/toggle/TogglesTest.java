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
package com.thoughtworks.go.server.service.support.toggle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TogglesTest {
    @Mock
    private FeatureToggleService featureToggleService;

    public static final String FEATURE_TOGGLE_KEY = "key";

    @Before
    public void setup() {
        initMocks(this);
        Toggles.initializeWith(featureToggleService);

        when(featureToggleService.isToggleOn(FEATURE_TOGGLE_KEY)).thenReturn(true);
    }

    @Test
    public void shouldDelegateToService_isToggleOn() {
        assertThat(Toggles.isToggleOn(FEATURE_TOGGLE_KEY), is(true));
        verify(featureToggleService).isToggleOn(FEATURE_TOGGLE_KEY);
    }

    @Test
    public void shouldBombIfServiceUnavailable_isToggleOn() {
        Toggles.initializeWith(null);
        try {
            Toggles.isToggleOn(FEATURE_TOGGLE_KEY);
            fail("Should have bombed!");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Toggles not initialized with feature toggle service"));
        }
    }
}
