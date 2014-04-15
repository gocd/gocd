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

shared_examples_for "stage_parent_tree" do
  describe "admin/shared/pipeline_tree" do

    describe "expand-collapse behaviour" do
      it "render the tree view for a pipeline on pipeline general tab" do
        in_params(:stage_parent => @stage_parent)
        in_params(:current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("li.collapsable") do
            with_tag("a[href='#{@stage_parent_edit_path}']", "pipeline")
            with_tag("ul.stages") do
              with_tag("li.expandable") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", "stage1")
                with_tag("ul.jobs.hidden") do
                  with_tag("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"tasks")}']", "dev")
                end
              end
            end
          end
        end
      end

      it "render the tree view for a stage" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("li.collapsable") do
            with_tag("a[href='#{@stage_parent_edit_path}']", "pipeline")
            with_tag("ul.stages") do
              with_tag("li.expandable") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", "stage1")
                with_tag("ul.jobs.hidden")
              end
              with_tag("li.expandable") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings" )}']", "stage3")
                with_tag("ul.jobs.hidden")
              end
              with_tag("li.collapsable") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", "stage2")
                with_tag("ul.jobs")
                without_tag("ul.jobs.hidden")
              end
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

        response.body.should have_tag("ul.pipeline") do
          with_tag("li") do
            with_tag("a[href='#{@stage_parent_edit_path}'][class=?]", "parent_selected")
            with_tag("ul.stages") do
              with_tag("li") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class=?]", "")
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class=?]", "selected") do
                  without_tag("li a[class='selected']") #make sure no job is selected
                end
              end
            end
          end
        end
      end

      it "should retain previously selected tab when navigating from stage to stage" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        # Previously on a stage with Permissions tab highlighted
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "permissions")
        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("ul.stages") do
            with_tag("li") do
              with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "permissions")}'][class=?]", "")
              with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "permissions")}'][class=?]", "selected")
            end
          end
        end
      end

      it "should retain 'jobs' tab selection when navigating from stage to stage" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        # Previously on a stage with Permissions tab highlighted
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")
        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("ul.stages") do
            with_tag("li") do
              with_tag("li a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "jobs")}'][class=?]", "")
              with_tag("li a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")}'][class=?]", "selected")
            end
          end
        end
      end

      it "should render the tree view with a job selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent, :current_tab=>"tasks")
        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("li") do
            with_tag("a[href='#{@stage_parent_edit_path}'][class=?]", "parent_selected")
            with_tag("ul.stages") do
              with_tag("li") do
                with_tag("li") do
                  with_tag("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class=?]", "")
                  with_tag("ul.jobs.hidden") do
                    with_tag("li a[class=''][href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"tasks")}']", "dev")
                  end
                end
                with_tag("li") do
                  with_tag("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class=?]", "parent_selected")
                  with_tag("ul.jobs") do
                    with_tag("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"tasks")}'][class='selected']", "dev")
                  end
                end
              end
            end
          end
        end
      end

      it "should retain the tab selection when moving from job to job" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent, :current_tab=>"artifacts")
        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("ul.jobs.hidden") do
            with_tag("li a[class=''][href='#{admin_job_edit_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"artifacts")}']", "dev")
          end
        end
        with_tag("li") do
          with_tag("ul.jobs") do
            with_tag("li a[href='#{admin_job_edit_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"artifacts")}'][class='selected']", "dev")
          end
        end
      end

      it "should render the tree view with a pipeline selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :current_tab=>"tasks")
        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        response.body.should have_tag("ul.pipeline") do
          with_tag("li") do
            with_tag("a[href='#{@stage_parent_edit_path}'][class=?]", "selected")
            with_tag("ul.stages") do
              with_tag("li") do
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class=?]", "")
                with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class=?]", "")
              end
            end
          end
        end
      end
    end

    it "render the tree view for a job" do
      in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent)

      render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

      response.body.should have_tag("ul.pipeline") do
        with_tag("li.collapsable") do
          with_tag("a[href='#{@stage_parent_edit_path}']", "pipeline")
          with_tag("ul.stages") do
            with_tag("li.expandable") do
              with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", "stage1")
              with_tag("ul.jobs.hidden")
            end
            with_tag("li.expandable") do
              with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings")}']", "stage3")
              with_tag("ul.jobs.hidden")
            end
            with_tag("li.collapsable") do
              with_tag("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", "stage2")
              with_tag("ul.jobs")
              without_tag("ul.jobs.hidden")
              with_tag("ul.jobs") do
                with_tag("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"tasks")}']", "dev")
              end
            end
          end
        end
      end
    end
  end
end
