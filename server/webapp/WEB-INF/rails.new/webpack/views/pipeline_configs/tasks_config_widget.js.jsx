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
const _               = require('lodash');
const $               = require('jquery');
const dragulaConfig   = require('helpers/dragula_config');
const Tasks           = require('models/pipeline_configs/tasks');
const PluggableTasks  = require('models/pipeline_configs/pluggable_tasks');
const ComponentMixins = require('helpers/mithril_component_mixins');
const TaskViews       = require('views/pipeline_configs/task_views_widget');
const PluginInfos     = require('models/pipeline_configs/plugin_infos');

let TaskTypeSelector = {
  oninit (vnode) {
    const self    = this;
    self.tasks    = vnode.attrs.tasks;
    self.selected = Stream('exec');
    self.addTask  = function (type) {
      if (Tasks.isBuiltInTaskType(type())) {
        self.tasks().createTask({type: type()});
      } else {
        self.tasks().createTask({type: type(), pluginInfo: PluginInfos.findById(type())});
      }
    };
  },

  view (vnode) {
    const items = _.reduce(_.merge({}, Tasks.Types, PluggableTasks.Types), (accumulator, value, key) => {
      accumulator.push({id: key, text: value.description});
      return accumulator;
    }, []);

    return (
      <f.row class='task-selector'>
        <f.select model={vnode.state}
                  attrName='selected'
                  class='inline'
                  label='Add task of type'
                  items={items}
                  size={3}/>
        <f.column size={2} end={true}>
          <a class='button add-button' href="javascript:void(0)"
             onclick={vnode.state.addTask.bind(vnode.state, vnode.state.selected)}>Add</a>
        </f.column>
      </f.row>
    );
  }
};

const TasksConfigWidget = {
  oninit (vnode) {
    this.args = vnode.attrs;
    ComponentMixins.HasViewModel.call(this);
  },

  view (vnode) {

    const dragDropConfig = function (elem, isInitialized) {
      if (!isInitialized) {
        const onDrop = function () {
          const reorderedTasks = _.map($(elem).find('.task-definition:not(.gu-mirror)'), (eachTaskElem) => {
            return vnode.attrs.tasks().taskAtIndex($(eachTaskElem).attr('data-task-index'));
          });
          vnode.attrs.tasks().setTasks(reorderedTasks);
        };

        const disableDrag = function (_el, h) {
          return !($('.task-summary').has(h).length > 0);
        };

        const options = {
          dragulaOptions: {
            invalid: disableDrag
          },
          onDropCallback: onDrop
        };

        dragulaConfig(elem, options);
      }
    };

    return (
      <f.accordion accordionTitles={[(<span>Tasks</span>)]}
                   accordionKeys={['job-tasks']}
                   selectedIndex={vnode.state.vmState('tasksSelected', Stream(0))}
                   class="accordion-inner">
        <div class='task-definitions' key="task-definitions">
          <f.row class='task-header'>
            <f.column size={2} largeSize={2}><span>Task Type</span></f.column>
            <f.column size={6} largeSize={6}><span>Properties</span></f.column>
            <f.column size={2} largeSize={2}><span>Run If</span></f.column>
            <f.column size={2} largeSize={2}><span>On Cancel</span></f.column>
          </f.row>

          <span config={dragDropConfig}>
          {vnode.attrs.tasks().mapTasks((task, taskIndex) => {
            const taskView = TaskViews[task.type()];
            return (m(taskView, {
              task,
              onRemove: vnode.attrs.tasks().removeTask.bind(vnode.attrs.tasks(), task),
              taskIndex,
              key:      task.uuid(),
              vm:       vnode.state.vmState(`task-${  taskIndex}`)
            }));
          })}
        </span>
          <TaskTypeSelector tasks={vnode.attrs.tasks} key="task-type-selector"/>
        </div>
      </f.accordion>
    );
  }
};

module.exports = TasksConfigWidget;
