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

describe PipelinesController do

  render_views

  before(:each) do
    @user = Username.new(CaseInsensitiveString.new("foo"))
    controller.stub(:pipeline_history_service).and_return(@pipeline_history_service=double())
    controller.stub(:go_config_service).and_return(@go_config_service=double())
    controller.stub(:pipeline_lock_service).and_return(@pipeline_lock_service=double())
    controller.stub(:current_user).and_return(@user)
    @go_config_service.stub(:isSecurityEnabled).and_return(false)
    controller.stub(:populate_config_validity)
  end

  describe :show_for_trigger do

    it "should render trigger with options with pegged revisions" do
      @yesterday = org.joda.time.DateTime.new.minusDays(1).toDate()
      job_history = JobHistory.withJob("unit", JobState::Completed, JobResult::Passed, @yesterday)
      stage1 = StageInstanceModel.new("stage", "21", StageResult::Cancelled, StageIdentifier.new("pipeline-name", 23, "stage", "21"))
      stage2 = StageInstanceModel.new("stage-1", "2", StageResult::Cancelled, StageIdentifier.new("pipeline-name", 23, "stage-1", "2"))

      stage1.setBuildHistory(job_history)
      stage2.setBuildHistory(job_history)

      stages = StageInstanceModels.new
      stages.add stage1
      stages.add stage2
      pim = PipelineHistoryMother.singlePipeline("pipeline-name", stages)
      modification = Modification.new("username", "I changed something", "foo@bar.com", @yesterday, "1234")
      revisions = ModificationsMother.createSvnMaterialRevisions( modification)
      hg_revisions = ModificationsMother.createHgMaterialRevisions()
      revisions.addAll(hg_revisions)

      material_configs = revisions.getMaterials().convertToConfigs()

      svn_material_config = material_configs.get(0)
      svn_material_config.setConfigAttributes({SvnMaterialConfig::FOLDER => "Folder", SvnMaterialConfig::CHECK_EXTERNALS=> "true"})
      svn_material_config.setName(CaseInsensitiveString.new("SvnName"))

      pim.setMaterialRevisionsOnBuildCause(revisions)
      pim.setMaterialConfigs(material_configs)

      @pipeline_history_service.should_receive(:latest).with('pipeline-name', @user).and_return(pim)
      @go_config_service.should_receive(:variablesFor).with("pipeline-name").and_return(EnvironmentVariablesConfig.new())

      post 'show_for_trigger', :pipeline_name => 'pipeline-name', "pegged_revisions" =>{svn_material_config.getPipelineUniqueFingerprint() => "hello_world"}
      expect(response.body).to have_selector(".material_summary #material-number-0.updated[title='hello_world']", :text=>"hello_world")
      expect(response.body).to have_selector("#material-number-0-pegged[title='hello_world']")
      expect(response.body).to have_selector(".material_details #material-number-0-autocomplete-content")
    end
  end

end
