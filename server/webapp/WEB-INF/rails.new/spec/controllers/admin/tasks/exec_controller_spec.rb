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

require 'spec_helper'
load File.join(File.dirname(__FILE__), 'task_controller_examples.rb')

describe Admin::TasksController do
  include TaskMother
  include FormUI

  before do
    @example_task = exec_task
    @task_type = exec_task.getTaskType()
    @updated_payload = {:command => 'lsblah', :workingDirectory => exec_task.workingDirectory()}
    @updated_task = exec_task("lsblah")

    @new_task = ExecTask.new

    @create_payload= {:command => 'ls', :workingDirectory => "hero/ka/directory", :argListString => "-la"}
    @created_task= exec_task
  end

  it_should_behave_like :task_controller

end
