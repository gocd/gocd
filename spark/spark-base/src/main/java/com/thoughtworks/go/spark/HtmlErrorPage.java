/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.spark;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.String.valueOf;

public abstract class HtmlErrorPage {

    public static String errorPage(int code, String message) {
        return Holder.INSTANCE.replaceAll(buildRegex("status_code"), valueOf(code))
                .replaceAll(buildRegex("error_message"), message);
    }

    private static String buildRegex(final String value) {
        return "\\{\\{" + value + "\\}\\}";
    }


    private static class Holder {
        private static final String INSTANCE = fileContents();

        private static String fileContents() {
            try (InputStream in = Holder.class.getResourceAsStream("/error.html")) {
                return IOUtils.toString(in, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
