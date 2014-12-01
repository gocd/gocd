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

import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FeatureToggleServiceTest {
    @Mock
    public FeatureToggleRepository repository;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldListAllFeatureToggles() throws Exception {
        List<FeatureToggle> existingToggles = Arrays.asList(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.allToggles()).thenReturn(existingToggles);

        FeatureToggleService service = new FeatureToggleService(repository);

        assertThat(service.allToggles(), is(existingToggles));
    }

    @Test
    public void shouldKnowWhetherAToggleIsAvailable() throws Exception {
        List<FeatureToggle> existingToggles = Arrays.asList(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.allToggles()).thenReturn(existingToggles);

        FeatureToggleService service = new FeatureToggleService(repository);

        assertThat(service.isToggleAvailable("key1"), is(true));
        assertThat(service.isToggleAvailable("NON_EXISTENT_KEY"), is(false));
    }

    @Test
    public void shouldKnowWhetherAToggleIsOnOrOff() throws Exception {
        List<FeatureToggle> existingToggles = Arrays.asList(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.allToggles()).thenReturn(existingToggles);

        FeatureToggleService service = new FeatureToggleService(repository);

        assertThat(service.isToggleOn("key1"), is(true));
        assertThat(service.isToggleOn("key2"), is(false));
    }

    @Test
    public void shouldSayThatNonExistentTogglesAreOff() throws Exception {
        List<FeatureToggle> existingToggles = Arrays.asList(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.allToggles()).thenReturn(existingToggles);

        FeatureToggleService service = new FeatureToggleService(repository);

        assertThat(service.isToggleOn("NON_EXISTENT_KEY"), is(false));
    }
}
