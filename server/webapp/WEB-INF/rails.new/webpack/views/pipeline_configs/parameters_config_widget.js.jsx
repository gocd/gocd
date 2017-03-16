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

let m = require('mithril');
let f = require('helpers/form_helper');

const Stream          = require('mithril/stream');
const ComponentMixins = require('helpers/mithril_component_mixins');
const tt              = require('helpers/pipeline_configs/tooltips');

const ParametersConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
    ComponentMixins.ManagesCollection.call(this, {
      as: 'Parameter',
      onInitialize () {
        this.changed();
      }
    });
  },

  view (vnode) {
    return (
      <f.accordion accordionTitles={[(<span>
        Parameters
        <f.tooltip tooltip={{content: tt.pipeline.parameters.main}}/>
      </span>)]}
                   accordionKeys={['parameters']}
                   selectedIndex={vnode.state.vmState('parametersSelected', Stream(-1))}
                   class='parameters accordion-inner'>
        <div>
          {vnode.state.map((parameter) => {
            return (
              <f.row class='parameter' data-parameter-name={parameter.name()} key={parameter.uuid()}>
                <f.input model={parameter}
                         attrName='name'
                         placeholder='Name'
                         validate={true}
                         onChange={vnode.state.changed.bind(vnode.state)}/>
                <f.input model={parameter}
                         attrName='value'
                         placeholder='Value'
                         onChange={vnode.state.changed.bind(vnode.state)}/>
                <f.column size={1} end={true}>
                  {vnode.state.removeLink.call(vnode.state, parameter)}
                </f.column>
              </f.row>
            );
          })}
        </div>
      </f.accordion>
    );
  }
};

module.exports = ParametersConfigWidget;
