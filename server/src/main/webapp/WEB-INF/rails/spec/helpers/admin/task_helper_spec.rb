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

describe Admin::TaskHelper do
  include Admin::TaskHelper

  def task_view_service
    @task_view_service ||= double("task_view_service")
  end

  it "should get css class names for each task" do
    expect(task_css_class("exec")).to eq("lookup_icon")
    expect(task_css_class("fetch")).to eq("")
    expect(task_css_class("ant")).to eq("")
    expect(task_css_class("nant")).to eq("")
    expect(task_css_class("rake")).to eq("")
  end

  it "should get all task options" do
    expect(task_view_service).to receive(:getTaskViewModels).and_return([tvm_of(ExecTask.new("ls", "-la", "Hello")), tvm_of(AntTask.new), tvm_of(NantTask.new), tvm_of(RakeTask.new)])

    result = task_options
    expect(result).to eq([["Ant", "ant"], ["NAnt", "nant"], ["Rake", "rake"], ["More...", "exec"]])
  end

  def tvm_of task
    TaskViewModel.new task, ""
  end
end
