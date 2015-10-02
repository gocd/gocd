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

package com.thoughtworks.go.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.parser.ConfigReferenceElements;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.validation.*;
import com.thoughtworks.go.config.validation.GoConfigValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.metrics.domain.context.Context;
import com.thoughtworks.go.metrics.domain.probes.ProbeType;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.XmlUtils;
import com.thoughtworks.go.util.XsdErrorTranslator;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import static com.thoughtworks.go.config.parser.GoConfigClassLoader.classParser;
import static org.apache.commons.io.IOUtils.toInputStream;

public class MagicalGoConfigXmlLoader {
    private static final Logger LOGGER = Logger.getLogger(MagicalGoConfigXmlLoader.class);

    public static final List<GoConfigPreprocessor> PREPROCESSORS = Arrays.asList(
            new TemplateExpansionPreprocessor(),
            new ConfigParamPreprocessor());

    public static final List<GoConfigValidator> VALIDATORS = Arrays.asList(
            new ArtifactDirValidator(),
            new EnvironmentAgentValidator(),
            new EnvironmentPipelineValidator(),
            new ServerIdImmutabilityValidator(),
            new CommandRepositoryLocationValidator(new SystemEnvironment())
    );

    public static final List<GoConfigXMLValidator> XML_VALIDATORS = Arrays.asList((GoConfigXMLValidator)new UniqueOnCancelValidator());

    private static final Cloner CLONER = new Cloner();
    private ConfigCache configCache;
    private SAXBuilder builder;
    private final ConfigElementImplementationRegistry registry;
    private final MetricsProbeService metricsProbeService;

    public MagicalGoConfigXmlLoader(ConfigCache configCache, ConfigElementImplementationRegistry registry, MetricsProbeService metricsProbeService) {
        this.configCache = configCache;
        this.metricsProbeService = metricsProbeService;
        builder = new SAXBuilder();
        this.registry = registry;
    }

    public GoConfigHolder loadConfigHolder(final String content) throws Exception {
        CruiseConfig configForEdit;
        CruiseConfig config;
        Context context = metricsProbeService.begin(ProbeType.CONVERTING_CONFIG_XML_TO_OBJECT);
        try {
            LOGGER.debug("[Config Save] Loading config holder");
            String md5 = CachedDigestUtils.md5Hex(content);
            Element element = parseInputStream(new ByteArrayInputStream(content.getBytes()));
            LOGGER.debug("[Config Save] Updating config cache with new XML");

            configForEdit = classParser(element, BasicCruiseConfig.class, configCache, new GoCipher(), registry, new ConfigReferenceElements()).parse();
            setMd5(configForEdit, md5);
            configForEdit.setOrigins(new FileConfigOrigin());
            config = preprocessAndValidate(configForEdit);
        } finally {
            metricsProbeService.end(ProbeType.CONVERTING_CONFIG_XML_TO_OBJECT, context);
        }

        return new GoConfigHolder(config, configForEdit);
    }


    public static void setMd5(CruiseConfig configForEdit, String md5) throws NoSuchFieldException, IllegalAccessException {
        Field field = BasicCruiseConfig.class.getDeclaredField("md5");
        field.setAccessible(true);
        field.set(configForEdit, md5);
    }

    public CruiseConfig preprocessAndValidate(CruiseConfig config) throws Exception {
        LOGGER.debug("[Config Save] In preprocessAndValidate: Cloning.");
        Context context = metricsProbeService.begin(ProbeType.PREPROCESS_AND_VALIDATE);
        CruiseConfig cloned;
        try {
            cloned = CLONER.deepClone(config);
            LOGGER.debug("[Config Save] In preprocessAndValidate: Validating.");
            validateCruiseConfig(cloned);
            LOGGER.debug("[Config Save] In preprocessAndValidate: Done.");
        } finally {
            metricsProbeService.end(ProbeType.PREPROCESS_AND_VALIDATE, context);
        }
        return cloned;
    }

    public static List<ConfigErrors> validate(CruiseConfig config) {
        preprocess(config);
        List<ConfigErrors> validationErrors = new ArrayList<ConfigErrors>();
        validationErrors.addAll(config.validateAfterPreprocess());
        return validationErrors;
    }

    public static void preprocess(CruiseConfig cruiseConfig) {
        for (GoConfigPreprocessor preProcessor : PREPROCESSORS) {
            preProcessor.process(cruiseConfig);
        }
    }

    private CruiseConfig validateCruiseConfig(CruiseConfig config) throws Exception {
        LOGGER.debug("[Config Save] In validateCruiseConfig: Starting.");
        Context context = metricsProbeService.begin(ProbeType.VALIDATING_CONFIG);
        try {
            List<ConfigErrors> allErrors = validate(config);
            if (!allErrors.isEmpty()) {
                throw new GoConfigInvalidException(config, allErrors);
            }

            LOGGER.debug("[Config Save] In validateCruiseConfig: Running validate.");
            for (GoConfigValidator validator : VALIDATORS) {
                validator.validate(config);
            }

            LOGGER.debug("[Config Save] In validateCruiseConfig: Done.");
        } finally {
            metricsProbeService.end(ProbeType.VALIDATING_CONFIG, context);
        }
        return config;
    }

    private Element parseInputStream(InputStream inputStream) throws Exception {
        Element element = XmlUtils.validate(inputStream, GoConfigSchema.getCurrentSchema(), new XsdErrorTranslator(), builder, registry.xsds());
        validateDom(element, registry);
        return element;
    }

    public static void validateDom(Element element, final ConfigElementImplementationRegistry registry) throws Exception {
        for (GoConfigXMLValidator xmlValidator : XML_VALIDATORS) {
            xmlValidator.validate(element, registry);
        }
    }

    public <T> T fromXmlPartial(String partial, Class<T> o) throws Exception {
        return fromXmlPartial(toInputStream(partial), o);
    }

    public <T> T fromXmlPartial(InputStream inputStream, Class<T> o) throws Exception {
        Document document = new SAXBuilder().build(inputStream);
        Element element = document.getRootElement();
        return classParser(element, o, configCache, new GoCipher(), registry, new ConfigReferenceElements()).parse();
    }

}
