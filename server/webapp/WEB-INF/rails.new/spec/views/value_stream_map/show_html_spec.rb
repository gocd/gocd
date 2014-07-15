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

require File.expand_path(File.dirname(__FILE__) + '/../../spec_helper')

describe "value_stream_map/show.html.erb" do
  include GoUtil

  before(:each)  do
    in_params :pipeline_name => 'P1', :pipeline_counter => "1"
    template.stub!(:url_for_pipeline).with('P1').and_return('link_for_P1')
  end

  describe "render html" do
    it "should render pipeline label for VSM page breadcrumb when it is available" do
      pipeline = mock('pipeline instance')
      pipeline.should_receive(:getLabel).and_return('test')
      assigns[:pipeline] = pipeline

      render "value_stream_map/show.html.erb"

      response.body.should have_tag("ul.entity_title") do
        with_tag("li.name") do
          with_tag("a[href='link_for_P1']", "P1")
        end
        with_tag("li.last") do
          with_tag("h1", "test")
        end
      end
    end

    it "should render pipeline counter for VSM page breadcrumb when pipeline label is not available" do
      assigns[:pipeline] = nil

      render "value_stream_map/show.html.erb"

      response.body.should have_tag("ul.entity_title") do
        with_tag("li.name") do
          with_tag("a[href='link_for_P1']", "P1")
        end
        with_tag("li.last") do
          with_tag("h1", "1")
        end
      end
    end

    it "should give VSM page a title with VSM of pipelineName" do
      render "value_stream_map/show.html.erb"

      assigns[:view_title].should == "Value Stream Map of P1"
    end

  end

end