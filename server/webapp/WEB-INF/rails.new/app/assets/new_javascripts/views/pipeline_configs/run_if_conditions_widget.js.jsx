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
  'mithril', 'helpers/form_helper'
], function (m, f) {
  var RunIfConditionsWidget = {
    view: function (_ctrl, args) {
      var task = args.task;
      return (
          <div class='runif'>
            <f.row>
              <f.column>
                <label>Run If Current Job Status
                  <span class='asterisk'>*</span>
                </label>
              </f.column>
            </f.row>
            <f.row>
              <f.multiSelectionBox value='passed'
                                   attrName='runIf'
                                   model={task}
                                   label='Passed'
                                   size={2}/>
              <f.multiSelectionBox value='failed'
                                   attrName='runIf'
                                   model={task}
                                   label='Failed'
                                   size={2}/>
              <f.multiSelectionBox value='any'
                                   attrName='runIf'
                                   model={task}
                                   label='Any'
                                   size={2}
                                   end={true}/>
            </f.row>
          </div>
      );
    }
  };
  return RunIfConditionsWidget;
});
