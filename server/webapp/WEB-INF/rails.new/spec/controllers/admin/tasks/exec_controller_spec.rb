##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'rails_helper'
require_relative 'task_controller_examples'

describe Admin::TasksController do
  include TaskMother
  include FormUI

  before do
    @example_task = exec_task_with_ant_oncancel_task
    @task_type = exec_task_with_ant_oncancel_task.getTaskType()
    @updated_payload = {:command => 'lsblah', :workingDirectory => exec_task_with_ant_oncancel_task.workingDirectory(), :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'ant', :antOnCancel => {:buildFile => "build.xml", :target => "compile", :workingDirectory => "default/wd"}}}
    @updated_task = exec_task_with_ant_oncancel_task("lsblah")

    @new_task = ExecTask.new

    @create_payload= {:command => 'ls', :workingDirectory => "hero/ka/directory", :argListString => "-la", :hasCancelTask => "1", :onCancelConfig=> { :onCancelOption => 'ant', :antOnCancel => {:buildFile => "build.xml", :target => "compile", :workingDirectory => "default/wd"}}}
    @created_task= exec_task_with_ant_oncancel_task
  end

  it_should_behave_like :task_controller

  def controller_specific_setup task_view_service
    allow(task_view_service).to receive(:taskInstanceFor).with("ant").and_return(ant_task)
  end
end
