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

  before :each do
    @example_task = ant_task
    @task_type = ant_task.getTaskType()
    @updated_payload = {:buildFile => "newB", :target => "newT", :workingDirectory => "newWD"}
    @updated_task = ant_task("newB", "newT", "newWD")

    @new_task = AntTask.new
    @create_payload= {:buildFile => 'build.xml', :target => "compile",:workingDirectory => "dir"}
    @created_task= ant_task("build.xml", "compile", "dir")
  end

  it_should_behave_like :task_controller

end
