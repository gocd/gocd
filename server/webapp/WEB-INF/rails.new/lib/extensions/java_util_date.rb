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

Java::JavaUtil::Date.class_eval do
  def iso8601
    Time.at(self.getTime()/1000).iso8601
  end

  def display_time
    com.thoughtworks.go.util.TimeConverter.convert(self);
  end

  def to_long_display_date_time
    pattern = java.text.SimpleDateFormat.new("dd MMM, yyyy 'at' HH:mm:ss [Z]")
    pattern.format(self)
  end

  def as_json(options = nil)
    Time.zone.at(self.getTime().to_f/1000).as_json(options)
  end

end
