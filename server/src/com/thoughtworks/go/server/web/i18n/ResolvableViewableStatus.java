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

package com.thoughtworks.go.server.web.i18n;

import com.thoughtworks.go.domain.ViewableStatus;
import org.springframework.context.MessageSourceResolvable;

public class ResolvableViewableStatus implements MessageSourceResolvable {

    private ViewableStatus status;
    public static final String CODE_PREFIX = "label.status.";
    private static final Object[] NO_ARGUMENT = new Object[0];


    public ResolvableViewableStatus(ViewableStatus status) {
        this.status = status;
    }

    public String[] getCodes() {
        return new String[]{CODE_PREFIX + status.getStatus().toLowerCase()};
    }

    public Object[] getArguments() {
        return NO_ARGUMENT;
    }

    public String getDefaultMessage() {
        return status.getStatus();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResolvableViewableStatus that = (ResolvableViewableStatus) o;

        if (status != null ? !status.equals(that.status) : that.status != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (status != null ? status.hashCode() : 0);
    }

    public String toString() {
        return status.getStatus();
    }
}
