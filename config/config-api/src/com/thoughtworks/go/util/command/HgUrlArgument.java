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

package com.thoughtworks.go.util.command;

import java.net.URI;

import com.thoughtworks.go.config.ConfigAttributeValue;

@ConfigAttributeValue(fieldName = "url")
public class HgUrlArgument extends UrlArgument {
    private String url;
    public static final String DOUBLE_HASH = "##";
    private final String SINGLE_HASH = "#";

    public HgUrlArgument(String url) {
        super(url);
        this.url = url;
    }

    protected String uriToDisplay(URI uri) {
        return uri.toString().replace(SINGLE_HASH, DOUBLE_HASH);
    }

    protected String sanitizeUrl() {
        return url.replace(DOUBLE_HASH, SINGLE_HASH);
    }
}
