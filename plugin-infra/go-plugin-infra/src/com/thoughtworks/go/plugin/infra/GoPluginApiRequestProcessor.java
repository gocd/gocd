package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;

public interface GoPluginApiRequestProcessor {
    GoApiResponse process(GoApiRequest goPluginApiRequest);
}
