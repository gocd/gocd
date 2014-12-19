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

shared_examples_for :stage_parent_tree do
  describe "admin/shared/pipeline_tree" do

    describe "expand-collapse behaviour" do
      it "render the tree view for a pipeline on pipeline general tab" do
        in_params(:stage_parent => @stage_parent)
        in_params(:current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.find("li.collapsable") do |li_1|
            expect(li_1).to have_selector("a[href='#{@stage_parent_edit_path}']", "pipeline")
            li_1.find("ul.stages") do |ul_2|
              ul_2.find("li.expandable") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")
                li_2.find("ul.jobs.hidden") do |ul_3|
                  expect(ul_3).to have_selector("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"tasks")}']", :text => "dev")
                end
              end
            end
          end
        end
      end

      it "render the tree view for a stage" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.all("li.collapsable") do |li_1s|
            expect(li_1s[0]).to have_selector("a[href='#{@stage_parent_edit_path}']", "pipeline")
            li_1s[0].find("ul.stages") do |ul_2|
              ul_2.find("li.expandable") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")
                expect(li_2).to have_selector("ul.jobs.hidden")
              end
              ul_2.find("li.expandable") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings" )}']", :text => "stage3")
                expect(li_2).to have_selector("ul.jobs.hidden")
              end
              ul_2.find("li.collapsable") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", :text => "stage2")
                expect(li_2).to have_selector("ul.jobs")

                expect(li_2).not_to have_selector("ul.jobs.hidden")
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

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.all("li") do |li_1s|
            expect(li_1s[0]).to have_selector("a[href='#{@stage_parent_edit_path}'][class='parent_selected']")
            li_1s[0].find("ul.stages") do |ul_2|
              ul_2.find("li") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")
                li_2.find("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='selected']") do |a|
                  expect(a).not_to have_selector("li a[class='selected']") #make sure no job is selected
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

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.find("ul.stages") do |ul_2|
            ul_2.find("li") do |li_2|
              expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "permissions")}'][class='']")
              expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "permissions")}'][class='selected']")
            end
          end
        end
      end

      it "should retain 'jobs' tab selection when navigating from stage to stage" do
        pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2"].to_java(java.lang.String))

        # Previously on a stage with Permissions tab highlighted
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.find("ul.stages") do |ul_2|
            ul_2.find("li") do |li_2|
              expect(li_2).to have_selector("li a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "jobs")}'][class='']")
              expect(li_2).to have_selector("li a[href='#{admin_job_listing_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "jobs")}'][class='selected']")
            end
          end
        end
      end

      it "should render the tree view with a job selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent, :current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.all("li") do |li_1s|
            expect(li_1s[0]).to have_selector("a[href='#{@stage_parent_edit_path}'][class=?]", "parent_selected")
            li_1s[0].find("ul.stages") do |ul_2|
              ul_2.find("li") do |li_2|
                li_2.find("li") do |li_3|
                  expect(li_3).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")
                  li_3.find("ul.jobs.hidden") do |ul_4|
                    expect(ul_4).to have_selector("li a[class=''][href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"tasks")}']", :text => "dev")
                  end
                end
                li_2.find("li") do |li_3|
                  expect(li_3).to have_selector("a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='parent_selected']")
                  li_3.find("ul.jobs") do |ul_4|
                    expect(ul_4).to have_selector("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"tasks")}'][class='selected']", :text => "dev")
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

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.all("ul.jobs.hidden") do |ul_2s|
            expect(ul_2s[0]).to have_selector("li a[class=''][href='#{admin_job_edit_path(:pipeline_name => "pipeline", :stage_name => "stage1", :job_name => "dev", :current_tab=>"artifacts")}']", :text => "dev""dev")
          end
        end
        Capybara.string(response.body).all('li').tap do |li_1s|
          li_1s[0].all("ul.jobs") do |ul_2s|
            expect(ul_2s[1]).to have_selector("li a[href='#{admin_job_edit_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"artifacts")}'][class='selected']", :text => "dev""dev")
          end
        end
      end

      it "should render the tree view with a pipeline selected" do
        in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :current_tab=>"tasks")

        render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

        Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
          ul_1.all("li") do |li_1s|
            expect(li_1s[0]).to have_selector("a[href='#{@stage_parent_edit_path}'][class='selected']")
            li_1s[0].find("ul.stages") do |ul_2|
              ul_2.find("li") do |li_2|
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}'][class='']")
                expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}'][class='']")
              end
            end
          end
        end
      end
    end

    it "render the tree view for a job" do
      in_params(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :stage_parent => @stage_parent)

      render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => @pipeline, :stage_parent => @stage_parent}}

      Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
        ul_1.all("li.collapsable") do |li_1s|
          expect(li_1s[0]).to have_selector("a[href='#{@stage_parent_edit_path}']", :text => "pipeline")
          li_1s[0].find("ul.stages") do |ul_2|
            ul_2.find("li.expandable") do |li_2|
              expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage1", :current_tab => "settings")}']", :text => "stage1")
              expect(li_2).to have_selector("ul.jobs.hidden")
            end
            ul_2.find("li.expandable") do |li_2|
              expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage3", :current_tab => "settings")}']", :text => "stage3")
              expect(li_2).to have_selector("ul.jobs.hidden")
            end
            ul_2.find("li.collapsable") do |li_2|
              expect(li_2).to have_selector("li a[href='#{admin_stage_edit_path(:stage_parent => @stage_parent, :pipeline_name => "pipeline", :stage_name => "stage2", :current_tab => "settings")}']", :text => "stage2")
              expect(li_2).to have_selector("ul.jobs")
              expect(li_2).not_to have_selector("ul.jobs.hidden")

              li_2.find("ul.jobs") do |ul_3|
                expect(ul_3).not_to have_selector("li a[href='#{admin_tasks_listing_path(:pipeline_name => "pipeline", :stage_name => "stage2", :job_name => "dev", :current_tab=>"tasks")}']", :text => "dev")
              end
            end
          end
        end
      end
    end
  end
end
