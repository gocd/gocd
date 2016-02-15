package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.BaseCollection;

@ConfigTag("pipelines")
@ConfigCollection(value = PipelineConfig.class)
public class PipelineConfigsPartials extends BaseCollection<PipelineConfigs>
{
}
