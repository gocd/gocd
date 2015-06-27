package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.server.messaging.GoMessageTopic;
import com.thoughtworks.go.server.messaging.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands when a config material update has completed
 */
@Component
public class ConfigMaterialUpdateCompletedTopic extends GoMessageTopic<MaterialUpdateCompletedMessage> {

    @Autowired
    public ConfigMaterialUpdateCompletedTopic(MessagingService messaging) {
        super(messaging, "config-material-update-completed");
    }
}