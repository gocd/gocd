/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
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
 * @understands how to migrate from a previous version of config
 */
@Component
public class GoConfigMigration {
    private static final Logger LOG = Logger.getLogger(GoConfigMigration.class);
    private final String schemaVersion = "schemaVersion";
    private final UpgradeFailedHandler upgradeFailed;
    private final ConfigRepository configRepository;
    private final TimeProvider timeProvider;
    private ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;

    public static final String UPGRADE = "Upgrade";

    @Autowired
    public GoConfigMigration(final ConfigRepository configRepository, final TimeProvider timeProvider, ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this(new UpgradeFailedHandler() {
            public void handle(Exception e) {
                e.printStackTrace();
                System.err.println(
                        "There are errors in the Cruise config file.  Please read the error message and correct the errors.\n"
                                + "Once fixed, please restart Cruise.\nError: " + e.getMessage());
                LOG.fatal(
                        "There are errors in the Cruise config file.  Please read the error message and correct the errors.\n"
                                + "Once fixed, please restart Cruise.\nError: " + e.getMessage());
                // Send exit signal in a separate thread otherwise it will deadlock jetty
                new Thread(new Runnable() {
                    public void run() {
                        System.exit(1);
                    }
                }).start();

            }
        }, configRepository, timeProvider, configCache, registry);
    }

    GoConfigMigration(UpgradeFailedHandler upgradeFailed, ConfigRepository configRepository, TimeProvider timeProvider,
                      ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this.upgradeFailed = upgradeFailed;
        this.configRepository = configRepository;
        this.timeProvider = timeProvider;
        this.configCache = configCache;
        this.registry = registry;
    }

    public GoConfigMigrationResult upgradeIfNecessary(File configFile, final String currentGoServerVersion) {
        try {
            return upgradeValidateAndVersion(configFile, true, currentGoServerVersion);
        } catch (Exception e) {
            upgradeFailed.handle(e);
        }
        return GoConfigMigrationResult.unexpectedFailure("Failed to upgrade");
    }

