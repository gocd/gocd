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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.exception.UncheckedValidationException;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.MaterialsMother;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.helper.ModificationsMother.aCheckIn;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTest {
    private MaterialRevisions materialRevisions;
    private User user;

    @Test
    void shouldTrimTheUserNameAndMatcher() {
        user = new User(" UserName ", "Full User Name", new String[]{" README "}, " user@mail.com ", true);
        assertThat(user.getName()).isEqualTo("UserName");
        assertThat(user.getMatcher()).isEqualTo(("README"));
        assertThat(user.getEmail()).isEqualTo("user@mail.com");
        assertThat(user.getDisplayName()).isEqualTo("Full User Name");
    }

    @Test
    void shouldNotMatchWhenUserDidNotSetUpTheMatcher() {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "readme")));
        assertThat(new User("UserName", new String[]{null}, "user@mail.com", true).matchModification(materialRevisions)).isFalse();
        assertThat(new User("UserName", new String[]{""}, "user@mail.com", true).matchModification(materialRevisions)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenEmailIsEmpty() {
        assertThat(new User("UserName", new String[]{"README"}, null, true).matchNotification(null, StageEvent.All, null)).isFalse();
        assertThat(new User("UserName", new String[]{"README"}, "", true).matchNotification(null, StageEvent.All, null)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenNotificationFilterMatchesMyCheckinOnGivenStageFixed() {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "readme")));
        user = new User("UserName", new String[]{"README"}, "user@mail.com", true);
        user.setNotificationFilters(
                Arrays.asList(new NotificationFilter("cruise", "dev", StageEvent.Fixed, true)));
        assertThat(user.matchNotification(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed, materialRevisions)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenNotificationFilterMatchesAnyCheckinOnGivenStageFixed() {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "xyz")));
        user = new User("UserName", new String[]{"README"}, "user@mail.com", true);
        user.setNotificationFilters(
                Arrays.asList(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false)));
        assertThat(user.matchNotification(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed, materialRevisions)).isTrue();
    }

    @Test
    void shouldAddMultiple() {
        user = new User("UserName", new String[]{" JH ,Pavan,JEZ,"}, "user@mail.com", true);
        assertThat(user.matcher()).isEqualTo(new Matcher("JH,Pavan,JEZ"));
    }

    @Test
    void shouldPopulateEmptyListWhenMatcherDoesNotInitialized() {
        user = new User("UserName", new String[]{""}, "user@mail.com", true);
        HashMap<String, Object> data = new HashMap<>();
        user.populateModel(data);
        Object value = data.get("matchers");
        assertThat(value).isEqualTo(new Matcher(""));
    }

    @Test
    void shouldPopulateMatchers() {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        HashMap<String, Object> data = new HashMap<>();
        user.populateModel(data);
        Object value = data.get("matchers");
        assertThat(value).isEqualTo(new Matcher("Jez,Pavan"));
    }

    @Test
    void shouldValidateEmailLesserThan255() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateEmail();
    }

    @Test
    void shouldValidateLoginNameIsNotBlank() {
        user = new User("", new String[]{"Jez,Pavan"}, "user@mail.com", true);

        assertThatCode(() -> user.validateLoginName())
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldValidateWhenLoginNameExists() throws Exception {
        user = new User("bob", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateLoginName();
    }

    @Test
    void shouldAcceptNullValueForEmail() throws ValidationException {
        new User("UserName", "My Name", null).validateEmail();
    }

    @Test
    void shouldInvalidateEmailWhenEmailIsNotValid() {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "mail.com", true);
        try {
            user.validateEmail();
            fail("validator should capture the email");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    void shouldInvalidateEmailMoreThan255Of() {
        user = new User("UserName", new String[]{"Jez,Pavan"}, chars(256), true);
        try {
            user.validateEmail();
            fail("validator should capture the email");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    void shouldValidateMatcherForLessThan255() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateMatcher();
    }

    @Test
    void shouldInvalidateMatcherMoreThan255Of() throws Exception {
        user = new User("UserName", new String[]{onlyChars(200), onlyChars(55)}, "user@mail.com", true);
        try {
            user.validateMatcher();
            fail("validator should capture the matcher");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    void shouldValidateMatcherWithSpecialCharacters() throws Exception {
        user = new User("UserName", new String[]{"any/*?!@#$%%^&*()[]{}\\|`~"}, "user@mail.com", true);
        user.validateMatcher();
    }

    @Test
    void shouldEquals() throws Exception {
        User user1 = new User("UserName", new String[]{"A", "b"}, "user@mail.com", true);
        User user2 = new User("UserName", new String[]{}, "user@mail.com", true);
        user2.setMatcher("A, b");
        assertThat(user2).isEqualTo(user1);
    }

    @Test
    void shouldNotBeEqualIfFullNamesAreDifferent() {
        assertThat(new User("user1", "moocow-user1", "moocow@example.com").equals(new User("user1", "moocow", "moocow@example.com"))).isFalse();
    }

    @Test
    void shouldUnderstandSplittingMatcherString() {
        User user = new User("UserName", new String[]{"A", "b"}, "user@mail.com", true);
        assertThat(user.getMatchers()).isEqualTo(Arrays.asList("A", "b"));
        user = new User("UserName", new String[]{"A,b"}, "user@mail.com", true);
        assertThat(user.getMatchers()).isEqualTo(Arrays.asList("A", "b"));
        user = new User("UserName", new String[]{""}, "user@mail.com", true);
        List<String> matchers = Collections.emptyList();
        assertThat(user.getMatchers()).isEqualTo(matchers);
        user = new User("UserName", new String[]{"b,A"}, "user@mail.com", true);
        assertThat(user.getMatchers()).isEqualTo(Arrays.asList("A", "b"));
    }

    @Test
    void shouldThrowExceptionIfFilterWithAllEventAlreadyExist() {
        User user = new User("foo");
        user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.All, false));
        try {
            user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
            fail("shouldThrowExceptionIfFilterWithAllEventAlreadyExist");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    void shouldThrowExceptionIfFilterWithSameEventAlreadyExist() {
        User user = new User("foo");
        user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
        try {
            user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
            fail("shouldThrowExceptionIfFilterWithSameEventAlreadyExist");
        } catch (Exception e) {
            assertThat(e.getMessage()).contains("Duplicate notification filter");
        }
    }

    @Test
    void shouldCopyUser() {
        User user = new User("user", "User", new String[]{"match"}, "email", false);
        user.setId(100);
        user.addNotificationFilter(new NotificationFilter("p1", "S1", StageEvent.Fixed, true));
        User clonedUser = new User(user);
        assertThat(clonedUser).isEqualTo(user);
        assertThat(clonedUser.getId()).isEqualTo(user.getId());
        assertThat(clonedUser).isNotSameAs(user);
        assertThat(clonedUser.getNotificationFilters()).isEqualTo(user.getNotificationFilters());
        assertThat(clonedUser.getNotificationFilters()).isNotSameAs(user.getNotificationFilters());
        assertThat(clonedUser.getNotificationFilters().get(0)).isNotSameAs(user.getNotificationFilters().get(0));
    }

    @Test
    void shouldRemoveNotificationFilter() {
        User user = new User("u");
        NotificationFilter filter1 = new NotificationFilter("p1", "s1", StageEvent.Fails, true);
        filter1.setId(1);
        NotificationFilter filter2 = new NotificationFilter("p1", "s2", StageEvent.Fails, true);
        filter2.setId(2);
        user.addNotificationFilter(filter1);
        user.addNotificationFilter(filter2);

        user.removeNotificationFilter(filter1.getId());

        assertThat(user.getNotificationFilters().size()).isEqualTo(1);
        assertThat(user.getNotificationFilters().contains(filter2)).isTrue();

    }

    @Nested
    class UpdateNotificationFilter {
        @Test
        void shouldErrorOutWhenNotificationFilterWithIdDoesNotExist() {
            NotificationFilter notificationFilter = mock(NotificationFilter.class);
            User user = new User("bob");
            user.setId(100L);
            when(notificationFilter.getId()).thenReturn(1L);

            assertThatCode(() -> user.updateNotificationFilter(notificationFilter))
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Notification filter with id '1' was not found!");
        }

        @Test
        void shouldErrorOutWithValidationErrorWhenAddingSameNotificationFilterAgain() {
            NotificationFilter notifyForBrokenBuild = notificationFilter(1L, "up42", "up42_stage", StageEvent.Breaks);
            NotificationFilter notifyForFixedBuild = notificationFilter(2L, "up42", "up42_stage", StageEvent.Fixed);
            User user = new User("bob");
            user.setId(100L);
            user.addNotificationFilter(notifyForBrokenBuild);
            user.addNotificationFilter(notifyForFixedBuild);

            NotificationFilter updatedFilter = notificationFilter(2L, "up42", "up42_stage", StageEvent.Breaks);
            assertThatCode(() -> user.updateNotificationFilter(updatedFilter))
                    .isInstanceOf(UncheckedValidationException.class)
                    .hasMessage("Duplicate notification filter found for: {pipeline: \"up42\", stage: \"up42_stage\", event: \"Breaks\"}");
        }

        @Test
        void shouldUpdateNotificationFilter() {
            NotificationFilter notifyForBrokenBuild = notificationFilter(1L, "up42", "up42_stage", StageEvent.Breaks);
            User user = new User("bob");
            user.setId(100L);
            user.addNotificationFilter(notifyForBrokenBuild);
            NotificationFilter updatedFilter = notificationFilter(1L, "up42", "up42_stage", StageEvent.All);

            user.updateNotificationFilter(updatedFilter);

            assertThat(user.getNotificationFilters())
                    .hasSize(1)
                    .contains(updatedFilter);
        }

        @Test
        void shouldNotThrowDuplicateFilterWhenUpdatingFilterFromEventTypeAllToOthers() {
            NotificationFilter notifyForBrokenBuild = notificationFilter(1L, "up42", "up42_stage", StageEvent.All);
            User user = new User("bob");
            user.setId(100L);
            user.addNotificationFilter(notifyForBrokenBuild);
            NotificationFilter updatedFilter = notificationFilter(1L, "up42", "up42_stage", StageEvent.Breaks);

            user.updateNotificationFilter(updatedFilter);

            assertThat(user.getNotificationFilters())
                .hasSize(1)
                .contains(updatedFilter);
        }
    }

    private String chars(int numbersOf) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbersOf - "@gmail.com".length(); i++) {
            builder.append("A");
        }
        return builder.toString() + "@gmail.com";
    }

    private String onlyChars(int numbersOf) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbersOf; i++) {
            builder.append("A");
        }
        return builder.toString();
    }

    private NotificationFilter notificationFilter(long id, String pipeline, String stage, StageEvent event) {
        NotificationFilter notifyForBreakingBuild = new NotificationFilter(pipeline, stage, event, true);
        notifyForBreakingBuild.setId(id);
        return notifyForBreakingBuild;
    }
}
