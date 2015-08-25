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

describe "/admin/tasks/plugin/_task_entry.html.erb" do

  before(:each) do
    @tvm = double("tvm")
    @tvm_of_cancel_task = double("tvm for cancel")
    @task_config_index = 1
    @task_config = double("config")
    view.stub(:admin_task_decrement_index_path).and_return('admin_task_decrement_index_path')
    view.stub(:admin_task_increment_index_path).and_return('admin_task_increment_index_path')
    view.stub(:admin_task_edit_path).and_return('admin_task_edit_path')
    view.stub(:admin_task_delete_path).and_return('admin_task_delete_path')
    view.stub(:md5_field).and_return('md5')
    assign(:tasks, [])
  end

  it 'should display plugin name in the header' do
    @tvm.should_receive(:getTaskType).and_return("getTaskType")
    @tvm.should_receive(:getTypeForDisplay).and_return("Some Type For Display")
    view.stub(:render_pluggable_template) do |r, options|
      options[:modify_onclick_callback].inspect
    end

    render :partial => "admin/tasks/plugin/task_entry.html.erb",:locals => {:scope => {:tvm => @tvm, :tvm_of_cancel_task => @tvm_of_cancel_task,
                                                                                    :task_config_index => @task_config_index, :task_config => @task_config}}

    response.body.should include("title: 'Edit Some Type For Display task'")
  end
end
