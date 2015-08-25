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
load File.join(File.dirname(__FILE__), "..",  "auto_refresh_examples.rb")

describe 'stages/stage.html.erb' do

  include StageModelMother, GoUtil, JobMother, PipelineModelMother, ApplicationHelper

  before(:each) do
    stub_server_health_messages
    view.stub(:is_user_an_admin?).and_return(true)
    view.stub(:config_change_path)

    in_params :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 3
    @stage_history_page = stage_history_page(10)
    assign :stage_history_page, stage_history_page(10)
    empty_material_revision_arr = [].to_java(MaterialRevision)
    @revisions = MaterialRevisions.new( empty_material_revision_arr)
    @revisions.addAll(ModificationsMother.multipleModifications())
    @revisions.addAll(ModificationsMother.multipleModifications(MaterialsMother.hgMaterial()))
    view.stub(:render_comment).and_return("link to traker")
    assign :pipeline, @pipeline =  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false, @revisions).getLatestPipelineInstance()
    assign :current_tab,  "overview"
    assign :fbh_pipeline_instances, []
    assign :current_server_health_states, ServerHealthStates.new([])
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
      view.stub(:can_view_admin_page?).and_return(false)
      assign :user, Username.new(CaseInsensitiveString.new("bob"))
    end

    describe "stage html" do
      before(:each) do
        assign :stage, stage_with_three_runs()
        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        view.stub(:stage_detail_tab_path)
        assign :failing_tests, StageTestRuns.new(12, 0, 0)
      end

      it "should render stage history" do
        render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
        expect(response).to have_selector "#stage-history-page[type='hidden'][value='" + @stage_history_page.currentPage().to_s() +"']"
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

      describe :auto_refresh do
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

      describe :title_bar do

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
            expect(f).to have_selector("span.time", :text => /\s+at\s+/)
          end
        end

        it "should show duration" do
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"
          Capybara.string(response.body).find(".run_details .duration").tap do |f|
            expect(f).to have_selector("span.label", :text => "Duration:")
            expect(f).to have_selector("span.time", :text => /\d{2}:\d{2}:\d{2}/)
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
            expect(f).to have_selector(".time", :text => /\s+at\s+/)
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
          in_params :action => 'foo'
          view.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
            "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
          end
          view.should_receive(:stage_detail_tab_path).with(:stage_counter => 1, :action => "foo").and_return("url_to_1")
          view.should_receive(:stage_detail_tab_path).with(:stage_counter => 3, :action => "foo").and_return("url_to_3")
          render :template => "stages/stage.html.erb", :layout => "layouts/pipelines.html.erb"

          Capybara.string(response.body).find(".other_runs").tap do |other_runs|
            other_runs.find("li a[href='url_to_1']").tap do |f|
              expect(f).to have_selector "span", :text => "Run: 1 of 3"
              expect(f).to have_selector ".color_code.Passed"
              expect(f).to have_selector ".message", :text => "Passed"
            end
            other_runs.find("li a[href='url_to_3']").tap do |f|
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
        view.stub(:stage_detail_tab_path)
        assign :failing_tests, StageTestRuns.new(12, 0, 0)
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
        view.stub(:stage_detail_tab_path)

        assign :failing_tests, StageTestRuns.new(12, 0, 0)
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
        view.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
          "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
        end

        view.stub(:stage_detail_tab_path).with({:action=>"overview"}).and_return("overview_link")
        view.stub(:stage_detail_tab_path).with({:action=>"pipeline"}).and_return("pipeline_link")
        view.stub(:stage_detail_tab_path).with({:action=>"jobs"}).and_return("jobs_link")
        view.stub(:stage_detail_tab_path).with({:action=>"materials"}).and_return("materials_link")
        view.stub(:stage_detail_tab_path).with({:action=>"tests"}).and_return("fbh_link")
        view.stub(:stage_detail_tab_path).with({:action=>"stage_config"}).and_return("config_link")
        view.should_receive(:stage_detail_tab_path).with(:format => 'json', :action=>'jobs').and_return("link_to_json")
        in_params :action => 'jobs'
        assign :jobs, []
        render

        Capybara.string(response.body).find(".sub_tabs_container").tap do |f|
          expect(f).to have_selector "a[href='pipeline_link']", :text => "Pipeline Dependencies"
          expect(f).to have_selector "a[href='materials_link']", :text => "Materials"
          expect(f).to have_selector ".current a[href='jobs_link']", :text => "Jobs"
          expect(f).to have_selector "a[href='fbh_link']", :text => "Tests"
          expect(f).to have_selector "a[href='overview_link']", :text => "Overview"
          expect(f).to have_selector "a[href='config_link']", :text => "Config"
        end

        Capybara.string(response.body).find("script[type='text/javascript']", :visible => false).tap do |script_tag|
          expect(script_tag.text).to include "link_to_json"
        end
      end
    end

    describe "jobs section" do
      before(:each) do
        assign :stage, stage_with_5_jobs()
        params[:pipeline_name] = "cruise"

        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        view.stub(:stage_detail_tab_path)

        assign :failing_tests, StageTestRuns.new(12, 0, 0)
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

    describe "pipeline tab" do
      before(:each) do
        down1 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down1", "stage", java.util.Date.new())
        down2 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down2", "stage", java.util.Date.new())
        pim = PipelineHistoryMother.singlePipeline("pipline-name", StageInstanceModels.new)
        assign :graph, PipelineDependencyGraphOld.new(pim, PipelineInstanceModels.createPipelineInstanceModels([down1, down2]))
        params[:action] = 'pipeline'
      end

      it "should render PDG" do
        render
        Capybara.string(response.body).find("#pipeline_visualization").tap do |f|
          expect(f).to have_selector ".upstream"
          expect(f).to have_selector ".current"
          expect(f).to have_selector ".downstream"
        end
      end

      it "should render unsupported message if IE8" do
        view.stub(:is_ie8?).and_return(true)
        render

        Capybara.string(response.body).find("div.vsm_not_supported.notification").tap do |f|
          expect(f).to have_selector "p.information", :text => /^[\s\S]*Your browser is not supported. Please either upgrade your browser or use a different browser to view <a href='http:\/\/www.go.cd\/documentation\/user\/current\/navigation\/value_stream_map\.html' target='_blank'>Value Stream Map<\/a>.[\s\S]*$/
        end
      end

      it "should not render unsupported message if IE9 or Firefox or Chrome or Safari or Opera" do
        view.stub(:is_ie8?).and_return(false)
        render

        expect(response).to_not have_selector "div.vsm_not_supported.notification"
      end

    end

    describe "fbh tab" do
      before(:each) do
        params[:action] = 'tests'
        view.stub(:failure_details_path).and_return("/path/to/failures")
      end
      describe :failingTestsWithMultiplePipelines do
        before(:each) do
          job_identifier = JobIdentifier.new(nil, 1, nil, nil, nil, "job-1")
          @failing_tests = StageTestRuns.new(2,1,1)
          @failing_tests.add(10, "1.0", "suite1", "test1-2", TestStatus::Failure, job_identifier)
          @failing_tests.add(10, "1.0", "suite1", "test1-1", TestStatus::Error, job_identifier)
          @failing_tests.add(11, "1.1", "suite1", "test1-1", TestStatus::Error, job_identifier)
          @failing_tests.add(9, "0.9", "suite1", "test1-2", TestStatus::Failure, job_identifier)
          @failing_tests.addUser(10, "1.0", "user1")

          @failing_tests.addUser(10, "1.0", "user2")
          @failing_tests.addUser(11, "1.1", "user3")
          @failing_tests.removeDuplicateTestEntries()

          assign :failing_tests, @failing_tests
          assign :stage, failing_stage("dev")
        end

        describe "fbh partial caching" do
          before(:each) do
            assign :response_format, "html"
          end

          it "should cache fbh partial" do
            @failing_stage = StageMother.custom("dev")
            @failing_stage.getJobInstances().get(0).fail()
            @failing_stage.calculateResult()

            @passed_stage = StageMother.custom("dev")
            @passed_stage.passed()
            @passed_stage.setIdentifier(@failing_stage.getIdentifier())
            check_fragment_caching(@failing_stage, @passed_stage, proc {|stage| [ViewCacheKey.new.forFbhOfStagesUnderPipeline(stage.getIdentifier().pipelineIdentifier()), {:subkey => ViewCacheKey.new.forFailedBuildHistoryStage( stage, "html" )}]}) do |stage|
              assign :stage, stage_model_for(stage)
              render
            end
          end

          it "should use html to scope the key to format" do
            failing_stage = StageMother.custom("dev")
            failing_stage.getJobInstances().get(0).fail()
            failing_stage.calculateResult()

            assign :stage, stage_model_for(failing_stage)
            view.stub(:view_cache_key).and_return(key = double('view_cache_key'))
            key.should_receive(:forFbhOfStagesUnderPipeline).with(failing_stage.getIdentifier().pipelineIdentifier()).and_return("pipeline_id_based_key")
            key.should_receive(:forFailedBuildHistoryStage).with(failing_stage, "html").and_return("stage_fbh_html_key")
            view.should_receive(:cache).with("pipeline_id_based_key", :subkey => "stage_fbh_html_key", :skip_digest=>true)
            render
          end
        end

        it "should contain header text" do
          render
          Capybara.string(response.body).find(".non_passing_tests").tap do |f|
            expect(f).to have_selector ".message", :text => "New Tests Broken Since: (ordered by check-in/material time)"
            expect(f).to have_selector ".counts .total", :text => "Tests Run: 2"
            expect(f).to have_selector ".counts .failures", :text => "Total Failures: 1"
            expect(f).to have_selector ".counts .errors", :text => "Total Errors: 1"
          end
        end

        it "should display message for empty stages" do
          render
          expect(response).to have_selector(".non_passing_tests #failing_pipeline2 .block_to_hide_or_reveal_by_above_pipeline_bar", :text => "These changes did not break any of the currently failing tests.")
        end

        it "should display user that trigger the pipeline" do
          render
          expect(response).to have_selector(".non_passing_tests #failing_pipeline0 .users", :text => "By user1, user2")
          expect(response).to have_selector(".non_passing_tests #failing_pipeline1 .users", :text => "By user3")
          expect(response).to_not have_selector(".non_passing_tests #failing_pipeline2 .users")
        end

        it "should be grouped by pipeline" do
          render

          Capybara.string(response.body).find(".non_passing_tests").tap do |f|
            expect(f).to have_selector ".counts .failures", :text => "Total Failures: 1"
            expect(f).to have_selector ".counts .errors", :text => "Total Errors: 1"
          end
          Capybara.string(response.body).find(".non_passing_tests #failing_pipeline0").tap do |f|
            expect(f).to have_selector ".pipeline_bar .pipeline_label", :text => "Pipeline Label: 1.0"
            expect(f).to have_selector ".pipeline_bar .counts .failures", :text => "Unique Failures: 1"
            expect(f).to have_selector ".pipeline_bar .counts .errors", :text => "Unique Errors: 0"
            expect(f).to have_selector ".suite .suite_name", :text => "suite1"
            f.find(".test_suite").tap do |test_suite|
              expect(test_suite).to have_selector ".test_case .test_name .name", :text => "test1-2"
              expect(test_suite).to_not have_selector ".test_case .test_name .name", :text => "test1-1"

            end
          end
          Capybara.string(response.body).find(".non_passing_tests #failing_pipeline1").tap do |f|
            expect(f).to have_selector ".pipeline_bar .pipeline_label", :text => "Pipeline Label: 1.1"
            expect(f).to have_selector ".pipeline_bar .counts .failures", :text => "Unique Failures: 0"
            expect(f).to have_selector ".pipeline_bar .counts .errors", :text => "Unique Errors: 1"
            expect(f).to have_selector ".suite .suite_name", :text => "suite1"
            f.find(".test_suite").tap do |test_suite|
              expect(test_suite).to_not have_selector ".test_case .test_name .name", :text => "test1-2"
              expect(test_suite).to have_selector ".test_case .test_name .name", :text => "test1-1"
            end
          end

          Capybara.string(response.body).find(".non_passing_tests #failing_pipeline2").tap do |f|
            expect(f).to have_selector ".pipeline_bar .pipeline_label", :text => "Pipeline Label: 0.9"
            expect(f).to have_selector ".pipeline_bar .counts .failures", :text => "Unique Failures: 0"
            expect(f).to have_selector ".pipeline_bar .counts .errors", :text => "Unique Errors: 0"
            expect(f).to_not have_selector ".test_suite"
          end
        end
      end

      describe :testsCounts do
        before(:each) do
          @failing_tests = StageTestRuns.new(2, 0, 0)
          assign :failing_tests, @failing_tests
        end



        it "should show the number of test runs if the stage passed" do
          assign :failing_tests, @failing_tests
          assign :stage, stage_with_all_jobs_passed()

          render
          Capybara.string(response.body).find(".non_passing_tests").tap do |f|
            expect(f).to have_selector "h3 .counts .total", :text => "Tests Run: 2"
            expect(f).to have_selector "h3 .counts .failures", :text => "Total Failures: 0"
            expect(f).to have_selector "h3 .counts .errors", :text => "Total Errors: 0"
            expect(f).to have_selector "h3 .message", :text => "The stage passed"
          end
          expect(response).to_not have_selector(".non_passing_tests .failing_pipeline")
        end

        it "should show a message and the failure/error count if the stage passed" do
          assign :failing_tests, StageTestRuns.new(4, 1, 1)
          assign :stage, stage_with_all_jobs_passed()

          render

          Capybara.string(response.body).find(".non_passing_tests").tap do |f|
            expect(f).to have_selector "h3 .counts .total", :text => "Tests Run: 4"
            expect(f).to have_selector "h3 .counts .failures", :text => "Total Failures: 1"
            expect(f).to have_selector "h3 .counts .errors", :text => "Total Errors: 1"
          end
          Capybara.string(response.body).find(".non_passing_tests").tap do |f|
            expect(f).to have_selector "h3 .message", :text => "Although all the jobs in this stage have passed, there are some tests that have failed."
            expect(f).to have_selector "h3 .message", :text => "This is may be due to the test task configuration in the build script. You might want to fail the build on test failures."
          end
        end
      end

      it "should return no tests configured message" do
        assign :failing_tests, StageTestRuns.new(0, 0, 0)
        render

        Capybara.string(response.body).find(".non_passing_tests").tap do |f|
          expect(f).to have_selector "h3 .message", :text => "There are tests configured in this stage but could not compute results."
        end

        expect(response).to_not have_selector(".non_passing_tests .failing_pipeline")
        expect(response).to_not have_selector(".non_passing_tests .counts")
      end

      describe :failingTests do
        before(:each) do
          @failing_tests = StageTestRuns.new(4, 0, 0)
          job1 = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "job1")
          job2 = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "job2")
          @failing_tests.add(12, "1.2", "suite1", "test1-1", TestStatus::Error, job1)
          @failing_tests.add(12, "1.2", "suite1", "test1-2", TestStatus::Failure, job1)
          @failing_tests.add(12, "1.2", "suite1", "test1-2", TestStatus::Failure, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-1", TestStatus::Error, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-2", TestStatus::Failure, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-2", TestStatus::Error, job1)
          assign :failing_tests, @failing_tests
          stage = failing_stage("dev")
          assign :stage, stage
        end

        it "should render failure message if set" do
          assign :failing_tests_error_message, "Unable to connect to shine"
          render
          expect(response).to have_selector(".non_passing_tests .error", :text => "Unable to connect to shine")
        end

        it "should be grouped by test suite" do
          render
          Capybara.string(response.body).all(".non_passing_tests .suite .suite_name").tap do |f|
            expect(f[0].text).to eq "suite1"
            expect(f[1].text).to eq "suite2"
          end
        end

        it "should show test cases under suite" do
          render
          Capybara.string(response.body).all(".non_passing_tests .test_suite").tap do |test_suites|


            first = test_suites[0]
            second = test_suites[1]

            first.all(".test_case").tap do |test_cases|
              expect(test_cases[0]).to have_selector ".test_name .name", :text => "test1-1"
              expect(test_cases[0]).to have_selector(".jobs a[href='/tab/build/detail/pipeline/1/stage/1/job1']", :text => "job1")
              expect(test_cases[0]).to have_selector(".test_status .Error", :text => /^[\s\S]*$/)

              expect(test_cases[1]).to have_selector ".test_name .name", :text => "test1-2"
              expect(test_cases[1]).to have_selector(".jobs a[href='/tab/build/detail/pipeline/1/stage/1/job1']", :text => "job1")
              expect(test_cases[1]).to have_selector(".jobs a[href='/tab/build/detail/pipeline/1/stage/1/job2']", :text => "job2")
              expect(test_cases[1]).to have_selector(".test_status .Failure", :text => /^[\s\S]*$/)

            end
            second.all(".test_case").tap do |test_cases|
              expect(test_cases[0]).to have_selector ".test_name .name", :text => "test2-1"
              expect(test_cases[0]).to have_selector(".jobs a[href='/tab/build/detail/pipeline/1/stage/1/job2']", :text => "job2")
              expect(test_cases[0]).to have_selector(".test_status .Error", :text => /^[\s\S]*$/)

              expect(test_cases[1]).to have_selector ".test_name .name", :text => "test2-2"
              expect(test_cases[1]).to have_selector(".jobs a[href='/tab/build/detail/pipeline/1/stage/1/job2']", :text => "job2")
              expect(test_cases[1]).to have_selector(".test_status .Failure", :text => /^[\s\S]*$/)

            end
          end
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
          expect(f).to have_selector("#chart_details_container #highcharts")
          expect(f.find("#chart_details_container script[type='text/javascript']", :visible => false).text).to include "tooltipData:"
          expect(f.find("#chart_details_container script[type='text/javascript']", :visible => false).text).to include "data:"
        end
      end

      it "should show prev and next links if page_number is 2" do
        params[:page_number] = "2"

        render

        Capybara.string(response.body).find("#stage_stats .stats").tap do |f|
          expect(f).to have_selector("a[href='#{stage_detail_tab_path(:action => "stats", :page_number => "1")}']", :text => "Newer")
          expect(f).to have_selector("a[href='#{stage_detail_tab_path(:action => "stats", :page_number => "3")}']", :text => "Older")
        end
      end
    end

    describe "config tab" do
      before(:each) do
        params[:action] = "stage_config"
        assign :ran_with_config_revision,  GoConfigRevision.new("config-xml", "my-md5", "loser", "2.3.0", TimeProvider.new);
        view.stub(:is_user_an_admin?).and_return(true)
      end

      it "should render Config tab" do
        render
        expect(response).to have_selector("#ran_with_config .config")
      end

      it "should render Config tab with message if user is not admin" do
        view.stub(:is_user_an_admin?).and_return(false)
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

