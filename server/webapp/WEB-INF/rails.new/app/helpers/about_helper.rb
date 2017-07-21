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

module AboutHelper
  def jvm_version
    system_property('java.version')
  end

  def os_information
    [system_property('os.name'), system_property('os.version')].join(' ')
  end

  def system_property(property)
    system_environment.getPropertyImpl(property)
  end

  def available_space
    artifact_dir = artifacts_dir_holder.getArtifactsDir
    number_to_human_size artifact_dir.getUsableSpace()
  end

  def schema_version
    system_service.getSchemaVersion()
  end
end
