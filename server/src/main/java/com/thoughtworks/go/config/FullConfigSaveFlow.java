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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.TimeProvider;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public abstract class FullConfigSaveFlow {
    protected final MagicalGoConfigXmlLoader loader;
    protected final MagicalGoConfigXmlWriter writer;
    protected final TimeProvider timeProvider;
    protected final ConfigRepository configRepository;
    protected final CachedGoPartials cachedGoPartials;
    protected final GoConfigFileWriter fileWriter;
    protected final ConfigElementImplementationRegistry configElementImplementationRegistry;
    protected final GoConfigCloner cloner = new GoConfigCloner();
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

    public FullConfigSaveFlow(MagicalGoConfigXmlLoader loader, MagicalGoConfigXmlWriter writer,
                              ConfigElementImplementationRegistry configElementImplementationRegistry,
                              TimeProvider timeProvider,
                              ConfigRepository configRepository, CachedGoPartials cachedGoPartials,
                              GoConfigFileWriter fileWriter) {
        this.loader = loader;
        this.writer = writer;
        this.configElementImplementationRegistry = configElementImplementationRegistry;
        this.timeProvider = timeProvider;
        this.configRepository = configRepository;
        this.cachedGoPartials = cachedGoPartials;
        this.fileWriter = fileWriter;
    }

    public abstract GoConfigHolder execute(FullConfigUpdateCommand updatingCommand, List<PartialConfig> partials, String currentUser) throws Exception;

    protected void postValidationUpdates(CruiseConfig configForEdit, String xmlString) throws NoSuchFieldException, IllegalAccessException {
        String md5 = CachedDigestUtils.md5Hex(xmlString);

        configForEdit.setOrigins(new FileConfigOrigin());

        MagicalGoConfigXmlLoader.setMd5(configForEdit, md5);
    }

    protected String toXmlString(CruiseConfig configForEdit) throws Exception {
        Document document = documentFrom(configForEdit);

        validateDocument(document);

        return toXmlString(document);
    }

    protected void checkinToConfigRepo(String currentUser, CruiseConfig updatedConfig, String xmlString) throws Exception {
        LOGGER.debug("[Config Save] Checkin updated config to git: Starting.");
        configRepository.checkin(new GoConfigRevision(xmlString, updatedConfig.getMd5(), currentUser, CurrentGoCDVersion.getInstance().formatted(), timeProvider));
        LOGGER.debug("[Config Save] Checkin updated config to git: Done.");
    }

    protected void writeToConfigXml(String xmlString) {
        LOGGER.debug("[Config Save] Writing config xml to file: Starting.");
        this.fileWriter.writeToConfigXmlFile(xmlString);
        LOGGER.debug("[Config Save] Writing config xml to file: Done.");
    }

    protected CruiseConfig configForEditWithPartials(FullConfigUpdateCommand updatingCommand, List<PartialConfig> partials) {
        CruiseConfig config = updatingCommand.configForEdit();
        config.setPartials(partials);

        return config;
    }

    protected CruiseConfig preprocessAndValidate(CruiseConfig configForEdit) throws Exception {
        return loader.preprocessAndValidate(configForEdit);
    }

    protected org.jdom2.Document documentFrom(CruiseConfig configForEdit) {
        LOGGER.debug("[Config Save] Building Document from CruiseConfig object: Starting.");
        Document document = writer.documentFrom(configForEdit);
        LOGGER.debug("[Config Save] Building Document from CruiseConfig object: Done.");

        return document;
    }

    protected void validateDocument(Document document) throws Exception {
        LOGGER.debug("[Config Save] In XSD validation: Starting.");
        writer.verifyXsdValid(document);
        LOGGER.debug("[Config Save] In XSD validation: Done.");

        LOGGER.debug("[Config Save] In DOM validation: Starting.");
        MagicalGoConfigXmlLoader.validateDom(document.getRootElement(), configElementImplementationRegistry);
        LOGGER.debug("[Config Save] In DOM validation: Done.");
    }

    protected String toXmlString(Document document) throws IOException {
        LOGGER.debug("[Config Save] Serializing Document to xml: Starting.");
        String xmlString = writer.toString(document);
        LOGGER.debug("[Config Save] Serializing Document to xml: Done.");

        return xmlString;
    }

    protected void setMergedConfigForEditOn(GoConfigHolder validatedConfigHolder, List<PartialConfig> partials) {
        if (partials.isEmpty()) return;

        LOGGER.debug("[Config Save] Updating GoConfigHolder with mergedCruiseConfigForEdit: Starting.");
        CruiseConfig mergedCruiseConfigForEdit = cloner.deepClone(validatedConfigHolder.configForEdit);
        mergedCruiseConfigForEdit.merge(partials, true);
        validatedConfigHolder.mergedConfigForEdit = mergedCruiseConfigForEdit;
        LOGGER.debug("[Config Save] Updating GoConfigHolder with mergedCruiseConfigForEdit: Done.");
    }
}
