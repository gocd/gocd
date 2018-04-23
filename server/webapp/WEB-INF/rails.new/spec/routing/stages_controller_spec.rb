##########################################################################
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
##########################################################################

require 'rails_helper'

describe StagesController do

  describe "stage" do

    it "should resolve url with dots in pipline and stage name" do
      expect(:get => "/pipelines/blah.pipe-line/1/_blah.stage/1").to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "blah.pipe-line", :stage_name => "_blah.stage", :pipeline_counter => "1", :stage_counter => "1"})
      expect(:get => stage_detail_tab_default_path(:pipeline_counter => "1", :pipeline_name => "blah.pipe-line", :stage_counter => "1", :stage_name => "_blah.stage")).to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "blah.pipe-line", :stage_name => "_blah.stage", :pipeline_counter => "1", :stage_counter => "1"})
    end

    it "should resolve piplines/stage-locator to the stage action" do
      expect(:get => "/pipelines/pipeline_name/10/stage_name/2").to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2"})
      expect(:get => stage_detail_tab_default_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2")).to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2"})
    end

    it "should json url for stage" do
      expect(:get => "/pipelines/pipeline_name/10/stage_name/2.json").to route_to({:controller => "stages", :action => "overview", :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "json"})
      expect(:get => stage_detail_tab_default_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "json")).to route_to({:controller => "stages", :action => "overview", :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "json"})
    end

    it "should resolve 'rerun jobs' action" do
      expect(:post => "/pipelines/pipeline_name/10/stage_name/2/rerun-jobs").to route_to({:controller => "stages", :action => "rerun_jobs", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2"})
      expect(:post => rerun_jobs_path({:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 2})).to route_to({:controller => "stages", :action => "rerun_jobs", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2"})
    end

    it "should generate stage details url for a tab" do
      expect(:get => stage_detail_tab_jobs_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 2)).to route_to({:controller => "stages", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2", :action => "jobs"})
      expect(:get => stage_detail_tab_jobs_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 2, :format => 'json')).to route_to({:controller => "stages", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2", :action => "jobs", :format => "json"})
      expect(:get => stage_detail_tab_overview_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 2)).to route_to({:controller => "stages", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2", :action => "overview"})
      expect(:get => stage_detail_tab_overview_path(:pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => 10, :stage_counter => 2, :format => 'json')).to route_to({:controller => "stages", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "2", :action => "overview", :format => "json"})

      expect(:get => "/pipelines/pipeline_name/10/stage_name/5/jobs").to route_to({:controller => "stages", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "5", :action => "jobs"})
    end

    it "should resolve piplines/stage-locator to the stage action" do
      expect(:get => "/pipelines/pipeline_name/10/stage_name/2.xml").to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "xml"})

      expect(:get => stage_detail_tab_overview_path( :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "xml")).to route_to({:controller => "stages", :action => 'overview', :pipeline_name => "pipeline_name", :stage_name => "stage_name", :pipeline_counter => "10", :stage_counter => "2", :format => "xml"})
    end
  end

  describe "history" do
    it "should route to action" do
      expect(:get => "/history/stage/pipeline_name/10/stage_name/5?page=3").to route_to({:controller => "stages", :action => "history", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "5", :page => "3"})
      expect(:get => stage_history_path( :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "5", :page => "3")).to route_to({:controller => "stages", :action => "history", :pipeline_name => "pipeline_name", :pipeline_counter => "10", :stage_name => "stage_name", :stage_counter => "5", :page => "3"})
    end
  end

  describe "config_change" do
    it "should route to action" do
      expect(:get => "/config_change/between/md5_value_2/and/md5_value_1").to route_to({:controller => "stages", :action => "config_change", :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
      expect(:get => config_change_path( :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1")).to route_to({:controller => "stages", :action => "config_change", :later_md5 => "md5_value_2", :earlier_md5 => "md5_value_1"})
    end
  end
end
