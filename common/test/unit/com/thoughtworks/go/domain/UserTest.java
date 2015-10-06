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

package com.thoughtworks.go.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.helper.MaterialsMother;
import org.hamcrest.core.Is;
import org.junit.Test;

import static com.thoughtworks.go.helper.ModificationsMother.aCheckIn;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class UserTest {
    private MaterialRevisions materialRevisions;
    private User user;

    @Test
    public void shouldTrimTheUserNameAndMatcher() throws Exception {
        user = new User(" UserName ","Full User Name", new String[]{" README "}, " user@mail.com ", true);
        assertThat(user.getName(), is("UserName"));
        assertThat(user.getMatcher(), is(("README")));
        assertThat(user.getEmail(), is("user@mail.com"));
        assertThat(user.getDisplayName(), is("Full User Name"));
    }

    @Test
    public void shouldNotMatchWhenUserDidNotSetUpTheMatcher() throws Exception {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "readme")));
        assertThat(new User("UserName", new String[]{null}, "user@mail.com", true).matchModification(materialRevisions), is(false));
        assertThat(new User("UserName", new String[]{""}, "user@mail.com", true).matchModification(materialRevisions),
                is(false));
    }

    @Test
    public void shouldReturnFalseWhenEmailIsEmpty() {
        assertThat(
                new User("UserName", new String[]{"README"}, null, true).matchNotification(null, StageEvent.All, null),
                is(false));
        assertThat(new User("UserName", new String[]{"README"}, "", true).matchNotification(null, StageEvent.All, null),
                is(false));
    }

    @Test
    public void shouldReturnTrueWhenNotificationFilterMatchesMyCheckinOnGivenStageFixed() {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "readme")));
        user = new User("UserName", new String[]{"README"}, "user@mail.com", true);
        user.setNotificationFilters(
                Arrays.asList(new NotificationFilter("cruise", "dev", StageEvent.Fixed, true)));
        assertThat(
                user.matchNotification(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed, materialRevisions),
                is(true));
    }

    @Test
    public void shouldReturnTrueWhenNotificationFilterMatchesAnyCheckinOnGivenStageFixed() {
        materialRevisions = new MaterialRevisions(new MaterialRevision(MaterialsMother.svnMaterial(), aCheckIn("100", "xyz")));
        user = new User("UserName", new String[]{"README"}, "user@mail.com", true);
        user.setNotificationFilters(
                Arrays.asList(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false)));
        assertThat(
                user.matchNotification(new StageConfigIdentifier("cruise", "dev"), StageEvent.Fixed, materialRevisions),
                is(true));
    }

    @Test
    public void shouldAddMultiple() throws Exception {
        user = new User("UserName", new String[]{" JH ,Pavan,JEZ,"}, "user@mail.com", true);
        assertThat(user.matcher(), is(new Matcher("JH,Pavan,JEZ")));
    }

    @Test
    public void shouldPopulateEmptyListWhenMatcherDoesNotInitialized() throws Exception {
        user = new User("UserName", new String[]{""}, "user@mail.com", true);
        HashMap<String, Object> data = new HashMap<String, Object>();
        user.populateModel(data);
        Object value = data.get("matchers");
        assertThat((Matcher) value, is(new Matcher("")));
    }

    @Test
    public void shouldPopulateMatchers() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        HashMap<String, Object> data = new HashMap<String, Object>();
        user.populateModel(data);
        Object value = data.get("matchers");
        assertThat((Matcher) value, is(new Matcher("Jez,Pavan")));
    }

    @Test
    public void shouldValidateEmailLesserThan255() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateEmail();
    }

    @Test(expected = ValidationException.class)
    public void shouldValidateLoginNameIsNotBlank() throws Exception {
        user = new User("", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateLoginName();
    }

    @Test
    public void shouldValidateWhenLoginNameExists() throws Exception {
        user = new User("bob", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateLoginName();
    }

    @Test
    public void shouldAcceptNullValueForEmail() throws ValidationException {
       new User("UserName", "My Name", null).validateEmail();
    }

    @Test
    public void shouldInvalidateEmailWhenEmailIsNotValid() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "mail.com", true);
        try {
            user.validateEmail();
            fail("validator should capture the email");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldInvalidateEmailMoreThan255Of() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, chars(256), true);
        try {
            user.validateEmail();
            fail("validator should capture the email");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldValidateMatcherForLessThan255() throws Exception {
        user = new User("UserName", new String[]{"Jez,Pavan"}, "user@mail.com", true);
        user.validateMatcher();
    }

    @Test
    public void shouldInvalidateMatcherMoreThan255Of() throws Exception {
        user = new User("UserName", new String[]{onlyChars(200), onlyChars(55)}, "user@mail.com", true);
        try {
            user.validateMatcher();
            fail("validator should capture the matcher");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void shouldValidateMatcherWithSpecialCharacters() throws Exception {
        user = new User("UserName", new String[]{"any/*?!@#$%%^&*()[]{}\\|`~"}, "user@mail.com", true);
        user.validateMatcher();
    }

    @Test
    public void shouldEquals() throws Exception {
        User user1 = new User("UserName", new String[]{"A", "b"}, "user@mail.com", true);
        User user2 = new User("UserName", new String[]{}, "user@mail.com", true);
        user2.setMatcher("A, b");
        assertThat(user2, is(user1));
    }

    @Test
    public void shouldNotBeEqualIfFullNamesAreDifferent(){
        assertFalse(new User("user1", "moocow-user1", "moocow@example.com").equals(new User("user1", "moocow", "moocow@example.com")));
    }

    @Test
    public void shouldUnderstandSplittingMatcherString() {
        User user = new User("UserName", new String[]{"A", "b"}, "user@mail.com", true);
        assertThat(user.getMatchers(), is(Arrays.asList("A", "b")));
        user = new User("UserName", new String[]{"A,b"}, "user@mail.com", true);
        assertThat(user.getMatchers(), is(Arrays.asList("A", "b")));
        user = new User("UserName", new String[]{""}, "user@mail.com", true);
        List<String> matchers = Collections.emptyList();
        assertThat(user.getMatchers(), is(matchers));
        user = new User("UserName", new String[]{"b,A"}, "user@mail.com", true);
        assertThat(user.getMatchers(), is(Arrays.asList("A", "b")));
    }

    @Test
    public void shouldThrowExceptionIfFilterWithAllEventAlreadyExist() {
        User user = new User("foo");
        user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.All, false));
        try {
            user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
            fail("shouldThrowExceptionIfFilterWithAllEventAlreadyExist");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("already exists"));
        }
    }

    @Test
    public void shouldThrowExceptionIfFilterWithSameEventAlreadyExist() {
        User user = new User("foo");
        user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
        try {
            user.addNotificationFilter(new NotificationFilter("cruise", "dev", StageEvent.Fixed, false));
            fail("shouldThrowExceptionIfFilterWithSameEventAlreadyExist");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("already exists"));
        }
    }

    @Test
    public void shouldCopyUser(){
        User user = new User("user", "User", new String[]{"match"}, "email", false);
        user.setId(100);
        user.addNotificationFilter(new NotificationFilter("p1","S1", StageEvent.Fixed, true));
        User clonedUser = new User(user);
        assertThat(clonedUser, is(user));
        assertThat(clonedUser.getId(), Is.is(user.getId()));
        assertThat(clonedUser, not(sameInstance(user)));
        assertThat(clonedUser.getNotificationFilters(), is(user.getNotificationFilters()));
        assertThat(clonedUser.getNotificationFilters(), not(sameInstance(user.getNotificationFilters())));
        assertThat(clonedUser.getNotificationFilters().get(0), not(sameInstance(user.getNotificationFilters().get(0))));
    }

    @Test
    public void shouldRemoveNotificationFilter(){
        User user = new User("u");
        NotificationFilter filter1 = new NotificationFilter("p1","s1", StageEvent.Fails, true);
        filter1.setId(1);
        NotificationFilter filter2 = new NotificationFilter("p1","s2", StageEvent.Fails, true);
        filter2.setId(2);
        user.addNotificationFilter(filter1);
        user.addNotificationFilter(filter2);

        user.removeNotificationFilter(filter1.getId());

        assertThat(user.getNotificationFilters().size(), is(1));
        assertThat(user.getNotificationFilters().contains(filter2), is(true));

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
}
