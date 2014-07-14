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

require File.join(File.dirname(__FILE__), "..", "..", "spec_helper")
load File.join(File.dirname(__FILE__), "..",  "auto_refresh_examples.rb")

describe 'stages/stage.html.erb' do

  include StageModelMother, GoUtil, JobMother, PipelineModelMother, ApplicationHelper

  before do
    class << template
      include ApplicationHelper
      include StageModelMother
    end

    stub_server_health_messages
    template.stub!(:is_user_an_admin?).and_return(true)
    template.stub!(:config_change_path)

    in_params :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 3
    @stage_history_page = assigns[:stage_history_page] = stage_history_page(10)
    empty_material_revision_arr = [].to_java(MaterialRevision)
    @revisions = MaterialRevisions.new( empty_material_revision_arr)
    @revisions.addAll(ModificationsMother.multipleModifications())
    @revisions.addAll(ModificationsMother.multipleModifications(MaterialsMother.hgMaterial()))
    template.stub(:render_comment).and_return("link to traker")
    assigns[:pipeline] = @pipeline =  pipeline_model("pipeline_name", "blah_label", false, false, "working with agent", false, @revisions).getLatestPipelineInstance()
    assigns[:current_tab] =  "overview"
    assigns[:fbh_pipeline_instances] = []
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
    before do
      params[:action] = "overview"
      template.stub!(:can_view_admin_page?).and_return(false)
      assigns[:user] = Username.new(CaseInsensitiveString.new("bob"))
    end

    describe "stage html" do
      before do
        assigns[:stage] = stage_with_three_runs()
        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        template.stub!(:stage_detail_path)
        assigns[:failing_tests] = StageTestRuns.new(12, 0, 0)
      end

      it "should render stage history" do
        render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
        response.capture(:rail).should have_tag "#stage-history-page[type='hidden'][value='" + @stage_history_page.currentPage().to_s() +"']"
        response.capture(:rail).should have_tag "#stage_history"
      end

      it "should render latest materials revisions" do
        render 'stages/stage.html.erb'
        @revisions.each do |revision|
          latest = revision.getLatestModification()
          revision.getModifications().each do |mod|
            if mod==latest
              response.should have_tag ".change .revision dd", mod.getRevision()
            else
              response.should_not have_tag ".change .revision dd", mod.getRevision()
            end
          end
        end
        response.should_not have_tag ".modified_files"
      end

      describe :auto_refresh do
        before do
          @partial = 'stages/stage.html.erb'
          @ajax_refresher = /StageDetailAjaxRefresher/
        end

        it_should_behave_like "auto_refresh"
      end

      it "should set title" do
        render 'stages/stage.html.erb'
        assigns[:view_title].should == 'Stage Detail'
      end

      describe :title_bar do

        it "should show stage result" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag ".result" do
            with_tag ".Passed"
            with_tag ".message", "Passed"
          end
        end

        it "should show build cause message" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag ".run_details .schedule_info" do
            with_tag("span.label", "Automatically triggered")
            with_tag("span.time", /\s+at\s+/)
          end
        end

        it "should show duration" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag ".run_details .duration" do
            with_tag("span.label", "Duration:")
            with_tag("span.time", /\d{2}:\d{2}:\d{2}/)
          end
        end

        it "should show compare link" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"

          status_bar = response.capture(:status_bar)

          status_bar.should have_tag ".run_details" do
            with_tag("span.compare_pipeline a[href='/compare/cruise/0/with/1']", "Compare")
          end
        end

        it "should show the schedule info" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
