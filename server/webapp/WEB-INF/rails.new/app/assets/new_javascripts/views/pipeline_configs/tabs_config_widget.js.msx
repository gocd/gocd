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
  'mithril', 'helpers/form_helper', 'helpers/mithril_component_mixins', 'helpers/pipeline_configs/tooltips'
], function (m, f, ComponentMixins, tt) {
  var TabsConfigWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
      ComponentMixins.ManagesCollection.call(this, {
        as:           'Tab',
        onInitialize: function () {
          this.changed();
        }
      });
    },

    view: function (ctrl) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>Tabs<f.tooltip tooltip={{content: tt.tabs.main, size:'small'}}/></span>
                        )
        ]}
                     accordionKeys={['job-tabs']}
                     selectedIndex={ctrl.vmState('tabsSelected', m.prop(-1))}
                     class="accordion-inner">
          <div class='job-tabs position-tooltip'>
            {ctrl.map(function (tab) {
              return (
                <f.row class='tab' data-tab-name={tab.name()} key={tab.uuid()}>
                  <f.input model={tab}
                           attrName='name'
                           placeholder='Tab name'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={true}
                           size={3}
                           largeSize={3}
                           tooltip={{
                             content: tt.tabs.name,
                             direction: 'bottom',
                             size: 'small'
                           }}/>
                  <f.input model={tab}
                           attrName='path'
                           placeholder='Path of artifact'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={'all'}
                           size={3}
                           largeSize={3}
                           tooltip={{
                             content: tt.tabs.path,
                             direction: 'bottom',
                             size: 'medium'
                           }}/>
                  <f.column size={3} end={true}>
                    {ctrl.removeLink.call(ctrl, tab)}
                  </f.column>
                </f.row>
              );
            })}
          </div>
        </f.accordion>
      );
    }
  };

  return TabsConfigWidget;
});
