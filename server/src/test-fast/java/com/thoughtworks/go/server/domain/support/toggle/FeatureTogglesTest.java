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
package com.thoughtworks.go.server.domain.support.toggle;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FeatureTogglesTest {
    @Test
    public void shouldDoNothingWhenThereAreNoAvailableTogglesToMerge() throws Exception {
        FeatureToggles emptyAvailableToggles = new FeatureToggles();
        FeatureToggles nonEmptyOverridingToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));

        FeatureToggles actual = emptyAvailableToggles.overrideWithTogglesIn(nonEmptyOverridingToggles);
        assertThat(actual, is(new FeatureToggles()));
    }

    @Test
    public void shouldDoNothingWhenNoOverrideTogglesAreProvided() throws Exception {
        FeatureToggles nonEmptyAvailableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles emptyOverridingToggles = new FeatureToggles();

        FeatureToggles actual = nonEmptyAvailableToggles.overrideWithTogglesIn(emptyOverridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "desc1", true))));
    }

    @Test
    public void shouldOverrideDescription_WithValueChangedFlagTrue_WhenValueHasBeenChanged() throws Exception {
        FeatureToggles availableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles overridingToggles = new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", false));

        FeatureToggles actual = availableToggles.overrideWithTogglesIn(overridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", false).withValueHasBeenChangedFlag(true))));
    }

    @Test
    public void shouldOverrideDescription_WithValueChangedFlagFalse_WhenValueHasNotBeenChanged() throws Exception {
        FeatureToggles availableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles overridingToggles = new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", true));

        FeatureToggles actual = availableToggles.overrideWithTogglesIn(overridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", true).withValueHasBeenChangedFlag(false))));
    }

    @Test
    public void shouldNotOverrideDescriptionIfOverriddenDescriptionIsNotPresent() throws Exception {
        FeatureToggles availableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles overridingToggles = new FeatureToggles(new FeatureToggle("key1", null, false));

        FeatureToggles actual = availableToggles.overrideWithTogglesIn(overridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "desc1", false).withValueHasBeenChangedFlag(true))));
    }

    @Test
    public void shouldChangeValueOfAnExistingToggle() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true), new FeatureToggle("key2", "desc2", false));

        FeatureToggles newToggles = existingToggles.changeToggleValue("key2", true);

        assertThat(existingToggles.all().size(), is(2));
        assertThat(existingToggles.all().get(0), is(new FeatureToggle("key1", "desc1", true)));
        assertThat(existingToggles.all().get(1), is(new FeatureToggle("key2", "desc2", false)));

        assertThat(newToggles.all().size(), is(2));
        assertThat(newToggles.all().get(0), is(new FeatureToggle("key1", "desc1", true)));
        assertThat(newToggles.all().get(1), is(new FeatureToggle("key2", "desc2", true)));
    }

    @Test
    public void shouldAppendANewToggleWhenTryingToChangeValueOfANonExistentToggle() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true), new FeatureToggle("key2", "desc2", false));

        FeatureToggles newToggles = existingToggles.changeToggleValue("key_NOT_EXISTENT", true);

        assertThat(existingToggles.all().size(), is(2));
        assertThat(existingToggles.all().get(0), is(new FeatureToggle("key1", "desc1", true)));
        assertThat(existingToggles.all().get(1), is(new FeatureToggle("key2", "desc2", false)));

        assertThat(newToggles.all().size(), is(3));
        assertThat(newToggles.all().get(0), is(new FeatureToggle("key1", "desc1", true)));
        assertThat(newToggles.all().get(1), is(new FeatureToggle("key2", "desc2", false)));
        assertThat(newToggles.all().get(2), is(new FeatureToggle("key_NOT_EXISTENT", null, true)));
    }
}