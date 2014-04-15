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

$LOAD_PATH.push(FileUtils::pwd + '/lib')
require 'rubygems'
require 'hoe'
require './lib/antwrap.rb'

def apply_default_hoe_properties(hoe)
  hoe.remote_rdoc_dir = ''
  hoe.rubyforge_name = 'antwrap'
  hoe.author = 'Caleb Powell'
  hoe.email = 'caleb.powell@gmail.com'
  hoe.url = 'http://rubyforge.org/projects/antwrap/'
  hoe.summary = 'A Ruby module that wraps the Apache Ant build tool. Antwrap can be used to invoke Ant Tasks from a Ruby or a JRuby script.'
  hoe.description = hoe.paragraphs_of('README.txt', 2..5).join("\n\n")
  hoe.changes = hoe.paragraphs_of('History.txt', 0..1).join("\n\n")
  puts "Current changes in this release_______________ "
  puts "#{hoe.changes}"
  puts "----------------------------------------------"
end

#builds the MRI Gem
Hoe.new('Antwrap', Antwrap::VERSION) do |hoe|
  apply_default_hoe_properties hoe
  hoe.extra_deps << ["rjb", ">= 1.0.3"]
end

#builds the JRuby Gem
Hoe.new('Antwrap', Antwrap::VERSION) do |hoe|
  apply_default_hoe_properties hoe
  hoe.spec_extras = {:platform => 'java'}
end
#Gem::manage_gems
#require 'rake/gempackagetask'
#
#def create_spec(spec, platform)
#  spec.name          = 'Antwrap'
#  spec.version       = '0.6.0'
#  spec.author        = 'Caleb Powell'
#  spec.email         = 'caleb.powell@gmail.com'
#  spec.homepage      = 'http://rubyforge.org/projects/antwrap/'
#  spec.platform      = platform
#  spec.summary       = "A Ruby module that wraps the Apache Ant build tool, enabling Ant Tasks to be invoked from a Ruby/JRuby scripts."
#  candidates      = Dir.glob("{lib,test,docs}/**/*")
#  spec.files         = candidates.delete_if do |item|
#    item.include?(".svn") || item.include?("apache-ant-1.7.0")
#  end
#  spec.require_path  = 'lib'
#  spec.autorequire   = 'antwrap'
#  spec.test_file     = 'test/antwrap_test.rb'
#  spec.has_rdoc      = true
#  spec.extra_rdoc_files  = ['README', 'COPYING']
#end        
#
#jruby_spec = Gem::Specification.new do |spec| 
#  create_spec(spec, 'java')
#end
#
#ruby_spec = Gem::Specification.new do |spec| 
#  create_spec(spec, Gem::Platform::RUBY) 
#  spec.add_dependency("rjb", ">= 1.0.3")
#end
#
#Rake::GemPackageTask.new(ruby_spec) do |pkg|
#  puts "Creating Ruby Gem"
#end
#
#Rake::GemPackageTask.new(jruby_spec) do |pkg|
#  puts "Creating JRuby Gem"
#end

#task :gems => [:pkg => '/Antwrap-0.5.1.gem'] 


