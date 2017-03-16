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


define(['mithril', 'helpers/form_helper', 'helpers/pipeline_configs/tooltips'],
  function (m, f, tt) {
    var MaterialFilterWidget = {
      view: function (_ctrl, args) {
        // TODO: make this an 'intelligent' text component that maps to an array.
        return (
          <div class='ignore-pattern'>
            <f.row>
              <f.inputWithLabel attrName='ignore'
                                label="Ignore pattern"
                                model={args.material.filter()}
                                size={12}
                                largeSize={12}
                                tooltip={{
                                  content:   tt.material.filter(args.material.invertFilter() ? 'included' : 'excluded'),
                                  direction: 'bottom',
                                  size:      'small'
                                }}/>
            </f.row>
            <f.row>
              <f.checkBox model={args.material}
                          attrName='invertFilter'
                          label='Invert the file filter, e.g. a Blacklist becomes a Whitelist instead.'
                          size={12}
                          largeSize={12}/>
            </f.row>
          </div>
        );
      }
    };

    return MaterialFilterWidget;
  });
