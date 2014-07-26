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

class JobInstanceAPIModel
  attr_reader :id, :name, :state, :result, :scheduled_date

  def initialize(job_instance_model)
    @id = job_instance_model.getId()
    @name = job_instance_model.getName()
    @state = job_instance_model.getState().to_s
    @result = job_instance_model.getResult().to_s
    @scheduled_date = job_instance_model.getScheduledDate().to_s
  end
end