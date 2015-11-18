# Copyright 2008 Caleb Powell 
# Licensed under the Apache License, Version 2.0 (the "License"); 
# you may not use this file except in compliance with the License. 
# You may obtain a copy of the License at 
#
#   http://www.apache.org/licenses/LICENSE-2.0 
# 
# Unless required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" BASIS, 
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
# See the License for the specific language governing permissions and limitations 
# under the License.

require 'antwrap_utilities'
require 'ant_project'
require 'ant_task'
module Antwrap
  autoload :ApacheAnt, 'ant_libraries.rb'
  autoload :JavaLang, 'ant_libraries.rb'
  autoload :XmlOrg, 'ant_libraries.rb'
  VERSION = "0.7.5"
end