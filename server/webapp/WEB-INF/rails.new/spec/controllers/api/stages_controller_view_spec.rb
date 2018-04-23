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

require 'rails_helper'

describe Api::StagesController do
  render_views

  before :each do
    controller.go_cache.clear
    allow(controller).to receive(:stage_service).and_return(@stage_service = double())
    # allow(controller).to receive(:set_locale)
    allow(controller).to receive(:populate_config_validity)

  end

  it "should load stage data" do
    stage = StageMother.create_passed_stage("pipeline_name", 30, "stage_name", 2, "dev", java.util.Date.new())
    stage.setPipelineId(120)
    expect(@stage_service).to receive(:stageById).with(99).and_return(stage)
    get 'index', params: {:id => "99", :no_layout => true}, format: :xml

    doc = Nokogiri::XML(response.body)
    stage_element = doc.xpath("stage[@name='stage_name'][@counter='2']")
    expect(stage_element).to_not be_nil_or_empty

    stage_element.tap do |entry|
      expect(entry.xpath("link[@rel='self'][@href='http://test.host/go/api/stages/#{stage.getId()}.xml']")).to_not be_nil_or_empty
      expect(entry.xpath("pipeline[@name='pipeline_name'][@counter='30'][@label='LABEL-30'][@href='http://test.host/go/api/pipelines/pipeline_name/120.xml']")).to_not be_nil_or_empty
      expect(entry.xpath("updated").text).to eq(DateUtils.formatISO8601(stage.latestTransitionDate()))
      expect(entry.xpath("result").text).to eq(StageResult::Passed.to_s)
      expect(entry.xpath("state").text).to eq("Completed")
      expect(entry.xpath("approvedBy").text).to eq(GoConstants::DEFAULT_APPROVED_BY)

      job_element = entry.xpath("jobs")
      expect(job_element).to_not be_nil_or_empty
      job_element.tap do |node|
        expect(node.xpath("job[@href='http://test.host/go/api/jobs/-1.xml']")).to_not be_nil_or_empty
      end
    end
  end
end
