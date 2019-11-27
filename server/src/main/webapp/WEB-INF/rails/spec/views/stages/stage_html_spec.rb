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
require_relative '../auto_refresh_examples'

describe 'stages/stage.html.erb' do

  include StageModelMother
  include GoUtil
  include JobMother
  include PipelineModelMother
  include ApplicationHelper

  before(:each) do
    allow(view).to receive(:is_user_an_admin?).and_return(true)
    allow(view).to receive(:admin_config_change_path)
    @system_environment = double('system environment')

    in_params :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 3
    @stage_history_page = stage_history_page(10)
    assign :stage_history_page, stage_history_page(10)
    empty_material_revision_arr = [].to_java(MaterialRevision)
    @revisions = MaterialRevisions.new( empty_material_revision_arr)
    @revisions.addAll(ModificationsMother.multipleModifications())
    @revisions.addAll(ModificationsMother.multipleModifications(MaterialsMother.hgMaterial()))
    allow(view).to receive(:system_environment).and_return(@system_environment)
    allow(@system_environment).to receive(:isShineEnabled).and_return(true)
    allow(view).to receive(:render_comment).and_return("link to traker")
    assign :pipeline, @pipeline =  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false, @revisions).getLatestPipelineInstance()
    assign :current_tab,  "overview"
    assign :fbh_pipeline_instances, []
  end

  def mat_revisions()
    modification = Modification.new(@date=java.util.Date.new, "1234", "label-1")
    modification.setUserName("username")
    modification.setComment("I changed something")
    modification.setModifiedFiles([ModifiedFile.new("nimmappa/foo.txt", "", ModifiedAction::added)])
    svn_revisions = ModificationsMother.createSvnMaterialRevisions(modification)
    svn_revisions.getMaterialRevision(0).markAsChanged()
    svn_revisions.materials().get(0).setName("SvnName")
    return svn_revisions
  end

  def pipeline(svn_revisions, counter)
    stage_models = StageInstanceModels.new
    stage_models.add(stage_model("cruise", "10"))
    pipeline = PipelineInstanceModel.createPipeline("pipeline_name", -1, "label", BuildCause.createManualForced(), stage_models)
    pipeline.setCounter(counter);
    pipeline.setMaterialRevisionsOnBuildCause(svn_revisions)
    pipeline
  end

  describe "overview_tab" do
    before(:each) do
      params[:action] = "overview"
      allow(view).to receive(:can_view_admin_page?).and_return(false)
      assign :user, Username.new(CaseInsensitiveString.new("bob"))
    end

    describe "stage html" do
      before(:each) do
        assign :stage, stage_with_three_runs()
        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
      end

      it "should render stage history" do
        render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
        expect(response).to have_selector("#stage-history-page[type='hidden'][value='" + @stage_history_page.currentPage().to_s() +"']", visible: :hidden)
        expect(response).to have_selector "#stage_history"
      end

      it "should render latest materials revisions" do
        render
        @revisions.each do |revision|
          latest = revision.getLatestModification()

          revision.getModifications().each do |mod|
            if mod==latest
              expect(response).to have_selector ".change .revision dd", :text => mod.getRevision()
            else
              expect(response).to_not have_selector ".change .revision dd", :text => mod.getRevision()
            end
          end
        end
        expect(response).to_not have_selector ".modified_files"
      end

      describe "auto_refresh" do
        before(:each) do
          @partial = 'stages/stage.html.erb'
          @ajax_refresher = /StageDetailAjaxRefresher/
        end

        it_should_behave_like :auto_refresh
      end

      it "should set title" do
        render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
        page = Capybara::Node::Simple.new(response.body)
        expect(page.title).to include("Stage Detail")
      end

      describe "title_bar" do

        it "should show stage result" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"

          Capybara.string(response.body).find(".result").tap do |result|
            expect(result).to have_selector(".Passed")
            expect(result).to have_selector(".message", :text => "Passed")
          end
        end

        it "should show build cause message" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"


          Capybara.string(response.body).find(".run_details .schedule_info").tap do |f|
            expect(f).to have_selector("span.label", :text => "Automatically triggered")
            expect(f).to have_selector("span.time[data]")
          end
        end

        it "should show duration" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
          Capybara.string(response.body).find(".run_details .duration").tap do |f|
            expect(f).to have_selector("span.label", :text => "Duration:")
            expect(f).to have_selector("span.time")
          end
        end

        it "should show compare link" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"

          Capybara.string(response.body).find(".run_details").tap do |f|
            expect(f).to have_selector("span.compare_pipeline a[href='/compare/cruise/0/with/1']", :text => "Compare")
          end
        end

        it "should show the schedule info" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
          Capybara.string(response.body).find(".schedule_info").tap do |f|
            expect(f).to have_selector(".label", :text => "Automatically triggered")
            expect(f).to have_selector("span.time[data]")
          end
        end

        it "should show this stage run among others in its pipeline instance" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
          Capybara.string(response.body).find(".run_results .run").tap do |f|
            expect(f).to have_selector("span.label", :text => "Run:")
            expect(f).to have_selector("#show_other_stage_runs", :text => "2 of 3")
          end
        end

        it "should show links to other stage runs" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
          Capybara.string(response.body).find(".other_runs").tap do |other_runs|
            other_runs.find("li a[href='/pipelines/pipeline_name/10/stage_name/1/overview']").tap do |f|
              expect(f).to have_selector "span", :text => "Run: 1 of 3"
              expect(f).to have_selector ".color_code.Passed"
              expect(f).to have_selector ".message", :text => "Passed"
            end
            other_runs.find("li a[href='/pipelines/pipeline_name/10/stage_name/3/overview']").tap do |f|
              expect(f).to have_selector "span", :text => "Run: 3 of 3"
              expect(f).to have_selector ".color_code.Failed"
              expect(f).to have_selector ".message", :text => "Failed"
            end
          end
        end
      end
    end

    describe "jobs section with all jobs passed" do
      before(:each) do
        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        assign(:stage, stage_with_all_jobs_passed())
      end

      it "should expand passed section if all jobs passed" do
        render

        Capybara.string(response.body).find(".jobs").tap do |jobs|
          expect(jobs).to have_selector(".jobs_failed.hidereveal_collapsed")
          expect(jobs).to have_selector(".jobs_passed")
          expect(jobs).to_not have_selector(".jobs_passed.hidereveal_collapsed")
          expect(jobs).to have_selector(".jobs_in_progress.hidereveal_collapsed")
        end
      end
    end

    describe "jobs section with passed jobs" do
      before(:each) do
        assign :stage, stage_with_three_runs()

        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
      end

      it "should hide in progress section if no jobs are in progress" do
        render

        expect(response.body).to have_selector ".jobs .jobs_in_progress.hidereveal_collapsed"
      end

      it "should hide failed section if no jobs failed" do
        render
        expect(response.body).to have_selector ".jobs .jobs_failed.hidereveal_collapsed"
      end

      it "should render urls" do
        in_params :action => 'jobs'
        assign :jobs, []
        render

        Capybara.string(response.body).find(".sub_tabs_container").tap do |f|
          expect(f).to have_selector "a[href='/pipelines/pipeline_name/10/stage_name/3/materials']", :text => "Materials"
          expect(f).to have_selector ".current a[href='/pipelines/pipeline_name/10/stage_name/3/jobs']", :text => "Jobs"
          expect(f).to have_selector "a[href='/pipelines/pipeline_name/10/stage_name/3/overview']", :text => "Overview"
          expect(f).to have_selector "a[href='/pipelines/pipeline_name/10/stage_name/3/stage_config']", :text => "Config"
        end

        Capybara.string(response.body).find("script[type='text/javascript']", :visible => false).tap do |script_tag|
          expect(script_tag.text).to include "/pipelines/pipeline_name/10/stage_name/3/jobs.json"
        end
      end
    end

    describe "jobs section" do
      before(:each) do
        assign :stage, stage_with_5_jobs()
        params[:pipeline_name] = "cruise"

        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
      end

      it "should expand in progress section if any jobs are in progress" do
        render
        expect(response.body).to have_selector ".jobs .jobs_in_progress"
        expect(response.body).to_not have_selector ".jobs .jobs_in_progress.hidereveal_collapsed"
      end

      it "should expand failed section if any jobs failed" do
        render
        expect(response.body).to have_selector ".jobs .jobs_failed"
        expect(response.body).to_not have_selector ".jobs .jobs_failed.hidereveal_collapsed"
      end

      it "should display jobs sections" do
        render
        Capybara.string(response.body).find(".jobs").tap do |jobs|
          jobs.find(".jobs_failed").tap do |job|
            expect(job).to have_selector "span", :text => "Failed: 2"
            job.all(".job").tap do |f|
              expect(f[0]).to have_selector("a", :text => /^[\s\S]*fifth[\s\S]*$/)
              expect(f[0]).to have_selector("a .color_code_small.Cancelled")
              expect(f[1]).to have_selector("a", :text => /^[\s\S]*first[\s\S]*$/)
              expect(f[1]).to have_selector("a .color_code_small.Failed")
            end
          end
          jobs.find(".jobs_passed").tap do |job|
            expect(job).to have_selector "span", :text => "Passed: 2"
            job.all(".job").tap do |f|
              expect(f[0]).to have_selector("a", :text => /^[\s\S]*second[\s\S]*$/)
              expect(f[0]).to have_selector("a .color_code_small.Passed")
              expect(f[1]).to have_selector("a", :text => /^[\s\S]*third[\s\S]*$/)
              expect(f[1]).to have_selector("a .color_code_small.Passed")
            end
          end

          jobs.find(".jobs_in_progress").tap do |job|
            expect(job).to have_selector "span", :text => "In Progress: 1"
            job.all(".job").tap do |f|
              expect(f[0]).to have_selector("a", :text => /^[\s\S]*fourth[\s\S]*$/)
              expect(f[0]).to have_selector("a .color_code_small.Active")
            end
          end

          jobs.find(".hidereveal_collapsed").tap do |job_group|
            expect(job_group).to have_selector ".hidereveal_expander"
            expect(job_group).to have_selector ".hidereveal_content"
            expect(job_group.text).to include "make_collapsable"
          end
        end
      end

    end
  end
  describe "non overview tabs" do

    before(:each) do
      assign :stage, stage_with_three_runs()
    end

    describe "jobs tab" do
      before(:each) do
        params[:action] = "jobs"
        assign :jobs, jobs_model
      end

      it "should render jobs tab" do
        render
        expect(response).to have_selector ".jobs_summary .job .job_name a", :text => "first"
      end

    end

    describe "materials tab" do
      before(:each) do
        params[:action] = "materials"
      end

      it "should render jobs tab" do
        render
        cnt_modifications = 0
        @revisions.each do |revision|
          revision.getModifications().each do |mod|
            expect(response).to have_selector ".change .revision dd", mod.getRevision()
            cnt_modifications += 1
          end
        end
        Capybara.string(response.body).all(".modified_files").tap do |modified_files|
          expect(modified_files.count()).to eq cnt_modifications
        end
      end
    end

    describe "stats tab" do
      before(:each) do
        params[:action] = "stats"
        assign :chart_stage_duration, [{"link"=> "/pipelines/pipeline-name/1/stage/1", "x" => 1, "y"=>10.0}, {"link" => "/pipelines/pipeline-name/2/stage/1", "x" => 2, "y"=> 20.0}].to_json
        assign :chart_tooltip_data, {"1_60" => ["00:10:00", "22 Feb, 2008 at 10:21:23 [+0530]", "LABEL-1"], "2_120" => ["00:20:00", "22 Feb, 2008 at 10:21:23 [+0530]", "LABEL-2"]}.to_json
        assign :pagination, Pagination.pageStartingAt(12, 200, 10)
        assign :start_end_dates, ["start date", "end date"]
        assign :chart_scale, "some scale"
      end

      it "should render stats tab with chart for given stage" do
        render
        Capybara.string(response.body).find("#stage_stats .stats").tap do |f|
          expect(f).to have_selector("#chart_details_container #stage-duration-graph")
          expect(f.find("#chart_details_container script[type='text/javascript']", :visible => false).text).to include "var graph_data"
          expect(f.find("#chart_details_container script[type='text/javascript']", :visible => false).text).to include "showStageDurationGraph"
        end
      end

      it "should show prev and next links if page_number is 2" do
        params[:page_number] = "2"

        render

        Capybara.string(response.body).find("#stage_stats .stats").tap do |f|
          expect(f).to have_selector("a[href='#{stage_detail_tab_stats_path(:action => "stats", :page_number => "1")}']", :text => "Newer")
          expect(f).to have_selector("a[href='#{stage_detail_tab_stats_path(:action => "stats", :page_number => "3")}']", :text => "Older")
        end
      end
    end

    describe "config tab" do
      before(:each) do
        params[:action] = "stage_config"
        assign :ran_with_config_revision,  GoConfigRevision.new("config-xml", "my-md5", "loser", "2.3.0", TimeProvider.new);
        allow(view).to receive(:is_user_an_admin?).and_return(true)
      end

      it "should render Config tab" do
        render
        expect(response).to have_selector("#ran_with_config .config")
      end

      it "should render Config tab with message if user is not admin" do
        allow(view).to receive(:is_user_an_admin?).and_return(false)
        render

        Capybara.string(response.body).find("#ran_with_config .config").tap do |f|
          expect(f).to have_selector("div.notification p.information", :text => "Historical configuration is available only for Go Administrators.")
        end
      end

      it "should render the config file contents in config tab" do
        render
        Capybara.string(response.body).find("#ran_with_config .config").tap do |f|
          expect(f).to have_selector("pre#content_container.wrap_pre", :text => "config-xml")
        end
      end

      it "should render information message if no revision is found" do
        assign :ran_with_config_revision, nil
        render
        Capybara.string(response.body).find("#ran_with_config div.config").tap do |f|
          expect(f).to have_selector("div.notification p.information", :text => "Historical configuration is not available for this stage run.")
        end
      end
    end
  end
end

