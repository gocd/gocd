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

describe Admin::StagesController, "view" do
  include ConfigSaveStubbing
  include MockRegistryModule
  describe "actions" do
    integrate_views

    before do
      controller.stub(:populate_health_messages) do
        stub_server_health_messages
      end
      controller.stub(:populate_config_validity)
      controller.stub(:checkConfigFileValid)

      @cruise_config = CruiseConfig.new()
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
        response.status.should == "200 OK"
        response.body.should have_tag("div[class='form_item checkbox_row fetch_materials']")
        response.body.should have_tag("div[class='form_item checkbox_row clean_working_dir']")
      end

       it "should display 'on success' & 'manual' radio buttons" do
        get :edit, :stage_parent=> "pipelines",  :current_tab => :settings, :pipeline_name => @pipeline.name().to_s, :stage_name => @pipeline.get(0).name().to_s
        response.status.should == "200 OK"
        response.body.should have_tag("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}'][checked='checked']")
        response.body.should have_tag("label[for='auto']", 'On Success')
        response.body.should have_tag("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
        response.body.should have_tag("label[for='manual']", 'Manual')
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
        assigns[:pipeline].should == @pipeline
        assigns[:cruise_config].should == @cruise_config

        response.body.should have_tag("tr") do
          with_tag("td", "stage-name")
          with_tag("td", "1")
          with_tag("td", "On Success")
        end
        response.body.should have_tag("tr") do
          with_tag("td", "stage-second")
          with_tag("td", "2")
          with_tag("td", "Manual")
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
         response.status.should == "200 OK"
         response.body.should have_tag("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::SUCCESS}'][checked='checked']")
         response.body.should have_tag("label[for='auto']", 'On Success')
         response.body.should have_tag("input[type='radio'][name='stage[#{StageConfig::APPROVAL}][#{Approval::TYPE}]'][value='#{Approval::MANUAL}']")
         response.body.should have_tag("label[for='manual']", 'Manual')
       end

    end

    describe "when loading new" do
      before(:each) do
        ReflectionUtil.setField(@cruise_config, "md5", "1234abcd")
        controller.should_receive(:load_pipeline) do
          assigns[:cruise_config] = @cruise_config
          assigns[:pipeline] = @pipeline
        end
        controller.should_receive(:load_pause_info) do
          assigns[:pause_info] = PipelinePauseInfo.paused("just for fun", "loser")
        end
      end

      it "should load form for new stage" do
        get :new, :stage_parent=> "pipelines", :pipeline_name => "pipeline-name"
        assigns[:cruise_config].should == @cruise_config
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

          response.status.should == "200 OK"
          response.body.should have_text("Saved successfully")
          response.location.should =~ %r{/admin/pipelines/pipeline-name/stages?}
        end
      end
    end

    def assert_has_new_form
      response.body.should have_tag("form[action='/admin/pipelines/pipeline-name/stages']") do
        with_tag("input[type='text'][name='stage[name]']")
        with_tag("input[type='hidden'][name='config_md5'][id='config_md5'][value='1234abcd']")
      end
    end
  end
end
