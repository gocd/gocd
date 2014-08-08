package com.thoughtworks.go.plugin.infra;


import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultGoApplicationAccessor extends GoApplicationAccessor {

    private Map<String, GoPluginApiRequestProcessor> processorMap = new HashMap<String, GoPluginApiRequestProcessor>();

    public void registerProcessorFor(String request, GoPluginApiRequestProcessor processor) {
        processorMap.put(request, processor);
    }

    @Override
    public GoApiResponse submit(final GoApiRequest request) {
        if (processorMap.containsKey(request.api())) {
            return processorMap.get(request.api()).process(request);
        }
        return unhandledApiRequest();
    }

    private DefaultGoApiResponse unhandledApiRequest() {
        return new DefaultGoApiResponse(401);
    }
}

