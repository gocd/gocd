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

package com.thoughtworks.go.server.domain.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;

public class UsersXmlViewModel implements XmlRepresentable {

    private Collection<User> users;

    public UsersXmlViewModel(Collection<User> users){
        this.users = users;
    }

    public Document toXml(XmlWriterContext writerContext) throws DocumentException, IOException {
        Document document = new DOMDocument();
        document.add(rootUsersNodeWithChildNodesFor(users));
        return document;
    }

    public String httpUrl(String baseUrl) {
        return null;
    }

    //Methods that understand the shape of the final document
    private Element rootUsersNodeWithChildNodesFor(Collection<User> users) {
        return nodeWithChildren(arrayNode("users"), userNodes(users));
    }

    private Element userNode(User user) {
        return nodeWithChildren(new DOMElement("user"),
                                    textNode("name", user.getName()),
                                    textNode("displayName", user.getDisplayName()),
                                    textNode("matcher", user.getMatcher()),
                                    textNode("email", user.getEmail()),
                                    booleanNode("emailMe", user.isEmailMe()),
                                    booleanNode("enabled", user.isEnabled()));
    }

    private Element[] userNodes(Collection<User> users) {
        List<Element> userNodes = new ArrayList<Element>();
        for (User user : users){
            userNodes.add(userNode(user));
        }
        return userNodes.toArray(new Element[]{});
    }

    //General low level XML methods
    private Element nodeWithChildren(Element parent, Element... children){
        for (Element child : children) {
            parent.add(child);
        }
        return parent;
    }

    private Element arrayNode(String name){
        return nodeOfType(name, "array");
    }

    private Element textNode(String name, String value){
        Element result = new DOMElement(name);
        result.addText(value == null ? "" : value);
        return result;
    }

    private Element booleanNode(String name, boolean aBool){
        final Element result = nodeOfType(name, "boolean");
        result.addText(String.valueOf(aBool));
        return result;
    }

    private Element nodeOfType(String name, String typeName) {
        Element result = new DOMElement(name);
        result.addAttribute("type", typeName);
        return result;
    }
}
