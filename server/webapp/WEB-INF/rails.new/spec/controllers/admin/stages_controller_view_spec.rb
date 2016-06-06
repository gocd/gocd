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

describe Admin::StagesController, "view" do
  include GoUtil
  include ConfigSaveStubbing
  include MockRegistryModule
  describe "actions" do
    render_views

    before do
      controller.stub(:populate_config_validity)
      controller.stub(:checkConfigFileValid)

      @cruise_config = BasicCruiseConfig.new()
      cruise_config_mother = GoConfigMother.new
      @pipeline = cruise_config_mother.addPipeline(@cruise_config, "pipeline-name", "stage-name", ["build-name"].to_java(java.lang.String))
      second_stage = StageConfig.new(CaseInsensitiveString.new("stage-second"), JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("build-2")), JobConfig.new(CaseInsensitiveString.new("build-3"))].to_java(JobConfig)))
      second_stage.updateApproval(Approval.manualApproval())
      @pipeline.add(second_stage)
    end

    describe "edit settings" do
      before(:each) do
        @go_config_service = stub_service(:go_config_service)
        @pipeline_pause_service = stub_service(:pipeline_pause_service)
        controller.should_receive(:load_pipeline) do
          controller.instance_variable_set('@cruise_config', @cruise_config)
          controller.instance_variable_set('@pipeline', @pipeline)
        end
        @pause_info = PipelinePauseInfo.paused("just for fun", "loser")
        @pipeline_pause_service.should_receive(:pipelinePauseInfo).with("pipeline-name").and_return(@pause_info)
        @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
      end

      it "should display 'fetch materials' & 'clean working directory' checkbox" do
        get :edit, :stage_parent=> "pipelines", :current_tab => :settings, :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s
        expect(response.status).to eq(200)
        expect(response.body).to have_selector("div[class='form_item checkbox_row fetch_materials']")
        expect(response.body).to have_selector("div[class='form_item checkbox_row clean_working_dir']")
      end

      it "should display 'on success' & 'manual' radio buttons" do
        get :edit, :stage_parent=> "pipelines",  :current_tab => :settings, :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s
        expect(response.status).to eq(200)
        expect(response.body).to have_selector("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}'][checked='checked']")
        expect(response.body).to have_selector("label[for='auto']", :text=>"On Success")
        expect(response.body).to have_selector("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
        expect(response.body).to have_selector("label[for='manual']", :text=>"Manual")
      end

    end

    describe "when listing loads pipeline successfully" do
      before(:each) do
        controller.should_receive(:load_pipeline) do
          controller.instance_variable_set('@cruise_config', @cruise_config)
          controller.instance_variable_set('@pipeline', @pipeline)
        end
      end

      it "should render stages for pipeline" do
        get :index, :stage_parent=> "pipelines",  :pipeline_name => "pipeline"
        expect(assigns(:pipeline)).to eq(@pipeline)
        expect(assigns(:cruise_config)).to eq(@cruise_config)

        Capybara.string(response.body).find("table.stages_listing").tap do |table|
          table.find("tr.stage_stage-name").tap do |tr|
            expect(tr).to have_selector("td.stage_name", :text=>"stage-name")
            expect(tr).to have_selector("td.number_of_jobs", :text=>"1")
            expect(tr).to have_selector("td.approval_type", :text=>"On Success")
          end
          table.find("tr.stage_stage-second").tap do |tr|
            expect(tr).to have_selector("td.stage_name", :text=>"stage-second")
            expect(tr).to have_selector("td.number_of_jobs", :text=>"2")
            expect(tr).to have_selector("td.approval_type", :text=>"Manual")
          end
        end
      end
    end

    describe "foo" do
      before(:each) do
        controller.should_receive(:load_pipeline) do
          controller.instance_variable_set('@cruise_config', @cruise_config)
          controller.instance_variable_set('@pipeline', @pipeline)
        end
      end

      it "should display 'on success' & 'manual' radio buttons" do
        get :edit, :stage_parent=> "pipelines", :current_tab => :settings, :pipeline_name => "pipeline", :stage_name => "stage-name"
        expect(response.status).to eq(200)
        expect(response.body).to have_selector("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}'][checked='checked']")
        expect(response.body).to have_selector("label[for='auto']", :text=>"On Success")
        expect(response.body).to have_selector("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
        expect(response.body).to have_selector("label[for='manual']", :text=>"Manual")
      end

    end

    describe "when loading new" do
      before(:each) do
        ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
        controller.should_receive(:load_pipeline) do
          controller.instance_variable_set('@cruise_config', @cruise_config)
          controller.instance_variable_set('@pipeline', @pipeline)
        end
        controller.should_receive(:load_pause_info) do
          controller.instance_variable_set('@pause_info', PipelinePauseInfo.paused("just for fun", "loser"))
        end
      end

      it "should load form for new stage" do
        get :new, :stage_parent=> "pipelines", :pipeline_name => "pipeline-name"
        expect(assigns(:cruise_config)).to eq(@cruise_config)
        assert_has_new_form
      end
    end

    describe "when creating" do
      before(:each) do
        ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
        @cruise_config.pipelineConfigByName(CaseInsensitiveString.new("pipeline-name")).findBy(CaseInsensitiveString.new("stage-foo")).should be_nil
        @go_config_service = stub_service(:go_config_service)
      end

      describe "with valid data" do
        before do
          @result = stub_localized_result
          @user = current_user
          stub_save_for_success
          @go_config_service.stub(:registry).and_return(MockRegistryModule::MockRegistry.new)
        end

        after do
          assert_save_arguments "some-md5"
        end

        it "should save stage fields" do
          post :create, :stage_parent=> "pipelines", :pipeline_name => "pipeline-name", :stage => {:name => "stage-foo", :jobs => [{:name => "another-job"}]}, :config_md5 => "some-md5"
          @cruise_config.pipelineConfigByName(CaseInsensitiveString.new("pipeline-name")).findBy(CaseInsensitiveString.new("stage-foo")).should_not be_nil

          expect(response.status).to eq(200)
          expect(response.body).to have_content("Saved successfully")
          expect(response.location).to have_content(%r{/admin/pipelines/pipeline-name/stages?})
        end
      end
    end

    def assert_has_new_form
      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline-name/stages']").tap do |form|
        expect(form).to have_selector("input[type='text'][name='stage[name]']")
        expect(form).to have_selector("input[type='hidden'][name='config_md5'][id='config_md5'][value='1234abcd']")
      end
    end
  end
end
