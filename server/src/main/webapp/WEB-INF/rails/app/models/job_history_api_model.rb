#
# Copyright 2019 ThoughtWorks, Inc.
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

class JobHistoryAPIModel
  attr_reader :pagination, :jobs

  def initialize(pagination, job_instances)
    @pagination = PaginationAPIModel.new(pagination)

    @jobs = job_instances.collect do |job_instance|
      JobInstanceAPIModel.new(job_instance)
    end
  end
end