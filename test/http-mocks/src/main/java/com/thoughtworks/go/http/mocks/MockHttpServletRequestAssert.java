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

package com.thoughtworks.go.http.mocks;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.util.Objects;

import javax.servlet.http.HttpSession;

public class MockHttpServletRequestAssert<SELF extends MockHttpServletRequestAssert<SELF>> extends AbstractObjectAssert<SELF, MockHttpServletRequest> {

    protected MockHttpServletRequestAssert(MockHttpServletRequest actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public static MockHttpServletRequestAssert assertThat(MockHttpServletRequest actual) {
        return new MockHttpServletRequestAssert(actual, MockHttpServletRequestAssert.class);
    }

    public SELF doesNotHaveSessionAttribute(String attributeName) {
        if (getSession().getAttribute(attributeName) != null) {
            failWithMessage("Expected session not to contain attribute <%s>, but found <%s>: <%s>", attributeName, attributeName, getSession().getAttribute(attributeName));
        }
        return myself;
    }

    public SELF hasSessionAttribute(String name, Object expectedAttributeValue) {
        final Object actualAttributeValue = getSession().getAttribute(name);
        if (!Objects.areEqual(expectedAttributeValue, actualAttributeValue)) {
            failWithMessage("Expected session to contain attribute <%s>: <%s>, but found <%s>: <%s>", name, expectedAttributeValue, name, actualAttributeValue);
        }
        return myself;
    }

    public SELF hasSameSession(HttpSession originalSession) {
        final HttpSession session = getSession();
        if (!session.equals(originalSession)) {
            failWithMessage("Expected session to be <%s> but it was <%s>", originalSession, session);
        }
        return myself;
    }

    private SELF sessionExist() {
        if (actual.getSession(false) == null) {
            failWithMessage("Expected session to exist on request but it is <null>.");
        }
        return myself;
    }

    private HttpSession getSession() {
        sessionExist();
        return actual.getSession(false);
    }

}
