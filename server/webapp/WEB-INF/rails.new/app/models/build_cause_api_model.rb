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

class BuildCauseAPIModel
  attr_reader :trigger_message, :trigger_forced, :approver, :material_revisions

  def initialize(build_cause)
    @trigger_message = build_cause.getBuildCauseMessage()
    @trigger_forced = build_cause.isForced()
    @approver = build_cause.getApprover()
    @material_revisions = []
    build_cause.getMaterialRevisions().each do |material_revision_instance_model|
      @material_revisions << MaterialRevisionInstanceAPIModel.new(material_revision_instance_model)
    end
  end
end