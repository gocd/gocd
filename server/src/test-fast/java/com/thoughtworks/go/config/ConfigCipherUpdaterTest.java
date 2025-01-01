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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistrar;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.AESCipherProvider;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.DESCipherProvider;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.thoughtworks.go.config.ConfigCipherUpdater.FLAWED_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ConfigCipherUpdaterTest {
    private SystemEnvironment systemEnvironment = new SystemEnvironment();
    private ConfigCipherUpdater updater;
    private String timestamp;
    private ConfigCache configCache;
    private ConfigElementImplementationRegistry registry;
    private final String passwordEncryptedWithFlawedCipher = "ruRUF0mi2ia/BWpWMISbjQ==";
    private MagicalGoConfigXmlLoader magicalGoConfigXmlLoader;
    private final String password = "password";
    private File originalConfigFile;

    @BeforeEach
    public void setUp(@TempDir File configDir) throws Exception {
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_DIR_PROPERTY, configDir.getAbsolutePath());
        final Date currentTime = new DateTime().toDate();
        timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(currentTime);
        updater = new ConfigCipherUpdater(systemEnvironment, new TimeProvider() {
            @Override
            public Date currentTime() {
                return currentTime;
            }
        });
        configCache = new ConfigCache();
        registry = new ConfigElementImplementationRegistry();
        ConfigElementImplementationRegistrar registrar = new ConfigElementImplementationRegistrar(registry);
        registrar.initialize();
        magicalGoConfigXmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);
        File configFileEncryptedWithFlawedCipher = new ClassPathResource("cruise-config-with-encrypted-with-flawed-cipher.xml").getFile();
        writeStringToFile(systemEnvironment.getDESCipherFile(), FLAWED_VALUE, UTF_8);
        ReflectionUtil.setStaticField(DESCipherProvider.class, "cachedKey", null);
        ReflectionUtil.setStaticField(AESCipherProvider.class, "cachedKey", null);
        originalConfigFile = new File(systemEnvironment.getCruiseConfigFile());
        writeStringToFile(originalConfigFile, readFileToString(configFileEncryptedWithFlawedCipher, UTF_8), UTF_8);
    }

    @AfterEach
    public void tearDown() throws Exception {
        new SystemEnvironment().clearProperty(SystemEnvironment.CONFIG_DIR_PROPERTY);
    }

    @Test
    public void shouldNotMigrateAnythingIfCipherFileIsNotPresent_FreshInstalls() {
        FileUtils.deleteQuietly(systemEnvironment.getDESCipherFile());
        FileUtils.deleteQuietly(systemEnvironment.getAESCipherFile());
        ReflectionUtil.setStaticField(DESCipherProvider.class, "cachedKey", null);
        ReflectionUtil.setStaticField(AESCipherProvider.class, "cachedKey", null);

        updater.migrate();
        assertThat(systemEnvironment.getDESCipherFile().exists()).isFalse();
        assertThat(systemEnvironment.getAESCipherFile().exists()).isFalse();
    }

    @Test
    public void shouldMigrateEncryptedPasswordsThatWereEncryptedWithFlawedCipher() throws Exception {
        String originalConfig = readFileToString(originalConfigFile, UTF_8);
        assertThat(originalConfig).contains("encryptedPassword=\"" + passwordEncryptedWithFlawedCipher + "\"");

        updater.migrate();
        writeStringToFile(new File(systemEnvironment.getCruiseConfigFile()), ConfigMigrator.migrate(readFileToString(new File(systemEnvironment.getCruiseConfigFile()), UTF_8)), UTF_8);

        File copyOfOldConfig = new File(systemEnvironment.getConfigDir(), "cipher.original." + timestamp);
        assertThat(copyOfOldConfig.exists()).isTrue();
        assertThat(readFileToString(copyOfOldConfig, UTF_8).equals(FLAWED_VALUE)).isTrue();
        String newCipher = readFileToString(systemEnvironment.getDESCipherFile(), UTF_8);
        assertThat(newCipher.equals(FLAWED_VALUE)).isFalse();
        File editedConfigFile = new File(systemEnvironment.getCruiseConfigFile());
        String editedConfig = readFileToString(editedConfigFile, UTF_8);
        assertThat(editedConfig.contains("encryptedPassword=\"" + passwordEncryptedWithFlawedCipher + "\"")).isFalse();
        CruiseConfig config = magicalGoConfigXmlLoader.loadConfigHolder(editedConfig).config;
        MaterialConfigs materialConfigs = config.getAllPipelineConfigs().get(0).materialConfigs();
        SvnMaterialConfig svnMaterial = materialConfigs.getSvnMaterial();
        assertThat(svnMaterial.getPassword()).isEqualTo(password);
        assertThat(svnMaterial.getEncryptedPassword()).startsWith("AES:");
        assertThat(new GoCipher().decrypt(svnMaterial.getEncryptedPassword())).isEqualTo("password");
        P4MaterialConfig p4Material = materialConfigs.getP4Material();
        assertThat(p4Material.getPassword()).isEqualTo(password);
        assertThat(p4Material.getEncryptedPassword()).startsWith("AES:");
        assertThat(new GoCipher().decrypt(p4Material.getEncryptedPassword())).isEqualTo("password");
        TfsMaterialConfig tfsMaterial = materialConfigs.getTfsMaterial();
        assertThat(tfsMaterial.getPassword()).isEqualTo(password);

        assertThat(tfsMaterial.getEncryptedPassword()).startsWith("AES:");
        assertThat(new GoCipher().decrypt(tfsMaterial.getEncryptedPassword())).isEqualTo("password");
    }

    @Test
    public void shouldMigrateEncryptedManagerPasswordsEncryptedWithFlawedCipher() throws Exception {
        String originalConfig = readFileToString(originalConfigFile, UTF_8);

        assertThat(originalConfig).contains("encryptedPassword=\"" + passwordEncryptedWithFlawedCipher + "\"");

        updater.migrate();
        writeStringToFile(new File(systemEnvironment.getCruiseConfigFile()), ConfigMigrator.migrate(readFileToString(new File(systemEnvironment.getCruiseConfigFile()), UTF_8)), UTF_8);
        File copyOfOldConfig = new File(systemEnvironment.getConfigDir(), "cipher.original." + timestamp);
        assertThat(copyOfOldConfig.exists()).isTrue();
        assertThat(readFileToString(copyOfOldConfig, UTF_8).equals(FLAWED_VALUE)).isTrue();
        assertThat(readFileToString(systemEnvironment.getDESCipherFile(), UTF_8).equals(FLAWED_VALUE)).isFalse();
        File editedConfigFile = new File(systemEnvironment.getCruiseConfigFile());
        String editedConfig = readFileToString(editedConfigFile, UTF_8);
        assertThat(editedConfig.contains("encryptedManagerPassword=\"" + passwordEncryptedWithFlawedCipher + "\"")).isFalse();
        CruiseConfig config = magicalGoConfigXmlLoader.loadConfigHolder(editedConfig).config;
        SecurityAuthConfig migratedLdapConfig = config.server().security().securityAuthConfigs().get(0);

        assertThat(migratedLdapConfig.getProperty("Password").getValue()).isEqualTo(password);
        assertThat(new GoCipher().decrypt(migratedLdapConfig.getProperty("Password").getEncryptedValue())).isEqualTo(password);
    }

    @Test
    public void shouldMigrateEncryptedValuesEncryptedWithFlawedCipher() throws Exception {
        String originalConfig = readFileToString(originalConfigFile, UTF_8);
        assertThat(originalConfig).contains("<encryptedValue>" + passwordEncryptedWithFlawedCipher + "</encryptedValue>");

        updater.migrate();
        writeStringToFile(new File(systemEnvironment.getCruiseConfigFile()), ConfigMigrator.migrate(readFileToString(new File(systemEnvironment.getCruiseConfigFile()), UTF_8)), UTF_8);
        File copyOfOldConfig = new File(systemEnvironment.getConfigDir(), "cipher.original." + timestamp);
        assertThat(copyOfOldConfig.exists()).isTrue();
        assertThat(readFileToString(copyOfOldConfig, UTF_8).equals(FLAWED_VALUE)).isTrue();
        assertThat(readFileToString(systemEnvironment.getDESCipherFile(), UTF_8).equals(FLAWED_VALUE)).isFalse();
        File editedConfigFile = new File(systemEnvironment.getCruiseConfigFile());
        String editedConfig = readFileToString(editedConfigFile, UTF_8);
        assertThat(editedConfig.contains("<encryptedValue>" + passwordEncryptedWithFlawedCipher + "</encryptedValue>")).isFalse();

        CruiseConfig config = magicalGoConfigXmlLoader.loadConfigHolder(editedConfig).config;
        EnvironmentVariablesConfig secureVariables = config.getAllPipelineConfigs().get(0).getSecureVariables();
        assertThat(secureVariables.first().getValue()).isEqualTo(password);
        assertThat(new GoCipher().decrypt(secureVariables.first().getEncryptedValue())).isEqualTo(password);
    }

    @Test
    public void shouldNotTryMigratingOlderConfigsWhichWereNotEncryptedWithFlawedCipher() throws IOException, CryptoException {
        String goodCipher = "269298bc31c44620";
        writeStringToFile(systemEnvironment.getDESCipherFile(), goodCipher, UTF_8);
        File originalConfigFile = new File(systemEnvironment.getCruiseConfigFile());
        copyFile(new ClassPathResource("cruise-config-with-encrypted-with-safe-cipher.xml").getFile(), originalConfigFile);
        String originalConfig = readFileToString(originalConfigFile, UTF_8);
        assertThat(originalConfig).contains("encryptedPassword=\"pVyuW5ny9I6YT4Ou+KLZhQ==\"");

        updater.migrate();
        writeStringToFile(new File(systemEnvironment.getCruiseConfigFile()), ConfigMigrator.migrate(readFileToString(new File(systemEnvironment.getCruiseConfigFile()), UTF_8)), UTF_8);
        File copyOfOldConfig = new File(systemEnvironment.getConfigDir(), "cipher.original." + timestamp);
        assertThat(copyOfOldConfig.exists()).isFalse();
        assertThat(readFileToString(systemEnvironment.getDESCipherFile(), UTF_8).equals(goodCipher)).isTrue();
        File configFile = new File(systemEnvironment.getCruiseConfigFile());
        String editedConfig = readFileToString(configFile, UTF_8);
        assertThat(editedConfig).contains("encryptedPassword=\"" + new GoCipher().encrypt("password") + "\"");
    }

}
