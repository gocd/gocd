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

shared_examples_for :stage_parent_tree do
  describe "admin/shared/pipeline_tree" do

    describe "expand-collapse behaviour" do
      it "render the tree view for a pipeline on pipeline general tab" do
        in_params(:stage_parent => @stage_parent)
        in_params(:current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.find("li.collapsable[1]") do |pipeline|
            expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}']", :text => "pipeline")

            pipeline.all("ul.stages li.expandable").tap do |collapsed_stages|
              expect(collapsed_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")

              collapsed_stages[0].all("ul.jobs.hidden").tap do |jobs|
                expect(jobs[0]).to have_selector("a[href='#{admin_tasks_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"tasks")}']", :text => "dev")
              end
            end
          end
        end
      end

      it "render the tree view for a stage" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.find("li.collapsable[1]") do |pipeline|
            expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}']", :text => "pipeline")

            pipeline.all("ul.stages li.expandable").tap do |collapsed_stages|
              expect(collapsed_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")
              expect(collapsed_stages[0]).to have_selector("ul.jobs.hidden")

              expect(collapsed_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings")}']", :text => "stage3")
              expect(collapsed_stages[1]).to have_selector("ul.jobs.hidden")
            end

            pipeline.all("ul.stages li.collapsable").tap do |expanded_stages|
              expect(expanded_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", :text => "stage2")
              expect(expanded_stages[0]).to have_selector("ul.jobs")
              expect(expanded_stages[0]).not_to have_selector("ul.jobs.hidden")
            end
          end
        end
      end
    end

    describe "selection of the current config" do

      it "should render the tree view with a stage selected" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.find("li.collapsable[1]") do |pipeline|
            expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}'][class='parent_selected']")

            pipeline.all(:xpath, ".//ul[@class='stages']/li").tap do |all_stages|
              expect(all_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")

              expect(all_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='selected']")
            end
          end
        end
      end

      it "should retain previously selected tab when navigating from stage to stage" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        # Previously on a stage with Permissions tab highlighted
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "permissions")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.all(:xpath, ".//ul[@class='stages']/li").tap do |all_stages|
            expect(all_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "permissions")}'][class='']")
            expect(all_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "permissions")}'][class='selected']")
          end
        end
      end

      it "should retain 'jobs' tab selection when navigating from stage to stage" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        # Previously on a stage with Permissions tab highlighted
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.all(:xpath, ".//ul[@class='stages']/li").tap do |all_stages|
            expect(all_stages[0]).to have_selector("a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "jobs")}'][class='']")
            expect(all_stages[1]).to have_selector("a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")}'][class='selected']")
          end
        end
      end

      it "should render the tree view with a job selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent, :current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.find("li.collapsable[1]") do |pipeline|
            expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}'][class='parent_selected']")

            pipeline.all(:xpath, ".//ul[@class='stages']/li").tap do |all_stages|
              expect(all_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")
              all_stages[0].find("ul.jobs.hidden") do |jobs|
                expect(jobs).to have_selector("a[class=''][href='#{admin_tasks_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab => "tasks")}']", :text => "dev")
              end

              expect(all_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='parent_selected']")
              all_stages[1].find("ul.jobs") do |jobs|
                expect(jobs).to have_selector("li a[href='#{admin_tasks_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab => "tasks")}'][class='selected']", :text => "dev")
              end
            end
          end
        end
      end

      it "should retain the tab selection when moving from job to job" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent, :current_tab=>"artifacts")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.all("ul.stages li.expandable ul.jobs.hidden").tap do |jobs|
            expect(jobs[0]).to have_selector("a[class=''][href='#{admin_job_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"artifacts")}']", :text => "dev")
          end

          tree_view.all("ul.stages li.collapsable ul.jobs").tap do |jobs|
            expect(jobs[0]).to have_selector("a[href='#{admin_job_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"artifacts")}'][class='selected']", :text => "dev")
          end
        end
      end

      it "should render the tree view with a pipeline selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
          tree_view.find("li.collapsable[1]") do |pipeline|
            expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}'][class='selected']")

            pipeline.all(:xpath, ".//ul[@class='stages']/li").tap do |all_stages|
              expect(all_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")

              expect(all_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='']")
            end
          end
        end
      end
    end

    it "render the tree view for a job" do
      in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent)

      render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

      Capybara.string(response.body).find('ul.pipeline').tap do |tree_view|
        tree_view.find("li.collapsable[1]") do |pipeline|
          expect(pipeline).to have_selector("a[href='#{@stage_parent_edit_path}']", :text => "pipeline")

          pipeline.all("ul.stages li.expandable").tap do |collapsed_stages|
              expect(collapsed_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")
              expect(collapsed_stages[0]).to have_selector("ul.jobs.hidden")

              expect(collapsed_stages[1]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings")}']", :text => "stage3")
              expect(collapsed_stages[1]).to have_selector("ul.jobs.hidden")
          end

          pipeline.all("ul.stages li.collapsable").tap do |expanded_stages|
            expect(expanded_stages[0]).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", :text => "stage2")
            expect(expanded_stages[0]).to have_selector("ul.jobs")
            expect(expanded_stages[0]).not_to have_selector("ul.jobs.hidden")

            expanded_stages[0].find("ul.jobs") do |jobs|
              expect(jobs).to have_selector("a[href='#{admin_tasks_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"tasks")}']", :text => "dev")
            end
          end
        end
      end
    end
  end
end
