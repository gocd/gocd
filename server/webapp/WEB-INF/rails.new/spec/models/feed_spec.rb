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

describe Feed do
  describe "whent there are no stages" do

    before(:each) do
      @feed = Feed.new(Username::ANONYMOUS, double(:feed => FeedEntries.new), HttpLocalizedOperationResult.new)
    end

    it "should report now as updated date if there are no jobs" do
      @feed.updated_date.should == Time.now.iso8601
    end

    it "should have nil for first" do
      @feed.first.should == nil
    end

    it "should have nil for last" do
      @feed.first.should == nil
    end
  end

  describe "when there is one job" do

    before(:each) do
      @date = java_date_utc(2004, 12, 25, 12, 0, 0)
      @job = com.thoughtworks.go.domain.feed.stage.StageFeedEntry.new(1, 99, com.thoughtworks.go.domain.StageIdentifier.new("cruise/1/stage/1"), 1, @date, StageResult::Cancelled)
      @feed = Feed.new(Username::ANONYMOUS, double(:feed => FeedEntries.new([@job])), HttpLocalizedOperationResult.new)
    end

    it "should report updated_date as that of first job" do
      @feed.updated_date.should == @date.iso8601
    end

    it "should return a list of jobs" do
      @feed.entries.should == FeedEntries.new([@job])
    end

  end

  describe "when traversing a list" do

    before(:each) do
      @date = java_date_utc(2004, 12, 25, 12, 0, 0)
      @stage1 = com.thoughtworks.go.domain.feed.stage.StageFeedEntry.new(1, 99, com.thoughtworks.go.domain.StageIdentifier.new("cruise/1/stage/1"), 1, @date, StageResult::Cancelled)
      @stage2 = com.thoughtworks.go.domain.feed.stage.StageFeedEntry.new(2, 99, com.thoughtworks.go.domain.StageIdentifier.new("cruise/1/stage/1"), 1, @date, StageResult::Cancelled)
      @jobInstanceService = double()
      @result = HttpLocalizedOperationResult.new
    end

    it "should return a list of jobs before the supplied one" do
      expected = FeedEntries.new([:some_job])
      @jobInstanceService.should_receive(:feedBefore).with(Username.new(CaseInsensitiveString.new('poovan')), 10, @result).and_return( expected)
      job_feed = Feed.new(Username.new(CaseInsensitiveString.new('poovan')), @jobInstanceService, @result, :before => "10")
      job_feed.entries.should == expected
    end
  end
end
