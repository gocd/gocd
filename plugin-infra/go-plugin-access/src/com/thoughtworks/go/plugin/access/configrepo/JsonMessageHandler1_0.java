package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.migration.Migration_1;
import com.thoughtworks.go.plugin.configrepo.CRError_1;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.messages.ParseDirectoryMessage_1;
import com.thoughtworks.go.plugin.configrepo.messages.ParseDirectoryResponseMessage_1;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.List;

public class JsonMessageHandler1_0 implements JsonMessageHandler {

    private static final Logger LOGGER = Logger.getLogger(JsonMessageHandler1_0.class);

    private final GsonCodec codec;
    private final Migration_1 migration_1;

    public JsonMessageHandler1_0(){
        codec = new GsonCodec();
        migration_1 = new Migration_1();
    }

    @Override
    public String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage_1 requestMessage = prepareMessage_1(destinationFolder, configurations);
        return codec.getGson().toJson(requestMessage);
    }

    private ParseDirectoryMessage_1 prepareMessage_1(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage_1 requestMessage = new ParseDirectoryMessage_1(destinationFolder);
        for(CRConfigurationProperty conf : configurations)
        {
            requestMessage.addConfiguration(conf.getKey(),conf.getValue(),conf.getEncryptedValue());
        }
        return requestMessage;
    }

    @Override
    public CRParseResult responseMessageForParseDirectory(String responseBody) {
        ParseDirectoryResponseMessage_1 responseMessage_1 = deserializeResponse(responseBody);

        if(responseMessage_1.hasErrors())
        {
            LOGGER.warn("Configuration repository plugin has reported errors");
            return new CRParseResult(null,migrate(responseMessage_1.getErrors()));
        }

        CRPartialConfig_1 partialConfig_1 = responseMessage_1.getConfig();
        ErrorCollection errors = validatePartialConfig(partialConfig_1);
        if(!errors.isEmpty())
        {
            LOGGER.warn("Configuration repository plugin has returned invalid configuration");
            return new CRParseResult(null,migrate(responseMessage_1.getErrors()));
        }

        CRPartialConfig partialConfig;
        try{
            partialConfig = migrate(partialConfig_1);
            return new CRParseResult(partialConfig);
        }
        catch (Exception e) {
            String errorMessage = String.format("Failed to migrate json response v1.0 to current contract version. Error: %s.", e.getMessage());
            return new CRParseResult(null,errorMessage);
        }
    }

    private List<CRError> migrate(List<CRError_1> errors) {
        return migration_1.migrateErrors(errors);
    }

    private ParseDirectoryResponseMessage_1 deserializeResponse(String responseBody) {
        return codec.parseDirectoryResponseMessage_1FromJson(responseBody);
    }

    private ErrorCollection validatePartialConfig(CRPartialConfig_1 partialConfig_1) {
        ErrorCollection errors = new ErrorCollection();
        partialConfig_1.getErrors(errors);
        return errors;
    }

    private CRPartialConfig_1 deserializePartialConfig_1(String responseBody) {
        CRPartialConfig_1 partialConfig_1;
        try {
            partialConfig_1 = codec.partialConfig_1FromJson(responseBody);
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Unable to de-serialize json response. Error: %s.", e.getMessage()));
        }
        return partialConfig_1;
    }

    private CRPartialConfig migrate(CRPartialConfig_1 partialConfig_1) {
        return migration_1.migrate(partialConfig_1);
    }
}
