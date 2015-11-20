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

require File.expand_path(File.join(File.dirname(__FILE__), '..', 'spec_helpers'))

module EclipseHelper

  def setupExample(group, projectName, options = {})
	options[:symName] ? symName = options[:symName] :symName = File.basename(projectName)
	requiredPlugins = nil
	if options[:requires]
	  requiredPlugins = "Require-Bundle: #{options[:requires]};bundle-version=\"1.1.0\",\n"
	end
        write "#{group}/#{projectName}/META-INF/MANIFEST.MF", <<-MANIFEST
Manifest-Version: 1.0
Name: #{projectName}
Bundle-Version: 1.1
Specification-Title: "Examples for #{File.basename(projectName)}"
Specification-Version: "1.0"
Specification-Vendor: "Acme Corp.".
Implementation-Title: "#{File.basename(projectName)}"
Implementation-Version: "build57"
Implementation-Vendor: "Acme Corp."
Bundle-SymbolicName: #{symName}
#{requiredPlugins}
MANIFEST
        write "#{group}/#{projectName}/.project", <<-DOT_PROJECT
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
        <name>#{File.basename(projectName)}</name>
        <comment></comment>
        <projects>
        </projects>
        <buildSpec>
                <buildCommand>
                        <name>org.eclipse.jdt.core.javabuilder</name>
                        <arguments>
                        </arguments>
                </buildCommand>
                <buildCommand>
                        <name>org.eclipse.pde.ManifestBuilder</name>
                        <arguments>
                        </arguments>
                </buildCommand>
                <buildCommand>
                        <name>org.eclipse.pde.SchemaBuilder</name>
                        <arguments>
                        </arguments>
                </buildCommand>
        </buildSpec>
        <natures>
                <nature>org.eclipse.pde.PluginNature</nature>
                <nature>org.eclipse.jdt.core.javanature</nature>
        </natures>
</projectDescription>
DOT_PROJECT

write "#{group}/#{projectName}/.classpath", <<-DOT_CLASSPATH
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
        <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.6"/>
        <classpathentry kind="con" path="org.eclipse.pde.core.requiredPlugins"/>
        <classpathentry kind="src" path="src"/>
        <classpathentry combineaccessrules="false" kind="src" path="/another.plugin"/>
        <classpathentry kind="output" path="bin"/>
</classpath>
DOT_CLASSPATH

write "#{group}/#{projectName}/plugin.xml", <<-PLUGIN_XML
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.0"?>
<plugin>
<!--some comment
-->
</plugin>
PLUGIN_XML
        write "#{group}/#{projectName}/build.properties", <<-BUILD_PROPERTIES
source.. = src/
output.. = bin/
javacDefaultEncoding.. = UTF-8
bin.includes = META-INF/,\
               .,\
               plugin.xml,\
               rsc/,
BUILD_PROPERTIES
  end
end

