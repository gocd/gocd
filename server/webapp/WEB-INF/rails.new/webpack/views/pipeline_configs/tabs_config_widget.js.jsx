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

const TabsConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
    ComponentMixins.ManagesCollection.call(this, {
      as: 'Tab',
      onInitialize () {
        this.changed();
      }
    });
  },

  view (vnode) {
    return (
      <f.accordion accordionTitles={[(<span>Tabs<f.tooltip tooltip={{content: tt.tabs.main, size: 'small'}}/></span>)]}
                   accordionKeys={['job-tabs']}
                   selectedIndex={vnode.state.vmState('tabsSelected', Stream(-1))}
                   class="accordion-inner">
        <div class='job-tabs position-tooltip'>
          {vnode.state.map((tab) => {
            return (
              <f.row class='tab' data-tab-name={tab.name()} key={tab.uuid()}>
                <f.input model={tab}
                         attrName='name'
                         placeholder='Tab name'
                         onChange={vnode.state.changed.bind(vnode.state)}
                         validate={true}
                         size={3}
                         largeSize={3}
                         tooltip={{
                           content:   tt.tabs.name,
                           direction: 'bottom',
                           size:      'small'
                         }}/>
                <f.input model={tab}
                         attrName='path'
                         placeholder='Path of artifact'
                         onChange={vnode.state.changed.bind(vnode.state)}
                         validate={'all'}
                         size={3}
                         largeSize={3}
                         tooltip={{
                           content:   tt.tabs.path,
                           direction: 'bottom',
                           size:      'medium'
                         }}/>
                <f.column size={3} end={true}>
                  {vnode.state.removeLink.call(vnode.state, tab)}
                </f.column>
              </f.row>
            );
          })}
        </div>
      </f.accordion>
    );
  }
};

module.exports = TabsConfigWidget;
