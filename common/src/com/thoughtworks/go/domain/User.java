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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.exception.ValidationException;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.Filter;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.validation.Validator;
import org.apache.commons.lang.StringUtils;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.lang.String.format;

public class  User extends PersistentObject {
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
        this(name,"", "");
    }

    public User(String name, String displayName, String email) {
        this(name, displayName, new String[]{""}, email, false);
    }

    public User(String name, List<String> matcher, String email, boolean emailMe) {
        this(name, matcher.toArray(new String[0]), email, emailMe);
    }

    public User(String name, String[] matcher, String email, boolean emailMe) {
        this(name, "", matcher, email, emailMe);
    }

    public User(String name, String displayName, String[] matcher, String email, boolean emailMe) {
        setName(name);
        setMatcher(new Matcher(matcher).toString());
        setEmail(email);
        setDisplayName(displayName);
        this.enabled = true;
        this.emailMe = emailMe;
    }

    public User(User user){
        this(user.name, user.displayName, new String[]{user.matcher}, user.email, user.emailMe);
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
    /**
     * only used by ibatis
     *
     * @java.lang.Deprecated
     */
    public String getMatcher() {
        return matcher;
    }

    public List<String> getMatchers() {
        List<String> matchers = new Matcher(matcher).toCollection();
        Collections.sort(matchers);
        return matchers;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = StringUtils.trim(email);
    }

    public void setMatcher(String matcher) {
        this.matcher = new Matcher(matcher).toString();
    }

    public void handler(UserHandler handler) {
        handler.visit(this);
    }

    boolean matchModification(MaterialRevisions materialRevisions) {
        if (StringUtils.isEmpty(this.matcher)) {
            return false;
        }
        return materialRevisions.containsMyCheckin(new Matcher(matcher));
    }

    public boolean matchNotification(StageConfigIdentifier stageIdentifier, StageEvent event,
                                     MaterialRevisions materialRevisions) {
        if (!shouldSendEmailToMe()) {
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
        if (emailMe != user.emailMe) {
            return false;
        }
        if (enabled != user.enabled) {
            return false;
        }
        if (email != null ? !email.equals(user.email) : user.email != null) {
            return false;
        }
        if (matcher != null ? !matcher.equals(user.matcher) : user.matcher != null) {
            return false;
        }
        if (name != null ? !name.equals(user.name) : user.name != null) {
            return false;
        }
        if (displayName != null ? !displayName.equals(user.displayName) : user.displayName != null) {
            return false;
        }
        return true;
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

    public String toString() {
        return String.format("User[name=%s, displayName= %s, matcher=%s, email=%s, emailMe=%s]", name, displayName, matcher, email, emailMe);
    }

    public List<NotificationFilter> getNotificationFilters() {
        return notificationFilters;
    }

    public void setNotificationFilters(List<NotificationFilter> notificationFilters) {
        this.notificationFilters = notificationFilters;
    }

    private boolean shouldSendEmailToMe() {
        return isEmailMe() && !StringUtils.isEmpty(email);
    }

    public void populateModel(HashMap<String, Object> model) {
        model.put("matchers", matcher());
        model.put("email", email);
        model.put("emailMe", emailMe);
        model.put("notificationFilters", notificationFilters);
    }

    public Matcher matcher() {
        return new Matcher(matcher);
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
        validate(Validator.EMAIL, getEmail());
    }

    public void validateLoginName() throws ValidationException {
        validate(Validator.presenceValidator("Login name field must be non-blank."), getName());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }

    public void enable() {
        this.enabled = true;
    }

    public boolean isAnonymous() {
        return this.name.equals(CaseInsensitiveString.str(Username.ANONYMOUS.getUsername()));
    }

    public void addNotificationFilter(NotificationFilter another) {
        checkForDuplicates(another);
        notificationFilters.add(another);
    }

    private void checkForDuplicates(NotificationFilter another) {
        for (NotificationFilter filter : notificationFilters) {
            if (filter.include(another)) {
                bomb(format("Notification filter for [%s] event of stage[%s] already exists", filter.getEvent(),
                    filter.getPipelineName() + "/" + filter.getStageName()));
            }
        }
    }

    public void removeNotificationFilter(final long filterId) {
        ArrayList<NotificationFilter> toBeDeleted = new ArrayList<>();
        ListUtil.filterInto(toBeDeleted,notificationFilters, new Filter<NotificationFilter>() {
            @Override
            public boolean matches(NotificationFilter filter) {
                return filter.getId() == filterId;
            }
        });
        notificationFilters.removeAll(toBeDeleted);
    }

    public boolean hasSubscribedFor(String pipelineName, String stageName) {
        for (NotificationFilter notificationFilter : notificationFilters) {
            if (notificationFilter.appliesTo(pipelineName, stageName)) {
                return true;
            }
        }
        return false;
    }
}
