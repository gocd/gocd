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
package com.thoughtworks.go.util;

import org.apache.commons.lang3.Strings;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

public class XsdErrorTranslator extends DefaultHandler {
    private boolean validationError = false;
    private SAXParseException saxParseException = null;

    private static final int NONE = 0;
    private static final int CAPITALIZE = 1;
    private static final int HUMANIZE = 2;
    private static final int REMOVE_TYPE_SUFFIX = 2;

    private static final List<ErrorMapping> ERROR_MAPPING = List.of(
        new ErrorMapping(
            "cvc-attribute.3: The value '(.*)' of attribute '(.*)' on element '(.*)' is not valid with respect to its type, '(.*)'",
            "\"{0}\" is invalid for {2} {1}",
            NONE, NONE, CAPITALIZE),

        new ErrorMapping(
            "cvc-complex-type.4: Attribute '(.*)' must appear on element '(.*)'.",
            "\"{0}\" is required for {1}",
            HUMANIZE | CAPITALIZE, CAPITALIZE),

        //Add more things to group 4 apart from Mingle. This is to handle xsd inline element type.

        new ErrorMapping(
            "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https\\?://.+' for type '#AnonType_siteUrlserverAttributeGroup'.",
            "siteUrl \"{0}\" is invalid. It must start with ‘http://’ or ‘https://’",
            NONE),

        new ErrorMapping(
            "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https://.+' for type '#AnonType_secureSiteUrlserverAttributeGroup'.",
            "secureSiteUrl \"{0}\" is invalid. It must be a secure URL (should start with ‘https://’)",
            NONE),

        new ErrorMapping(
            "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern 'https://.+' for type '#AnonType_urlluauType'.",
            "url \"{0}\" is invalid. It must be a secure URL (should start with ‘https://’)",
            NONE),

        new ErrorMapping(
            "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern '(.*)' for type '#AnonType_(.*?)(mingle)Type'.",
            "{2} in {3} is invalid. \"{0}\" should conform to the pattern - {1}",
            NONE, NONE, HUMANIZE | CAPITALIZE | REMOVE_TYPE_SUFFIX, CAPITALIZE),

        new ErrorMapping(
            "cvc-pattern-valid: Value '(.*)' is not facet-valid with respect to pattern '(.*)' for type '(.*)'.",
            "{2} is invalid. \"{0}\" should conform to the pattern - {1}",
            NONE, NONE, HUMANIZE | CAPITALIZE | REMOVE_TYPE_SUFFIX),

        new ErrorMapping(
            "cvc-minLength-valid: Value '(.*)' with length = '0' is not facet-valid with respect to minLength '1' for type '#AnonType_commandexec'.",
            "Command attribute cannot be blank in a command snippet.",
            NONE, NONE),

        new ErrorMapping(
            "cvc-elt.1: Cannot find the declaration of element '(.*)'.",
            "Invalid XML tag \"{0}\" found.",
            NONE),

        new ErrorMapping(
            "cvc-[^:]+: (.*)",
            "{0}")
    );

    public static String humanize(String s) {
        String[] words = splitByCharacterTypeCamelCase(s);
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].toLowerCase();
        }
        return String.join(" ", words);
    }

    private record ErrorMapping(Pattern pattern, String replacement, int[] transforms) {
            private ErrorMapping(String pattern, String replacement, int... transforms) {
                this(Pattern.compile(pattern), replacement, transforms);
            }

            public Optional<String> translate(String message) {
                final Matcher matcher = pattern.matcher(message);
                return matcher.matches()
                    ? Optional.of(MessageFormat.format(replacement, applyTransforms(extractArguments(matcher))))
                    : Optional.empty();
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
                        args[i] = humanize(args[i]);
                    }
                    if ((transform & CAPITALIZE) != 0) {
                        args[i] = capitalize(args[i]);
                    }
                    if ((transform & REMOVE_TYPE_SUFFIX) != 0) {
                        args[i] = Strings.CS.replace(args[i], " type", "");
                    }
                }
                return args;
            }
        }

    @Override
    public void error(SAXParseException exception) {
        if (!validationError) {
            validationError = true;
            saxParseException = exception;
        }
    }

    @Override
    public void fatalError(SAXParseException exception) {
        if (!validationError) {
            validationError = true;
            saxParseException = exception;
        }
    }

    @Override
    public void warning(SAXParseException exception) {

    }

    public String translate() {
        String msg = saxParseException.getMessage();
        return ERROR_MAPPING.stream()
            .flatMap(errorMapping -> errorMapping.translate(msg).stream())
            .findFirst()
            .orElse(msg);
    }

    public boolean hasValidationError() {
        return validationError;
    }
}