#          puts response.body
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag(".schedule_info") do
            with_tag(".label", "Automatically triggered")
            with_tag(".time", /\s+at\s+/)
          end
        end

        it "should show this stage run among others in its pipeline instance" do
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag ".run_results .run" do
            with_tag("span.label", "Run:")
            with_tag("#show_other_stage_runs", "2 of 3")
          end
        end

        it "should show links to other stage runs" do
          in_params :action => 'foo'
          template.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
            "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
          end
          template.should_receive(:stage_detail_tab_path).with(:stage_counter => 1, :action => "foo").and_return("url_to_1")
          template.should_receive(:stage_detail_tab_path).with(:stage_counter => 3, :action => "foo").and_return("url_to_3")
          render 'stages/stage.html.erb', :layout => "pipelines.html.erb"
          status_bar = response.capture(:status_bar)
          status_bar.should have_tag(".other_runs") do |other_runs|
            other_runs[0].should have_tag "li" do |li|
                  li[0].should have_tag("a[href=url_to_1]") do |tag|
                    tag.should have_tag("span","Run: 1 of 3")
                    tag.should have_tag(".color_code.Passed")
                    tag.should have_tag(".message", "Passed")
                  end
                  li[1].should have_tag("a[href=url_to_3]") do |tag|
                    tag.should have_tag("span","Run: 3 of 3")
                    tag.should have_tag(".color_code.Failed")
                    tag.should have_tag(".message", "Failed")
                  end
            end
          end
        end
      end
    end

    describe "jobs section with all jobs passed" do
      before do
        assigns[:stage] = stage_with_all_jobs_passed()

        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        template.stub!(:stage_detail_path)

        assigns[:failing_tests] = StageTestRuns.new(12, 0, 0)
      end

      it "should expand passed section if all jobs passed" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs .jobs_passed" do |jobs|
          jobs.should_not have_tag(".hidereveal_collapsed")
        end
        response.body.should have_tag ".jobs .jobs_failed" do |jobs|
          jobs.should have_tag(".hidereveal_collapsed")
        end
        response.body.should have_tag ".jobs .jobs_in_progress" do |jobs|
          jobs.should have_tag(".hidereveal_collapsed")
        end
      end

    end

    describe "jobs section with passed jobs" do
      before do
        assigns[:stage] = stage_with_three_runs()

        params[:pipeline_name] = "cruise"
        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        template.stub!(:stage_detail_path)

        assigns[:failing_tests] = StageTestRuns.new(12, 0, 0)
      end

      it "should hide in progress section if no jobs are in progress" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs .jobs_in_progress" do |jobs|
          jobs.should have_tag(".hidereveal_collapsed")
        end
      end

      it "should hide failed section if no jobs failed" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs .jobs_failed" do |jobs|
          jobs.should have_tag(".hidereveal_collapsed")
        end
      end

      it "should render urls" do
        template.stub(:stage_detail_tab_path).with(instance_of(Hash)).and_return do |params|
          "url_for_#{params[:pipeline_counter]}_run_#{params[:stage_counter]}"
        end

        template.stub!(:stage_detail_tab_path).with({:action=>"overview"}).and_return("overview_link")
        template.stub!(:stage_detail_tab_path).with({:action=>"pipeline"}).and_return("pipeline_link")
        template.stub!(:stage_detail_tab_path).with({:action=>"jobs"}).and_return("jobs_link")
        template.stub!(:stage_detail_tab_path).with({:action=>"materials"}).and_return("materials_link")
        template.stub!(:stage_detail_tab_path).with({:action=>"tests"}).and_return("fbh_link")
        template.stub!(:stage_detail_tab_path).with({:action=>"config"}).and_return("config_link")
        template.should_receive(:stage_detail_tab_path).with(:format => 'json', :action=>'jobs').and_return("link_to_json")
        in_params :action => 'jobs'
        assigns[:jobs] = []
        render 'stages/stage.html.erb'

        response.should have_tag ".sub_tabs_container" do
          with_tag "a[href='pipeline_link']", "Pipeline Dependencies"
          with_tag "a[href='materials_link']", "Materials"
          with_tag ".current a[href='jobs_link']", "Jobs"
          with_tag "a[href='fbh_link']", "Tests"
          with_tag "a[href='overview_link']", "Overview"
          with_tag "a[href='config_link']", "Config"
        end
        response.should have_tag("script[type='text/javascript']", /link_to_json/)
      end
    end

    describe "jobs section" do
      before do
        assigns[:stage] = stage_with_5_jobs()
        params[:pipeline_name] = "cruise"

        params[:pipeline_counter] = "1"
        params[:stage_name] = "dev"
        template.stub!(:stage_detail_path)

        assigns[:failing_tests] = StageTestRuns.new(12, 0, 0)
      end

      it "should expand in progress section if any jobs are in progress" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs .jobs_in_progress" do |jobs|
          jobs.should_not have_tag(".hidereveal_collapsed")
        end
      end

      it "should expand failed section if any jobs failed" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs .jobs_failed" do |jobs|
          jobs.should_not have_tag(".hidereveal_collapsed")
        end
      end

      it "should display jobs sections" do
        render 'stages/stage.html.erb'
        response.body.should have_tag ".jobs" do
          with_tag ".jobs_failed" do
            with_tag "span", "Failed: 2"
            with_tag(".job") do
              with_tag("a", "first")
              with_tag(".color_code_small.Failed")
            end
            with_tag(".job") do
              with_tag("a", "fifth")
              with_tag(".color_code_small.Failed")
            end
          end
          with_tag ".jobs_passed"  do
            with_tag "span", "Passed: 2"
            with_tag(".job") do
              with_tag("a", "second")
              with_tag(".color_code_small.Passed")
            end
            with_tag(".job") do
              with_tag("a", "third")
              with_tag(".color_code_small.Passed")
            end
          end
          with_tag ".jobs_in_progress" do
            with_tag "span", "In Progress: 1"
            with_tag(".job") do
              with_tag("a", /fourth/)
              with_tag(".color_code_small.Active")
            end
          end

          with_tag ".hidereveal_collapsed" do |job_groups|
            job_groups.each do |job_group|
              job_group.should have_tag ".hidereveal_expander"
              job_group.should have_tag ".hidereveal_content"
              job_group.to_s.should =~/make_collapsable/
            end
          end
        end
      end

    end
  end
  describe "non overview tabs" do

    before do
      assigns[:stage] = stage_with_three_runs()
    end

    describe "jobs tab" do
      before do
        params[:action] = "jobs"

        assigns[:jobs] = jobs_model
      end

      it "should render jobs tab" do
        render 'stages/stage.html.erb'
        response.should have_tag ".jobs_summary .job .job_name a", "first"
      end

    end

    describe "materials tab" do
      before do
        params[:action] = "materials"
      end

      it "should render jobs tab" do
        render 'stages/stage.html.erb'
        cnt_modifications = 0
        @revisions.each do |revision|
          revision.getModifications().each do |mod|
            response.should have_tag ".change .revision dd", mod.getRevision()
            cnt_modifications += 1
          end
        end
        response.should have_tag ".modified_files" do |modified_files|
          modified_files.size.should == cnt_modifications
        end
      end
    end

    describe "pipeline tab" do
      before do
        down1 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down1", "stage", java.util.Date.new())
        down2 = PipelineHistoryMother.pipelineHistoryItemWithOneStage("down2", "stage", java.util.Date.new())
        pim = PipelineHistoryMother.singlePipeline("pipline-name", StageInstanceModels.new)
        assigns[:graph] = PipelineDependencyGraphOld.new(pim, PipelineInstanceModels.createPipelineInstanceModels([down1, down2]))
        params[:action] = 'pipeline'
      end

      it "should render PDG" do
        render 'stages/stage.html.erb'
        response.should have_tag "#pipeline_visualization" do
          with_tag ".upstream"
          with_tag ".current"
          with_tag ".downstream"
        end
        response.capture(:rail).should be_nil
      end

      it "should render unsupported message if IE8" do
        template.stub!(:is_ie8?).and_return(true)

        render 'stages/stage.html.erb'

        response.should have_tag "div.vsm_not_supported.notification" do
          with_tag("p.information", "Your browser is not supported. Please either upgrade your browser or use a different browser to view Value Stream Map.")
        end
      end

      it "should not render unsupported message if IE9 or Firefox or Chrome or Safari or Opera" do
        template.stub!(:is_ie8?).and_return(false)

        render 'stages/stage.html.erb'

        response.should_not have_tag "div.vsm_not_supported.notification"
      end

    end

    describe "fbh tab" do
      before do
        params[:action] = 'tests'
        template.stub!(:failure_details_path).and_return("/path/to/failures")
      end
      describe :failingTestsWithMultiplePipelines do
        before do
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

          assigns[:failing_tests] = @failing_tests
          assigns[:stage] = failing_stage("dev")
        end

        describe "fbh partial caching" do
          before do
            assigns[:response_format] = "html"
          end

          it "should cache fbh partial" do
            @failing_stage = StageMother.custom("dev")
            @failing_stage.getJobInstances().get(0).fail()
            @failing_stage.calculateResult()

            @passed_stage = StageMother.custom("dev")
            @passed_stage.passed()
            @passed_stage.setIdentifier(@failing_stage.getIdentifier())
            check_fragment_caching(@failing_stage, @passed_stage, proc {|stage| [ViewCacheKey.new.forFbhOfStagesUnderPipeline(stage.getIdentifier().pipelineIdentifier()), {:subkey => ViewCacheKey.new.forFailedBuildHistoryStage( stage, "html" )}]}) do |stage|
              assigns[:stage] = stage_model_for(stage)
              render 'stages/stage.html.erb'
            end
          end

          it "should use html to scope the key to format" do
            failing_stage = StageMother.custom("dev")
            failing_stage.getJobInstances().get(0).fail()
            failing_stage.calculateResult()

            assigns[:stage] = stage_model_for(failing_stage)
            template.stub!(:view_cache_key).and_return(key = mock('view_cache_key'))
            key.should_receive(:forFbhOfStagesUnderPipeline).with(failing_stage.getIdentifier().pipelineIdentifier()).and_return("pipeline_id_based_key")
            key.should_receive(:forFailedBuildHistoryStage).with(failing_stage, "html").and_return("stage_fbh_html_key")
            template.should_receive(:cache).with("pipeline_id_based_key", :subkey => "stage_fbh_html_key")
            render 'stages/stage.html.erb'
          end
        end

        it "should contain header text" do
          render 'stages/stage.html.erb'
          response.should have_tag(".non_passing_tests") do
            with_tag(".message",  "New Tests Broken Since: (ordered by check-in/material time)")
            with_tag(".counts .total",  "Tests Run: 2")
            with_tag(".counts .failures",  "Total Failures: 1")
            with_tag(".counts .errors",  "Total Errors: 1")
          end
        end

        it "should display message for empty stages" do
          render 'stages/stage.html.erb'
          response.should have_tag(".non_passing_tests #failing_pipeline2 .block_to_hide_or_reveal_by_above_pipeline_bar", "These changes did not break any of the currently failing tests.")
        end

        it "should display user that trigger the pipeline" do
          render 'stages/stage.html.erb'
          response.should have_tag(".non_passing_tests #failing_pipeline0 .users", "By user1, user2")
          response.should have_tag(".non_passing_tests #failing_pipeline1 .users", "By user3")
          response.should_not have_tag(".non_passing_tests #failing_pipeline2 .users")
        end

        it "should be grouped by pipeline" do
          render 'stages/stage.html.erb'

          response.should have_tag(".non_passing_tests") do
            with_tag(".counts .failures",  "Total Failures: 1")
            with_tag(".counts .errors",  "Total Errors: 1")
          end
          response.should have_tag(".non_passing_tests #failing_pipeline0") do
            with_tag(".pipeline_bar .pipeline_label", "Pipeline Label: 1.0")
            with_tag(".pipeline_bar .counts .failures", "Unique Failures: 1")
            with_tag(".pipeline_bar .counts .errors", "Unique Errors: 0")
            with_tag(".suite .suite_name", "suite1")
            with_tag(".test_suite") do
              with_tag(".test_case .test_name .name", "test1-2")
              without_tag(".test_case .test_name .name", "test1-1")
            end
          end

          response.should have_tag(".non_passing_tests #failing_pipeline1") do
            with_tag(".pipeline_bar .pipeline_label", "Pipeline Label: 1.1")
            with_tag(".pipeline_bar .counts .failures", "Unique Failures: 0")
            with_tag(".pipeline_bar .counts .errors", "Unique Errors: 1")
            with_tag(".suite .suite_name", "suite1")
            with_tag(".test_suite") do
              without_tag(".test_case .test_name .name", "test1-2")
              with_tag(".test_case .test_name .name", "test1-1")
            end
          end

          response.should have_tag(".non_passing_tests #failing_pipeline2") do
            with_tag(".pipeline_bar .pipeline_label", "Pipeline Label: 0.9")
            with_tag(".pipeline_bar .counts .failures", "Unique Failures: 0")
            with_tag(".pipeline_bar .counts .errors", "Unique Errors: 0")
            without_tag(".test_suite")
          end
        end
      end

      describe :testsCounts do
        before do
          @failing_tests = StageTestRuns.new(2, 0, 0)
          assigns[:failing_tests] = @failing_tests
        end



        it "should show the number of test runs if the stage passed" do
          assigns[:failing_tests] = @failing_tests
          assigns[:stage] = stage_with_all_jobs_passed()

          render 'stages/stage.html.erb'

          response.should have_tag(".non_passing_tests") do
            with_tag("h3 .counts .total", "Tests Run: 2")
            with_tag("h3 .counts .failures", "Total Failures: 0")
            with_tag("h3 .counts .errors", "Total Errors: 0")
            with_tag("h3 .message", "The stage passed")
          end
          response.should_not have_tag(".non_passing_tests .failing_pipeline")
        end

        it "should show a message and the failure/error count if the stage passed" do
          assigns[:failing_tests] = StageTestRuns.new(4, 1, 1)
          assigns[:stage] = stage_with_all_jobs_passed()

          render 'stages/stage.html.erb'

          response.should have_tag(".non_passing_tests") do
            with_tag("h3 .counts .total", "Tests Run: 4")
            with_tag("h3 .counts .failures", "Total Failures: 1")
            with_tag("h3 .counts .errors", "Total Errors: 1")
          end
          response.should have_tag(".non_passing_tests") do
            with_tag("h3 .message", "Although all the jobs in this stage have passed, there are some tests that have failed.")
            with_tag("h3 .message", "This is may be due to the test task configuration in the build script. You might want to fail the build on test failures.")
          end
        end
      end

      it "should return no tests configured message" do
        assigns[:failing_tests] = StageTestRuns.new(0, 0, 0)
        render 'stages/stage.html.erb'

        response.should have_tag(".non_passing_tests") do
          with_tag("h3 .message", "There are tests configured in this stage but could not compute results.")
        end

        response.should_not have_tag(".non_passing_tests .failing_pipeline")
        response.should_not have_tag(".non_passing_tests .counts")
      end

      describe :failingTests do
        before do
          @failing_tests = StageTestRuns.new(4, 0, 0)
          job1 = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "job1")
          job2 = JobIdentifier.new('pipeline', 1, "label", "stage", "1", "job2")
          @failing_tests.add(12, "1.2", "suite1", "test1-1", TestStatus::Error, job1)
          @failing_tests.add(12, "1.2", "suite1", "test1-2", TestStatus::Failure, job1)
          @failing_tests.add(12, "1.2", "suite1", "test1-2", TestStatus::Failure, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-1", TestStatus::Error, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-2", TestStatus::Failure, job2)
          @failing_tests.add(12, "1.2", "suite2", "test2-2", TestStatus::Error, job1)
          assigns[:failing_tests] = @failing_tests
          stage = failing_stage("dev")
          assigns[:stage] = stage
        end

        it "should render failure message if set" do
          assigns[:failing_tests_error_message] = "Unable to connect to shine"
          render 'stages/stage.html.erb'
          response.should have_tag(".non_passing_tests .error", "Unable to connect to shine")
        end

        it "should be grouped by test suite" do
          render 'stages/stage.html.erb'
          response.should have_tag(".non_passing_tests") do
            with_tag(".suite .suite_name", "suite1")
            with_tag(".suite .suite_name", "suite2")
          end
        end

        it "should show test cases under suite" do
          render 'stages/stage.html.erb'

          response.should have_tag(".non_passing_tests") do
            with_tag(".test_suite") do
              with_tag(".test_case") do
                with_tag(".test_name .name", "test1-1")
                with_tag(".jobs") do
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job1']", "job1")
                end
                with_tag(".test_status .Error", "&nbsp;")
              end
              with_tag(".test_case") do
                with_tag(".test_name .name", "test1-2")
                with_tag(".jobs") do
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job1']", "job1")
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job2']", "job2")
                end
                with_tag(".test_status .Failure", "&nbsp;")
              end
            end
            with_tag(".test_suite") do
              with_tag(".test_case") do
                with_tag(".test_name .name", "test2-1")
                with_tag(".jobs") do
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job2']", "job2")
                end
                with_tag(".test_status .Error", "&nbsp;")
              end
              with_tag(".test_case") do
                with_tag(".test_name .name", "test2-2")
                with_tag(".jobs") do
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job1']", "job1")
                  with_tag("a[href='/tab/build/detail/pipeline/1/stage/1/job2']", "job2")
                end
                with_tag(".test_status .Failure", "&nbsp;")
              end
            end
          end
        end
      end
    end

    describe "stats tab" do
      before do
        params[:action] = "stats"
        assigns[:chart_stage_duration] = [{"link"=> "/pipelines/pipeline-name/1/stage/1", "x" => 1, "y"=>10.0}, {"link" => "/pipelines/pipeline-name/2/stage/1", "x" => 2, "y"=> 20.0}].to_json
        assigns[:chart_tooltip_data] = {"1_60" => ["00:10:00", "22 Feb, 2008 at 10:21:23 [+0530]", "LABEL-1"], "2_120" => ["00:20:00", "22 Feb, 2008 at 10:21:23 [+0530]", "LABEL-2"]}.to_json
        assigns[:pagination] = Pagination.pageStartingAt(12, 200, 10)
        assigns[:start_end_dates] = ["start date", "end date"]
      end

      it "should render stats tab with chart for given stage" do
        render 'stages/stage.html.erb'

        response.body.should have_tag "#stage_stats .stats" do
          with_tag("#chart_details_container #highcharts")
          with_tag("#chart_details_container script[type='text/javascript']", /tooltipData:/)
          with_tag("#chart_details_container script[type='text/javascript']", /data:/)
        end
      end

      it "should show prev and next links if page_number is 2" do
        params[:page_number] = "2"

        render 'stages/stage.html.erb'

        response.body.should have_tag "#stage_stats .stats" do
          with_tag("a[href='#{stage_detail_tab_path(:action => "stats", :page_number => "1")}']", "Newer")
          with_tag("a[href='#{stage_detail_tab_path(:action => "stats", :page_number => "3")}']", "Older")
        end
      end
    end

    describe "config tab" do
      before do
        params[:action] = "config"
        assigns[:ran_with_config_revision] =  GoConfigRevision.new("config-xml", "my-md5", "loser", "2.3.0", com.thoughtworks.go.licensing.Edition::Enterprise, TimeProvider.new);
        template.stub(:is_user_an_admin?).and_return(true)
      end

      it "should render Config tab" do
        render 'stages/stage.html.erb'
        response.body.should have_tag("#ran_with_config .config")
      end

      it "should render Config tab with message if user is not admin" do
        template.stub(:is_user_an_admin?).and_return(false)
        render 'stages/stage.html.erb'
        response.body.should have_tag("#ran_with_config .config") do
           with_tag("div.notification p.information", "Historical configuration is available only for Go Administrators.")
        end
      end

      it "should render the config file contents in config tab" do
        render 'stages/stage.html.erb'
        response.body.should have_tag("#ran_with_config div.config") do
          with_tag("pre#content_container.wrap_pre", "config-xml")
        end
      end

      it "should render information message if no revision is found" do
        assigns[:ran_with_config_revision] = nil
        render 'stages/stage.html.erb'
        response.body.should have_tag("#ran_with_config div.config") do
          with_tag("div.notification p.information", "Historical configuration is not available for this stage run.")
        end
      end
    end
  end
end

