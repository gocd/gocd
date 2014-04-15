require 'spec_helper'
require 'tmpdir'

# so the stdlib module is available...
module Test; module Unit; module Assertions; end; end; end

module RSpec::Core

  describe Configuration do

    let(:config) { Configuration.new }

    describe "#load_spec_files" do

      it "loads files using load" do
        config.files_to_run = ["foo.bar", "blah_spec.rb"]
        config.should_receive(:load).twice
        config.load_spec_files
      end

      it "loads each file once, even if duplicated in list" do
        config.files_to_run = ["a_spec.rb", "a_spec.rb"]
        config.should_receive(:load).once
        config.load_spec_files
      end

      context "with rspec-1 loaded" do
        before do
          Object.const_set(:Spec, Module.new)
          ::Spec::const_set(:VERSION, Module.new)
          ::Spec::VERSION::const_set(:MAJOR, 1)
        end
        after  { Object.__send__(:remove_const, :Spec) }
        it "raises with a helpful message" do
          expect {
            config.load_spec_files
          }.to raise_error(/rspec-1 has been loaded/)
        end
      end
    end

    describe "#treat_symbols_as_metadata_keys_with_true_values?" do
      it 'defaults to false' do
        config.treat_symbols_as_metadata_keys_with_true_values?.should be_false
      end

      it 'can be set to true' do
        config.treat_symbols_as_metadata_keys_with_true_values = true
        config.treat_symbols_as_metadata_keys_with_true_values?.should be_true
      end
    end

    describe "#mock_framework" do
      it "defaults to :rspec" do
        config.should_receive(:require).with('rspec/core/mocking/with_rspec')
        config.mock_framework
      end
    end

    describe "#mock_framework="do
      it "delegates to mock_with" do
        config.should_receive(:mock_with).with(:rspec)
        config.mock_framework = :rspec
      end
    end

    shared_examples "a configurable framework adapter" do |m|
      it "yields a config object if the framework_module supports it" do
        custom_config = Struct.new(:custom_setting).new
        mod = Module.new
        mod.stub(:configuration => custom_config)

        config.send m, mod do |mod_config|
          mod_config.custom_setting = true
        end

        custom_config.custom_setting.should be_true
      end

      it "raises if framework module doesn't support configuration" do
        mod = Module.new

        lambda do
          config.send m, mod do |mod_config|
          end
        end.should raise_error(/must respond to `configuration`/)
      end
    end

    describe "#mock_with" do
      before { config.stub(:require) }

      it_behaves_like "a configurable framework adapter", :mock_with

      [:rspec, :mocha, :rr, :flexmock].each do |framework|
        context "with #{framework}" do
          it "requires the adapter for #{framework}" do
            config.should_receive(:require).with("rspec/core/mocking/with_#{framework}")
            config.mock_with framework
          end
        end
      end

      context "with a module" do
        it "sets the mock_framework_adapter to that module" do
          mod = Module.new
          config.mock_with mod
          config.mock_framework.should eq(mod)
        end
      end

      it "uses the null adapter when set to any unknown key" do
        config.should_receive(:require).with('rspec/core/mocking/with_absolutely_nothing')
        config.mock_with :crazy_new_mocking_framework_ive_not_yet_heard_of
      end

      context 'when there are already some example groups defined' do
        it 'raises an error since this setting must be applied before any groups are defined' do
          RSpec.world.stub(:example_groups).and_return([double.as_null_object])
          expect {
            config.mock_with :mocha
          }.to raise_error(/must be configured before any example groups are defined/)
        end

        it 'does not raise an error if the default `mock_with :rspec` is re-configured' do
          config.mock_framework # called by RSpec when configuring the first example group
          RSpec.world.stub(:example_groups).and_return([double.as_null_object])
          config.mock_with :rspec
        end

        it 'does not raise an error if re-setting the same config' do
          groups = []
          RSpec.world.stub(:example_groups => groups)
          config.mock_with :mocha
          groups << double.as_null_object
          config.mock_with :mocha
        end
      end
    end

    describe "#expectation_framework" do
      it "defaults to :rspec" do
        config.should_receive(:require).with('rspec/expectations')
        config.expectation_frameworks
      end
    end

    describe "#expectation_framework=" do
      it "delegates to expect_with=" do
        config.should_receive(:expect_with).with(:rspec)
        config.expectation_framework = :rspec
      end
    end

    describe "#expect_with" do
      before { config.stub(:require) }

      it_behaves_like "a configurable framework adapter", :expect_with

      [
        [:rspec,  'rspec/expectations'],
        [:stdlib, 'test/unit/assertions']
      ].each do |framework, required_file|
        context "with #{framework}" do
          it "requires #{required_file}" do
            config.should_receive(:require).with(required_file)
            config.expect_with framework
          end
        end
      end

      it "supports multiple calls" do
        config.expect_with :rspec
        config.expect_with :stdlib
        config.expectation_frameworks.should eq [RSpec::Matchers, Test::Unit::Assertions]
      end

      it "raises if block given with multiple args" do
        lambda do
          config.expect_with :rspec, :stdlib do |mod_config|
          end
        end.should raise_error(/expect_with only accepts/)
      end

      it "raises ArgumentError if framework is not supported" do
        expect do
          config.expect_with :not_supported
        end.to raise_error(ArgumentError)
      end

      context 'when there are already some example groups defined' do
        it 'raises an error since this setting must be applied before any groups are defined' do
          RSpec.world.stub(:example_groups).and_return([double.as_null_object])
          expect {
            config.expect_with :rspec
          }.to raise_error(/must be configured before any example groups are defined/)
        end

        it 'does not raise an error if the default `expect_with :rspec` is re-configured' do
          config.expectation_frameworks # called by RSpec when configuring the first example group
          RSpec.world.stub(:example_groups).and_return([double.as_null_object])
          config.expect_with :rspec
        end

        it 'does not raise an error if re-setting the same config' do
          groups = []
          RSpec.world.stub(:example_groups => groups)
          config.expect_with :stdlib
          groups << double.as_null_object
          config.expect_with :stdlib
        end
      end
    end

    describe "#expecting_with_rspec?" do
      before { config.stub(:require) }

      it "returns false by default" do
        config.should_not be_expecting_with_rspec
      end

      it "returns true when `expect_with :rspec` has been configured" do
        config.expect_with :rspec
        config.should be_expecting_with_rspec
      end

      it "returns true when `expect_with :rspec, :stdlib` has been configured" do
        config.expect_with :rspec, :stdlib
        config.should be_expecting_with_rspec
      end

      it "returns true when `expect_with :stdlib, :rspec` has been configured" do
        config.expect_with :stdlib, :rspec
        config.should be_expecting_with_rspec
      end

      it "returns false when `expect_with :stdlib` has been configured" do
        config.expect_with :stdlib
        config.should_not be_expecting_with_rspec
      end
    end

    describe "#files_to_run" do
      it "loads files not following pattern if named explicitly" do
        config.files_or_directories_to_run = "spec/rspec/core/resources/a_bar.rb"
        config.files_to_run.should eq([      "spec/rspec/core/resources/a_bar.rb"])
      end

      it "prevents repetition of dir when start of the pattern" do
        config.pattern = "spec/**/a_spec.rb"
        config.files_or_directories_to_run = "spec"
        config.files_to_run.should eq(["spec/rspec/core/resources/a_spec.rb"])
      end

      it "does not prevent repetition of dir when later of the pattern" do
        config.pattern = "rspec/**/a_spec.rb"
        config.files_or_directories_to_run = "spec"
        config.files_to_run.should eq(["spec/rspec/core/resources/a_spec.rb"])
      end

      context "with <path>:<line_number>" do
        it "overrides inclusion filters set on config" do
          config.filter_run_including :foo => :bar
          config.files_or_directories_to_run = "path/to/file.rb:37"
          config.inclusion_filter.size.should eq(1)
          config.inclusion_filter[:locations].keys.first.should match(/path\/to\/file\.rb$/)
          config.inclusion_filter[:locations].values.first.should eq([37])
        end

        it "overrides inclusion filters set before config" do
          config.force(:inclusion_filter => {:foo => :bar})
          config.files_or_directories_to_run = "path/to/file.rb:37"
          config.inclusion_filter.size.should eq(1)
          config.inclusion_filter[:locations].keys.first.should match(/path\/to\/file\.rb$/)
          config.inclusion_filter[:locations].values.first.should eq([37])
        end

        it "clears exclusion filters set on config" do
          config.exclusion_filter = { :foo => :bar }
          config.files_or_directories_to_run = "path/to/file.rb:37"
          config.exclusion_filter.should be_empty,
            "expected exclusion filter to be empty:\n#{config.exclusion_filter}"
        end

        it "clears exclusion filters set before config" do
          config.force(:exclusion_filter => { :foo => :bar })
          config.files_or_directories_to_run = "path/to/file.rb:37"
          config.exclusion_filter.should be_empty,
            "expected exclusion filter to be empty:\n#{config.exclusion_filter}"
        end
      end

      context "with default pattern" do
        it "loads files named _spec.rb" do
          config.files_or_directories_to_run = "spec/rspec/core/resources"
          config.files_to_run.should eq([      "spec/rspec/core/resources/a_spec.rb"])
        end

        it "loads files in Windows", :if => RSpec.windows_os? do
          config.files_or_directories_to_run = "C:\\path\\to\\project\\spec\\sub\\foo_spec.rb"
          config.files_to_run.should eq([      "C:/path/to/project/spec/sub/foo_spec.rb"])
        end

        it "loads files in Windows when directory is specified", :if => RSpec.windows_os? do
          config.files_or_directories_to_run = "spec\\rspec\\core\\resources"
          config.files_to_run.should eq([      "spec/rspec/core/resources/a_spec.rb"])
        end
      end

      context "with default default_path" do
        it "loads files in the default path when run by rspec" do
          config.stub(:command) { 'rspec' }
          config.files_or_directories_to_run = []
          config.files_to_run.should_not be_empty
        end

        it "does not load files in the default path when run by ruby" do
          config.stub(:command) { 'ruby' }
          config.files_or_directories_to_run = []
          config.files_to_run.should be_empty
        end
      end
    end

    %w[pattern= filename_pattern=].each do |setter|
      describe "##{setter}" do
        context "with single pattern" do
          before { config.send(setter, "**/*_foo.rb") }
          it "loads files following pattern" do
            file = File.expand_path(File.dirname(__FILE__) + "/resources/a_foo.rb")
            config.files_or_directories_to_run = file
            config.files_to_run.should include(file)
          end

          it "loads files in directories following pattern" do
            dir = File.expand_path(File.dirname(__FILE__) + "/resources")
            config.files_or_directories_to_run = dir
            config.files_to_run.should include("#{dir}/a_foo.rb")
          end

          it "does not load files in directories not following pattern" do
            dir = File.expand_path(File.dirname(__FILE__) + "/resources")
            config.files_or_directories_to_run = dir
            config.files_to_run.should_not include("#{dir}/a_bar.rb")
          end
        end

        context "with multiple patterns" do
          it "supports comma separated values" do
            config.send(setter, "**/*_foo.rb,**/*_bar.rb")
            dir = File.expand_path(File.dirname(__FILE__) + "/resources")
            config.files_or_directories_to_run = dir
            config.files_to_run.should include("#{dir}/a_foo.rb")
            config.files_to_run.should include("#{dir}/a_bar.rb")
          end

          it "supports comma separated values with spaces" do
            config.send(setter, "**/*_foo.rb, **/*_bar.rb")
            dir = File.expand_path(File.dirname(__FILE__) + "/resources")
            config.files_or_directories_to_run = dir
            config.files_to_run.should include("#{dir}/a_foo.rb")
            config.files_to_run.should include("#{dir}/a_bar.rb")
          end
        end
      end
    end

    describe "path with line number" do
      it "assigns the line number as a location filter" do
        config.files_or_directories_to_run = "path/to/a_spec.rb:37"
        config.filter.should eq({:locations => {File.expand_path("path/to/a_spec.rb") => [37]}})
      end
    end

    context "with full_description" do
      it "overrides filters" do
        config.filter_run :focused => true
        config.full_description = "foo"
        config.filter.should_not have_key(:focused)
      end
    end

    context "with line number" do

      it "assigns the file and line number as a location filter" do
        config.files_or_directories_to_run = "path/to/a_spec.rb:37"
        config.filter.should eq({:locations => {File.expand_path("path/to/a_spec.rb") => [37]}})
      end

      it "assigns multiple files with line numbers as location filters" do
        config.files_or_directories_to_run = "path/to/a_spec.rb:37", "other_spec.rb:44"
        config.filter.should eq({:locations => {File.expand_path("path/to/a_spec.rb") => [37],
                                                File.expand_path("other_spec.rb") => [44]}})
      end

      it "assigns files with multiple line numbers as location filters" do
        config.files_or_directories_to_run = "path/to/a_spec.rb:37", "path/to/a_spec.rb:44"
        config.filter.should eq({:locations => {File.expand_path("path/to/a_spec.rb") => [37, 44]}})
      end
    end

    context "with multiple line numbers" do
      it "assigns the file and line numbers as a location filter" do
        config.files_or_directories_to_run = "path/to/a_spec.rb:1:3:5:7"
        config.filter.should eq({:locations => {File.expand_path("path/to/a_spec.rb") => [1,3,5,7]}})
      end
    end

    it "assigns the example name as the filter on description" do
      config.full_description = "foo"
      config.filter.should eq({:full_description => /foo/})
    end

    it "assigns the example names as the filter on description if description is an array" do
      config.full_description = [ "foo", "bar" ]
      config.filter.should eq({:full_description => Regexp.union(/foo/, /bar/)})
    end

    describe "#default_path" do
      it 'defaults to "spec"' do
        config.default_path.should eq('spec')
      end
    end

    describe "#include" do

      module InstanceLevelMethods
        def you_call_this_a_blt?
          "egad man, where's the mayo?!?!?"
        end
      end

      it_behaves_like "metadata hash builder" do
        def metadata_hash(*args)
          config.include(InstanceLevelMethods, *args)
          config.include_or_extend_modules.last.last
        end
      end

      context "with no filter" do
        it "includes the given module into each example group" do
          RSpec.configure do |c|
            c.include(InstanceLevelMethods)
          end

          group = ExampleGroup.describe('does like, stuff and junk', :magic_key => :include) { }
          group.should_not respond_to(:you_call_this_a_blt?)
          group.new.you_call_this_a_blt?.should eq("egad man, where's the mayo?!?!?")
        end
      end

      context "with a filter" do
        it "includes the given module into each matching example group" do
          RSpec.configure do |c|
            c.include(InstanceLevelMethods, :magic_key => :include)
          end

          group = ExampleGroup.describe('does like, stuff and junk', :magic_key => :include) { }
          group.should_not respond_to(:you_call_this_a_blt?)
          group.new.you_call_this_a_blt?.should eq("egad man, where's the mayo?!?!?")
        end
      end

    end

    describe "#extend" do

      module ThatThingISentYou
        def that_thing
        end
      end

      it_behaves_like "metadata hash builder" do
        def metadata_hash(*args)
          config.extend(ThatThingISentYou, *args)
          config.include_or_extend_modules.last.last
        end
      end

      it "extends the given module into each matching example group" do
        RSpec.configure do |c|
          c.extend(ThatThingISentYou, :magic_key => :extend)
        end

        group = ExampleGroup.describe(ThatThingISentYou, :magic_key => :extend) { }
        group.should respond_to(:that_thing)
      end

    end

    describe "#run_all_when_everything_filtered?" do

      it "defaults to false" do
        config.run_all_when_everything_filtered?.should be_false
      end

      it "can be queried with question method" do
        config.run_all_when_everything_filtered = true
        config.run_all_when_everything_filtered?.should be_true
      end
    end

    %w[color color_enabled].each do |color_option|
      describe "##{color_option}=" do
        context "given true" do
          context "with non-tty output and no autotest" do
            it "does not set color_enabled" do
              config.output_stream = StringIO.new
              config.output_stream.stub(:tty?) { false }
              config.tty = false
              config.send "#{color_option}=", true
              config.send(color_option).should be_false
            end
          end

          context "with tty output" do
            it "does not set color_enabled" do
              config.output_stream = StringIO.new
              config.output_stream.stub(:tty?) { true }
              config.tty = false
              config.send "#{color_option}=", true
              config.send(color_option).should be_true
            end
          end

          context "with tty set" do
            it "does not set color_enabled" do
              config.output_stream = StringIO.new
              config.output_stream.stub(:tty?) { false }
              config.tty = true
              config.send "#{color_option}=", true
              config.send(color_option).should be_true
            end
          end

          context "on windows" do
            before do
              @original_host  = RbConfig::CONFIG['host_os']
              RbConfig::CONFIG['host_os'] = 'mingw'
              config.stub(:require)
              config.stub(:warn)
            end

            after do
              RbConfig::CONFIG['host_os'] = @original_host
            end

            context "with ANSICON available" do
              before(:all) do
                @original_ansicon = ENV['ANSICON']
                ENV['ANSICON'] = 'ANSICON'
              end

              after(:all) do
                ENV['ANSICON'] = @original_ansicon
              end

              it "enables colors" do
                config.output_stream = StringIO.new
                config.output_stream.stub(:tty?) { true }
                config.send "#{color_option}=", true
                config.send(color_option).should be_true
              end

              it "leaves output stream intact" do
                config.output_stream = $stdout
                config.stub(:require) do |what|
                  config.output_stream = 'foo' if what =~ /Win32/
                end
                config.send "#{color_option}=", true
                config.output_stream.should eq($stdout)
              end
            end

            context "with ANSICON NOT available" do
              it "warns to install ANSICON" do
                config.stub(:require) { raise LoadError }
                config.should_receive(:warn).
                  with(/You must use ANSICON/)
                config.send "#{color_option}=", true
              end

              it "sets color_enabled to false" do
                config.stub(:require) { raise LoadError }
                config.send "#{color_option}=", true
                config.color_enabled = true
                config.send(color_option).should be_false
              end
            end
          end
        end
      end

      it "prefers incoming cli_args" do
        config.output_stream = StringIO.new
        config.output_stream.stub :tty? => true
        config.force :color => true
        config.color = false
        config.color.should be_true
      end
    end

    describe '#formatter=' do
      it "delegates to add_formatter (better API for user-facing configuration)" do
        config.should_receive(:add_formatter).with('these','options')
        config.add_formatter('these','options')
      end
    end

    describe "#add_formatter" do

      it "adds to the list of formatters" do
        config.add_formatter :documentation
        config.formatters.first.should be_an_instance_of(Formatters::DocumentationFormatter)
      end

      it "finds a formatter by name (w/ Symbol)" do
        config.add_formatter :documentation
        config.formatters.first.should be_an_instance_of(Formatters::DocumentationFormatter)
      end

      it "finds a formatter by name (w/ String)" do
        config.add_formatter 'documentation'
        config.formatters.first.should be_an_instance_of(Formatters::DocumentationFormatter)
      end

      it "finds a formatter by class" do
        formatter_class = Class.new(Formatters::BaseTextFormatter)
        config.add_formatter formatter_class
        config.formatters.first.should be_an_instance_of(formatter_class)
      end

      it "finds a formatter by class name" do
        Object.const_set("ACustomFormatter", Class.new(Formatters::BaseFormatter))
        config.add_formatter "ACustomFormatter"
        config.formatters.first.should be_an_instance_of(ACustomFormatter)
      end

      it "finds a formatter by class fully qualified name" do
        RSpec.const_set("CustomFormatter", Class.new(Formatters::BaseFormatter))
        config.add_formatter "RSpec::CustomFormatter"
        config.formatters.first.should be_an_instance_of(RSpec::CustomFormatter)
      end

      it "requires a formatter file based on its fully qualified name" do
        config.should_receive(:require).with('rspec/custom_formatter2') do
          RSpec.const_set("CustomFormatter2", Class.new(Formatters::BaseFormatter))
        end
        config.add_formatter "RSpec::CustomFormatter2"
        config.formatters.first.should be_an_instance_of(RSpec::CustomFormatter2)
      end

      it "raises NameError if class is unresolvable" do
        config.should_receive(:require).with('rspec/custom_formatter3')
        lambda { config.add_formatter "RSpec::CustomFormatter3" }.should raise_error(NameError)
      end

      it "raises ArgumentError if formatter is unknown" do
        lambda { config.add_formatter :progresss }.should raise_error(ArgumentError)
      end

      context "with a 2nd arg defining the output" do
        it "creates a file at that path and sets it as the output" do
          path = File.join(Dir.tmpdir, 'output.txt')
          config.add_formatter('doc', path)
          config.formatters.first.output.should be_a(File)
          config.formatters.first.output.path.should eq(path)
        end
      end
    end

    describe "#filter_run_including" do
      it_behaves_like "metadata hash builder" do
        def metadata_hash(*args)
          config.filter_run_including(*args)
          config.inclusion_filter
        end
      end

      it "sets the filter with a hash" do
        config.filter_run_including :foo => true
        config.inclusion_filter[:foo].should be(true)
      end

      it "sets the filter with a symbol" do
        RSpec.configuration.stub(:treat_symbols_as_metadata_keys_with_true_values? => true)
        config.filter_run_including :foo
        config.inclusion_filter[:foo].should be(true)
      end

      it "merges with existing filters" do
        config.filter_run_including :foo => true
        config.filter_run_including :bar => false

        config.inclusion_filter[:foo].should be(true)
        config.inclusion_filter[:bar].should be(false)
      end
    end

    describe "#filter_run_excluding" do
      it_behaves_like "metadata hash builder" do
        def metadata_hash(*args)
          config.filter_run_excluding(*args)
          config.exclusion_filter
        end
      end

      it "sets the filter" do
        config.filter_run_excluding :foo => true
        config.exclusion_filter[:foo].should be(true)
      end

      it "sets the filter using a symbol" do
        RSpec.configuration.stub(:treat_symbols_as_metadata_keys_with_true_values? => true)
        config.filter_run_excluding :foo
        config.exclusion_filter[:foo].should be(true)
      end

      it "merges with existing filters" do
        config.filter_run_excluding :foo => true
        config.filter_run_excluding :bar => false

        config.exclusion_filter[:foo].should be(true)
        config.exclusion_filter[:bar].should be(false)
      end
    end

    describe "#inclusion_filter" do
      it "returns {} even if set to nil" do
        config.inclusion_filter = nil
        config.inclusion_filter.should eq({})
      end
    end

    describe "#inclusion_filter=" do
      it "treats symbols as hash keys with true values when told to" do
        RSpec.configuration.stub(:treat_symbols_as_metadata_keys_with_true_values? => true)
        config.inclusion_filter = :foo
        config.inclusion_filter.should eq({:foo => true})
      end

      it "overrides any inclusion filters set on the command line or in configuration files" do
        config.force(:inclusion_filter => { :foo => :bar })
        config.inclusion_filter = {:want => :this}
        config.inclusion_filter.should eq({:want => :this})
      end
    end

    describe "#exclusion_filter" do
      it "returns {} even if set to nil" do
        config.exclusion_filter = nil
        config.exclusion_filter.should eq({})
      end

      describe "the default :if filter" do
        it "does not exclude a spec with  { :if => true } metadata" do
          config.exclusion_filter[:if].call(true).should be_false
        end

        it "excludes a spec with  { :if => false } metadata" do
          config.exclusion_filter[:if].call(false).should be_true
        end

        it "excludes a spec with  { :if => nil } metadata" do
          config.exclusion_filter[:if].call(nil).should be_true
        end
      end

      describe "the default :unless filter" do
        it "excludes a spec with  { :unless => true } metadata" do
          config.exclusion_filter[:unless].call(true).should be_true
        end

        it "does not exclude a spec with { :unless => false } metadata" do
          config.exclusion_filter[:unless].call(false).should be_false
        end

        it "does not exclude a spec with { :unless => nil } metadata" do
          config.exclusion_filter[:unless].call(nil).should be_false
        end
      end
    end

    describe "#exclusion_filter=" do
      it "treats symbols as hash keys with true values when told to" do
        RSpec.configuration.stub(:treat_symbols_as_metadata_keys_with_true_values? => true)
        config.exclusion_filter = :foo
        config.exclusion_filter.should eq({:foo => true})
      end

      it "overrides any exclusion filters set on the command line or in configuration files" do
        config.force(:exclusion_filter => { :foo => :bar })
        config.exclusion_filter = {:want => :this}
        config.exclusion_filter.should eq({:want => :this})
      end
    end

    describe "line_numbers=" do
      before { config.filter_manager.stub(:warn) }

      it "sets the line numbers" do
        config.line_numbers = ['37']
        config.filter.should eq({:line_numbers => [37]})
      end

      it "overrides filters" do
        config.filter_run :focused => true
        config.line_numbers = ['37']
        config.filter.should eq({:line_numbers => [37]})
      end

      it "prevents subsequent filters" do
        config.line_numbers = ['37']
        config.filter_run :focused => true
        config.filter.should eq({:line_numbers => [37]})
      end
    end

    describe "#full_backtrace=" do
      context "given true" do
        it "clears the backtrace clean patterns" do
          config.full_backtrace = true
          config.backtrace_clean_patterns.should eq([])
        end
      end

      context "given false" do
        it "restores backtrace clean patterns" do
          config.full_backtrace = false
          config.backtrace_clean_patterns.should eq(RSpec::Core::Configuration::DEFAULT_BACKTRACE_PATTERNS)
        end
      end

      it "doesn't impact other instances of config" do
        config_1 = Configuration.new
        config_2 = Configuration.new

        config_1.full_backtrace = true
        config_2.backtrace_clean_patterns.should_not be_empty
      end
    end

    describe "#cleaned_from_backtrace? defaults" do
      it "returns true for rspec files" do
        config.cleaned_from_backtrace?("lib/rspec/core.rb").
          should be_true
      end

      it "returns true for spec_helper" do
        config.cleaned_from_backtrace?("spec/spec_helper.rb").
          should be_true
      end

      it "returns true for java files (for JRuby)" do
        config.cleaned_from_backtrace?("org/jruby/RubyArray.java:2336").
          should be_true
      end
    end

    describe "#debug=true" do
      before do
        if defined?(Debugger)
          @orig_debugger = Debugger
          Object.send(:remove_const, :Debugger)
        else
          @orig_debugger = nil
        end
        Object.const_set("Debugger", debugger)
      end

      after do
        Object.send(:remove_const, :Debugger)
        Object.const_set("Debugger", @orig_debugger) if @orig_debugger
      end

      let(:debugger) { double('Debugger').as_null_object }

      it "requires 'ruby-debug'" do
        config.should_receive(:require).with('ruby-debug')
        config.debug = true
      end

      it "starts the debugger" do
        config.stub(:require)
        debugger.should_receive(:start)
        config.debug = true
      end
    end

    describe "#debug=false" do
      it "does not require 'ruby-debug'" do
        config.should_not_receive(:require).with('ruby-debug')
        config.debug = false
      end
    end

    describe "#output=" do
      it "sets the output" do
        output = mock("output")
        config.output = output
        config.output.should equal(output)
      end
    end

    describe "#libs=" do
      it "adds directories to the LOAD_PATH" do
        $LOAD_PATH.should_receive(:unshift).with("a/dir")
        config.libs = ["a/dir"]
      end
    end

    describe "#requires=" do
      it "requires paths" do
        config.should_receive(:require).with("a/path")
        config.requires = ["a/path"]
      end
    end

    describe "#add_setting" do
      describe "with no modifiers" do
        context "with no additional options" do
          before do
            config.add_setting :custom_option
          end

          it "defaults to nil" do
            config.custom_option.should be_nil
          end

          it "adds a predicate" do
            config.custom_option?.should be_false
          end

          it "can be overridden" do
            config.custom_option = "a value"
            config.custom_option.should eq("a value")
          end
        end

        context "with :default => 'a value'" do
          before do
            config.add_setting :custom_option, :default => 'a value'
          end

          it "defaults to 'a value'" do
            config.custom_option.should eq("a value")
          end

          it "returns true for the predicate" do
            config.custom_option?.should be_true
          end

          it "can be overridden with a truthy value" do
            config.custom_option = "a new value"
            config.custom_option.should eq("a new value")
          end

          it "can be overridden with nil" do
            config.custom_option = nil
            config.custom_option.should eq(nil)
          end

          it "can be overridden with false" do
            config.custom_option = false
            config.custom_option.should eq(false)
          end
        end
      end

      context "with :alias => " do
        it "is deprecated" do
          RSpec::should_receive(:warn).with(/deprecated/)
          config.add_setting :custom_option
          config.add_setting :another_custom_option, :alias => :custom_option
        end
      end

      context "with :alias_with => " do
        before do
          config.add_setting :custom_option, :alias_with => :another_custom_option
        end

        it "delegates the getter to the other option" do
          config.another_custom_option = "this value"
          config.custom_option.should eq("this value")
        end

        it "delegates the setter to the other option" do
          config.custom_option = "this value"
          config.another_custom_option.should eq("this value")
        end

        it "delegates the predicate to the other option" do
          config.custom_option = true
          config.another_custom_option?.should be_true
        end
      end
    end

    describe "#configure_group" do
      it "extends with 'extend'" do
        mod = Module.new
        group = ExampleGroup.describe("group", :foo => :bar)

        config.extend(mod, :foo => :bar)
        config.configure_group(group)
        group.should be_a(mod)
      end

      it "extends with 'module'" do
        mod = Module.new
        group = ExampleGroup.describe("group", :foo => :bar)

        config.include(mod, :foo => :bar)
        config.configure_group(group)
        group.included_modules.should include(mod)
      end

      it "requires only one matching filter" do
        mod = Module.new
        group = ExampleGroup.describe("group", :foo => :bar)

        config.include(mod, :foo => :bar, :baz => :bam)
        config.configure_group(group)
        group.included_modules.should include(mod)
      end

      it "includes each one before deciding whether to include the next" do
        mod1 = Module.new do
          def self.included(host)
            host.metadata[:foo] = :bar
          end
        end
        mod2 = Module.new

        group = ExampleGroup.describe("group")

        config.include(mod1)
        config.include(mod2, :foo => :bar)
        config.configure_group(group)
        group.included_modules.should include(mod1)
        group.included_modules.should include(mod2)
      end

      module IncludeOrExtendMeOnce
        def self.included(host)
          raise "included again" if host.instance_methods.include?(:foobar)
          host.class_eval { def foobar; end }
        end

        def self.extended(host)
          raise "extended again" if host.respond_to?(:foobar)
          def host.foobar; end
        end
      end

      it "doesn't include a module when already included in ancestor" do
        config.include(IncludeOrExtendMeOnce, :foo => :bar)

        group = ExampleGroup.describe("group", :foo => :bar)
        child = group.describe("child")

        config.configure_group(group)
        config.configure_group(child)
      end

      it "doesn't extend when ancestor is already extended with same module" do
        config.extend(IncludeOrExtendMeOnce, :foo => :bar)

        group = ExampleGroup.describe("group", :foo => :bar)
        child = group.describe("child")

        config.configure_group(group)
        config.configure_group(child)
      end
    end

    describe "#alias_example_to" do
      it_behaves_like "metadata hash builder" do
        after do
          RSpec::Core::ExampleGroup.module_eval do
            class << self
              undef :my_example_method if method_defined? :my_example_method
            end
          end
        end
        def metadata_hash(*args)
          config.alias_example_to :my_example_method, *args
          group = ExampleGroup.describe("group")
          example = group.my_example_method("description")
          example.metadata
        end
      end
    end

    describe "#reset" do
      it "clears the reporter" do
        config.reporter.should_not be_nil
        config.reset
        config.instance_variable_get("@reporter").should be_nil
      end

      it "clears the formatters" do
        config.add_formatter "doc"
        config.reset
        config.formatters.should be_empty
      end
    end

    describe "#force" do
      it "forces order" do
        config.force :order => "default"
        config.order = "rand"
        config.order.should eq("default")
      end

      it "forces order and seed with :order => 'rand:37'" do
        config.force :order => "rand:37"
        config.order = "default"
        config.order.should eq("rand")
        config.seed.should eq(37)
      end

      it "forces order and seed with :seed => '37'" do
        config.force :seed => "37"
        config.order = "default"
        config.seed.should eq(37)
        config.order.should eq("rand")
      end

      it "forces 'false' value" do
        config.add_setting :custom_option
        config.custom_option = true
        config.custom_option?.should be_true
        config.force :custom_option => false
        config.custom_option?.should be_false
        config.custom_option = true
        config.custom_option?.should be_false
      end
    end

    describe '#seed' do
      it 'returns the seed as an int' do
        config.seed = '123'
        config.seed.should eq(123)
      end
    end

    describe '#randomize?' do
      context 'with order set to :random' do
        before { config.order = :random }

        it 'returns true' do
          config.randomize?.should be_true
        end
      end

      context 'with order set to nil' do
        before { config.order = nil }

        it 'returns false' do
          config.randomize?.should be_false
        end
      end
    end

    describe '#order=' do
      context 'given "random:123"' do
        before { config.order = 'random:123' }

        it 'sets order to "random"' do
          config.order.should eq('random')
        end

        it 'sets seed to 123' do
          config.seed.should eq(123)
        end
      end

      context 'given "default"' do
        before do
          config.order = 'rand:123'
          config.order = 'default'
        end

        it "sets the order to nil" do
          config.order.should be_nil
        end

        it "sets the seed to nil" do
          config.seed.should be_nil
        end
      end
    end
  end
end
