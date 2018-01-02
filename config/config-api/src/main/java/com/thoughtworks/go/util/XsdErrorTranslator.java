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

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XsdErrorTranslator extends DefaultHandler {
    private boolean validationError = false;
    private SAXParseException saxParseException = null;

    static List<MappingEntry> errorMapping = new ArrayList<>();

    private static final int NONE = 0;
    private static final int CAPITALIZE = 1;
    private static final int HUMANIZE = 2;
    private static final int REMOVE_TYPE_SUFFIX = 2;

    static {
        addMapping(
                "cvc-attribute.3: The value '(.*)' of attribute '(.*)' on element '(.*)' is not valid with respect to its type, '(.*)'",
                "\"{0}\" is invalid for {2} {1}",
                NONE, NONE, CAPITALIZE);

        addMapping(
                "cvc-complex-type.4: Attribute '(.*)' must appear on element '(.*)'.",
                "\"{0}\" is required for {1}",
                HUMANIZE | CAPITALIZE , CAPITALIZE);

        //Add more things to group 4 apart from Mingle. This is to handle xsd inline element type.

        addMapping(
                "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https\\?://.+' for type '#AnonType_siteUrlserverAttributeGroup'.",
                "siteUrl \"{0}\" is invalid. It must start with ‘http://’ or ‘https://’", NONE);

        addMapping(
                 "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https://.+' for type '#AnonType_secureSiteUrlserverAttributeGroup'.",
                 "secureSiteUrl \"{0}\" is invalid. It must be a secure URL (should start with ‘https://’)", NONE) ;

        addMapping(
                 "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https://.+' for type '#AnonType_urlluauType'.",
                 "url \"{0}\" is invalid. It must be a secure URL (should start with ‘https://’)", NONE) ;


        addMapping(
                "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern '(.*)' for type '#AnonType_(.*?)(mingle)Type'.",
                "{2} in {3} is invalid. \"{0}\" should conform to the pattern - {1}",
                NONE, NONE, HUMANIZE | CAPITALIZE | REMOVE_TYPE_SUFFIX, CAPITALIZE) ;

        addMapping(
                "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern '(.*)' for type '(.*)'.",
                "{2} is invalid. \"{0}\" should conform to the pattern - {1}",
                NONE, NONE, HUMANIZE | CAPITALIZE | REMOVE_TYPE_SUFFIX) ;

        addMapping(
                "cvc-minLength-valid: Value '(.*)' with length = '0' is not facet-valid with respect to minLength '1' for type '#AnonType_commandexec'.",
                "Command attribute cannot be blank in a command snippet.",
                NONE, NONE) ;

        addMapping(
                "cvc-elt.1: Cannot find the declaration of element '(.*)'.",
                "Invalid XML tag \"{0}\" found.",
                NONE) ;


        addMapping(
                "cvc-[^:]+: (.*)",
                "{0}");


    }

    private static void addMapping(String pattern, String replacement, int... transforms) {
        errorMapping.add(new MappingEntry(pattern, replacement, transforms));
    }

    private static class MappingEntry {
        public final Pattern pattern;
        public final String replacement;
        private final int[] transforms;

        public MappingEntry(String pattern, String replacement, int[] transforms) {
            this.transforms = transforms;
            this.pattern = Pattern.compile(pattern);
            this.replacement = replacement;
        }

        public String translate(String message) {
            final Matcher matcher = pattern.matcher(message);
            if (matcher.matches()) {
                return MessageFormat.format(replacement, applyTransforms(extractArguments(matcher)));
            }
            return null;
        }

        private String[] extractArguments(Matcher matcher) {
            String[] args = new String[matcher.groupCount()];
            for (int i = 1; i <= matcher.groupCount(); i++) {
                args[i - 1] = matcher.group(i);
            }
            return args;
        }

        private Object[] applyTransforms(String[] args) {
            for (int i = 0, transformsLength = transforms.length; i < transformsLength; i++) {
                int transform = transforms[i];
                if ((transform & HUMANIZE) != 0) {
                    args[i] = StringUtil.humanize(args[i]);
                }
                if ((transform & CAPITALIZE) != 0) {
                    args[i] = StringUtils.capitalize(args[i]);
                }
                if ((transform & REMOVE_TYPE_SUFFIX) != 0) {
                    args[i] = StringUtils.replace(args[i], " type", "");
                }
            }
            return args;
        }
    }

    public void error(SAXParseException exception) throws SAXException {
        if (!validationError) {
            validationError = true;
            saxParseException = exception;
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (!validationError) {
            validationError = true;
            saxParseException = exception;
        }
    }

    public void warning(SAXParseException exception) throws SAXException {

    }

    public String translate() {
        String msg = saxParseException.getMessage();
        for (MappingEntry mappingEntry : errorMapping) {
            String translated = mappingEntry.translate(msg);
            if (translated != null) {
                return translated;
            }
        }
        return msg;
    }

    public boolean hasValidationError() {
        return validationError;
    }
}
