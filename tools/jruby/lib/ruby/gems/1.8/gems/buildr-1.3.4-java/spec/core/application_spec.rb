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


describe Buildr::Application do

  describe 'home_dir' do
    it 'should point to ~/.buildr' do
      Buildr.application.home_dir.should eql(File.expand_path('.buildr', ENV['HOME']))
    end

    it 'should point to existing directory' do
      File.directory?(Buildr.application.home_dir).should be_true
    end
  end

  describe '#run' do
    it 'should execute *_load methods in order' do
      order = [:load_gems, :load_artifact_ns, :load_tasks, :raw_load_buildfile]
      order.each { |method| Buildr.application.should_receive(method).ordered }
      Buildr.application.stub!(:exit) # With this, shows the correct error instead of SystemExit.
      Buildr.application.run
    end

    it 'should load imports after loading buildfile' do
      method = Buildr.application.method(:raw_load_buildfile)
      Buildr.application.should_receive(:raw_load_buildfile) do
        Buildr.application.should_receive(:load_imports)
        method.call
      end
      Buildr.application.stub!(:exit) # With this, shows the correct error instead of SystemExit.
      Buildr.application.run
    end

    it 'should evaluate all projects after loading buildfile' do
      Buildr.application.should_receive(:load_imports) do
        Buildr.should_receive(:projects)
      end
      Buildr.application.stub!(:exit) # With this, shows the correct error instead of SystemExit.
      Buildr.application.run
    end
  end

  describe 'environment' do
    it 'should return value of BUILDR_ENV' do
      ENV['BUILDR_ENV'] = 'qa'
      Buildr.application.environment.should eql('qa')
    end

    it 'should default to development' do
      Buildr.application.environment.should eql('development')
    end

    it 'should set environment name from -e argument' do
      ARGV.push('-e', 'test')
      Buildr.application.send(:handle_options)
      Buildr.application.environment.should eql('test')
      ENV['BUILDR_ENV'].should eql('test')
    end
    
    it 'should be echoed to user' do
      write 'buildfile'
      ENV['BUILDR_ENV'] = 'spec'
      Buildr.application.send(:handle_options)
      lambda { Buildr.application.send :load_buildfile }.should show(%r{(in .*, spec)})
    end
  end

  describe 'gems' do
    before do
      class << Buildr.application
        public :load_gems
      end
    end

    def load_with_yaml
      write 'build.yaml', <<-YAML
        gems:
        - rake
        - rspec >= 1.2
      YAML
      Buildr.application.load_gems
    end

    it 'should return empty array if no gems specified' do
      Buildr.application.load_gems
      Buildr.application.gems.should be_empty
    end

    it 'should return one entry for each gem specified in buildr.yaml' do
      load_with_yaml
      Buildr.application.gems.size.should be(2)
    end

    it 'should return a Gem::Specification for each installed gem' do
      load_with_yaml
      Buildr.application.gems.each { |gem| gem.should be_kind_of(Gem::Specification) }
    end

    it 'should parse Gem name correctly' do
      load_with_yaml
      Buildr.application.gems.map(&:name).should include('rspec', 'rake')
    end

    it 'should find installed version of Gem' do
      load_with_yaml
      Buildr.application.gems.each { |gem| gem.version.should eql(Gem.loaded_specs[gem.name].version) }
    end
  end


  describe 'load_gems' do
    before do
      class << Buildr.application
        public :load_gems
      end
      @spec = Gem::Specification.new do |spec|
        spec.name = 'foo'
        spec.version = '1.2'
      end
      $stdout.stub!(:isatty).and_return(true)
    end

    it 'should do nothing if no gems specified' do
      lambda { Buildr.application.load_gems }.should_not raise_error
    end

    it 'should install nothing if specified gems already installed' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem.loaded_specs['rspec']])
      Util.should_not_receive(:ruby)
      lambda { Buildr.application.load_gems }.should_not raise_error
    end

    it 'should fail if required gem not found in remote repository' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([])
      lambda { Buildr.application.load_gems }.should raise_error(LoadError, /cannot be found/i)
    end

    it 'should fail if need to install gem and not running in interactive mode' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $stdout.should_receive(:isatty).and_return(false)
      lambda { Buildr.application.load_gems }.should raise_error(LoadError, /this build requires the gems/i)
    end

    it 'should ask permission before installing required gems' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $terminal.should_receive(:agree).with(/install/, true)
      lambda { Buildr.application.load_gems }.should raise_error
    end

    it 'should fail if permission not granted to install gem' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $terminal.should_receive(:agree).and_return(false)
      lambda { Buildr.application.load_gems }.should raise_error(LoadError, /cannot build without/i)
    end

    it 'should install gem if permission granted' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $terminal.should_receive(:agree).and_return(true)
      Util.should_receive(:ruby) do |*args|
        args.should include('install', 'foo', '-v', '1.2')
      end
      Buildr.application.should_receive(:gem).and_return(false)
      Buildr.application.load_gems
    end

    it 'should reload gem cache after installing required gems' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $terminal.should_receive(:agree).and_return(true)
      Util.should_receive(:ruby)
      Gem.source_index.should_receive(:load_gems_in).with(Gem::SourceIndex.installed_spec_directories)
      Buildr.application.should_receive(:gem).and_return(false)
      Buildr.application.load_gems
    end

    it 'should load previously installed gems' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem.loaded_specs['rspec']])
      Buildr.application.should_receive(:gem).with('rspec', Gem.loaded_specs['rspec'].version.to_s)
      Buildr.application.load_gems
    end

    it 'should load newly installed gems' do
      Buildr.application.should_receive(:listed_gems).and_return([Gem::Dependency.new('foo', '>=1.1')])
      Gem::SourceInfoCache.should_receive(:search).and_return([@spec])
      $terminal.should_receive(:agree).and_return(true)
      Util.should_receive(:ruby)
      Buildr.application.should_receive(:gem).with('foo', @spec.version.to_s)
      Buildr.application.load_gems
    end

    it 'should default to >=0 version requirement if not specified' do
      write 'build.yaml', 'gems: foo'
      Gem::SourceInfoCache.should_receive(:search).with(Gem::Dependency.new('foo', '>=0')).and_return([])
      lambda { Buildr.application.load_gems }.should raise_error
    end

    it 'should parse exact version requirement' do
      write 'build.yaml', 'gems: foo 2.5'
      Gem::SourceInfoCache.should_receive(:search).with(Gem::Dependency.new('foo', '=2.5')).and_return([])
      lambda { Buildr.application.load_gems }.should raise_error
    end

    it 'should parse range version requirement' do
      write 'build.yaml', 'gems: foo ~>2.3'
      Gem::SourceInfoCache.should_receive(:search).with(Gem::Dependency.new('foo', '~>2.3')).and_return([])
      lambda { Buildr.application.load_gems }.should raise_error
    end

    it 'should parse multiple version requirements' do
      write 'build.yaml', 'gems: foo >=2.0 !=2.1'
      Gem::SourceInfoCache.should_receive(:search).with(Gem::Dependency.new('foo', ['>=2.0', '!=2.1'])).and_return([])
      lambda { Buildr.application.load_gems }.should raise_error
    end
  end

