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
package com.thoughtworks.go.config;

import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.DESCipherProvider;
import com.thoughtworks.go.security.DESEncrypter;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.decodeHex;

@Component
public class ConfigCipherUpdater {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ConfigCipherUpdater.class.getName());
    private final SystemEnvironment systemEnvironment;
    private final TimeProvider timeProvider;
    protected static final String FLAWED_VALUE = "64d04c1676ce2085";

    @Autowired
    public ConfigCipherUpdater(SystemEnvironment systemEnvironment, TimeProvider timeProvider) {
        this.systemEnvironment = systemEnvironment;
        this.timeProvider = timeProvider;
    }

    public void migrate() {
        File cipherFile = systemEnvironment.getDESCipherFile();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(timeProvider.currentTime());

        File backupCipherFile = new File(systemEnvironment.getConfigDir(), "cipher.original." + timestamp);
        File configFile = new File(systemEnvironment.getCruiseConfigFile());
        File backupConfigFile = new File(configFile.getParentFile(), configFile.getName() + ".original." + timestamp);
        try {
            if (!cipherFile.exists() || !FileUtils.readFileToString(cipherFile, UTF_8).equals(FLAWED_VALUE)) {
                return;
            }
            LOGGER.info("Found unsafe cipher {} on server, Go will make an attempt to rekey", FLAWED_VALUE);
            FileUtils.copyFile(cipherFile, backupCipherFile);
            LOGGER.info("Old cipher was successfully backed up to {}", backupCipherFile.getAbsoluteFile());
            FileUtils.copyFile(configFile, backupConfigFile);
            LOGGER.info("Old config was successfully backed up to {}", backupConfigFile.getAbsoluteFile());

            String oldCipher = FileUtils.readFileToString(backupCipherFile, UTF_8);
            new DESCipherProvider(systemEnvironment).resetCipher();

            String newCipher = FileUtils.readFileToString(cipherFile, UTF_8);

            if (newCipher.equals(oldCipher)) {
                LOGGER.warn("Unable to generate a new safe cipher. Your cipher is unsafe.");
                FileUtils.deleteQuietly(backupCipherFile);
                FileUtils.deleteQuietly(backupConfigFile);
                return;
            }
            Document document = new SAXBuilder().build(configFile);
            List<String> encryptedAttributes = Arrays.asList("encryptedPassword", "encryptedManagerPassword");
            List<String> encryptedNodes = Arrays.asList("encryptedValue");
            XPathFactory xPathFactory = XPathFactory.instance();
            for (String attributeName : encryptedAttributes) {
                XPathExpression<Element> xpathExpression = xPathFactory.compile(String.format("//*[@%s]", attributeName), Filters.element());
                List<Element> encryptedPasswordElements = xpathExpression.evaluate(document);
                for (Element element : encryptedPasswordElements) {
                    Attribute encryptedPassword = element.getAttribute(attributeName);
                    encryptedPassword.setValue(reEncryptUsingNewKey(decodeHex(oldCipher), decodeHex(newCipher), encryptedPassword.getValue()));
                    LOGGER.debug("Replaced encrypted value at {}", element.toString());
                }
            }
            for (String nodeName : encryptedNodes) {
                XPathExpression<Element> xpathExpression = xPathFactory.compile(String.format("//%s", nodeName), Filters.element());
                List<Element> encryptedNode = xpathExpression.evaluate(document);
                for (Element element : encryptedNode) {
                    element.setText(reEncryptUsingNewKey(decodeHex(oldCipher), decodeHex(newCipher), element.getValue()));
                    LOGGER.debug("Replaced encrypted value at {}", element.toString());
                }
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(configFile)) {
                XmlUtils.writeXml(document, fileOutputStream);
            }
            LOGGER.info("Successfully re-encrypted config");
        } catch (Exception e) {
            LOGGER.error("Re-keying of cipher failed with error: [{}]", e.getMessage(), e);
            if (backupCipherFile.exists()) {
                try {
                    FileUtils.copyFile(backupCipherFile, cipherFile);
                } catch (IOException e1) {
                    LOGGER.error("Could not replace the cipher file [{}] with original one [{}], please do so manually. Error: [{}]", cipherFile.getAbsolutePath(), backupCipherFile.getAbsolutePath(), e.getMessage(), e);
                    bomb(e1);
                }
            }
        }
    }

    private String reEncryptUsingNewKey(byte[] oldCipher, byte[] newCipher, String encryptedValue) throws CryptoException {
        return DESEncrypter.reEncryptUsingNewKey(oldCipher, newCipher, encryptedValue);
    }

}
