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
require 'java_adapter'
module Antwrap
  module ApacheAnt
    DefaultLogger = JavaAdapter.import_class("org.apache.tools.ant.DefaultLogger")
    Main = JavaAdapter.import_class("org.apache.tools.ant.Main")
    Project = JavaAdapter.import_class("org.apache.tools.ant.Project")
    RuntimeConfigurable = JavaAdapter.import_class("org.apache.tools.ant.RuntimeConfigurable")
    Target = JavaAdapter.import_class("org.apache.tools.ant.Target")
    UnknownElement = JavaAdapter.import_class("org.apache.tools.ant.UnknownElement")
  end
  
  module JavaLang
    System = JavaAdapter.import_class("java.lang.System")
  end
  
  module XmlSax
    AttributeListImpl = JavaAdapter.import_class("org.xml.sax.helpers.AttributeListImpl")
  end
end