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
package com.thoughtworks.go.apiv1.mailserver.representers;

import com.thoughtworks.go.api.base.OutputLinkWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.MailHost;
import com.thoughtworks.go.config.materials.PasswordDeserializer;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.spark.Routes;

import java.util.Collections;
import java.util.function.Consumer;

public class MailServerRepresenter {
    private static final PasswordDeserializer PASSWORD_DESERIALIZER = new PasswordDeserializer();

    public static void toJSON(OutputWriter writer, MailHost mailhost) {
        writer
                .addLinks(links())
                .add("hostname", mailhost.getHostName())
                .add("port", mailhost.getPort())
                .add("username", mailhost.getUsername())
                .add("encrypted_password", mailhost.getEncryptedPassword())
                .add("tls", mailhost.isTls())
                .add("sender_email", mailhost.getFrom())
                .add("admin_email", mailhost.getAdminMail());

        if (!mailhost.errors().isEmpty()) {
            writer.addChild("errors", errorWriter -> {
                new ErrorGetter(Collections.emptyMap()).toJSON(errorWriter, mailhost);
            });
        }

    }

    public static MailHost fromJSON(JsonReader jsonReader) {
        MailHost mailHost = new MailHost(new GoCipher());

        jsonReader
                .readStringIfPresent("hostname", mailHost::setHostName)
                .readIntIfPresent("port", mailHost::setPort)
                .readStringIfPresent("username", mailHost::setUsername)

                .readBooleanIfPresent("tls", mailHost::setTls)
                .readStringIfPresent("sender_email", mailHost::setFrom)
                .readStringIfPresent("admin_email", mailHost::setAdminMail);

        String password = jsonReader.getStringOrDefault("password", null);
        String encryptedPassword = jsonReader.getStringOrDefault("encrypted_password", null);
        mailHost.setEncryptedPassword(PASSWORD_DESERIALIZER.deserialize(password, encryptedPassword, mailHost));

        return mailHost;
    }

    private static Consumer<OutputLinkWriter> links() {
        return linksWriter -> linksWriter
                .addLink("self", Routes.MailServer.BASE)
                .addAbsoluteLink("doc", Routes.MailServer.DOC);
    }

}
