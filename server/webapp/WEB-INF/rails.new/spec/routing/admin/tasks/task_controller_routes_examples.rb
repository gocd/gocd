##########################GO-LICENSE-START################################
# Copyright 2017 ThoughtWorks, Inc.
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

shared_examples_for :task_controller_routes do
  describe "routes" do
    describe "index" do
      it "should resolve templates as :stage_parent" do
        expect({:get => "/admin/templates/dev.foo/stages/test.bar/job/job-1.baz/tasks"}).to route_to(:controller => "admin/tasks", :action => "index", :stage_parent => "templates", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :current_tab => "tasks")
      end

      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks"}).to route_to(:controller => "admin/tasks", :action => "index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_tasks_listing_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :stage_parent => "pipelines", :current_tab => "tasks")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks")
      end
    end

    describe "increment_index" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/increment"}).to route_to(:controller => "admin/tasks", :action => "increment_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_increment_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/increment")
      end
    end

    describe "decrement_index" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/task/1/index/decrement"}).to route_to(:controller => "admin/tasks", :action => "decrement_index", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_decrement_index_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => "1", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/task/1/index/decrement")
      end
    end

    describe "edit" do
      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1/edit"}).to route_to(:controller => "admin/tasks", :action => "edit", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_edit_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/2/edit")
      end

      it "should only accept numerical task_index(s)" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello/edit")
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/100abc200/edit")
      end
    end

    describe "delete" do
      it "should resolve" do
        expect({:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/1"}).to route_to(:controller => "admin/tasks", :action => "destroy", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_delete_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 2, :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/2")
      end

      it "should only accept numerical task_index(s)" do
        expect({:delete => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello"}).to route_to(:controller => "application", :action => "unresolved", :url => "admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/hello")
      end
    end

    describe "update" do
      it "should resolve" do
        expect({:put => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/1"}).to route_to(:controller => "admin/tasks", :action => "update", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :task_index => "1", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_update_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :task_index => 1, :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/1")
      end

    end

    describe "new" do
      it "should resolve" do
        expect({:get => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}/new"}).to route_to(:controller => "admin/tasks", :action => "new", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_new_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}/new")
      end

    end

    describe "create" do
      it "should resolve" do
        expect({:post => "/admin/pipelines/dev.foo/stages/test.bar/job/job-1.baz/tasks/#{@task_type}"}).to route_to(:controller => "admin/tasks", :action => "create", :pipeline_name => "dev.foo", :stage_name => "test.bar", :job_name => "job-1.baz", :type => "#{@task_type}", :stage_parent => "pipelines", :current_tab => "tasks")
      end

      it "should generate" do
        expect(admin_task_create_path(:pipeline_name => "foo.bar", :stage_name => "baz", :job_name => "quux", :type => "#{@task_type}", :stage_parent => "pipelines")).to eq("/admin/pipelines/foo.bar/stages/baz/job/quux/tasks/#{@task_type}")
      end

    end
  end
end