describe Buildr::Generate do
  include EclipseHelper
  describe 'it should find a single eclipse project' do
    top = "top_#{__LINE__}"

    before do
      setupExample(top, 'single')
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
    end

    it 'should create a valid buildfile' do
      define('first')
      File.exists?(File.join(top, 'single', '.project')).should be_true
      File.exists?(File.join(top, '.project')).should be_false
      File.exists?('.project').should be_false
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain("GROUP = \"#{top}\"")
      lambda { Buildr.application.run }.should_not raise_error
    end

    it "should not add project if the corresponding .project file is not an eclipse project" do
      buildFile = File.join(top, 'buildfile')
      FileUtils.rm(buildFile)
      write File.join(top, 'myproject', '.project'), '# Dummy file'
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
      file(buildFile).should exist
      file(buildFile).should contain('define "single"')
      file(buildFile).should_not contain('define "myproject"')
      lambda { Buildr.application.run }.should_not raise_error
    end

      it 'should honour Bundle-Version in MANIFEST.MF' do
	define('bundle_version')
	buildFile = File.join(top, 'buildfile')
	file(buildFile).should exist
	file(buildFile).should contain('define "single"')
	file(buildFile).should contain('define "single", :version => "1.1"')
	lambda { Buildr.application.run }.should_not raise_error
      end

    it "should pass source in build.properties to layout[:source, :main, :java] and layout[:source, :main, :scala]" do
      define('layout_source')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define')
      file(buildFile).should contain('define "single"')
      file(buildFile).should contain('layout[:source, :main, :java]')
      file(buildFile).should contain('layout[:source, :main, :scala]')
      lambda { Buildr.application.run }.should_not raise_error
    end

    it "should pass output in build.properties to layout[:target, :main], etc" do
      define('layout_target')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define')
      file(buildFile).should contain('define "single"')
      file(buildFile).should contain('layout[:target, :main]')
      file(buildFile).should contain('layout[:target, :main, :java]')
      file(buildFile).should contain('layout[:target, :main, :scala]')
      lambda { Buildr.application.run }.should_not raise_error
    end

    it "should package an eclipse plugin" do
      define('layout_target')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define')
      file(buildFile).should contain('package(:jar)')
      lambda { Buildr.application.run }.should_not raise_error
    end

  end

  describe 'it should check for a SymbolicName in MANIFEST.MF' do
    top = "top_#{__LINE__}"
    before do
      setupExample(top, 'single', { :symName => 'singleSymbolicName'} )
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
    end
    it "should set the project name to the SymbolicName from the MANIFEST.MF" do
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define "singleSymbolicName"')
      lambda { Buildr.application.run }.should_not raise_error
    end
  end

  describe 'it should accecpt singleton SymbolicName in MANIFEST.MF' do
    top = "top_#{__LINE__}"
    before do
      setupExample(top, 'single', { :symName => 'singleSymbolicName;singleton:=true'})
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
    end

    it "should not get confused if SymbolicName in MANIFEST.MF is a singleton:=true" do
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define "singleSymbolicName"')
      lambda { Buildr.application.run }.should_not raise_error
    end
  end

  describe 'it should find an eclipse project deep' do
    top = "top_#{__LINE__}"
    before do
      setupExample(top, 'nested/single')
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
    end

    it 'should create a valid buildfile for a nested project' do
      setupExample(top, 'single')
      define('nested/second')
      File.exists?(File.join(top, 'single', '.project')).should be_true
      File.exists?(File.join(top, '.project')).should be_false
      File.exists?('.project').should be_false
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should contain("GROUP = \"#{top}\"")
      file(buildFile).should contain('define "single"')
      lambda { Buildr.application.run }.should_not raise_error
    end

    it "should correct the path for a nested plugin" do
      define('base_dir')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('define "single"')
      file(buildFile).should contain('define "single", :version => "1.1", :base_dir => "nested/single"')
      lambda { Buildr.application.run }.should_not raise_error
    end

  end

  describe 'it should detect dependencies between projects' do
    top = "top_#{__LINE__}"
    before do
      setupExample(top, 'first')
      write(File.join(top, 'first', 'src','org','demo','Demo.java'))
      write(File.join(top, 'first', 'rsc','aResource.txt'))
      setupExample(top, 'second', { :requires => 'first'} )
      write(File.join(top, 'second', 'src','org','second','Demo.java'))
      setupExample(top, 'aFragment', { :fragment_host => 'second'})
      write(File.join(top, 'aFragment', 'fragment.xml'))
      File.open(File.join(top, 'buildfile'), 'w') { |file| file.write Generate.from_eclipse(File.join(Dir.pwd, top)).join("\n") }
    end

    it 'should add "compile.with dependencies" in MANIFEST.MF' do
      define('base_dir')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain('compile.with dependencies')
      file(buildFile).should contain('resources')
      lambda { Buildr.application.run }.should_not raise_error
    end
                                                           #dependencies << projects("first")

    it 'should honour Require-Bundle in MANIFEST.MF' do
      define('base_dir')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
      file(buildFile).should contain(/define "second"/)
      file(buildFile).should contain(                     /dependencies << projects\("first"\)/)
      file(buildFile).should contain(/define "second".*do.*dependencies << projects\("first"\).* end/m)
      lambda { Buildr.application.run }.should_not raise_error
    end

    # Fragments are only supported with buildr4osi which is not (yet?) part of buildr
    it 'should skip fragments.'  do
      define('base_dir')
      buildFile = File.join(top, 'buildfile')
      file(buildFile).should exist
#      system("cat #{buildFile}")  # if $VERBOSE
      file(buildFile).should contain('define "first"')
      lambda { Buildr.application.run }.should_not raise_error
    end

  end

end
