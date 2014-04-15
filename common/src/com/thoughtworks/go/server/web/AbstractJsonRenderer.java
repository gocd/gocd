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

package com.thoughtworks.go.server.web;

import java.util.regex.Pattern;

import org.springframework.context.MessageSourceResolvable;

public abstract class AbstractJsonRenderer implements JsonRenderer {
    private static final Pattern BACKSLASH = Pattern.compile("\\\\");
    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"");
    private static final String ESCAPED_BACKSLASH = "\\\\\\\\";
    private static final String ESCAPED_QUOTE = "\\\\\"";

    private GoRequestContext context;

    protected AbstractJsonRenderer(GoRequestContext requestContext) {
        this.context = requestContext;
    }

    public void renderResolved(MessageSourceResolvable resolvable) {
        if (context == null) {
            quote(resolvable.getDefaultMessage());
        } else {
            quote(context.getMessage(resolvable));
        }
    }

    public void url(String url) {
        String s1 = BACKSLASH.matcher(url).replaceAll(ESCAPED_BACKSLASH);
        String s2 = DOUBLE_QUOTE.matcher(s1).replaceAll(ESCAPED_QUOTE);

        if (context == null) {
            quote(s2);
        } else {
            quote(context.getFullRequestPath() + s2);
        }
    }

    public void quote(String string) {
        append("\"");
        append(string);
        append("\"");
    }


}
