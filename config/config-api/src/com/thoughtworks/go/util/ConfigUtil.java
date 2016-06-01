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

package com.thoughtworks.go.util;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.Task;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ObjectUtil.defaultIfNull;

public class ConfigUtil {
    private final String configFile;


    public ConfigUtil(String configFile) {
        this.configFile = defaultIfNull(configFile, "<no config file specified>");
    }

    public static List<String> allTasks(ConfigElementImplementationRegistry registry) {
        List<String> allTasks = new ArrayList<>();
        for (Class<? extends Task> task : registry.implementersOf(Task.class)) {
            ConfigTag tag = task.getAnnotation(ConfigTag.class);
            allTasks.add(tag.value());
        }
        return allTasks;
    }

    public Element getChild(Element e, ConfigTag tag) {
        Element child = child(e, tag);
        if (child == null) {
            throw bomb("Error finding child '" + tag + "' in config: " + configFile + elementOutput(e));
        }
        return child;
    }

    private Element child(Element e, ConfigTag tag) {
        return e.getChild(tag.value(), Namespace.getNamespace(tag.namespacePrefix(), tag.namespaceURI()));
    }

    public String getAttribute(Element e, String attribute) {
        Attribute attr = e.getAttribute(attribute);
        if(attr == null) {
            throw bomb("Error finding attribute '" + attribute + "' in config: " + configFile + elementOutput(e));
        }
        return attr.getValue();
    }

    public String elementOutput(Element e) {
        return "\n\t" + new XMLOutputter().outputString(e);
    }

    public boolean hasChild(Element e, ConfigTag tag) {
        return child(e, tag) != null;
    }

    public String getAttribute(Element e, String attribute, String defaultValue) {
        if (!hasAttribute(e, attribute)) { return defaultValue; }
        return getAttribute(e, attribute);
    }

    public boolean hasAttribute(Element e, String attribute) {
        return e.getAttribute(attribute) != null;
    }

    public boolean atTag(Element e, String tag) {
        return e.getName().equals(tag);
    }

    public boolean optionalAndMissingAttribute(Element e, ConfigAttribute attribute) {
        boolean optional = attribute.optional();
        boolean isMissingAttribute = !hasAttribute(e, attribute.value());
        if (!optional && isMissingAttribute) {
            throw bomb("Non optional attribute '" + attribute.value() + "' is not in element: " + elementOutput(e));
        }
        return optional && isMissingAttribute;
    }

    public Object getAttribute(Element e, ConfigAttribute attribute) {
        if (optionalAndMissingAttribute(e, attribute)) {
            return null;
        }
        return getAttribute(e, attribute.value());
    }

    public boolean optionalAndMissingTag(Element e, ConfigTag tag, boolean optional) {
        boolean isMissingElement = !hasChild(e, tag);

        if (!optional && isMissingElement) {
            throw bomb("Non optional tag '" + tag + "' is not in config file. Found: " + elementOutput(e));
        }

        return optional && isMissingElement;
    }
}
