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

  def go_contributors
    GO_CONTRIBUTORS
  end

  def go_team_members
    GO_TEAM_MEMBERS
  end

  GO_CONTRIBUTORS = (
  [
    'Adam Monago', 'Ajey Gore', 'Anju Antony', 'Barrow Kwan', 'Biju Philip Chacko', 'Chris Briesemeister', 'Cyndi Mitchell',
    'David Rice', 'Dipankar Gupta', 'Dheeraj Reddy', 'Erik Doernenburg', 'Huang Liang', 'Jason Pfetcher', 'Jason Yip', 'Jayne Barnes',
    'John Guerriere', 'Julian Simpson', 'Julias Shaw', 'Li Mo', 'Liu Yao', 'Martin Fowler', 'Megan Folsom', 'Michael Robinson', 'Paul Julius',
    'Praveen Asthagiri', 'Nagarjun K', 'Naresh Kapse', 'Ram Narayanan', 'Rene Medellin', 'Risha Mathias', 'Roy Singham', 'Rupesh Kumar',
    'Sam Newman', 'Sudhir Tiwari', 'Tim Reaves', 'Tom Sulston', 'Wang Ji', 'Wang Xiaoming', 'Zhang Lin'].sort +
    ['and the many people who have contributed code to the OSS CruiseControl project']
  ).join(', ')


  GO_TEAM_MEMBERS = [
    'Anandha Krishnan', 'Anush Ramani', 'Aravind Shimoga Venkatanaranappa', 'Arika Goyal', 'Bobby Norton', 'Chad Wathington',
    'Chris Read', 'Chris Stevenson', 'Chris Turner', 'Deepthi G Chandramouli', 'Gao Li', 'Gilberto Medrano', "H\u00E5kan R\u00E5berg",
    'Hu Kai', 'Janmejay Singh', 'Jef Bekes', 'Jenny Wong', 'Jez Humble', 'Joe Monahan', 'Jon Tirsen', 'Junaid Shah', 'Jyoti Singh',
    'Li Guanglei', 'Li Yanhui', 'Luke Barrett', 'Manish Pillewar', 'Marco Abis', 'Mark Chang', 'Maulik Suchak', 'Md Ali Ejaz',
    'Nandhakumar Ramanathan', 'Pavan K Sudarshan', 'Praveen D Shivanagoudar', 'Prince M Jain', 'Princy James', 'Qiao Liang',
    'Qiao Yandong', 'Qin Qihui', 'Raghunandan Ramakrishna Rao', 'Raghuram Bharathan', 'Rajesh Muppalla', 'Rajeshvaran Appasamy',
    'Ricky Lui', 'Sachin Sudheendra', 'Santosh G Hegde', 'Sara Paul', 'Sharanya Bathey', 'Shilpa Goley', 'Shilpa Nukala',
    'Shweta Sripathi Bhat', 'Sreekanth Vadagiri', 'Srikanth Seshadri', 'Srinivas Upadhya', 'Sriram Narayan', 'Tian Yue',
    'Vinay Dayananda', 'Vipul Garg', 'Xu Wei', 'Yang Hada', 'Yogi Kulkarni', 'Zhao Bing'
  ].sort.join(', ')
end
