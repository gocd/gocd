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

package com.thoughtworks.go.config.parser;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.util.ConfigUtil;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import org.jdom.Element;

public class GoConfigAttributeLoader {
    private final ConfigUtil configUtil = new ConfigUtil("magic");
    private final Element e;
    private final Field field;
    private static Map<Field, ConfigAttribute> isAttributes = new HashMap<>();

    public static boolean isAttribute(Field field) {
        return findAttribute(field) != null;
    }

    private static ConfigAttribute findAttribute(Field field) {
        if (!isAttributes.containsKey(field)) {
            isAttributes.put(field, field.getAnnotation(ConfigAttribute.class));
        }
        ConfigAttribute attribute = isAttributes.get(field);
        return attribute;
    }

    public static GoConfigAttributeLoader attributeParser(Element e, Field field) {
        return new GoConfigAttributeLoader(e, field);
    }

    private GoConfigAttributeLoader(Element e, Field field) {
        this.e = e;
        this.field = field;
    }

    public Object parse(Object defaultValue) {
        ConfigAttribute attribute = findAttribute(field);
        Object val = configUtil.getAttribute(e, attribute);
        if (!attribute.allowNull() && val == null && defaultValue == null) {
            bomb("Field '" + field.getName() + "' is still set to null. Must give a default value.");
        }
        return val;
    }
}
