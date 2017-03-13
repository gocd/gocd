/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

let m                                = require('mithril');
let f                                = require('helpers/form_helper');
let tt                               = require('helpers/pipeline_configs/tooltips');
let ParametersConfigWidget           = require('views/pipeline_configs/parameters_config_widget');
let TrackingToolWidget               = require('views/pipeline_configs/tracking_tool_widget');
let EnvironmentVariablesConfigWidget = require('views/pipeline_configs/environment_variables_config_widget');

const ComponentMixins = require('helpers/mithril_component_mixins');
const Stream          = require('mithril/stream');
const s               = require('string-plus');
const _               = require('lodash');


const PipelineSettingsWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {
    const pipeline                    = vnode.attrs.pipeline();
    const pipelineAutoScheduleMessage = pipeline.isFirstStageAutoTriggered() ? 'Automatically triggered' : 'Manually triggered';

    return (
      <f.accordion accordionTitles={[(<span>Pipeline Settings</span>)]}
                   accordionKeys={['pipeline-settings']}
                   selectedIndex={vnode.state.vmState('pipelineSettingsSelected', Stream(-1))}
                   class='pipeline-settings'>
        <div>
          <f.row>
            <f.inputWithLabel model={pipeline}
                              attrName='labelTemplate'
                              validate={true}
                              isRequired={true}
                              size={4}
                              tooltip={{
                                content:   <tt.pipeline.labelTemplate callback={pipeline.labelTemplate}/>,
                                direction: 'bottom',
                                size:      'large'
                              }}/>
            <f.checkBox model={pipeline}
                        attrName='enablePipelineLocking'
                        addPadding={true}
                        size={4}
                        largeSize={3}
                        tooltip={{
                          content:   tt.pipeline.enablePipelineLocking,
                          direction: 'bottom',
                          class:     'tooltip-spacing'
                        }}/>

            <f.column size={4} largeSize={5} end={true} class="pipeline-schedule">
              <label>{pipelineAutoScheduleMessage}</label>
              <f.tooltip tooltip={{
                content:   pipeline.isFirstStageAutoTriggered() ? tt.pipeline.automaticPipelineScheduling : tt.pipeline.manualPipelineScheduling,
                direction: 'bottom',
                size:      'small'
              }}/>
            </f.column>
          </f.row>
          <f.row>
            <f.inputWithLabel model={pipeline.timer()}
                              attrName='spec'
                              label='Cron timer specification'
                              validate={true}
                              tooltip={{
                                content:   <tt.pipeline.timer.spec callback={pipeline.timer().spec}/>,
                                direction: 'bottom',
                                size:      'large'
                              }}/>

            <f.checkBox model={pipeline.timer()}
                        class="end"
                        addPadding={true}
                        attrName='onlyOnChanges'
                        label='Run only on new material'
                        disabled={s.isBlank(pipeline.timer().spec())}
                        tooltip={{
                          content:   tt.pipeline.timer.onlyOnChanges,
                          direction: 'bottom',
                          class:     'tooltip-spacing'
                        }}/>

          </f.row>
          <f.row>
            <TrackingToolWidget trackingTool={pipeline.trackingTool}
                                vm={vnode.state.vmState('trackingToolConfig')}
                                key={_.result(pipeline.trackingTool(), 'uuid', 'tracking-tool-none')}/>
            <ParametersConfigWidget parameters={pipeline.parameters}
                                    key={pipeline.parameters().uuid()}
                                    vm={vnode.state.vmState('parametersConfig')}/>
            <EnvironmentVariablesConfigWidget title='Pipeline Environment Variables'
                                              variables={pipeline.environmentVariables}
                                              key={pipeline.environmentVariables().uuid()}
                                              vm={vnode.state.vmState('environmentVariablesConfig')}/>
          </f.row>
        </div>
      </f.accordion>
    );
  }
};
module.exports               = PipelineSettingsWidget;