end


describe Buildr, 'settings' do

  describe 'user' do

    it 'should be empty hash if no settings.yaml file' do
      Buildr.settings.user.should == {}
    end

    it 'should return loaded settings.yaml file' do
      write 'home/.buildr/settings.yaml', 'foo: bar'
      Buildr.settings.user.should == { 'foo'=>'bar' }
    end

    it 'should return loaded settings.yml file' do
      write 'home/.buildr/settings.yml', 'foo: bar'
      Buildr.settings.user.should == { 'foo'=>'bar' }
    end

    it 'should fail if settings.yaml file is not a hash' do
      write 'home/.buildr/settings.yaml', 'foo bar'
      lambda { Buildr.settings.user }.should raise_error(RuntimeError, /expecting.*settings.yaml/i)
    end

    it 'should be empty hash if settings.yaml file is empty' do
      write 'home/.buildr/settings.yaml'
      Buildr.settings.user.should == {}
    end
  end

  describe 'configuration' do
    it 'should be empty hash if no build.yaml file' do
      Buildr.settings.build.should == {}
    end

    it 'should return loaded build.yaml file' do
      write 'build.yaml', 'foo: bar'
      Buildr.settings.build.should == { 'foo'=>'bar' }
    end

    it 'should return loaded build.yml file' do
      write 'build.yml', 'foo: bar'
      Buildr.settings.build.should == { 'foo'=>'bar' }
    end

    it 'should fail if build.yaml file is not a hash' do
      write 'build.yaml', 'foo bar'
      lambda { Buildr.settings.build }.should raise_error(RuntimeError, /expecting.*build.yaml/i)
    end

    it 'should be empty hash if build.yaml file is empty' do
      write 'build.yaml'
      Buildr.settings.build.should == {}
    end
  end

  describe 'profiles' do
    it 'should be empty hash if no profiles.yaml file' do
      Buildr.settings.profiles.should == {}
    end

    it 'should return loaded profiles.yaml file' do
      write 'profiles.yaml', <<-YAML
        development:
          foo: bar
      YAML
      Buildr.settings.profiles.should == { 'development'=> { 'foo'=>'bar' } }
    end

    it 'should return loaded profiles.yml file' do
      write 'profiles.yml', <<-YAML
        development:
          foo: bar
      YAML
      Buildr.settings.profiles.should == { 'development'=> { 'foo'=>'bar' } }
    end

    it 'should fail if profiles.yaml file is not a hash' do
      write 'profiles.yaml', 'foo bar'
      lambda { Buildr.settings.profiles }.should raise_error(RuntimeError, /expecting.*profiles.yaml/i)
    end

    it 'should be empty hash if profiles.yaml file is empty' do
      write 'profiles.yaml'
      Buildr.settings.profiles.should == {}
    end
  end

  describe 'profile' do
    before do
    end

    it 'should be empty hash if no profiles.yaml' do
      Buildr.settings.profile.should == {}
    end

    it 'should be empty hash if no matching profile' do
      write 'profiles.yaml', <<-YAML
        test:
          foo: bar
      YAML
      Buildr.settings.profile.should == {}
    end

    it 'should return profile matching environment name' do
      write 'profiles.yaml', <<-YAML
        development:
          foo: bar
        test:
          foo: baz
      YAML
      Buildr.settings.profile.should == { 'foo'=>'bar' }
    end
  end

  describe 'buildfile task' do
    before do
      @buildfile_time = Time.now - 10
      write 'buildfile'; File.utime(@buildfile_time, @buildfile_time, 'buildfile')
    end
    
    it 'should point to the buildfile' do
      Buildr.application.buildfile.should point_to_path('buildfile')
    end
    
    it 'should be a defined task' do
      Buildr.application.buildfile.should == file(File.expand_path('buildfile'))
    end
    
    it 'should ignore any rake namespace' do
      namespace 'dummy_ns' do
        Buildr.application.buildfile.should point_to_path('buildfile')
      end
    end
    
    it 'should have the same timestamp as the buildfile' do
      Buildr.application.buildfile.timestamp.should be_close(@buildfile_time, 1)
    end
    
    it 'should have the same timestamp as build.yaml if the latter is newer' do
      write 'build.yaml'; File.utime(@buildfile_time + 5, @buildfile_time + 5, 'build.yaml')
      Buildr.application.run
      Buildr.application.buildfile.timestamp.should be_close(@buildfile_time + 5, 1)
    end
    
    it 'should have the same timestamp as the buildfile if build.yaml is older' do
      write 'build.yaml'; File.utime(@buildfile_time - 5, @buildfile_time - 5, 'build.yaml')
      Buildr.application.run
      Buildr.application.buildfile.timestamp.should be_close(@buildfile_time, 1)
    end
    
    it 'should have the same timestamp as build.rb in home dir if the latter is newer' do
      write 'home/buildr.rb'; File.utime(@buildfile_time + 5, @buildfile_time + 5, 'home/buildr.rb')
      Buildr.application.send :load_tasks
      Buildr.application.buildfile.timestamp.should be_close(@buildfile_time + 5, 1)
    end
  end
end


describe Buildr do
  
  describe 'environment' do
    it 'should be same as Buildr.application.environment' do
      Buildr.environment.should eql(Buildr.application.environment)
    end
  end

  describe 'application' do
    it 'should be same as Rake.application' do
      Buildr.application.should == Rake.application
    end
  end

  describe 'settings' do
    it 'should be same as Buildr.application.settings' do
      Buildr.settings.should == Buildr.application.settings
    end
  end
end

