package com.thoughtworks.go.server.domain.support.toggle;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class FeatureToggleTest {
    @Test
    public void shouldKnowWhenItIsOn() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", true).isOn(), is(true));
    }

    @Test
    public void shouldKnowWhenItIsOff() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).isOn(), is(false));
    }

    @Test
    public void shouldKnowWhenItHasTheSameKeyAsTheProvidedOne() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key1"), is(true));
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("KEY1"), is(true));
        assertThat(new FeatureToggle("KEY1", "desc1", false).hasSameKeyAs("key1"), is(true));
    }

    @Test
    public void shouldKnowWhenItDoesNotHaveTheSameKeyAsTheProvidedOne() throws Exception {
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key2"), is(false));
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("key1_and_suffix"), is(false));
        assertThat(new FeatureToggle("key1", "desc1", false).hasSameKeyAs("prefix_for_key1"), is(false));
    }
}