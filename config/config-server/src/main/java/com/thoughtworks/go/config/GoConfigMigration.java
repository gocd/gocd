/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;
import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;

/**
 * Understands how to migrate from a previous version of config
 */
@Component
public class GoConfigMigration {
    private static final Logger LOG = LoggerFactory.getLogger(GoConfigMigration.class.getName());
    private static final int XPATH_EXPRESSION_OPERATION_LIMIT = 200;
    private final TimeProvider timeProvider;

    @Autowired
    public GoConfigMigration(final TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public File revertFileToVersion(File configFile, GoConfigRevision currentConfigRevision) {
        File backupFile = getBackupFile(configFile, "invalid.");
        try {
            backup(configFile, backupFile);
            // FIXME the lack of charset here looks rather suspicious. But unclear how to fix without possible regressions.
            // Related to similar issue in MagicalGoConfigXmlWriter?
            FileUtils.writeStringToFile(configFile, currentConfigRevision.getContent());
        } catch (IOException e1) {
            throw new RuntimeException(String.format("Could not write to config file '%s'.", configFile.getAbsolutePath()), e1);
        }

        return backupFile;
    }

    public String upgradeIfNecessary(String content) {
        return upgrade(content, getCurrentSchemaVersion(content));
    }

    private void backup(File configFile, File backupFile) throws IOException {
        FileUtils.copyFile(configFile, backupFile);
        LOG.info("Config file is backed up, location: {}", backupFile.getAbsolutePath());
    }

    File getBackupFile(File configFile, final String prefix) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(timeProvider.currentTime());
        return new File(configFile + "." + prefix + timestamp);
    }

    private String upgrade(String content, int currentVersion) {
        int targetVersion = GoConfigSchema.currentSchemaVersion();
        return upgrade(content, currentVersion, targetVersion);
    }

    public String upgrade(String content, int currentVersion, int targetVersion) {
        LOG.info("Upgrading config file from version {} to version {}", currentVersion, targetVersion);
        List<URL> upgradeScripts = upgradeScripts(currentVersion, targetVersion);

        for (URL upgradeScript : upgradeScripts) {
            validate(content);
            content = upgrade(content, upgradeScript);
        }
        validate(content);
        LOG.info("Finished upgrading config file");
        return content;
    }

    private void validate(String content) {
        int currentVersion = getCurrentSchemaVersion(content);
        try {
            buildXmlDocument(new ByteArrayInputStream(content.getBytes()), GoConfigSchema.getResource(currentVersion));
        } catch (Exception e) {
            throw bomb("Cruise config file with version " + currentVersion + " is invalid. Unable to upgrade.", e);
        }
    }

    private String upgrade(String originalContent, URL upgradeScript) {
        try (InputStream xslt = upgradeScript.openStream()) {
            ByteArrayOutputStream convertedConfig = new ByteArrayOutputStream();
            transformer(upgradeScript.getPath(), xslt)
                    .transform(new StreamSource(new ByteArrayInputStream(originalContent.getBytes())), new StreamResult(convertedConfig));
            return convertedConfig.toString();
        } catch (TransformerException e) {
            throw bomb("Couldn't transform configuration file using upgrade script " + upgradeScript.getPath(), e);
        } catch (IOException e) {
            throw bomb("Couldn't write converted config file", e);
        }
    }

    private List<URL> upgradeScripts(int currentVersion, int targetVersion) {
        ArrayList<URL> xsls = new ArrayList<>();
        for (int i = currentVersion + 1; i <= targetVersion; i++) {
            String scriptFile = i + ".xsl";
            URL xsl = getResource("/upgrades/" + scriptFile);
            bombIfNull(xsl, () -> "Config File upgrade script named " + scriptFile + " is missing. Unable to perform upgrade.");
            xsls.add(xsl);
        }
        return xsls;
    }

    private URL getResource(String script) {
        return GoConfigMigration.class.getResource(script);
    }

    private Transformer transformer(String xsltName, InputStream xslt) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            tryIncreaseXpathExpressionOperationLimit(factory);
            return factory.newTransformer(new StreamSource(xslt));
        } catch (TransformerConfigurationException tce) {
            throw bomb("Couldn't parse XSL template " + xsltName, tce);
        }
    }

    private void tryIncreaseXpathExpressionOperationLimit(TransformerFactory factory) {
        try {
            factory.setAttribute("jdk.xml.xpathExprOpLimit", XPATH_EXPRESSION_OPERATION_LIMIT);
        } catch (IllegalArgumentException e) {
            LOG.info("Cannot increase Xpath Expression Operation Limit, may not be supported on this JDK. Continuing... [{}]", e.getMessage());
        }
    }

    private int getCurrentSchemaVersion(String content) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new ByteArrayInputStream(content.getBytes()));
            Element root = document.getRootElement();

            String schemaVersion = "schemaVersion";
            String currentVersion = root.getAttributeValue(schemaVersion) == null ? "0" : root.getAttributeValue(schemaVersion);
            return Integer.parseInt(currentVersion);
        } catch (Exception e) {
            throw bomb(e);
        }
    }
}
