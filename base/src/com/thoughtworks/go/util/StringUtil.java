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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public final class StringUtil {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final String DELIMITER = "_";
    private static final String DELIMITER_MATCHER = DELIMITER;
    private static final String DELIMITER_ESCAPE_SEQ = DELIMITER + DELIMITER;

    private StringUtil() {
    }

    public static String quote(String s) {
        return "\"" + s + "\"";
    }

    public static String quoteJavascriptString(String s) {
        s = s.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
        return quote(s);
    }

    public static String stripSpacesAndNewLines(String content) {
        return content.replaceAll("\\s+", "");
    }

    public static String base64Encode(byte[] bytes) {
        Base64 base64 = new Base64();
        return stripLineSeparator(base64.encodeToString(bytes));
    }

    public static String md5Digest(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return base64Encode(md.digest(bytes));
        } catch (NoSuchAlgorithmException nsae) {
            throw ExceptionUtils.bomb(nsae);
        }
    }

    public static String sha1Digest(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            return base64Encode(md.digest(bytes));
        } catch (NoSuchAlgorithmException nsae) {
            throw ExceptionUtils.bomb(nsae);
        }
    }

    public static String sha1Digest(File file) {
        InputStream input = null;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            input = new BufferedInputStream(new FileInputStream(file));
            int n;
            while (-1 != (n = input.read(buffer))) {
                digest.update(buffer, 0, n);
            }
            return base64Encode(digest.digest());
        } catch (Exception nsae) {
            throw ExceptionUtils.bomb(nsae);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public static String stripLineSeparator(String s) {
        return s.replaceAll("\\n", "").replaceAll("\\r", "");
    }

    public static String matchPattern(String regEx, String s) {
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String removeTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String wrapConfigVariable(String var) {
        return String.format("${%s}", var);
    }

    public static List<String> splitLines(String str) {
        return str.isEmpty() ? new ArrayList<String>() : Arrays.asList(str.split("\n"));
    }

    public static String passwordToString(String password) {
        if (password == null) {
            return "not-set";
        }
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            buffer.append('*');
        }
        return buffer.toString();
    }

    public static String shortUUID() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String humanize(String s) {
        String[] strings = StringUtils.splitByCharacterTypeCamelCase(s);
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            strings[i] = string.toLowerCase();
        }
        return StringUtils.join(strings, " ");
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String nullToBlank(String str) {
        return (str == null) ? "" : str;
    }

    public static String escapeAndJoinStrings(Object... items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if (item != null) {
                builder.append(item.toString().replaceAll(DELIMITER_MATCHER, DELIMITER_ESCAPE_SEQ));
            }
            if (i + 1 < items.length) {
                builder.append(DELIMITER);
            }
        }
        return builder.toString();
    }

    public static String joinForDisplay(final Collection<?> objects) {
        return ListUtil.join(objects, " | ").trim();
    }

    public static String stripTrailingSlash(String url) {
        if (isBlank(url)) {
            return url;
        }
        int length = url.length();
        return url.charAt(length - 1) == '/' ? url.substring(0, length - 1) : url;
    }

    public static Boolean matches(String regEx, String string) {
        return Pattern.matches(regEx, string);
    }

    public static String stripTillLastOccurrenceOf(String input, String pattern) {
        if (!isBlank(input) && !isBlank(pattern)) {
            int index = input.lastIndexOf(pattern);
            if (index > 0) {
                input = input.substring(index + pattern.length());
            }
        }
        return input;
    }

    public static String unQuote(String string) {
        return string == null ? null: string.replaceAll("^\"|\"$", "");
    }
}
