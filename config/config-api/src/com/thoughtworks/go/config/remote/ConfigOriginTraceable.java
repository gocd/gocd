package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.CruiseConfig;

/**
 * Configuration whose origin can be identified.
 */
public interface ConfigOriginTraceable {
    ConfigOrigin getOrigin();

    /**
     * Sets origin in this element and all children recursively.
     */
    void setOrigins(ConfigOrigin origins);
}
