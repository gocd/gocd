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
  'mithril', 'helpers/form_helper', 'helpers/mithril_component_mixins'
], function (m, f, ComponentMixins) {
  var PropertiesConfigWidget = {
    controller: function (args) {
      this.args = args;
      ComponentMixins.HasViewModel.call(this);
      ComponentMixins.ManagesCollection.call(this, {
        as:           'Property',
        plural:       'Properties',
        onInitialize: function () {
          this.changed();
        }
      });
    },

    view: function (ctrl) {
      return (
        <f.accordion accordionTitles={[
                        (
          <span>Properties</span>
                        )
        ]}
                     accordionKeys={['properties-tabs']}
                     selectedIndex={ctrl.vmState('propertiesSelected', m.prop(-1))}
                     class="accordion-inner">
          <div class='job-properties'>
            {ctrl.map(function (property) {
              return (
                <f.row class='property' data-property-source={property.source()} key={property.uuid()}>
                  <f.input model={property}
                           attrName='name'
                           placeholder='Property Name'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={true}
                           size={3}
                           largeSize={3}/>
                  <f.input model={property}
                           attrName='source'
                           placeholder='The XML file source'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={'all'}
                           size={3}
                           largeSize={3}/>
                  <f.input model={property}
                           attrName='xpath'
                           placeholder='XPath'
                           onChange={ctrl.changed.bind(ctrl)}
                           validate={'all'}
                           size={3}
                           largeSize={3}/>
                  <f.column size={1} largeSize={3} end={true}>
                    {ctrl.removeLink.call(ctrl, property)}
                  </f.column>
                </f.row>
              );
            })}
          </div>
        </f.accordion>
      );
    }
  };
  return PropertiesConfigWidget;

});
