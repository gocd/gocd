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

package com.thoughtworks.go.legacywrapper;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public abstract class SAXBasedExtractor extends DefaultHandler {
    public abstract void report(Map resultSet);

    private boolean canStop;

    public boolean canStop() {
        return canStop;
    }

    protected void canStop(boolean canStop) {
        this.canStop = canStop;
    }

    protected String getAttribute(Attributes attributes, String attributeName) {
        String attributeValue = attributes.getValue(attributeName);
        return attributeValue == null ? "" : attributeValue;
    }
}