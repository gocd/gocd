package com.thoughtworks.go.plugin.api;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;

public abstract class GoApplicationAccessor {
    public abstract GoApiResponse submit(GoApiRequest request);
}

