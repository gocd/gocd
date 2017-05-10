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

define(['mithril', 'helpers/form_helper', 'helpers/mithril_component_mixins', 'helpers/pipeline_configs/tooltips'], function (m, f, ComponentMixins, tt) {
  var ArtifactsConfigWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
      ComponentMixins.ManagesCollection.call(this, {
        as:           'Artifact',
        onInitialize: function () {
          this.changed();
        }
      });
    },

    view: function (ctrl) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>Artifacts<f.tooltip tooltip={{content: tt.artifacts.main, size:'small'}}/></span>
                        )
        ]}
                     accordionKeys={['job-artifacts']}
                     selectedIndex={ctrl.vmState('artifactsSelected', m.prop(-1))}
                     class="accordion-inner">
          <div class='job-artifacts position-tooltip'>
            {ctrl.map(function (artifact) {
              return (
                <f.row class='artifact' data-artifact-source={artifact.source()} key={artifact.uuid()}>
                  <f.input model={artifact}
                           attrName='source'
                           placeholder='Source'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={true}
                           size={3}
                           largeSize={3}
                           tooltip={{
                             content: tt.artifacts.source,
                             direction: 'bottom',
                             size: 'medium'
                           }}/>
                  <f.input model={artifact}
                           attrName='destination'
                           placeholder='Destination'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={'all'}
                           size={3}
                           largeSize={3}
                           tooltip={{
                             content: tt.artifacts.destination,
                             direction: 'bottom',
                             size: 'medium'
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
                              content: tt.artifacts.type,
                              direction: 'bottom',
                              size: 'medium'
                            }}/>
                  <f.column size={1} end={true}>
                    {ctrl.removeLink.call(ctrl, artifact)}
                  </f.column>
                </f.row>
              );
            })}
          </div>
        </f.accordion>
      );
    }
  };
  return ArtifactsConfigWidget;

});
