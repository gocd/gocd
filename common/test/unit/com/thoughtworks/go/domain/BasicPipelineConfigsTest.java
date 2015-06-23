package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigsBaseTest;

/**
 * Created by tomzo on 6/23/15.
 */
public class BasicPipelineConfigsTest extends PipelineConfigsBaseTest {

    @Override
    protected BasicPipelineConfigs createWithPipeline(PipelineConfig pipelineConfig) {
        return new BasicPipelineConfigs(pipelineConfig);
    }

    @Override
    protected BasicPipelineConfigs createEmpty() {
        return new BasicPipelineConfigs();
    }
}
