/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.domain.support.toggle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FeatureToggleTest {
    @Test
    public void shouldKnowWhenItIsOn() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", true).isOn()).isTrue();
    }

    @Test
    public void shouldKnowWhenItIsOff() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).isOn()).isFalse();
    }

    @Test
    public void shouldKnowWhenItHasTheSameKeyAsTheProvidedOne() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key1")).isTrue();
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("KEY1")).isTrue();
        assertThat(new FeatureToggle("KEY1", "desc1", false).hasSameKeyAs("key1")).isTrue();
    }

    @Test
    public void shouldKnowWhenItDoesNotHaveTheSameKeyAsTheProvidedOne() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key2")).isFalse();
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key1_and_suffix")).isFalse();
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("prefix_for_key1")).isFalse();
    }

    @Test
    public void shouldBeAbleToIndicateThatItsValueHasBeenChanged() throws Exception {
        FeatureToggle existingToggle = new FeatureToggle("key1", "desc1", false);
        FeatureToggle toggleWithValueChangedFlagSet = new FeatureToggle("key1", "desc1", false).withValueHasBeenChangedFlag(true);

        assertThat(existingToggle).isNotEqualTo(toggleWithValueChangedFlagSet);
        assertThat(toggleWithValueChangedFlagSet.hasBeenChangedFromDefault()).isTrue();
    }

    @Test
    public void shouldBeAbleToCompareItsValueWithThatOfAnotherToggle() throws Exception {
        FeatureToggle toggleWithValueTrue = new FeatureToggle("key1", "desc1", true);
        FeatureToggle toggleWithValueFalse = new FeatureToggle("key2", "desc2", false);

        assertThat(toggleWithValueTrue.hasSameValueAs(toggleWithValueTrue)).isTrue();
        assertThat(toggleWithValueTrue.hasSameValueAs(toggleWithValueFalse)).isFalse();
    }
}
