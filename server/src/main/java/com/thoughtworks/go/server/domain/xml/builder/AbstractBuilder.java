/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.domain.xml.builder;

import com.thoughtworks.go.util.DateUtils;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.dom.DOMElement;

import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AbstractBuilder<T, SELF> {
    protected final T parent;
    protected final SELF mySelf;

    public AbstractBuilder(Class<SELF> clazz, T parent) {
        this.parent = parent;
        this.mySelf = clazz.cast(this);
    }

    protected abstract Element current();

    public SELF textNode(String name, String text) {
        current().add(withNamespace(name).addText(text));
        return mySelf;
    }

    public SELF textNode(String name, Date date) {
        current().add(withNamespace(name).addText(DateUtils.formatISO8601(date)));
        return mySelf;
    }

    public SELF cdataNode(String name, String CDATA) {
        current().add(withNamespace(name).addCDATA(CDATA));
        return mySelf;
    }

    public SELF link(String href, String rel) {
        current().add(getLink(href, rel));
        return mySelf;
    }

    public SELF link(String href, String rel, String title, String type) {
        current().add(getLink(href, rel)
            .addAttribute("title", title)
            .addAttribute("type", type));
        return mySelf;
    }

    public SELF node(String name, Consumer<ElementBuilder> consumer) {
        DOMElement element = withNamespace(name);
        current().add(element);
        consumer.accept(new ElementBuilder(element));
        return mySelf;
    }

    public SELF emptyNode(String name) {
        DOMElement element = withNamespace(name);
        current().add(element);
        return mySelf;
    }

    public SELF attr(String name, String value) {
        current().addAttribute(name, value);
        return mySelf;
    }

    public SELF attr(String name, Enum<?> value) {
        return attr(name, value.toString());
    }

    public SELF attr(String name, Number value) {
        return attr(name, String.valueOf(value));
    }

    public SELF comment(String comment) {
        current().addComment(comment);
        return mySelf;
    }

    private Element getLink(String href, String rel) {
        return withNamespace("link")
            .addAttribute("rel", rel)
            .addAttribute("href", href);
    }

    private DOMElement withNamespace(String name) {
        DOMElement element = new DOMElement(name);
        getDefaultNameSpace().ifPresent(element::setNamespace);
        return element;
    }

    private Optional<Namespace> getDefaultNameSpace() {
        return Optional.ofNullable(current().getNamespace());
    }

}
