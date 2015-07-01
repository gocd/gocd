##########################################################################
# Copyright 2015 ThoughtWorks, Inc.
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


# Knows to render a java date to JSON
# Inspired from `active_support/json/encoding.rb`
class Java::JavaUtil::Date
  def as_json(options = nil) #:nodoc:
    org.apache.commons.lang.time.FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SZZ").format(self)
  end
end
