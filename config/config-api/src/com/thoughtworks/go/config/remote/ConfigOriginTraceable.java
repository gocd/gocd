package com.thoughtworks.go.config.remote;

/**
 * Configuration whose origin can be identified.
 */
public interface ConfigOriginTraceable {
    ConfigOrigin getOrigin();
}
