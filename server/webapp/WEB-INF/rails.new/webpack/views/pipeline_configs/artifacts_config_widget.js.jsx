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

const ArtifactsConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
    ComponentMixins.ManagesCollection.call(this, {
      as: 'Artifact',
      onInitialize () {
        this.changed();
      }
    });
  },

  view (vnode) {
    return (
      <f.accordion accordionTitles={[(<span>Artifacts<f.tooltip tooltip={{content: tt.artifacts.main, size: 'small'}}/></span>)]}
                   accordionKeys={['job-artifacts']}
                   selectedIndex={vnode.state.vmState('artifactsSelected', Stream(-1))}
                   class="accordion-inner">
        <div class='job-artifacts position-tooltip'>
          {vnode.state.map((artifact) => {
            return (
              <f.row class='artifact' data-artifact-source={artifact.source()} key={artifact.uuid()}>
                <f.input model={artifact}
                         attrName='source'
                         placeholder='Source'
                         onChange={vnode.state.changed.bind(vnode.state)}
                         validate={true}
                         size={3}
                         largeSize={3}
                         tooltip={{
                           content:   tt.artifacts.source,
                           direction: 'bottom',
                           size:      'medium'
                         }}/>
                <f.input model={artifact}
                         attrName='destination'
                         placeholder='Destination'
                         onChange={vnode.state.changed.bind(vnode.state)}
                         validate={'all'}
                         size={3}
                         largeSize={3}
                         tooltip={{
                           content:   tt.artifacts.destination,
                           direction: 'bottom',
                           size:      'medium'
                         }}/>
                <f.select model={artifact}
                          attrName='type'
                          class='inline'
                          items={[
                            {
                              id: 'build', text: 'build'
                            }, {
                              id: 'test', text: 'test'
                            }
                          ]}
                          size={2}
                          tooltip={{
                            content:   tt.artifacts.type,
                            direction: 'bottom',
                            size:      'medium'
                          }}/>
                <f.column size={1} end={true}>
                  {vnode.state.removeLink.call(vnode.state, artifact)}
                </f.column>
              </f.row>
            );
          })}
        </div>
      </f.accordion>
    );
  }
};

module.exports = ArtifactsConfigWidget;
