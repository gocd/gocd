package com.thoughtworks.go.server.domain.support.toggle;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FeatureTogglesTest {
    @Test
    public void shouldDoNothingWhenThereAreNoValuesToMerge() throws Exception {
        FeatureToggles emptyAvailableToggles = new FeatureToggles();
        FeatureToggles nonEmptyOverridingToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));

        FeatureToggles actual = emptyAvailableToggles.mergeMatchingOnesWithValuesFrom(nonEmptyOverridingToggles);
        assertThat(actual, is(new FeatureToggles()));
    }

    @Test
    public void shouldDoNothingWhenNoOverrideValuesAreProvided() throws Exception {
        FeatureToggles nonEmptyAvailableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles emptyOverridingToggles = new FeatureToggles();

        FeatureToggles actual = nonEmptyAvailableToggles.mergeMatchingOnesWithValuesFrom(emptyOverridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "desc1", true))));
    }

    @Test
    public void shouldOverrideDescription_WithValueChangedFlagTrue_WhenValueHasBeenChanged() throws Exception {
        FeatureToggles availableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles overridingToggles = new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", false));

        FeatureToggles actual = availableToggles.mergeMatchingOnesWithValuesFrom(overridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", false).withValueChanged(true))));
    }

    @Test
    public void shouldOverrideDescription_WithValueChangedFlagFalse_WhenValueHasNotBeenChanged() throws Exception {
        FeatureToggles availableToggles = new FeatureToggles(new FeatureToggle("key1", "desc1", true));
        FeatureToggles overridingToggles = new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", true));

        FeatureToggles actual = availableToggles.mergeMatchingOnesWithValuesFrom(overridingToggles);
        assertThat(actual, is(new FeatureToggles(new FeatureToggle("key1", "NEW_desc1_WITH_VALUE_CHANGED", true).withValueChanged(false))));
    }
}