/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  'mithril', 'lodash',
  'helpers/form_helper', 'helpers/pipeline_configs/tooltips', 'helpers/mithril_component_mixins',
  'models/pipeline_configs/tracking_tool'
], function (m, _,
             f, tt, ComponentMixins,
             TrackingTool) {

  var TrackingToolViews = {
    none: function () {
    },

    mingle: function (trackingTool) {
      return (
        <div>
          <f.row collapse>
            <f.inputWithLabel model={trackingTool()}
                              attrName='baseUrl'
                              label='Base URL'
                              type='url'
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content: <tt.trackingTool.mingle.baseUrl callback={trackingTool().baseUrl}/>,
                                direction: 'bottom',
                                size: 'medium'
                              }}
                              size={8}
                              largeSize={6}/>
          </f.row>

          <f.row collapse>
            <f.inputWithLabel model={trackingTool()}
                              attrName='projectIdentifier'
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content: <tt.trackingTool.mingle.projectIdentifier callback={trackingTool().projectIdentifier}/>,
                                direction: 'bottom',
                                size: 'medium'
                              }}
                              size={8}
                              largeSize={6}/>
          </f.row>

          <f.row collapse>
            <f.inputWithLabel model={trackingTool()}
                              attrName='mqlGroupingConditions'
                              validate={true}
                              tooltip={{
                                content: <tt.trackingTool.mingle.mqlGroupingConditions callback={trackingTool().mqlGroupingConditions}/>,
                                direction: 'bottom',
                                size: 'medium'
                              }}
                              size={8}
                              largeSize={6}/>
          </f.row>
        </div>
      );
    },

    generic: function (trackingTool) {
      return (
        <div>
          <f.row collapse>
            <f.inputWithLabel model={trackingTool()}
                              attrName='regex'
                              label='Regular expression'
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content: <tt.trackingTool.generic.regex callback={trackingTool().regex}/>,
                                direction: 'bottom',
                                size: 'large'
                              }}
                              size={8}
                              largeSize={6}/>
          </f.row>
          <f.row collapse>
            <f.inputWithLabel model={trackingTool()}
                              attrName='urlPattern'
                              label='URL Pattern'
                              type='url'
                              validate={true}
                              isRequired={true}
                              tooltip={{
                                content: <tt.trackingTool.generic.urlPattern callback={trackingTool().urlPattern}/>,
                                direction: 'bottom',
                                size: 'large'
                              }}
                              size={8}
                              largeSize={6}/>
          </f.row>
        </div>
      );
    }
  };

  var TrackingToolWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
      this.vmState('possibleTrackingTools', {
        none:    null,
        mingle:  (args.trackingTool() && args.trackingTool().type() === 'mingle') ? args.trackingTool() : TrackingTool.create('mingle'),
        generic: (args.trackingTool() && args.trackingTool().type() === 'generic') ? args.trackingTool() : TrackingTool.create('generic')
      });
    },

    view: function (ctrl, args) {
      var isChecked = function (type) {
        if (type === 'none' && !args.trackingTool()) {
          return true;
        }

        return args.trackingTool() && args.trackingTool().type() === type;
      };

      var trackingTools = _.merge({none: {type: undefined, description: "None"}}, TrackingTool.Types);

      return (
        <f.accordion accordionTitles={[
                        (
          <span>Tracking Tool<f.tooltip tooltip={{content: tt.trackingTool.main}}/></span>
                        )
        ]}
                     accordionKeys={['tracking-tool']}
                     selectedIndex={ctrl.vmState('trackingToolSelected', m.prop(-1))}
                     class="tracking-tool accordion-inner">
          <div>
            <f.row>
              <f.column size={4} end={true}>
                {_.map(trackingTools, function (value, key) {
                  return (
                    <span>
                    <input type="radio"
                           name='tracking-tool-button-group'
                           id={'tracking-tool-' + key}
                           checked={isChecked(key)}
                           onclick={args.trackingTool.bind(args, ctrl.vmState('possibleTrackingTools')[key])}/>
                    <label for={'tracking-tool-' + key}> {value.description}</label>
                  </span>
                  );
                })}
              </f.column>
            </f.row>

            <f.row class={'tracking-tool tracking-tool-' + (args.trackingTool() ? args.trackingTool().type() : 'none')}>
              <f.column size={6} end={true}>
                {TrackingToolViews[args.trackingTool() ? args.trackingTool().type() : 'none'](args.trackingTool)}
              </f.column>
            </f.row>
          </div>
        </f.accordion>
      );
    }
  };

  return TrackingToolWidget;
});
