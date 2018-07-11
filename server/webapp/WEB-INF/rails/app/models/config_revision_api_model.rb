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

class ConfigRevisionAPIModel
  attr_reader :md5, :username, :goVersion, :goEdition, :time, :schemaVersion, :commitSHA

  def initialize(config_revision_instance_model)
    @md5 = config_revision_instance_model.getMd5()
    @username = config_revision_instance_model.getUsername()
    @goVersion = config_revision_instance_model.getGoVersion()
    @time = config_revision_instance_model.getTime().getTime() unless config_revision_instance_model.getTime() == nil
    @schemaVersion = config_revision_instance_model.getSchemaVersion()
    @commitSHA = config_revision_instance_model.getCommitSHA()
  end
end
