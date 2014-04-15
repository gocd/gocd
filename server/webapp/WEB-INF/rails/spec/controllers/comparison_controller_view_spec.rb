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

require File.join(File.dirname(__FILE__), "..", "spec_helper")

describe ComparisonController, "view" do
  include StageModelMother

  integrate_views

  before do
    controller.stub(:populate_health_messages) do
      stub_server_health_messages
    end
    controller.stub(:pipeline_history_service).and_return(@phs = mock('PipelineHistoryService'))
    controller.stub!(:current_user).and_return(@loser = Username.new(CaseInsensitiveString.new("loser")))
    @pipeline_instances = PipelineInstanceModels.createPipelineInstanceModels()
    @pipeline_instances.setPagination(Pagination.pageStartingAt(51, 100, 10))
    (1..10).each do |i|
      @pipeline_instances << PipelineInstanceModel.createPipeline("some_pipeline", i, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
    end
  end

  describe "show" do

    before do
      @result = HttpOperationResult.new
      @other_result = HttpOperationResult.new

      HttpOperationResult.stub(:new).and_return(@result)

      @from_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
      @phs.stub(:findPipelineInstance).with("some_pipeline", 10, @loser, @result).and_return(@from_pipeline)

      @to_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 17, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
      @phs.stub(:findPipelineInstance).with("some_pipeline", 17, @loser, @result).and_return(@to_pipeline)

      @phs.stub(:findPipelineInstances).with(17, "some_pipeline", 10, @loser.getUsername()).and_return(@pipeline_instances)
      @phs.stub(:findPipelineInstances).with(10, "some_pipeline", 10, @loser.getUsername()).and_return(@pipeline_instances)

      @modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
      @modification.setUserName("username")
      @modification.setComment("#42 I changed something")
      @modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added), ModifiedFile.new("nimmappa/bar.txt", "", ModifiedAction::deleted),
                                      ModifiedFile.new("nimmappa/baz.txt", "", ModifiedAction::modified), ModifiedFile.new("nimmappa/quux.txt", "", ModifiedAction::unknown)])
      @revisions = MaterialRevisions.new([].to_java(MaterialRevision))

      @svn_revisions = ModificationsMother.createSvnMaterialRevisions(@modification)
      @svn_revisions.getMaterialRevision(0).markAsChanged()
      @svn_revisions.materials().get(0).setName(CaseInsensitiveString.new("SvnName"))
      @revisions.addAll(@svn_revisions)

      @hg_revisions = ModificationsMother.createHgMaterialRevisions()
      @revisions.addAll(@hg_revisions)

      @dependency_revisions = ModificationsMother.changedDependencyMaterialRevision("up_pipeline", 10, "label-10", "up_stage", 5, Time.now)
      @revisions.addRevision(@dependency_revisions)
    end

    it "should load mingle config for given pipeline" do
      controller.stub!(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new('loser')))
      controller.should_receive(:mingle_config_service).and_return(service = mock('MingleConfigService'))
      result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)
      controller.should_receive(:changeset_service).and_return(changeset_service = mock('ChangesetService'))
      changeset_service.should_receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, result, true, false).and_return(@revisions.getRevisions())
      stub_go_config_service

      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17'
      assigns[:mingle_config].should == mingle_config
      response.body.should include("http://test.host/api/card_activity/some_pipeline/10/to/17")
    end

    def stub_go_config_service
      go_config_service = stub_service(:go_config_service)
      go_config_service.stub(:isSecurityEnabled).and_return(true)
      go_config_service.stub(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      go_config_service.stub!(:getCommentRendererFor).with("some_pipeline").and_return(com.thoughtworks.go.config.TrackingTool.new())
      mother = GoConfigMother.new
      go_config_service.stub(:getCurrentConfig).and_return(mother.cruiseConfigWithPipelineUsingTwoMaterials())
    end

    it "should not fail if mingle not configured for given pipeline" do
      controller.stub!(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new('loser')))
      service = stub_service(:mingle_config_service)
      result = stub_localized_result()
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(nil)
      changeset_service = stub_service(:changeset_service)
      changeset_service.should_receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, result, true, false).and_return(@revisions.getRevisions())
      stub_go_config_service


      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17'
      assigns[:mingle_config].should == nil

      response.status.should == "200 OK"
      response.body.should include("tw_gadget.init") #because it has to show remote dependency gadgets anyway
      response.body.should_not include("tw_gadget.addGadget")
      response.body.should include("No mingle project configured for this pipeline.")

      assert_scm_modification_shown(@hg_revisions.getRevisions().get(0).getModifications().get(0))
    end

    it "should render error page when user doesn't have view access to pipeline" do
      controller.should_receive(:mingle_config_service).and_return(service = mock('MingleConfigService'))
      result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      result.unauthorized(LocalizedMessage.cannotViewPipeline("some_pipeline"), HealthStateType.unauthorisedForPipeline("some_pipeline"))
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', @loser, result).and_return(nil)

      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => 17
      assigns[:mingle_config].should be_nil
      response.should have_tag("div.biggest", ":(")
      response.body.should include("<h3>You do not have view permissions for pipeline 'some_pipeline'.</h3>")
      response.status.should == '401 Unauthorized'
    end

    it "should show error if pipelines don't exist" do
      controller.stub(:pipeline_history_service).and_return(phs = mock('PipelineHistoryService'))

      @result.unauthorized("You do not have view permissions for pipeline 'admin_only'.", "too bad for you!", HealthStateType.unauthorisedForPipeline("admin_only"))

      HttpOperationResult.should_receive(:new).and_return(@result)
      phs.should_receive(:findPipelineInstance).with("admin_only", 17, @loser, @result).and_return(nil)

      get :show, :pipeline_name => "admin_only", :from_counter => "10", :to_counter => "17"
      assigns[:from_pipeline].should be_nil
      assigns[:to_pipeline].should be_nil

      response.should have_tag("div.biggest", ":(")
      response.body.should include("<h3>You do not have view permissions for pipeline 'admin_only'. { too bad for you! }\n</h3>")
      response.status.should == '401 Unauthorized'
    end

    it "should render Card Activity and Checkins as tabs" do
      controller.stub!(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new("loser")))
      controller.should_receive(:mingle_config_service).and_return(service = mock('MingleConfigService'))
      result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)

      controller.should_receive(:changeset_service).and_return(changeset_service = mock('ChangesetService'))
      changeset_service.should_receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, result, true, false).and_return(@revisions.getRevisions())

      stub_go_config_service
      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => "17"

      response.body.should have_tag(".sub_tabs_container") do |tab_container|
        tab_container.should have_tag("ul li.card_activity a", "Card Activity")
        tab_container.should have_tag("ul li.checkins a", "Changes")
      end
      response.should have_tag("div#tab-content-of-card_activity")
      response.should have_tag("div#tab-content-of-checkins")
    end

    it "should render Checkins between the given pipeline instances" do
      controller.stub!(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new("loser")))
      controller.should_receive(:mingle_config_service).and_return(service = mock('MingleConfigService'))
      result = HttpLocalizedOperationResult.new

      HttpLocalizedOperationResult.stub(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)

      controller.should_receive(:changeset_service).and_return(changeset_service = mock('ChangesetService'))
      changeset_service.should_receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, result, true, true).and_return(@revisions.getRevisions())

      stub_go_config_service

      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17', :show_bisect => 'true'

      assigns[:material_revisions].size.should == 2
      assigns[:dependency_material_revisions].size.should == 1

      assert_scm_modification_shown(@hg_revisions.getRevisions().get(0).getModifications().get(0))
      assert_scm_modification_shown(@hg_revisions.getRevisions().get(0).getModifications().get(1))
      assert_scm_modification_shown(@modification) #for svn
      assert_dependency_modification_shown(@dependency_revisions.getModifications().get(0))
    end

    def assert_scm_modification_shown mod
      response.should have_tag("td.revision", /#{mod.getRevision()}/)
      response.should have_tag("td.modified_by", /#{mod.getUserDisplayName()}/)
      response.should have_tag("td.comment", /#{mod.getComment()}/)
    end

    def assert_dependency_modification_shown mod
      response.should have_tag("td.revision a", /#{mod.getRevision()}/)
      response.should have_tag("td.label a", /#{mod.getPipelineLabel()}/)
      response.should have_tag("td.completed_at", mod.getModifiedTime().iso8601)
    end

    it "should render errors when Checkins return an error" do
      controller.stub!(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new("loser")))
      controller.should_receive(:mingle_config_service).and_return(service = mock('MingleConfigService'))
      controller.should_receive(:changeset_service).and_return(changeset_service = mock('ChangesetService'))

      result = HttpLocalizedOperationResult.new
      HttpLocalizedOperationResult.stub(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      service.should_receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)

      changeset_service.should_receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, an_instance_of(HttpLocalizedOperationResult), true, false) do |name, from, to, loser, result|
        result.notFound(LocalizedMessage.string("PIPELINE_NOT_FOUND", ['some_pipeline']), HealthStateType.general(HealthStateScope.forPipeline('foo')))
      end

      get :show, :pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17', :no_layout => true #Using No layout here so that we can assert on that message.

      response.body.should == "Pipeline 'some_pipeline' not found.\n"
    end

  end

  describe "timeline_view" do


    it "should render timeline view" do
      @phs.should_receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)

      get :timeline, :pipeline_name => "some_pipeline", :page => "1", :other_pipeline_counter => "3", :suffix => "from"

      response.body.should have_tag(".modal_timeline") do |timeline_container|
        timeline_container.should have_tag(".pipeline_instance_list") do |pim_list|
          pim_list.should have_tag("ul li#pim_list_0 .pipeline_label", "some-label")
          pim_list.should have_tag("ul li#pim_list_9 .stage_bar")
        end
        timeline_container.should have_tag(".pipeline_instance_details") do |details_container|
          details_container.should have_tag("#pim_details_0 .pipeline_counter h3", "some-label")
          details_container.should have_tag("input.pipeline_counter[value='/compare/some_pipeline/5/with/3']")
          details_container.should have_tag("input.pipeline_counter[value='/compare/some_pipeline/1/with/3']")
        end
      end

      response.body.should_not have_tag(".modal_timeline .pipeline_instance_details a[href=?]", stage_detail_path(:pipeline_name => "some_pipeline", :pipeline_counter => 2, :stage_name => "dev", :stage_counter => 35))
    end

    it "should generate comparison paths for 'to' pipeline" do
      @phs.should_receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)

      get :timeline, :pipeline_name => "some_pipeline", :page => "1", :other_pipeline_counter => "3", :suffix => "to"

      response.body.should have_tag(".modal_timeline") do |timeline_container|
        timeline_container.should have_tag(".pipeline_instance_details") do |details_container|
          details_container.should have_tag("input.pipeline_counter[value='/compare/some_pipeline/3/with/5']")
          details_container.should have_tag("input.pipeline_counter[value='/compare/some_pipeline/3/with/1']")
        end
      end

    end

    it "should not render timeline page links when there is only one page" do
      @phs.should_receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)
      @pipeline_instances.setPagination(Pagination.pageStartingAt(0, 10, 20))

      get :timeline, :pipeline_name => "some_pipeline", :page => "1"

      response.body.should_not have_tag(".pagination")
    end

    it "should render timeline page links" do
      @pipeline_instances.setPagination(Pagination.pageByNumber(3, 10, 2))
      @phs.should_receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 3, 10, "loser").and_return(@pipeline_instances)

      get :timeline, :pipeline_name => "some_pipeline", :page => "3"

      response.body.should have_tag(".pagination") do |pagination|
        pagination.should have_tag("a#pim_pages_prev")
        pagination.should have_tag("a#pim_pages_1")
        pagination.should have_tag("a#pim_pages_2")
        pagination.should have_tag("span.current_page")
        pagination.should have_tag("a#pim_pages_4")
        pagination.should have_tag("a#pim_pages_5")
        pagination.should have_tag("a#pim_pages_next")  
      end
    end
  end
end
