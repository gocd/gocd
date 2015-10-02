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

describe "comparison/stage_bar_without_controls.html.erb" do

  include StageModelMother

  before :each do
    hg_revisions = ModificationsMother.createHgMaterialRevisions()
    @pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithModifications( hg_revisions , "user"), stage_history_for("dev", "prod"))

  end

  it "should display PIM stage details" do
    render :partial => "comparison/stage_bar_without_controls.html.erb", :locals => {:scope => {:pipeline => @pipeline, :fixed_pipeline => @pipeline, :suffix => "to"}}

    response_body = Capybara.string(response.body)
    expect(response_body).to have_selector("#compare_pipeline_to input[value='#{@pipeline.getLabel()}']")
    response_body.find("div.selected_pipeline_to").tap do |selected_pipeline|
      expect(selected_pipeline).to have_selector("div[class='stage_bar Passed'][title='dev (Passed)']")
      expect(selected_pipeline).to have_selector("div[class='stage_bar Passed'][title='prod (Passed)']")
    end
  end
end
