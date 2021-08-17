/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.support.toggle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TogglesTest {
    @Mock
    private FeatureToggleService featureToggleService;

    public static final String FEATURE_TOGGLE_KEY = "key";

    @BeforeEach
    public void setup() {
        Toggles.initializeWith(featureToggleService);
    }

    @Test
    public void shouldDelegateToService_isToggleOn() {
        when(featureToggleService.isToggleOn(FEATURE_TOGGLE_KEY)).thenReturn(true);
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
