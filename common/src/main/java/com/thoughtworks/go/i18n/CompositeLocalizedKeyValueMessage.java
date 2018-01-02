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

package com.thoughtworks.go.i18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;

public class CompositeLocalizedKeyValueMessage implements Localizable {

    private static final String DELIMITER = " ";
    private final List<Localizable> localizableMessages;

    CompositeLocalizedKeyValueMessage(Localizable... localizableMessages) {
        this.localizableMessages = Arrays.asList(localizableMessages);
    }

    @Override
    public String localize(Localizer localizer) {
        List<String> messages = new ArrayList<>();
        for (Localizable localizableMessage : localizableMessages) {
            messages.add(localizableMessage.localize(localizer));
        }
        return join(messages, DELIMITER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompositeLocalizedKeyValueMessage that = (CompositeLocalizedKeyValueMessage) o;

        if (localizableMessages != null ? !localizableMessages.equals(that.localizableMessages) : that.localizableMessages != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return localizableMessages != null ? localizableMessages.hashCode() : 0;
    }
}