    private GoConfigMigrationResult upgradeValidateAndVersion(File configFile, boolean shouldTryOlderVersion, String currentGoServerVersion) throws Exception {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            String xmlStringBeforeUpgrade = FileUtils.readFileToString(configFile);
            int currentVersion = getCurrentSchemaVersion(xmlStringBeforeUpgrade);
            String reloadedXml;
            if (shouldUpgrade(currentVersion)) {
                backup(configFile);
                reloadedXml = upgrade(xmlStringBeforeUpgrade, currentVersion);
                GoConfigHolder configHolder = reloadedConfig(stream, reloadedXml);
                reloadedXml = new String(stream.toByteArray());
                configRepository.checkin(new GoConfigRevision(reloadedXml, CachedDigestUtils.md5Hex(reloadedXml), UPGRADE,
                        currentGoServerVersion, timeProvider));
            } else {
                GoConfigHolder configHolder = reloadedConfig(stream, xmlStringBeforeUpgrade);
                reloadedXml = new String(stream.toByteArray());
            }
            FileUtils.writeStringToFile(configFile, reloadedXml);
        } catch (Exception e) {
            GoConfigRevision currentConfigRevision = configRepository.getCurrentRevision();
            if (shouldTryOlderVersion && ifVersionedConfig(currentConfigRevision)) {
                GoConfigMigrationResult goConfigMigrationResult = revertFileToVersion(configFile, currentConfigRevision, e);
                upgradeValidateAndVersion(configFile, false, currentGoServerVersion);
                return goConfigMigrationResult;
            } else {
                log(shouldTryOlderVersion);
                throw e;
            }
        }
        return GoConfigMigrationResult.success();
    }

    private GoConfigHolder reloadedConfig(ByteArrayOutputStream stream, String upgradedXmlString) throws Exception {
        GoConfigHolder configHolder = validateAfterMigrationFinished(upgradedXmlString);
        new MagicalGoConfigXmlWriter(configCache, registry).write(configHolder.configForEdit, stream, false);
        return configHolder;
    }

    private GoConfigMigrationResult revertFileToVersion(File configFile, GoConfigRevision currentConfigRevision, Exception e) throws Exception {
        File backupFile = getBackupFile(configFile, "invalid.");
        try {
            backup(configFile, backupFile);
            FileUtils.writeStringToFile(configFile, currentConfigRevision.getContent());
        } catch (IOException e1) {
            throw new RuntimeException(String.format("Could not write to config file '%s'.", configFile.getAbsolutePath()), e1);
        }

        String invalidConfigMessage = String.format("Go encountered an invalid configuration file while starting up. "
                + "The invalid configuration file has been renamed to ‘%s’ and a new configuration file has been automatically created using the last good configuration. Cause: '%s'",
                backupFile.getAbsolutePath(), e.getMessage());
        return GoConfigMigrationResult.failedToUpgrade(invalidConfigMessage);
    }

    private void log(boolean shouldTryOlderVersion) {
        if (shouldTryOlderVersion) {
            LOG.warn("There is no versioned configuration to use.");
        } else {
            LOG.warn("The versioned config file could be invalid or migrating the versioned config resulted in an invalid configuration");
        }
    }

    private boolean ifVersionedConfig(GoConfigRevision currentConfigRevision) {
        return currentConfigRevision != null;
    }

    public String upgradeIfNecessary(String content) {
        return upgrade(content, getCurrentSchemaVersion(content));
    }

    private boolean shouldUpgrade(int currentVersion) {
        return currentVersion < GoConfigSchema.currentSchemaVersion();
    }

    private GoConfigHolder validateAfterMigrationFinished(String content) throws Exception {
        return new MagicalGoConfigXmlLoader(configCache, registry).loadConfigHolder(content);
    }

    private void backup(File configFile) throws IOException {
        File backupFile = getBackupFile(configFile, "");
        backup(configFile, backupFile);
    }

    private void backup(File configFile, File backupFile) throws IOException {
        FileUtils.copyFile(configFile, backupFile);
        LOG.info("Config file is backed up, location: " + backupFile.getAbsolutePath());
    }

    File getBackupFile(File configFile, final String prefix) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(timeProvider.currentTime());
        return new File(configFile + "." + prefix + timestamp);
    }

    private String upgrade(String content, int currentVersion) {
        int targetVersion = GoConfigSchema.currentSchemaVersion();
        return upgrade(content, currentVersion, targetVersion);
    }

    private String upgrade(String content, int currentVersion, int targetVersion) {
        LOG.info("Upgrading config file from version " + currentVersion + " to version " + targetVersion);
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
            buildXmlDocument(new ByteArrayInputStream(content.getBytes()), GoConfigSchema.getResource(currentVersion), registry.xsds());
        } catch (Exception e) {
            throw bomb("Cruise config file with version " + currentVersion + " is invalid. Unable to upgrade.", e);
        }
    }

    private String upgrade(String originalContent, URL upgradeScript) {
        InputStream xslt = null;
        try {
            xslt = upgradeScript.openStream();
            ByteArrayOutputStream convertedConfig = new ByteArrayOutputStream();
            transformer(upgradeScript.getPath(), xslt)
                    .transform(new StreamSource(new ByteArrayInputStream(originalContent.getBytes())), new StreamResult(convertedConfig));
            return convertedConfig.toString();
        } catch (TransformerException e) {
            throw bomb("Couldn't transform configuration file using upgrade script " + upgradeScript.getPath(), e);
        } catch (IOException e) {
            throw bomb("Couldn't write converted config file", e);
        } finally {
            IOUtils.closeQuietly(xslt);
        }
    }

    private List<URL> upgradeScripts(int currentVersion, int targetVersion) {
        ArrayList<URL> xsls = new ArrayList<>();
        for (int i = currentVersion + 1; i <= targetVersion; i++) {
            URL xsl = getResource("/upgrades/" + i + ".xsl");
            bombIfNull(xsl, "Config File upgrade script named " + i + ".xsl is missing. Unable to perform upgrade.");
            xsls.add(xsl);
        }
        return xsls;
    }

    private URL getResource(String script) {
        return GoConfigMigration.class.getResource(script);
    }

    private Transformer transformer(String xsltName, InputStream xslt) {
        try {
            return TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        } catch (TransformerConfigurationException tce) {
            throw bomb("Couldn't parse XSL template " + xsltName, tce);
        }
    }

    private int getCurrentSchemaVersion(String content) {
        try {
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(new ByteArrayInputStream(content.getBytes()));
            Element root = document.getRootElement();

            String currentVersion = root.getAttributeValue(schemaVersion) == null ? "0" : root.getAttributeValue(schemaVersion);
            return Integer.parseInt(currentVersion);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    public static interface UpgradeFailedHandler {
        void handle(Exception e);
    }

}
