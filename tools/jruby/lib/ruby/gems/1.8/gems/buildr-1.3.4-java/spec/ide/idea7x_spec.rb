# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.


require File.join(File.dirname(__FILE__), '../spec_helpers')


module Idea7xHelper
  def ipr_xml_elements
    task('idea7x').invoke
    REXML::Document.new(File.read("#{@project_name}-7x.ipr")).root.elements
  end
  
  def ipr_module_elements
    ipr_xml_elements.to_a("//component[@name='ProjectModuleManager']/modules/module")
  end
  
  def ipr_module_filepaths
    ipr_module_elements.collect { |m| m.attributes['filepath'] }
  end
  
  def ipr_module_fileurls
    ipr_module_elements.collect { |m| m.attributes['fileurl'] }
  end
end


describe Idea7x do
  include Idea7xHelper
  
  describe "the project file" do
    before do
      @project_name = 'alphabet'
    end
    
    it "includes a module for the root project" do
      # Current behavior is to only generate IMLs for packaged projects
      define(@project_name) { project.version = '0.0.0'; package(:jar) } 
      ipr_module_elements.should have(1).element
      ipr_module_filepaths.should == ["$PROJECT_DIR$/alphabet-7x.iml"]
      ipr_module_fileurls.should  == ["file://$PROJECT_DIR$/alphabet-7x.iml"]
    end
    
    it "includes an IML for a subproject" do
      mkpath 'h'
      define(@project_name) do
        project.version = '0.0.0'; package(:jar)
        define('h') do
          package(:jar)
        end
      end
      
      ipr_module_elements.should have(2).elements
      ipr_module_filepaths.should include("$PROJECT_DIR$/h/alphabet-h-7x.iml")
      ipr_module_fileurls.should include("file://$PROJECT_DIR$/h/alphabet-h-7x.iml")
    end
    
    it "pays attention to the base_dir for a subproject" do
      mkpath 'aitch'
      define(@project_name) do
        project.version = '0.0.0'; package(:jar)
        define('h', :base_dir => 'aitch') do
          package(:jar)
        end
      end
     
      ipr_module_elements.should have(2).elements
      ipr_module_filepaths.should include("$PROJECT_DIR$/aitch/alphabet-h-7x.iml")
      ipr_module_fileurls.should include("file://$PROJECT_DIR$/aitch/alphabet-h-7x.iml")
    end
  end
end
