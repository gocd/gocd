#
# Copyright 2019 ThoughtWorks, Inc.
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
#

require 'rails_helper'

describe ComparisonController do
  include StageModelMother

  render_views

  before do
    allow(controller).to receive(:pipeline_history_service).and_return(@phs = double('PipelineHistoryService'))
    allow(controller).to receive(:current_user).and_return(@loser = Username.new(CaseInsensitiveString.new("loser")))
    allow(controller).to receive(:populate_config_validity)
    @pipeline_instances = PipelineInstanceModels.createPipelineInstanceModels()
    @pipeline_instances.setPagination(Pagination.pageStartingAt(51, 100, 10))
    (1..10).each do |i|
      @pipeline_instances << PipelineInstanceModel.createPipeline("some_pipeline", i, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
    end
  end

  describe "show" do

    before :each do
      @result = HttpOperationResult.new
      @other_result = HttpOperationResult.new

      allow(HttpOperationResult).to receive(:new).and_return(@result)

      @from_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 10, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
      allow(@phs).to receive(:findPipelineInstance).with("some_pipeline", 10, @loser, @result).and_return(@from_pipeline)

      @to_pipeline = PipelineInstanceModel.createPipeline("some_pipeline", 17, "some-label", BuildCause.createWithEmptyModifications(), stage_history_for("dev", "prod"))
      allow(@phs).to receive(:findPipelineInstance).with("some_pipeline", 17, @loser, @result).and_return(@to_pipeline)


      @modification = Modification.new(@date=java.util.Date.new, "1234", "label-1", nil)
      @modification.setUserName("username")
      @modification.setComment("#42 I changed something")
      @modification.setModifiedFiles([ModifiedFile.new("sample_folder/foo.txt", "", ModifiedAction::added), ModifiedFile.new("sample_folder/bar.txt", "", ModifiedAction::deleted),
                                      ModifiedFile.new("sample_folder/baz.txt", "", ModifiedAction::modified), ModifiedFile.new("sample_folder/quux.txt", "", ModifiedAction::unknown)])
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

    def stub_go_config_service
      go_config_service = stub_service(:go_config_service)
      allow(go_config_service).to receive(:isSecurityEnabled).and_return(true)
      allow(go_config_service).to receive(:checkConfigFileValid).and_return(com.thoughtworks.go.config.validation.GoConfigValidity.valid())
      allow(go_config_service).to receive(:getCommentRendererFor).with("some_pipeline").and_return(com.thoughtworks.go.config.TrackingTool.new())
      mother = GoConfigMother.new
      allow(go_config_service).to receive(:getCurrentConfig).and_return(mother.cruiseConfigWithPipelineUsingTwoMaterials())
    end

    it "should render error page when user doesn't have view access to pipeline" do
      expect(controller).to receive(:mingle_config_service).and_return(service = double('MingleConfigService'))
      result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      result.forbidden(com.thoughtworks.go.i18n.LocalizedMessage::forbiddenToViewPipeline("some_pipeline"), HealthStateType.forbiddenForPipeline("some_pipeline"))

      expect(service).to receive(:mingleConfigForPipelineNamed).with('some_pipeline', @loser, result).and_return(nil)

      get :show, params:{:pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => 17}

      expect(assigns[:mingle_config]).to be_nil
      Capybara.string(response.body).find("div.content_wrapper_outer").tap do |outer_div|
        outer_div.find("div.content_wrapper_inner").tap do |inner_div|
          inner_div.find("div.notification").tap do |notification|
            expect(notification).to have_selector("div.biggest", :text => ":(")
          end
        end
      end
      expect(response.body).to include("<h3>You do not have view permissions for pipeline &#39;some_pipeline&#39;.</h3>")
      expect(response.status).to eq(403)
    end

    it "should show error if pipelines don't exist" do
      allow(controller).to receive(:pipeline_history_service).and_return(phs = double('PipelineHistoryService'))

      @result.forbidden("You do not have view permissions for pipeline 'admin_only'.", "too bad for you!", HealthStateType.forbiddenForPipeline("admin_only"))

      expect(HttpOperationResult).to receive(:new).and_return(@result)
      expect(phs).to receive(:findPipelineInstance).with("admin_only", 17, @loser, @result).and_return(nil)

      get :show, params:{:pipeline_name => "admin_only", :from_counter => "10", :to_counter => "17"}

      expect(assigns[:from_pipeline]).to be_nil
      expect(assigns[:to_pipeline]).to be_nil
      Capybara.string(response.body).find("div.content_wrapper_outer").tap do |outer_div|
        outer_div.find("div.content_wrapper_inner").tap do |inner_div|
          inner_div.find("div.notification").tap do |notification|
            expect(notification).to have_selector("div.biggest", :text => ":(")
          end
        end
      end
      expect(response.body).to include("<h3>You do not have view permissions for pipeline &#39;admin_only&#39;. { too bad for you! }\n</h3>")
      expect(response.status).to eq(403)
    end

    it "should render Checkins between the given pipeline instances" do
      allow(controller).to receive(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new("loser")))
      expect(controller).to receive(:mingle_config_service).and_return(service = double('MingleConfigService'))
      result = HttpLocalizedOperationResult.new

      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      expect(service).to receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)

      expect(controller).to receive(:changeset_service).and_return(changeset_service = double('ChangesetService'))
      expect(changeset_service).to receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, result, true, true).and_return(@revisions.getRevisions())

      stub_go_config_service

      get :show, params:{:pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17', :show_bisect => 'true'}

      expect(assigns[:material_revisions].size).to eq(2)
      expect(assigns[:dependency_material_revisions].size).to eq(1)

      assert_scm_modification_shown(@hg_revisions.getRevisions().get(0).getModifications().get(0), 1, 0)
      assert_scm_modification_shown(@hg_revisions.getRevisions().get(0).getModifications().get(1), 1, 1)
      assert_scm_modification_shown(@modification, 0, 0) #for svn
      assert_dependency_modification_shown(@dependency_revisions.getModifications().get(0), 0, 0)
    end

    def assert_scm_modification_shown mod, table_counter, row_counter
      Capybara.string(response.body).find("div.rounded-corner-for-tab-container").tap do |outer|
        outer.all("table.material_modifications").tap do |tables|
          tables[table_counter].all("tr.change").tap do |rows|
            expect(rows[row_counter]).to have_selector("td.revision", :text => /#{mod.getRevision()}/)
            expect(rows[row_counter]).to have_selector("td.modified_by", :text => /#{mod.getUserDisplayName()}/)
            expect(rows[row_counter]).to have_selector("td.comment", :text => /#{mod.getComment()}/)
          end
        end
      end
    end

    def assert_dependency_modification_shown mod, table_counter, row_counter
      Capybara.string(response.body).all("table.dependency_material_modifications").tap do |tables|
        tables[table_counter].all("tr.change").tap do |rows|
          expect(rows[row_counter]).to have_selector("td.revision a", :text => /#{mod.getRevision()}/)
          expect(rows[row_counter]).to have_selector("td.label a", :text => /#{mod.getPipelineLabel()}/)
          expect(rows[row_counter]).to have_selector("td.completed_at", :text => mod.getModifiedTime().iso8601)
        end
      end
    end

    it "should render errors when Checkins return an error" do
      config_service = stub_service(:go_config_service)
      expect(config_service).to receive(:getCurrentConfig).and_return(new_config = BasicCruiseConfig.new)
      allow(controller).to receive(:current_user).and_return(loser = Username.new(CaseInsensitiveString.new("loser")))
      expect(controller).to receive(:mingle_config_service).and_return(service = double('MingleConfigService'))
      expect(controller).to receive(:changeset_service).and_return(changeset_service = double('ChangesetService'))

      result = HttpLocalizedOperationResult.new
      allow(HttpLocalizedOperationResult).to receive(:new).and_return(result)
      mingle_config = MingleConfig.new("https://some_host/path", "foo_bar_project", "mql != not(mql)")
      expect(service).to receive(:mingleConfigForPipelineNamed).with('some_pipeline', loser, result).and_return(mingle_config)

      expect(changeset_service).to receive(:revisionsBetween).with('some_pipeline', 10, 17, loser, an_instance_of(HttpLocalizedOperationResult), true, false) do |name, from, to, loser, result|
        result.notFound("pipleine '[\"some_pipeline\"]' not found.", HealthStateType.general(HealthStateScope.forPipeline('foo')))
      end

      get :show, params:{:pipeline_name => "some_pipeline", :from_counter => "10", :to_counter => '17', :no_layout => true} #Using No layout here so that we can assert on that message.

      expect(response.body).to eq("pipleine '[\"some_pipeline\"]' not found.\n")
    end
  end

  describe "timeline_view" do

    it "should render timeline view" do
      expect(@phs).to receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)

      get :timeline, params:{:pipeline_name => "some_pipeline", :page => "1", :other_pipeline_counter => "3", :suffix => "from"}

      Capybara.string(response.body).find("div#modal_timeline_container").find("div.modal_timeline").tap do |timeline_container|
        timeline_container.find("form").find("div.results").find("div.pipeline_instance_list").tap do |pipeline_instance|
          pipeline_instance.find("ul").tap do |list|
            expect(list.find("li#pim_list_0")).to have_selector("div.pipeline_label", :text => "some-label")
            list.find("li#pim_list_9").find("div.pipeline").find("div.stages").all("div.stage").tap do |stages|
              stages.each do |stage|
                expect(stage.find("div.stage_bar_wrapper")).to have_selector(".stage_bar")
              end
            end
          end
        end
        timeline_container.find("form").find("div.results").find("div.pipeline_instance_details").tap do |pipeline_instance_details|
          pipeline_instance_details.find("div#pim_details_0").tap do |pim_0|
            expect(pim_0.find("div.pipeline").all("div.stages")[0].find("div.pipeline_counter")).to have_selector("h3", :text => "some-label")
            expect(pim_0).to have_selector("input[value='/compare/some_pipeline/1/with/3']", visible: :hidden)
          end
          expect(pipeline_instance_details.find("div#pim_details_4")).to have_selector("input[value='/compare/some_pipeline/5/with/3']", visible: :hidden)
        end
      end

      expect(Capybara.string(@response.body).all(:xpath, "//div[@class='modal_timeline']//div[@class='pipeline_instance_details']//a[@href='pipelines/some_pipeline/2/dev/35']").count).to eq(0)
    end

    it "should generate comparison paths for 'to' pipeline" do
      expect(@phs).to receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)

      get :timeline, params:{:pipeline_name => "some_pipeline", :page => "1", :other_pipeline_counter => "3", :suffix => "to"}

      Capybara.string(response.body).find("div#modal_timeline_container").find("div.modal_timeline").tap do |timeline_container|
        timeline_container.find("form").find("div.results").find("div.pipeline_instance_details").tap do |pipeline_instance_details|
          expect(pipeline_instance_details.find("div#pim_details_4")).to have_selector("input[value='/compare/some_pipeline/3/with/5']", visible: :hidden)
          expect(pipeline_instance_details.find("div#pim_details_0")).to have_selector("input[value='/compare/some_pipeline/3/with/1']", visible: :hidden)
        end
      end
    end

    it "should not render timeline page links when there is only one page" do
      expect(@phs).to receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 1, 10, "loser").and_return(@pipeline_instances)
      @pipeline_instances.setPagination(Pagination.pageStartingAt(0, 10, 20))

      get :timeline, params:{:pipeline_name => "some_pipeline", :page => "1"}

      expect(Capybara.string(response.body).all(:xpath, "div[@class='pagination']").count).to eq(0)
    end

    it "should render timeline page links" do
      @pipeline_instances.setPagination(Pagination.pageByNumber(3, 10, 2))
      expect(@phs).to receive(:findPipelineInstancesByPageNumber).with("some_pipeline", 3, 10, "loser").and_return(@pipeline_instances)

      get :timeline, params:{:pipeline_name => "some_pipeline", :page => "3"}

      Capybara.string(response.body).find("div#modal_timeline_container").find("div.modal_timeline").find("form").
          find("div.results").find("div.pipeline_instance_list").find("div#pagination_bar").find("div.pagination").find("div.wrapper").tap do |pagination|
        expect(pagination).to have_selector("a#pim_pages_prev")
        expect(pagination).to have_selector("a#pim_pages_1")
        expect(pagination).to have_selector("a#pim_pages_2")
        expect(pagination).to have_selector("span.current_page")
        expect(pagination).to have_selector("a#pim_pages_4")
        expect(pagination).to have_selector("a#pim_pages_5")
        expect(pagination).to have_selector("a#pim_pages_next")
      end
    end
  end
end
