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
  'mithril',
  'helpers/form_helper', 'helpers/mithril_component_mixins',
  'helpers/pipeline_configs/tooltips'
], function (m,
             f, ComponentMixins, tt) {

  var ParametersConfigWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
      ComponentMixins.ManagesCollection.call(this, {
        as:           'Parameter',
        onInitialize: function () {
          this.changed();
        }
      });
    },

    view: function (ctrl) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>Parameters<f.tooltip tooltip={{content: tt.pipeline.parameters.main}}/></span>
                        )
        ]}
                     accordionKeys={['parameters']}
                     selectedIndex={ctrl.vmState('parametersSelected', m.prop(-1))}
                     class='parameters accordion-inner'>
          <div>
            {ctrl.map(function (parameter) {
              return (
                <f.row class='parameter' data-parameter-name={parameter.name()} key={parameter.uuid()}>
                  <f.input model={parameter}
                           attrName='name'
                           placeholder='Name'
                           validate={true}
                           onChange={ctrl.changed.bind(ctrl)}/>
                  <f.input model={parameter}
                           attrName='value'
                           placeholder='Value'
                           onChange={ctrl.changed.bind(ctrl)}/>
                  <f.column size={1} end={true}>
                    {ctrl.removeLink.call(ctrl, parameter)}
                  </f.column>
                </f.row>
              );
            })}
          </div>
        </f.accordion>
      );
    }
  };

  return ParametersConfigWidget;
});
