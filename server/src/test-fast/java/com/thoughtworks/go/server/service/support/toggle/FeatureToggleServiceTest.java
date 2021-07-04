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

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggle;
import com.thoughtworks.go.server.domain.support.toggle.FeatureToggles;
import com.thoughtworks.go.server.service.StubGoCache;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FeatureToggleServiceTest {
    @Mock
    private FeatureToggleRepository repository;
    @Mock
    private GoCache goCache;

    @Test
    public void shouldListAllFeatureToggles() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.availableToggles()).thenReturn(existingToggles);
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, goCache);

        assertThat(service.allToggles(), is(existingToggles));
    }

    @Test
    public void shouldKnowWhetherAToggleIsOnOrOff() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.availableToggles()).thenReturn(existingToggles);
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, goCache);

        assertThat(service.isToggleOn("key1"), is(true));
        assertThat(service.isToggleOn("key2"), is(false));
    }

    @Test
    public void shouldSayThatNonExistentTogglesAreOff() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.availableToggles()).thenReturn(existingToggles);
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, goCache);

        assertThat(service.isToggleOn("NON_EXISTENT_KEY"), is(false));
    }

    @Test
    public void honorTogglesDefinedInInUserTogglesEvenIfNotEnumeratedInAvailableToggles() throws Exception {
        FeatureToggles existingToggles = new FeatureToggles(
                new FeatureToggle("key1", "description1", true),
                new FeatureToggle("key2", "description2", false)
        );

        when(repository.userToggles()).thenReturn(new FeatureToggles(
                new FeatureToggle("key3", "whatever", true)
        ));

        FeatureToggleService service = new FeatureToggleService(repository, goCache);

        assertTrue(service.isToggleOn("key3"));
    }

    @Test
    public void shouldOverrideAvailableToggleValuesWithValuesFromUsersToggles() throws Exception {
        FeatureToggle availableToggle1 = new FeatureToggle("key1", "desc1", true);
        FeatureToggle availableToggle2 = new FeatureToggle("key2", "desc2", true);
        FeatureToggle availableToggle3 = new FeatureToggle("key3", "desc3", true);
        when(repository.availableToggles()).thenReturn(new FeatureToggles(availableToggle1, availableToggle2, availableToggle3));

        FeatureToggle userToggle1 = new FeatureToggle("key1", "NEW_desc1_WITH_NO_change_to_value", true);
        FeatureToggle userToggle2 = new FeatureToggle("key2", "NEW_desc2_WITH_CHANGE_TO_VALUE", false);
        when(repository.userToggles()).thenReturn(new FeatureToggles(userToggle1, userToggle2));

        FeatureToggleService service = new FeatureToggleService(repository, goCache);
        FeatureToggles toggles = service.allToggles();

        assertThat(toggles.all().size(), is(3));
        assertThat(toggles.all().get(0), is(new FeatureToggle("key1", "NEW_desc1_WITH_NO_change_to_value", true).withValueHasBeenChangedFlag(false)));
        assertThat(toggles.all().get(1), is(new FeatureToggle("key2", "NEW_desc2_WITH_CHANGE_TO_VALUE", false).withValueHasBeenChangedFlag(true)));
        assertThat(toggles.all().get(2), is(new FeatureToggle("key3", "desc3", true).withValueHasBeenChangedFlag(false)));
    }

    @Test
    public void shouldAllowChangingValueOfAValidFeatureToggle() throws Exception {
        FeatureToggle availableToggle1 = new FeatureToggle("key1", "desc1", true);
        when(repository.availableToggles()).thenReturn(new FeatureToggles(availableToggle1));
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, goCache);
        service.changeValueOfToggle("key1", false);

        verify(repository).changeValueOfToggle("key1", false);
    }

    @Test
    public void shouldNotAllowChangingValueOfAnInvalidFeatureToggle() throws Exception {
        FeatureToggle availableToggle1 = new FeatureToggle("key1", "desc1", true);
        when(repository.availableToggles()).thenReturn(new FeatureToggles(availableToggle1));
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, goCache);

        try {
            service.changeValueOfToggle("keyNOTVALID", true);
            fail("This should have failed with an exception, since the feature toggle is invalid.");
        } catch (RecordNotFoundException e) {
            assertThat(e.getMessage(), is("Feature toggle: 'keyNOTVALID' is not valid."));
        }
    }

    @Test
    public void shouldCacheFeatureToggleStatus() throws Exception {
        when(repository.availableToggles()).thenReturn(new FeatureToggles(new FeatureToggle("key1", "desc1", true)));
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, new StubGoCache(new TestTransactionSynchronizationManager()));
        service.allToggles();
        service.allToggles();
        service.isToggleOn("key1");
        service.isToggleOn("someOtherKey");

        verify(repository, times(1)).availableToggles();
    }

    @Test
    public void shouldInvalidateCacheWhenAFeatureTogglesValueIsChanged() throws Exception {
        when(repository.availableToggles()).thenReturn(new FeatureToggles(new FeatureToggle("key1", "desc1", true)));
        when(repository.userToggles()).thenReturn(new FeatureToggles());

        FeatureToggleService service = new FeatureToggleService(repository, new StubGoCache(new TestTransactionSynchronizationManager()));
        service.allToggles();
        verify(repository, times(1)).availableToggles();
        verify(repository, times(1)).userToggles();

        service.changeValueOfToggle("key1", false);
        verify(repository, times(1)).availableToggles();
        verify(repository, times(1)).userToggles();

        service.allToggles();
        verify(repository, times(2)).availableToggles();
        verify(repository, times(2)).userToggles();
    }
}
