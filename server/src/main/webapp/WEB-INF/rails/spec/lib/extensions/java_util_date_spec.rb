#
# Copyright Thoughtworks, Inc.
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

require 'rails_helper'

describe Java::JavaUtil::Date do
  before :each do
    @default_timezone = java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Colombo"))
  end

  after :each do
    java.util.TimeZone.setDefault(@default_timezone)
  end

  it "should format date as '20 Aug, 2010 at 18:03:44 [+0530]'" do
    date           = Dates.from(ZonedDateTime.of(2010, 8, 20, 18, 3, 44, 0, ZoneOffset.ofHoursMinutes(5, 30)))
    formmated_date = date.to_long_display_date_time
    expect(formmated_date).to eq("20 Aug, 2010 at 18:03:44 [+0530]")
  end

  it "should format date for single digit dates as '07 Jul, 2010 at 07:03:04 [+0530]'" do
    date           = Dates.from(ZonedDateTime.of(2010, 7, 7, 7, 3, 4, 0, ZoneOffset.ofHoursMinutes(5, 30)))
    formmated_date = date.to_long_display_date_time
    expect(formmated_date).to eq("07 Jul, 2010 at 07:03:04 [+0530]")
  end
end
