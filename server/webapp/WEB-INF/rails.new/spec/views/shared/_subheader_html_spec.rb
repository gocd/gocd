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

describe "/shared/_subheader.html.erb" do

  it 'should display title' do
    assign(:page_header, '<h1 class="entity_title">My Title</h1>')

    render :partial => "shared/subheader"

    expect(response).to have_selector("div.page_header h1.entity_title", :text => 'My Title')
  end

  it 'should display pipeline selector (personalize)' do
    assign(:page_header, 'My Title')
    assign(:show_pipeline_selector, true)

    stub_template "pipelines/_pipelines_selector.html.erb" => "PIPELINE SELECTOR"

    render :partial => "shared/subheader"

    assert_template partial: "pipelines/pipelines_selector", locals: {scope: {}}
  end

  it 'should display add environments link' do
    assign(:page_header, 'My Title')
    assign(:show_add_environments, true)

    render :partial => "shared/subheader"

    expect(response).to have_selector("div.page_header div.add_new_environment a.link_as_header_button[href='/environments/new']", :text => 'Add a new environment')
  end
end
