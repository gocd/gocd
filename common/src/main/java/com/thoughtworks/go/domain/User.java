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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.exception.UncheckedValidationException;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.validation.Validator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

public class User extends PersistentObject {
    private String name;
    private String displayName;
    private String matcher;
    private String email;
    private boolean emailMe;
    private boolean enabled;
    private List<NotificationFilter> notificationFilters = new ArrayList<>();

    public User() {
    }

    public User(String name) {
        this(name, "", "");
    }

    public User(String name, String displayName, String email) {
        this(name, displayName, "", email, false);
    }

    public User(String name, String matcher, String email, boolean emailMe) {
        this(name, "", matcher, email, emailMe);
    }

    public User(String name, String displayName, String matcher, String email, boolean emailMe) {
        setName(name);
        setDisplayName(displayName);
        setMatcher(matcher);
        setEmail(email);
        this.enabled = true;
        this.emailMe = emailMe;
    }

    public User(User user) {
        this(user.name, user.displayName, user.matcher, user.email, user.emailMe);
        this.enabled = user.enabled;
        this.id = user.id;
        for (NotificationFilter filter : user.notificationFilters) {
            this.notificationFilters.add(new NotificationFilter(filter));
        }
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEmailMe() {
        return emailMe;
    }

    public void setEmailMe(boolean emailMe) {
        this.emailMe = emailMe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtils.trim(name);
    }

    public Username getUsername() {
        return Username.valueOf(name);
    }

    public String getMatcher() {
        return matcher;
    }

    public List<String> getMatchers() {
        return new Matcher(matcher).toCollection();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = StringUtils.trim(email);
    }

    public void setMatcher(String matcher) {
        this.matcher = Matcher.normalize(matcher);
    }

    boolean matchModification(MaterialRevisions materialRevisions) {
        if (this.matcher == null || this.matcher.isEmpty()) {
            return false;
        }
        return materialRevisions.containsMyCheckin(new Matcher(matcher));
    }

    public boolean matchNotification(StageConfigIdentifier stageIdentifier, StageEvent event,
                                     MaterialRevisions materialRevisions) {
        if (!shouldSendEmail()) {
            return false;
        }
        for (NotificationFilter filter : notificationFilters) {
            if (filter.matchStage(stageIdentifier, event)) {
                if (filter.isAppliedOnAllCheckins() || matchModification(materialRevisions)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;
        return emailMe == user.emailMe &&
            enabled == user.enabled &&
            Objects.equals(email, user.email) &&
            Objects.equals(matcher, user.matcher) &&
            Objects.equals(name, user.name) &&
            Objects.equals(displayName, user.displayName);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (matcher != null ? matcher.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (emailMe ? 1 : 0);
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("User[name=%s, displayName= %s, matcher=%s, email=%s, emailMe=%s]", name, displayName, matcher, email, emailMe);
    }

    public List<NotificationFilter> getNotificationFilters() {
        return notificationFilters;
    }

    public void setNotificationFilters(List<NotificationFilter> notificationFilters) {
        this.notificationFilters = notificationFilters;
    }

    private boolean shouldSendEmail() {
        return isEmailMe() && email != null && !email.isEmpty();
    }

    private void validate(Validator<String> validator, String valueToValidate) throws ValidationException {
        ValidationBean validationBean = validator.validate(valueToValidate);
        if (!validationBean.isValid()) {
            throw new ValidationException(validationBean.getError());
        }
    }

    public void validateMatcher() throws ValidationException {
        validate(Validator.lengthValidator(255), getMatcher());
    }

    public void validateEmail() throws ValidationException {
        validate(Validator.lengthValidator(255), getEmail());
        validate(Validator.emailValidator(), getEmail());
    }

    public void validateLoginName() throws ValidationException {
        validate(Validator.presenceValidator("Login name field must be non-blank."), getName());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        setEnabled(false);
    }

    public void enable() {
        setEnabled(true);
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean isAnonymous() {
        return this.name.equals(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
    }

    public void addNotificationFilter(NotificationFilter another) {
        checkForDuplicates(another);
        notificationFilters.add(another);
    }

    private void checkForDuplicates(NotificationFilter another) {
        notificationFilters.stream()
            .filter(f -> f.include(another))
            .findFirst()
            .ifPresent(filter -> {
                String message = format("Duplicate notification filter found for: {pipeline: \"%s\", stage: \"%s\", event: \"%s\"}",
                    filter.getPipelineName(), filter.getStageName(), filter.getEvent());
                filter.addError("pipelineName", message);
                another.addError("pipelineName", message);
                throw new UncheckedValidationException(message);
            });
    }

    public void updateNotificationFilter(NotificationFilter notificationFilter) {
        NotificationFilter matchedFilter = notificationFilters.stream()
            .filter(filter -> filter.getId() == notificationFilter.getId())
            .findFirst()
            .orElseThrow(() -> new RecordNotFoundException(EntityType.NotificationFilter, notificationFilter.getId()));

        notificationFilters.remove(matchedFilter);
        checkForDuplicates(notificationFilter);
        notificationFilters.add(notificationFilter);
    }

    public void removeNotificationFilter(final long filterId) {
        List<NotificationFilter> toBeDeleted = notificationFilters.stream().filter(filter1 -> filter1.getId() == filterId).toList();
        notificationFilters.removeAll(toBeDeleted);
    }

    public boolean hasSubscribedFor(String pipelineName, String stageName) {
        return notificationFilters.stream().anyMatch(filter -> filter.appliesTo(pipelineName, stageName));
    }
}
