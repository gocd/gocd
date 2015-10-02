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

describe "comparison/show.html.erb" do
  include GoUtil, ReflectiveUtil, PipelineModelMother

  before :each do
    dependency_material_revision = ModificationsMother.changedDependencyMaterialRevision("up_pipeline", 10, "label-10", "up_stage", 5, Time.now)
    scm_material_revision = ModificationsMother.oneUserOneFile()
    package_material_revision = ModificationsMother.createPackageMaterialRevision("go-agent-13.2.17000.rpm", "user", '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03","TRACKBACK_URL":"/go/tab/build/detail/go-packages/244/dist/1/rpm"}')
    assign(:to_pipeline, @to_pipeline = pipeline_model("my-shiny-pipeline", "label-1").getLatestPipelineInstance())
    @to_pipeline.setCounter(20)
    assign(:from_pipeline, @from_pipeline = pipeline_model("pipeline", "label-2").getLatestPipelineInstance())
    @from_pipeline.setCounter(10)
    assign(:material_revisions, @material_revisions = [scm_material_revision.getMaterialRevision(0), package_material_revision])
    assign(:dependency_material_revisions, @dependency_material_revisions = [dependency_material_revision])
    mother = GoConfigMother.new
    assign(:cruise_config, @cruise_config = mother.cruiseConfigWithTwoPipelineGroups())
    in_params(:from_counter => "10", :to_counter => "20")
    assign(:pipeline_name, "my-shiny-pipeline")
    assign(:mingle_config, com.thoughtworks.go.config.MingleConfig.new("http://some-tracking-tool", "go", "mql"))
  end

  def ensure_show_bisect_message_is_shown parent_id
    view.should_receive(:compare_pipelines_path).twice.with(:show_bisect => true).and_return("http://foo.bar?baz=quux")
    render :template => 'comparison/show.html.erb'

    response_body = Capybara.string(response.body)
    expect(response_body).to have_selector("##{parent_id} div.information div.message span", :text => "This comparison involves a pipeline instance that was triggered with a non-sequential material revision.")
    expect(response_body).to have_selector("##{parent_id} div.information span.prompt a[href='http://foo.bar?baz=quux']", :text => "Continue")
  end

  describe :on_revisions_tab do
    it "should show message prompting user about showing revisions for bisect on to_pipeline" do
      set(@to_pipeline, "naturalOrder", 1.2)
      ensure_show_bisect_message_is_shown "tab-content-of-checkins"
    end

    it "should show message prompting user about showing revisions for bisect on from_pipeline" do
      set(@from_pipeline, "naturalOrder", 1.4)
      ensure_show_bisect_message_is_shown "tab-content-of-checkins"
    end

    it "should show material title for SCM material" do
      render :template => 'comparison/show.html.erb'

      expect(Capybara.string(response.body)).to have_selector("div.material_title", :text => "Subversion - URL: url, Username: user, CheckExternals: true")
    end

    it "should show material title for Dependency material" do
      render :template => 'comparison/show.html.erb'

      expect(Capybara.string(response.body)).to have_selector("div.material_title", :text => "Pipeline - up_pipeline")
    end

    describe "package material with plugin available" do
      before :each do
        RepositoryMetadataStore::getInstance().addMetadataFor "pluginid", PackageConfigurations.new()
        PackageMetadataStore::getInstance().addMetadataFor "pluginid", PackageConfigurations.new()
      end

      after :each do
        RepositoryMetadataStoreHelper::clear()
      end

      it "should show material title for Package material" do
        begin
          params[:pipeline_name] = "some_pipeline"
          package_material_revision = ModificationsMother.createPackageMaterialRevision("go-agent-13.2.17000.rpm", "", '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"/go/tab/build/detail/go-packages/244/dist/1/rpm"}')
          assign(:material_revisions, [package_material_revision])

          render :template => 'comparison/show.html.erb'

          response_body = Capybara.string(response.body)
          expect(response_body).to have_selector("div.material_title", :text => "Package - Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]")
          response_body.find("table.material_modifications").tap do |material_modifications|
            material_modifications.find("tr.change").tap do |changes|
              expect(changes).to have_selector("td.comment", :text => "Built on blrstdgobgr03.Trackback: /go/tab/build/detail/go-packages/244/dist/1/rpm")
              changes.find("td.comment").tap do |comment|
                expect(comment).to have_selector("a[href='/go/tab/build/detail/go-packages/244/dist/1/rpm']", :text => "/go/tab/build/detail/go-packages/244/dist/1/rpm")
              end
              expect(changes).to have_selector("td.modified_by", :text => /anonymous/)
            end
          end
        ensure
        end
      end
    end

    it "should render dependency pipeline revision details" do
      render :template => 'comparison/show.html.erb'

      response_body = Capybara.string(response.body)
      response_body.find("table.dependency_material_modifications").tap do |table|
        table.find("tr.change").tap do |changes|
          expect(changes).to have_selector("td.revision a", :text => "up_pipeline/10/up_stage/5")
          expect(changes).to have_selector("td.label a[href='#{vsm_show_path("up_pipeline",10)}']", :text => "label-10")
        end
      end
    end
  end

  describe :on_mingle_cards_tab do
    it "should show message prompting user about showing revisions for bisect on to_pipeline" do
      set(@to_pipeline, "naturalOrder", 1.2)
      ensure_show_bisect_message_is_shown "tab-content-of-card_activity"
    end

    it "should show message prompting user about showing revisions for bisect on from_pipeline" do
      set(@from_pipeline, "naturalOrder", 1.4)
      ensure_show_bisect_message_is_shown "tab-content-of-card_activity"
    end
  end

  it "should show warning message to user to make them look away when showing bisect diff" do
    set(@from_pipeline, "naturalOrder", 1.4)
    in_params(:show_bisect => true.to_s)
    render :template => 'comparison/show.html.erb'

    response_body = Capybara.string(response.body)
    expect(response_body).to have_selector("div.information div.warning", :text => "This comparison involves a pipeline instance that was triggered with a non-sequential material revision.")
    expect(response_body).to_not have_selector("div.info-box div.prompt a[href='http://foo.bar?baz=quux']", :text => "Continue")
  end

  it "should carry forward the show_bisect flag to card-api call" do
    set(@from_pipeline, "naturalOrder", 1.4)
    in_params(:show_bisect => true.to_s)
    render :template => 'comparison/show.html.erb'
    expect(Capybara.string(response.body)).to have_content("http://test.host/api/card_activity/my-shiny-pipeline/10/to/20?show_bisect=true")
  end

  it "should set show_bisect off as default when looking at bisect" do
    render :template => 'comparison/show.html.erb'
    expect(Capybara.string(response.body)).to have_content("http://test.host/api/card_activity/my-shiny-pipeline/10/to/20")
  end
end
