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
  if(RUBY_PLATFORM == 'java')
    require 'java'
    autoload :ApacheAnt, 'jruby_modules.rb'
    autoload :JavaLang, 'jruby_modules.rb'
    autoload :XmlOrg, 'jruby_modules.rb'
  else
    require 'rubygems'
    require 'rjb'
    autoload :ApacheAnt, 'rjb_modules.rb'
    autoload :JavaLang, 'rjb_modules.rb'
    autoload :XmlOrg, 'rjb_modules.rb'
  end
  
  VERSION = "0.7.0"
end