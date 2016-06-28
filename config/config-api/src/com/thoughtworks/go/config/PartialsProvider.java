package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;

import java.util.List;

public interface PartialsProvider {
    List<PartialConfig> lastPartials();
}
