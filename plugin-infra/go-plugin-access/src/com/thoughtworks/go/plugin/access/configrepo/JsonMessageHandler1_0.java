package com.thoughtworks.go.plugin.access.configrepo;


import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.access.configrepo.contract.*;
import com.thoughtworks.go.plugin.access.configrepo.messages.*;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {

    private static final Logger LOGGER = Logger.getLogger(JsonMessageHandler1_0.class);
    private static final int CURRENT_CONTRACT_VERSION = 1;

    private final GsonCodec codec;

    public JsonMessageHandler1_0(){
        codec = new GsonCodec();
    }

    @Override
    public String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage requestMessage = prepareMessage_1(destinationFolder, configurations);
        return codec.getGson().toJson(requestMessage);
    }

    private ParseDirectoryMessage prepareMessage_1(String destinationFolder, Collection<CRConfigurationProperty> configurations) {
        ParseDirectoryMessage requestMessage = new ParseDirectoryMessage(destinationFolder);
        for(CRConfigurationProperty conf : configurations)
        {
            requestMessage.addConfiguration(conf.getKey(),conf.getValue(),conf.getEncryptedValue());
        }
        return requestMessage;
    }
    private Map parseResponseToMap(String responseBody) {
        return (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    @Override
    public CRParseResult responseMessageForParseDirectory(String responseBody) {
        Map responseMap;
        try {
            responseMap = parseResponseToMap(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Parse directory result should be returned as map");
        }
        if (responseMap == null || responseMap.isEmpty()) {
            throw new RuntimeException("Empty response body");
        }

        if (responseMap.containsKey("target_version") && responseMap.get("target_version") != null) {
            Object targetVersion = responseMap.get("target_version");

            if (!(targetVersion instanceof Integer)) {
                throw new RuntimeException("Parse directory result 'target_version' should be an integer");
            }

            int version = (int) targetVersion;

            while(version < CURRENT_CONTRACT_VERSION)
            {
                migrate(responseBody,version);
                version++;
            }
        }

        // after migration, json should match contract
        ParseDirectoryResponseMessage parseDirectoryResponseMessage = codec.getGson().fromJson(responseBody,ParseDirectoryResponseMessage.class);

        ErrorCollection errors = new ErrorCollection();
        parseDirectoryResponseMessage.validateResponse(errors);
        throw  new RuntimeException("not implemented");
        //ParseDirectoryResponseMessage responseMessage_1 = deserializeResponse(responseBody);

        // here we create detailed message about all errors in configuration repository
        //StringBuilder errorsBuilder = new StringBuilder();

        /*
        if(responseMessage_1.hasErrors())
        {
            // These errors are defined by configuration plugin.
            // Plugin developer is fully responsible for those.

            errorsBuilder.append("Configuration repository plugin has reported errors:");
            for(CRError_1 error : responseMessage_1.getErrors())
            {
                errorsBuilder.append('\n');
                errorsBuilder.append('\t');// new line and ident on each error
                if(error.getLocation() != null)
                    errorsBuilder.append("At ").append(error.getLocation()).append(" - ");
                errorsBuilder.append(error.getMessage());
            }
            String fullErrorMessage = errorsBuilder.toString();
            LOGGER.warn(fullErrorMessage);
        }*/

        // continue looking for errors

        /*
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
            for(CRPipelineGroup group : partialConfig.getGroups()) {
                for (CRPipeline crPipeline : group.getPipelines()) {
                    MissingConfigLinkedNode missingValues = partialConfig.validateRequired(MissingConfigLinkedNode.first());

                    do {
                        errors.add(crPipeline,String.format("Pipeline %s is missing required config"));
                    }
                    while (!missingValues.isFirst());

                }
            }
        }
        catch (MissingConfigValue e) {
            String errorMessage = String.format(
                    "Plugin response did not contain all required configuration values. Missing value: %s; %s",
                    e.getPropertyName(), e.getMessage());
            return new CRParseResult(null,errorMessage);
        }
        catch (Exception e) {
            String errorMessage = String.format("Failed to migrate plugin json response v1.0 to current contract version. Error: %s.", e.getMessage());
            return new CRParseResult(null,errorMessage);
        }*/
    }

    private void migrate(String responseBody, int version) {

    }

    /*
    private List<CRError> migrate(List<CRError_1> errors) {
        return migration_1.migrateErrors(errors);
    }

    private ParseDirectoryResponseMessage deserializeResponse(String responseBody) {
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
    }*/
}
