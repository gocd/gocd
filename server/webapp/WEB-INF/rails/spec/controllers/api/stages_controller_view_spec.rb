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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe Api::StagesController do
  integrate_views

  before :each do
    controller.go_cache.clear
    controller.stub!(:stage_service).and_return(@stage_service = mock())
    controller.stub!(:set_locale)
    controller.stub(:licensed_agent_limit)
    controller.stub(:populate_config_validity)

  end

  it "should load stage data" do
    stage = StageMother.create_passed_stage("pipeline_name", 30, "stage_name", 2, "dev", java.util.Date.new())
    stage.setPipelineId(120)
    @stage_service.should_receive(:stageById).with(99).and_return(stage)
    get 'index', :id => "99", :format => "xml", :no_layout => true
    response.should have_tag("stage[name='stage_name'][counter='2']") do
      with_tag("link[rel='self'][href='http://test.host/go/api/stages/#{stage.getId()}.xml']")
      with_tag("pipeline[name='pipeline_name'][counter='30'][label='LABEL-30'][href='http://test.host/go/api/pipelines/pipeline_name/120.xml']")
      with_tag("updated", stage.latestTransitionDate().iso8601)
      with_tag("result", StageResult::Passed.to_s)
      with_tag("state", "Completed")
      with_tag("approvedBy", GoConstants::DEFAULT_APPROVED_BY)
      with_tag("jobs") do
        with_tag("job[href='http://test.host/go/api/jobs/-1.xml']")
      end
    end
  end

end
