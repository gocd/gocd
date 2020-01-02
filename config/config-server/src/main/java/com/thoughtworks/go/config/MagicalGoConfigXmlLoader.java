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

import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidMergeException;
import com.thoughtworks.go.config.parser.ConfigReferenceElements;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.config.preprocessor.ConfigRepoPartialPreprocessor;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.validation.*;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.config.parser.GoConfigClassLoader.classParser;
import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;

public class MagicalGoConfigXmlLoader {
    public static final List<GoConfigPreprocessor> PREPROCESSORS = Arrays.asList(
            new ConfigRepoPartialPreprocessor(),
            new TemplateExpansionPreprocessor(),
            new ConfigParamPreprocessor());
    public static final List<GoConfigXMLValidator> XML_VALIDATORS = Arrays.asList((GoConfigXMLValidator) new UniqueOnCancelValidator());
    private static final Logger LOGGER = LoggerFactory.getLogger(MagicalGoConfigXmlLoader.class);
    private static final SystemEnvironment systemEnvironment = new SystemEnvironment();
    public static final List<GoConfigValidator> VALIDATORS = Arrays.asList(
            new ArtifactDirValidator(),
            new ServerIdImmutabilityValidator(),
            new CommandRepositoryLocationValidator(systemEnvironment),
            new TokenGenerationKeyImmutabilityValidator(systemEnvironment)
    );
    private static final GoConfigCloner CLONER = new GoConfigCloner();
    private final ConfigElementImplementationRegistry registry;
    private ConfigCache configCache;

    public MagicalGoConfigXmlLoader(ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this.configCache = configCache;
        this.registry = registry;
    }

    public static void setMd5(CruiseConfig configForEdit, String md5) throws NoSuchFieldException, IllegalAccessException {
        Field field = BasicCruiseConfig.class.getDeclaredField("md5");
        field.setAccessible(true);
        field.set(configForEdit, md5);
    }

    public static List<ConfigErrors> validate(CruiseConfig config) {
        preprocess(config);
        List<ConfigErrors> validationErrors = new ArrayList<>();
        validationErrors.addAll(config.validateAfterPreprocess());
        return validationErrors;
    }

    public static void preprocess(CruiseConfig cruiseConfig) {
        for (GoConfigPreprocessor preProcessor : PREPROCESSORS) {
            preProcessor.process(cruiseConfig);
        }
    }

    public static void validateDom(Element element, final ConfigElementImplementationRegistry registry) throws Exception {
        for (GoConfigXMLValidator xmlValidator : XML_VALIDATORS) {
            xmlValidator.validate(element, registry);
        }
    }

    public GoConfigHolder loadConfigHolder(final String content, Callback callback) throws Exception {
        CruiseConfig configForEdit;
        CruiseConfig config;
        LOGGER.debug("[Config Save] Loading config holder");
        configForEdit = deserializeConfig(content);
        if (callback != null) callback.call(configForEdit);
        config = preprocessAndValidate(configForEdit);

        return new GoConfigHolder(config, configForEdit);
    }

    public GoConfigHolder loadConfigHolder(final String content) throws Exception {
        return loadConfigHolder(content, null);
    }

    public CruiseConfig deserializeConfig(String content) throws Exception {
        String md5 = CachedDigestUtils.md5Hex(content);
        Element element = parseInputStream(new ByteArrayInputStream(content.getBytes()));
        LOGGER.debug("[Config Save] Updating config cache with new XML");

        CruiseConfig configForEdit = classParser(element, BasicCruiseConfig.class, configCache, new GoCipher(), registry, new ConfigReferenceElements()).parse();
        setMd5(configForEdit, md5);
        configForEdit.setOrigins(new FileConfigOrigin());
        return configForEdit;
    }

    public CruiseConfig preprocessAndValidate(CruiseConfig config) throws Exception {
        LOGGER.debug("[Config Validation] In preprocessAndValidate: Cloning.");
        CruiseConfig cloned = CLONER.deepClone(config);
        LOGGER.debug("[Config Validation] In preprocessAndValidate: Validating.");
        validateCruiseConfig(cloned);
        LOGGER.debug("[Config Validation] In preprocessAndValidate: Done.");
        config.encryptSecureProperties(cloned);
        return cloned;
    }

    public CruiseConfig validateCruiseConfig(CruiseConfig config) throws Exception {
        LOGGER.debug("[Config Save] In validateCruiseConfig: Starting.");
        List<ConfigErrors> allErrors = validate(config);
        if (!allErrors.isEmpty()) {
            if (config.isLocal())
                throw new GoConfigInvalidException(config, allErrors);
            else
                throw new GoConfigInvalidMergeException(config, config.getMergedPartials(), allErrors);
        }

        LOGGER.debug("[Config Save] In validateCruiseConfig: Running validate.");
        for (GoConfigValidator validator : VALIDATORS) {
            validator.validate(config);
        }

        LOGGER.debug("[Config Save] In validateCruiseConfig: Done.");
        return config;
    }

    private Element parseInputStream(InputStream inputStream) throws Exception {
        Element rootElement = buildXmlDocument(inputStream, GoConfigSchema.getCurrentSchema(), registry.xsds()).getRootElement();
        validateDom(rootElement, registry);
        return rootElement;
    }

    public <T> T fromXmlPartial(String partial, Class<T> o) throws Exception {
        return fromXmlPartial(toInputStream(partial, UTF_8), o);
    }

    public <T> T fromXmlPartial(InputStream inputStream, Class<T> o) throws Exception {
        Document document = new SAXBuilder().build(inputStream);
        Element element = document.getRootElement();
        return classParser(element, o, configCache, new GoCipher(), registry, new ConfigReferenceElements()).parse();
    }

    public GoConfigPreprocessor getPreprocessorOfType(final Class<? extends GoConfigPreprocessor> clazz) {
        return MagicalGoConfigXmlLoader.PREPROCESSORS.stream().filter(item -> item.getClass().isAssignableFrom(clazz)).findFirst().orElse(null);
    }

    public interface Callback {
        void call(CruiseConfig cruiseConfig);
    }
}
