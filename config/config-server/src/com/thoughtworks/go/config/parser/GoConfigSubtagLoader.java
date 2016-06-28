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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.ConfigInterface;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ConfigUtil;
import org.jdom.Element;

import static com.thoughtworks.go.config.ConfigCache.annotationFor;
import static com.thoughtworks.go.config.ConfigCache.isAnnotationPresent;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoConfigSubtagLoader {
    private static Map<Field, ConfigSubtag> isSubTags = new HashMap<>();

    private final ConfigUtil configUtil = new ConfigUtil("magic");
    private final Element e;
    private final Field field;
    private ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;
    private final ConfigReferenceElements configReferenceElements;

    public static boolean isSubtag(Field field) {
        return findSubTag(field) != null;
    }

    private static ConfigSubtag findSubTag(Field field) {
        if (!isSubTags.containsKey(field)) {
            isSubTags.put(field, field.getAnnotation(ConfigSubtag.class));
        }
        ConfigSubtag configSubtag = isSubTags.get(field);
        return configSubtag;
    }

    public static GoConfigSubtagLoader subtagParser(Element e, Field field, ConfigCache configCache, final ConfigElementImplementationRegistry registry,
                                                        ConfigReferenceElements configReferenceElements) {
        return new GoConfigSubtagLoader(e, field, configCache, registry, configReferenceElements);
    }

    private GoConfigSubtagLoader(Element e, Field field, ConfigCache configCache, final ConfigElementImplementationRegistry registry, ConfigReferenceElements configReferenceElements) {
        this.e = e;
        this.field = field;
        this.configCache = configCache;
        this.registry = registry;
        this.configReferenceElements = configReferenceElements;
    }

    public Object parse() {
        Class<?> type = findTypeOfField();
        if (type == null) { return null; }

        ConfigTag tag = GoConfigClassLoader.configTag(type, configCache);
        if (configUtil.optionalAndMissingTag(e, tag, findSubTag(field).optional())) {
            return null;
        }

        return GoConfigClassLoader.classParser(configUtil.getChild(e, tag), type, configCache, new GoCipher(), registry, configReferenceElements).parse();
    }

    private Class<?> findTypeOfField() {
        Class<?> type = field.getType();
        if (isInterface(type)) {
            for (Element subElement : (List<Element>) e.getChildren()) {
                Class<?> concreteType = findConcreteTypeFrom(subElement, type);
                if (concreteType != null) {
                    return concreteType;
                }
            }
            boolean optional = annotationFor(field, ConfigSubtag.class).optional();
            if (optional) { return null; }
            throw bomb("Unable to find a tag of type '" + type.getSimpleName() + "' under element '" + e.getName()
                    + "'");
        }
        return field.getType();
    }

    private Class<?> findConcreteTypeFrom(Element element, Class<?> interfaceType) {
        for (Class<?> implementation : registry.implementersOf(interfaceType)) {
            if (GoConfigClassLoader.compare(element, implementation, configCache)) {
                return implementation;
            }
        }
        return null;
    }

    private boolean isInterface(Class<?> aClass) {
        return isAnnotationPresent(aClass, ConfigInterface.class);
    }

}
