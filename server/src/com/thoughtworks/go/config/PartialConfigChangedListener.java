package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;

import java.util.List;

/**
 * Created by tomzo on 6/17/15.
 */
public interface PartialConfigChangedListener {
    void onPartialConfigChanged(PartialConfig[] partials);
}
