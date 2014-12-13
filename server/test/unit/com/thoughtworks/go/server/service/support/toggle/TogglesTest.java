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
