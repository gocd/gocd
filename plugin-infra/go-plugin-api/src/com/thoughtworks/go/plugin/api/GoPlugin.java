package com.thoughtworks.go.plugin.api;

import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

@GoPluginApiMarker
public interface GoPlugin {

    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor);

    public GoPluginApiResponse handle(GoPluginApiRequest requestMessage);

    public GoPluginIdentifier pluginIdentifier();

}



