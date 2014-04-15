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

class Feed

  def initialize(user, service, operation_result, params = {})
    if params.has_key?(:before)
      @entries = service.feedBefore(user, params[:before].to_i, operation_result)
    else
      @entries = service.feed(user, operation_result)
    end
  end

  def updated_date
    @updated_date = @entries.lastUpdatedDate().iso8601
  end

  def entries
    @entries
  end

  def first
    @entries.firstEntryId()
  end

  def last
    @entries.lastEntryId()
  end

end
