package com.thoughtworks.go.config.exceptions;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

public class GoConfigInvalidMergeException extends GoConfigInvalidException {
    private List<PartialConfig> partialConfigs;

    public GoConfigInvalidMergeException(String summary,CruiseConfig cruiseConfig,
                                         List<PartialConfig> partialConfigs, List<ConfigErrors> allErrors) {
        super(cruiseConfig,allErrorsToString(allErrors, summary));
        this.partialConfigs = partialConfigs;
    }

    public GoConfigInvalidMergeException(String summary,CruiseConfig cruiseConfig,
                                         List<PartialConfig> partialConfigs, List<ConfigErrors> allErrors,Throwable e) {
        super(cruiseConfig,allErrorsToString(allErrors, summary),e);
        this.partialConfigs = partialConfigs;
    }

    public GoConfigInvalidMergeException(String summary,List<PartialConfig> partials, GoConfigInvalidException failed) {
        this(summary,failed.getCruiseConfig(), partials, failed.getCruiseConfig().getAllErrors(),failed);
    }

    private static String allErrorsToString(List<ConfigErrors> allErrors, String summary) {
        if(allErrors == null || allErrors.size() == 0)
            return "Error list empty";// should never be
        StringBuilder b = new StringBuilder();
        b.append(allErrors.size()).append("+ errors :: ");
        for(ConfigErrors e : allErrors)
        {
            b.append(e.firstError()).append(";; ");
        }
        return b.toString();
    }

    public List<PartialConfig> getPartialConfigs() {
        return partialConfigs;
    }
}