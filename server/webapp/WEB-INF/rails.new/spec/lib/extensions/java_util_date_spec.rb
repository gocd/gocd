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

describe Java::JavaUtil::Date do
  before :each do
    @default_timezone = java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Colombo"))
  end

  after :each do
    java.util.TimeZone.setDefault(@default_timezone)
  end

  it "should format date as '20 Aug, 2010 at 18:03:44 [+0530]'" do
    joda_date      = org.joda.time.DateTime.new(2010, 8, 20, 18, 3, 44, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
    formmated_date = joda_date.to_date.to_long_display_date_time
    formmated_date.should == "20 Aug, 2010 at 18:03:44 [+0530]"
  end

  it "should format date for single digit dates as '07 Jul, 2010 at 07:03:04 [+0530]'" do
    joda_date      = org.joda.time.DateTime.new(2010, 7, 7, 7, 3, 4, 0, org.joda.time.DateTimeZone.forOffsetHoursMinutes(5, 30))
    formmated_date = joda_date.to_date.to_long_display_date_time
    formmated_date.should == "07 Jul, 2010 at 07:03:04 [+0530]"
  end

  it "should convert date in UTC timezone" do
    date           = DateTime.new(2010, 7, 7, 7, 3, 4, '+530')
    formmated_date =java.util.Date.new(date.to_f*1000).as_json.to_s
    formmated_date.should == "2010-07-07T01:33:04.000Z"
  end

end
