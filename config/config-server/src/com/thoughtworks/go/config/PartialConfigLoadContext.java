package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.Configuration;

/**
 * Created by tomzo on 6/15/15.
 */
public interface PartialConfigLoadContext {
    Configuration configuration();
}
