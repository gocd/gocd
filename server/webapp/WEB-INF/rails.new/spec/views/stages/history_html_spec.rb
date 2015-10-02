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

describe 'stages/history.html.erb' do
  it "should render partial stage history with scope populated" do
    params[:tab] = "tab"
    @stage_history_page = double("stage_history_page")
    @pipeline = double("current_stage_pipeline")
    @current_config_version = double("current_config_version")
    stub_template "_stage_history.html.erb" => "STAGE HISTORY HTML"

    render
    assert_template partial: "stage_history.html", locals: {scope: {stage_history_page: @stage_history_page, tab: params[:tab], current_stage_pipeline: @pipeline, current_config_version: @current_config_version}}
    expect(rendered).to eq("STAGE HISTORY HTML")
  end
end
