package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfiguration;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.plugin.access.configrepo.migration.Migration_1;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;
import com.thoughtworks.go.plugin.configrepo.ErrorCollection;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.messages.ParseDirectoryMessage_1;

import java.util.Collection;

public class JsonMessageHandler1_0 implements JsonMessageHandler {

    private final GsonCodec codec;
    private final Migration_1 migration_1;

    public JsonMessageHandler1_0(){
        codec = new GsonCodec();
        migration_1 = new Migration_1();
    }

    @Override
    public String requestMessageForParseDirectory(String destinationFolder, Collection<CRConfiguration> configurations) {
        ParseDirectoryMessage_1 requestMessage = prepareMessage_1(destinationFolder, configurations);
        return codec.getGson().toJson(requestMessage);
    }

    private ParseDirectoryMessage_1 prepareMessage_1(String destinationFolder, Collection<CRConfiguration> configurations) {
        ParseDirectoryMessage_1 requestMessage = new ParseDirectoryMessage_1(destinationFolder);
        for(CRConfiguration conf : configurations)
        {
            requestMessage.addConfiguration(conf.getKey(),conf.getValue(),conf.getEncryptedValue());
        }
        return requestMessage;
    }

    @Override
    public CRPartialConfig responseMessageForParseDirectory(String responseBody) {
        CRPartialConfig_1 partialConfig_1 = deserializePartialConfig_1(responseBody);
        validatePartialConfig(partialConfig_1);

        CRPartialConfig partialConfig;
        try{
            partialConfig = migrate(partialConfig_1);
            return partialConfig;
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Unable to migrate json response v1.0 to current contract version. Error: %s.", e.getMessage()));
        }
    }

    private void validatePartialConfig(CRPartialConfig_1 partialConfig_1) {
        ErrorCollection errors = new ErrorCollection();
        partialConfig_1.getErrors(errors);
        if(!errors.isEmpty())
            throw new InvalidPartialConfigException(partialConfig_1,errors);
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
