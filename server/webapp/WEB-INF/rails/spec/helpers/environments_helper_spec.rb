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

require File.expand_path(File.dirname(__FILE__) + '/../spec_helper')


describe EnvironmentsHelper do
  include EnvironmentsHelper

  describe "stage_width" do
    it "should return 4.9em for a non latest stage with 4 stages" do
      stage_width_em(4, false, 20.0).should == "4.9167em"
    end

    it "should return 4.7em for the latest stage with 4 stages" do
      stage_width_em(4, true, 20.0).should == "4.75em"
    end

    it "should return 19.7em for 1 stage" do
      stage_width_em(1, true, 20.0).should == "19.75em"
    end

    it "should return 6.5667em for a non latest stage with 3 stages" do
      stage_width_em(3, false, 20.0).should == "6.5834em"
    end

    it "should return 6.3667em for the latest stage with 3 stages" do
      stage_width_em(3, true, 20.0).should == "6.4167em"
    end


    it "should return percent" do
      stage_width_percent(3, true, 20.0).should == "6.6667%"
    end
  end

  describe "environments last of" do
    it "should be true when the given element is last in the given collection" do
      list = java.util.ArrayList.new
      list.add(first = Object.new)
      list.add(middle = Object.new)
      list.add(last = Object.new)
      is_last(list, first).should be_false
      is_last(list, middle).should be_false
      is_last(list, last).should be_true
    end
  end
end
