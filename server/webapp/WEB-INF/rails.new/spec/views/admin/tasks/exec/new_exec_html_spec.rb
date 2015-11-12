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

describe "admin/tasks/plugin/new.html.erb" do
  include GoUtil, TaskMother, FormUI

  before :each do
    assign(:cruise_config, config = BasicCruiseConfig.new)
    set(config, "md5", "abcd1234")

    assign(:on_cancel_task_vms, @vms =  java.util.Arrays.asList([vm_for(exec_task('rm')), vm_for(ant_task), vm_for(nant_task), vm_for(rake_task), vm_for(fetch_task_with_exec_on_cancel_task)].to_java(TaskViewModel)))
    view.stub(:admin_task_create_path).and_return("task_create_path")
  end

  it "should render a simple exec task for create" do
    task = assign(:task, simple_exec_task)

    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render

    expect(response.body).to have_selector("form[action='task_create_path']")

    Capybara.string(response.body).find('form').tap do |form|
      form.all("div.fieldset") do |divs|
        expect(divs[0]).to have_selector("label", :text => "Command*")
        expect(divs[0]).to have_selector("input[name='task[command]']")
        expect(divs[0]).to have_selector("label", :text => "Arguments")
        expect(divs[0]).to have_selector("input[type='text'][name='task[args]']")
        expect(divs[0]).to have_selector("label", :text => "Working Directory")
        expect(divs[0]).to have_selector("input[name='task[workingDirectory]']")
      end
    end
  end

  it "should display empty div to load gist based auto complete" do
    task = assign(:task, simple_exec_task)
    assign(:task_view_model, Spring.bean("taskViewService").getViewModel(task, 'new'))

    render

    Capybara.string(response.body).all('div.gist_panel').tap do |divs|
      expect(divs[0]).to have_selector("div.gist_lookup")
      expect(divs[0]).not_to have_selector("div.gist_lookup .gist_based_auto_complete")
    end
  end
end

