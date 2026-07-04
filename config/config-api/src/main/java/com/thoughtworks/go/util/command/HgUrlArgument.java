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
package com.thoughtworks.go.util.command;

import com.thoughtworks.go.config.ConfigAttributeValue;

@ConfigAttributeValue(fieldName = "url")
public class HgUrlArgument extends UrlArgument {
    public static final String DOUBLE_HASH = "##";
    private final String SINGLE_HASH = "#";

    public HgUrlArgument(String url) {
        super(url);
    }


    @Override
    public String forDisplay() {
        return super.forDisplay().replace(SINGLE_HASH, DOUBLE_HASH);
    }

    @Override
    String sanitized() {
        return super.sanitized().replace(DOUBLE_HASH, SINGLE_HASH);
    }

    private String removePassword(String userInfo) {
        return userInfo.split(":")[0];
    }

    public String defaultRemoteUrl() {
        return modifyUserInfo(sanitized(), (scheme, userInfo) -> removePassword(userInfo));
    }
}
