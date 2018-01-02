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

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;

public class DefaultLocalizedResult implements LocalizedResult {
    private boolean isSuccessful = true;
    private String localizedKey;
    private Object[] args;

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void invalid(String localizedKey, Object... args) {
        this.localizedKey = localizedKey;
        this.args = args;
        isSuccessful = false;
    }

    public String message(Localizer localizer) {
        return localizer.localize(localizedKey, args);
    }

    public Localizable localizable() {
        return LocalizedMessage.string(localizedKey, args);
    }
}
