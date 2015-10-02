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

describe Admin::TaskHelper do
  include Admin::TaskHelper

  it "should get css class names for each task" do
    task_css_class("exec").should == "lookup_icon"
    task_css_class("fetch").should == ""
    task_css_class("ant").should == ""
    task_css_class("nant").should == ""
    task_css_class("rake").should == ""
  end

  it "should get all task options" do
    mock_task_view_service = double("task_view_service")
    allow(self).to receive(:task_view_service).and_return(mock_task_view_service)
    mock_task_view_service.should_receive(:getTaskViewModels).and_return([tvm_of(ExecTask.new("ls", "-la", "Hello")), tvm_of(AntTask.new), tvm_of(NantTask.new), tvm_of(RakeTask.new)])

    result = task_options
    result.should == [["Ant", "ant"], ["NAnt", "nant"], ["Rake", "rake"], ["More...", "exec"]]
  end

  def tvm_of task
    TaskViewModel.new task, "", ""
  end
end
