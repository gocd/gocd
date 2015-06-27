package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.messaging.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands messages about required material updates
 */
@Component
public class ConfigMaterialUpdateQueue extends GoMessageQueue<MaterialUpdateMessage> {
    @Autowired
    public ConfigMaterialUpdateQueue(MessagingService messaging) {
        super(messaging, "config-material-update-required");
    }
}