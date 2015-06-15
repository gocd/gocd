package com.thoughtworks.go.config;


import com.thoughtworks.go.config.remote.PartialConfig;

import java.io.File;

/**
 * Can obtain configuration objects from a source code tree.
 * Possible extension point for custom pipeline configuration format.
 * Expects a checked-out source code tree.
 * It does not understand versioning.
 * Each implementation defines its own pattern
 * to identify configuration files in repository structure.
 */
public interface PartialConfigProvider {

    // TODO consider: could have Parse() whose result is
    // stored by Go in memory so that single checkout is parsed only once.

    PartialConfig Load(File configRepoCheckoutDirectory, PartialConfigLoadContext context);

    // any further elements that could be obtained from config repo
}
