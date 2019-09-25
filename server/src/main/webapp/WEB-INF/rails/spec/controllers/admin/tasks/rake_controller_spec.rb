#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'
require_relative 'task_controller_examples'

describe Admin::TasksController do
  include TaskMother
  include FormUI

  before do
    @example_task = rake_task
    @task_type = rake_task.getTaskType()
    @updated_payload = {:buildFile => "newB", :target => "newT", :workingDirectory => "newWD"}
    @updated_task = rake_task("newB", "newT", "newWD")
    @subject = @updated_task
    @new_task = RakeTask.new

    @create_payload= {:buildFile => 'rakefile', :target => "test",:workingDirectory => "dir"}
    @created_task= rake_task("rakefile", "test", "dir")
  end

  it_should_behave_like :task_controller

  def controller_specific_setup task_view_service
    allow(task_view_service).to receive(:taskInstanceFor).with("rake").and_return(@example_task)
  end
end
