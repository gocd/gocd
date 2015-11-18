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
      Buildr.application.stub(:exit) # With this, shows the correct error instead of SystemExit.
      Buildr.application.run
    end

    it 'should load imports after loading buildfile' do
      method = Buildr.application.method(:raw_load_buildfile)
      Buildr.application.should_receive(:raw_load_buildfile) do
        Buildr.application.should_receive(:load_imports)
        method.call
      end
      Buildr.application.stub(:exit) # With this, shows the correct error instead of SystemExit.
      Buildr.application.run
    end

    it 'should evaluate all projects after loading buildfile' do
      Buildr.application.should_receive(:load_imports) do
        Buildr.should_receive(:projects)
      end
      Buildr.application.stub(:exit) # With this, shows the correct error instead of SystemExit.
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

  describe 'options' do
    it "should have 'tasks' as the sole default rakelib" do
      Buildr.application.send(:handle_options)
      Buildr.application.options.rakelib.should == ['tasks']
    end

    it 'should show the version when prompted with -V' do
      ARGV.push('-V')
      test_exit(0) { Buildr.application.send(:handle_options) }.should show(/Buildr #{Buildr::VERSION}.*/)
    end

    it 'should show the version when prompted with --version' do
      ARGV.push('--version')
      test_exit(0) { Buildr.application.send(:handle_options) }.should show(/Buildr #{Buildr::VERSION}.*/)
    end

    it 'should enable tracing with --trace' do
      ARGV.push('--trace')
      Buildr.application.send(:handle_options)
      Buildr.application.options.trace.should == true
    end

    it 'should enable tracing of [:foo, :bar] categories with --trace=foo,bar' do
      ARGV.push('--trace=foo,bar')
      Buildr.application.send(:handle_options)
      Buildr.application.options.trace.should == true
      Buildr.application.options.trace_categories.should == [:foo, :bar]
      trace?(:foo).should == true
      trace?(:not).should == false
    end

    it 'should enable tracing for all categories with --trace=all' do
      ARGV.push('--trace=all')
      Buildr.application.send(:handle_options)
      Buildr.application.options.trace.should == true
      Buildr.application.options.trace_all.should == true
      trace?(:foo).should == true
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
        - rspec ~> 2.9.0
      YAML
      Buildr.application.should_receive(:listed_gems).and_return([[Gem.loaded_specs['rspec'],Gem.loaded_specs['rake']],[]])
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
        spec.name = 'buildr-foo'
        spec.version = '1.2'
      end
      $stdout.stub(:isatty).and_return(true)
    end

    it 'should do nothing if no gems specified' do
      lambda { Buildr.application.load_gems }.should_not raise_error
    end

    it 'should install nothing if specified gems already installed' do
      Buildr.application.should_receive(:listed_gems).and_return([[Gem.loaded_specs['rspec']],[]])
      Util.should_not_receive(:ruby)
      lambda { Buildr.application.load_gems }.should_not raise_error
    end

    it 'should fail if required gem not installed' do
      Buildr.application.should_receive(:listed_gems).and_return([[],[Gem::Dependency.new('buildr-foo', '>=1.1')]])
      lambda { Buildr.application.load_gems }.should raise_error(LoadError, /cannot be found/i)
    end

    it 'should load previously installed gems' do
      Gem.loaded_specs['rspec'].should_receive(:activate)
      Buildr.application.should_receive(:listed_gems).and_return([[Gem.loaded_specs['rspec']],[]])
      #Buildr.application.should_receive(:gem).with('rspec', Gem.loaded_specs['rspec'].version.to_s)
      Buildr.application.load_gems
    end

    it 'should default to >=0 version requirement if not specified' do
      write 'build.yaml', 'gems: buildr-foo'
      should_attempt_to_load_dependency(Gem::Dependency.new('buildr-foo', '>= 0'))
    end

    it 'should parse exact version requirement' do
      write 'build.yaml', 'gems: buildr-foo 2.5'
      should_attempt_to_load_dependency(Gem::Dependency.new('buildr-foo', '=2.5'))
    end

    it 'should parse range version requirement' do
      write 'build.yaml', 'gems: buildr-foo ~>2.3'
      should_attempt_to_load_dependency(Gem::Dependency.new('buildr-foo', '~>2.3'))
    end

    it 'should parse multiple version requirements' do
      write 'build.yaml', 'gems: buildr-foo >=2.0 !=2.1'
      should_attempt_to_load_dependency(Gem::Dependency.new('buildr-foo', ['>=2.0', '!=2.1']))
    end

    def should_attempt_to_load_dependency(dep)
      missing_gems = Buildr.application.send(:listed_gems)[1]
      missing_gems.size.should eql(1)
      missing_gems[0].eql?(dep)
    end
  end

  describe 'load_tasks' do
    before do
      class << Buildr.application
        public :load_tasks
      end
      @original_loaded_features = $LOADED_FEATURES.dup
      Buildr.application.options.rakelib = ["tasks"]
    end

    after do
      $taskfiles = nil
      ($LOADED_FEATURES - @original_loaded_features).each do |new_load|
        $LOADED_FEATURES.delete(new_load)
      end
    end

    def write_task(filename)
      write filename, <<-RUBY
        $taskfiles ||= []
        $taskfiles << __FILE__
      RUBY
    end

    def loaded_tasks
      @loaded ||= Buildr.application.load_tasks
      $taskfiles
    end

    it "should load {options.rakelib}/foo.rake" do
      write_task 'tasks/foo.rake'
      loaded_tasks.should have(1).task
      loaded_tasks.first.should =~ %r{tasks/foo\.rake$}
    end

    it 'should load all *.rake files from the rakelib' do
      write_task 'tasks/bar.rake'
      write_task 'tasks/quux.rake'
      loaded_tasks.should have(2).tasks
    end

    it 'should not load files which do not have the .rake extension' do
      write_task 'tasks/foo.rb'
      write_task 'tasks/bar.rake'
      loaded_tasks.should have(1).task
      loaded_tasks.first.should =~ %r{tasks/bar\.rake$}
    end

    it 'should load files only from the directory specified in the rakelib option' do
      Buildr.application.options.rakelib = ['extensions']
      write_task 'extensions/amp.rake'
      write_task 'tasks/bar.rake'
      write_task 'extensions/foo.rake'
      loaded_tasks.should have(2).tasks
      %w[amp foo].each do |filename|
        loaded_tasks.select{|x| x =~ %r{extensions/#{filename}\.rake}}.should have(1).entry
      end
    end

    it 'should load files from all the directories specified in the rakelib option' do
      Buildr.application.options.rakelib = ['ext', 'more', 'tasks']
      write_task 'ext/foo.rake'
      write_task 'tasks/bar.rake'
      write_task 'tasks/zeb.rake'
      write_task 'more/baz.rake'
      loaded_tasks.should have(4).tasks
    end

    it 'should not load files from the rakelib more than once' do
      write_task 'tasks/new_one.rake'
      write_task 'tasks/already.rake'
      $LOADED_FEATURES << File.expand_path('tasks/already.rake')

      loaded_tasks.should have(1).task
      loaded_tasks.first.should =~ %r{tasks/new_one\.rake$}
    end
  end

  describe 'exception handling' do

    it 'should exit when given a SystemExit exception' do
      test_exit(3) { Buildr.application.standard_exception_handling { raise SystemExit.new(3) } }
    end

    it 'should exit with status 1 when given an OptionParser::ParseError exception' do
      test_exit(1) { Buildr.application.standard_exception_handling { raise OptionParser::ParseError.new() } }
    end

    it 'should exit with status 1 when given any other type of exception exception' do
      test_exit(1) { Buildr.application.standard_exception_handling { raise Exception.new() } }
    end

    it 'should print the class name and the message when receiving an exception (except when the exception is named Exception)' do

      # Our fake $stderr for the exercise. We could start it with a matcher instead
      class FakeStdErr

        attr_accessor :messages

        def puts(*args)
          @messages ||= []
          @messages += args
        end

        alias :write :puts
      end

      # Save the old $stderr and make sure to restore it in the end.
      old_stderr = $stderr
      begin

        $stderr = FakeStdErr.new
        test_exit(1) {
          Buildr.application.send :standard_exception_handling do
            class MyOwnNicelyNamedException < Exception
            end
            raise MyOwnNicelyNamedException.new('My message')
          end
        }.call
        $stderr.messages.select {|msg| msg =~ /.*MyOwnNicelyNamedException : My message.*/}.size.should == 1
        $stderr.messages.clear
        test_exit(1) {
          Buildr.application.send :standard_exception_handling do
            raise Exception.new('My message')
          end
        }.call
        $stderr.messages.select {|msg| msg =~ /.*My message.*/ && !(msg =~ /Exception/)}.size.should == 1
      end
      $stderr = old_stderr
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
      Buildr.application.buildfile.timestamp.should be_within(1).of(@buildfile_time)
    end

    it 'should have the same timestamp as build.yaml if the latter is newer' do
      write 'build.yaml'; File.utime(@buildfile_time + 5, @buildfile_time + 5, 'build.yaml')
      Buildr.application.run
      Buildr.application.buildfile.timestamp.should be_within(1).of(@buildfile_time + 5)
    end

    it 'should have the same timestamp as the buildfile if build.yaml is older' do
      write 'build.yaml'; File.utime(@buildfile_time - 5, @buildfile_time - 5, 'build.yaml')
      Buildr.application.run
      Buildr.application.buildfile.timestamp.should be_within(1).of(@buildfile_time)
    end

    it 'should have the same timestamp as build.rb in home dir if the latter is newer (until version 1.6)' do
      Buildr::VERSION.should < '1.6'
      buildfile_should_have_same_timestamp_as 'home/buildr.rb'
    end

    it 'should have the same timestamp as build.rb in home dir if the latter is newer' do
      buildfile_should_have_same_timestamp_as 'home/.buildr/buildr.rb'
    end

    it 'should have the same timestamp as .buildr.rb in buildfile dir if the latter is newer' do
      buildfile_should_have_same_timestamp_as '.buildr.rb'
    end

    it 'should have the same timestamp as _buildr.rb in buildfile dir if the latter is newer' do
      buildfile_should_have_same_timestamp_as '_buildr.rb'
    end

    def buildfile_should_have_same_timestamp_as(file)
      write file; File.utime(@buildfile_time + 5, @buildfile_time + 5, file)
      Buildr.application.send :load_tasks
      Buildr.application.buildfile.timestamp.should be_within(1).of(@buildfile_time + 5)
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

describe Rake do
  describe 'define_task' do
   it 'should restore call chain when invoke is called' do
     task1 = Rake::Task.define_task('task1') do
     end

     task2 = Rake::Task.define_task('task2') do
       chain1 = Thread.current[:rake_chain]
       task1.invoke
       chain2 = Thread.current[:rake_chain]
       chain2.should == chain1
     end

     task2.invoke
   end
 end
end
