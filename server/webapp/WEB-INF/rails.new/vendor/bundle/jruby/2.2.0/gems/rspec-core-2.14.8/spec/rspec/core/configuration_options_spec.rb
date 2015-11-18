require 'spec_helper'
require 'ostruct'
require 'rspec/core/drb_options'

describe RSpec::Core::ConfigurationOptions, :isolated_directory => true, :isolated_home => true do
  include ConfigOptionsHelper

  it "warns when HOME env var is not set", :unless => (RUBY_PLATFORM == 'java') do
    without_env_vars 'HOME' do
      coo = RSpec::Core::ConfigurationOptions.new([])
      coo.should_receive(:warn)
      coo.parse_options
    end
  end

  it "duplicates the arguments array" do
    args = ['-e', 'some spec']
    coo = RSpec::Core::ConfigurationOptions.new(args)
    coo.parse_options
    expect(args).to eq(['-e', 'some spec'])
  end

  describe "#configure" do
    it "sends libs before requires" do
      opts = config_options_object(*%w[--require a/path -I a/lib])
      config = double("config").as_null_object
      config.should_receive(:libs=).ordered
      config.should_receive(:setup_load_path_and_require).ordered
      opts.configure(config)
    end

    it "sends loads requires before loading specs" do
      opts = config_options_object(*%w[-rspec_helper])
      config = double("config").as_null_object
      expect(config).to receive(:setup_load_path_and_require).ordered
      expect(config).to receive(:files_or_directories_to_run=).ordered
      opts.configure(config)
    end

    it "sets up load path and requires before formatter" do
      opts = config_options_object(*%w[--require a/path -f a/formatter])
      config = double("config").as_null_object
      config.should_receive(:setup_load_path_and_require).ordered
      config.should_receive(:add_formatter).ordered
      opts.configure(config)
    end

    it "sends default_path before files_or_directories_to_run" do
      opts = config_options_object(*%w[--default_path spec])
      config = double("config").as_null_object
      config.should_receive(:force).with(:default_path => 'spec').ordered
      config.should_receive(:files_or_directories_to_run=).ordered
      opts.configure(config)
    end

    it "sends pattern before files_or_directories_to_run" do
      opts = config_options_object(*%w[--pattern **/*.spec])
      config = double("config").as_null_object
      config.should_receive(:force).with(:pattern => '**/*.spec').ordered
      config.should_receive(:files_or_directories_to_run=).ordered
      opts.configure(config)
    end

    it "assigns inclusion_filter" do
      opts = config_options_object(*%w[--tag awesome])
      config = RSpec::Core::Configuration.new
      opts.configure(config)
      expect(config.inclusion_filter).to have_key(:awesome)
    end

    it "merges the :exclusion_filter option with the default exclusion_filter" do
      opts = config_options_object(*%w[--tag ~slow])
      config = RSpec::Core::Configuration.new
      opts.configure(config)
      expect(config.exclusion_filter).to have_key(:slow)
    end

    it "forces color_enabled" do
      opts = config_options_object(*%w[--color])
      config = RSpec::Core::Configuration.new
      config.should_receive(:force).with(:color => true)
      opts.configure(config)
    end

    [
      ["--failure-exit-code", "3", :failure_exit_code, 3 ],
      ["--pattern", "foo/bar", :pattern, "foo/bar"],
      ["--failure-exit-code", "37", :failure_exit_code, 37],
      ["--default_path", "behavior", :default_path, "behavior"],
      ["--order", "rand", :order, "rand"],
      ["--seed", "37", :order, "rand:37"],
      ["--drb-port", "37", :drb_port, 37]
    ].each do |cli_option, cli_value, config_key, config_value|
      it "forces #{config_key}" do
        opts = config_options_object(*[cli_option, cli_value].compact)
        config = RSpec::Core::Configuration.new
        config.should_receive(:force) do |pair|
          expect(pair.keys.first).to eq(config_key)
          expect(pair.values.first).to eq(config_value)
        end
        opts.configure(config)
      end
    end

    it "sets debug directly" do
      opts = config_options_object("--debug")
      config = RSpec::Core::Configuration.new
      config.should_receive(:debug=).with(true)
      opts.configure(config)
    end

    it "merges --require specified by multiple configuration sources" do
      with_env_vars 'SPEC_OPTS' => "--require file_from_env" do
        opts = config_options_object(*%w[--require file_from_opts])
        config = RSpec::Core::Configuration.new
        config.should_receive(:require).with("file_from_opts")
        config.should_receive(:require).with("file_from_env")
        opts.configure(config)
      end
    end

    it "merges --I specified by multiple configuration sources" do
      with_env_vars 'SPEC_OPTS' => "-I dir_from_env" do
        opts = config_options_object(*%w[-I dir_from_opts])
        config = RSpec::Core::Configuration.new
        config.should_receive(:libs=).with(["dir_from_opts", "dir_from_env"])
        opts.configure(config)
      end
    end
  end

  describe "-c, --color, and --colour" do
    it "sets :color => true" do
      expect(parse_options('-c')).to include(:color => true)
      expect(parse_options('--color')).to include(:color => true)
      expect(parse_options('--colour')).to include(:color => true)
    end
  end

  describe "--no-color" do
    it "sets :color => false" do
      expect(parse_options('--no-color')).to include(:color => false)
    end

    it "overrides previous :color => true" do
      expect(parse_options('--color', '--no-color')).to include(:color => false)
    end

    it "gets overriden by a subsequent :color => true" do
      expect(parse_options('--no-color', '--color')).to include(:color => true)
    end
  end

  describe "-I" do
    example "adds to :libs" do
      expect(parse_options('-I', 'a_dir')).to include(:libs => ['a_dir'])
    end
    example "can be used more than once" do
      expect(parse_options('-I', 'dir_1', '-I', 'dir_2')).to include(:libs => ['dir_1','dir_2'])
    end
  end

  describe '--require' do
    example "requires files" do
      expect(parse_options('--require', 'a/path')).to include(:requires => ['a/path'])
    end
    example "can be used more than once" do
      expect(parse_options('--require', 'path/1', '--require', 'path/2')).to include(:requires => ['path/1','path/2'])
    end
  end

  describe "--format, -f" do
    it "sets :formatter" do
      [['--format', 'd'], ['-f', 'd'], '-fd'].each do |args|
        expect(parse_options(*args)).to include(:formatters => [['d']])
      end
    end

    example "can accept a class name" do
      expect(parse_options('-fSome::Formatter::Class')).to include(:formatters => [['Some::Formatter::Class']])
    end
  end

  describe "--profile, -p" do
    it "sets :profile_examples" do
      expect(parse_options('-p')).to include(:profile_examples => true)
      expect(parse_options('--profile')).to include(:profile_examples => true)
      expect(parse_options('-p', '4')).to include(:profile_examples => 4)
      expect(parse_options('--profile', '3')).to include(:profile_examples => 3)
    end
  end

  describe "--no-profile" do
    it "sets :profile_examples to false" do
      expect(parse_options('--no-profile')).to include(:profile_examples => false)
    end
  end

  describe '--line_number' do
    it "sets :line_number" do
      expect(parse_options('-l','3')).to include(:line_numbers => ['3'])
      expect(parse_options('--line_number','3')).to include(:line_numbers => ['3'])
    end

    it "can be specified multiple times" do
      expect(parse_options('-l','3', '-l', '6')).to include(:line_numbers => ['3', '6'])
      expect(parse_options('--line_number','3', '--line_number', '6')).to include(:line_numbers => ['3', '6'])
    end
  end

  describe "--example" do
    it "sets :full_description" do
      expect(parse_options('--example','foo')).to include(:full_description => [/foo/])
      expect(parse_options('-e','bar')).to include(:full_description => [/bar/])
    end
  end

  describe "--backtrace, -b" do
    it "sets full_backtrace on config" do
      expect(parse_options("--backtrace")).to include(:full_backtrace => true)
      expect(parse_options("-b")).to include(:full_backtrace => true)
    end
  end

  describe "--debug, -d" do
    it "sets :debug => true" do
      expect(parse_options("--debug")).to include(:debug => true)
      expect(parse_options("-d")).to include(:debug => true)
    end
  end

  describe "--fail-fast" do
    it "defaults to false" do
      expect(parse_options[:fail_fast]).to be_false
    end

    it "sets fail_fast on config" do
      expect(parse_options("--fail-fast")[:fail_fast]).to be_true
    end
  end

  describe "--failure-exit-code" do
    it "sets :failure_exit_code" do
      expect(parse_options('--failure-exit-code', '0')).to include(:failure_exit_code => 0)
      expect(parse_options('--failure-exit-code', '1')).to include(:failure_exit_code => 1)
      expect(parse_options('--failure-exit-code', '2')).to include(:failure_exit_code => 2)
    end

    it "overrides previous :failure_exit_code" do
      expect(parse_options('--failure-exit-code', '2', '--failure-exit-code', '3')).to include(:failure_exit_code => 3)
    end
  end

  describe "--options" do
    it "sets :custom_options_file" do
      expect(parse_options(*%w[-O my.opts])).to include(:custom_options_file => "my.opts")
      expect(parse_options(*%w[--options my.opts])).to include(:custom_options_file => "my.opts")
    end
  end

  describe "--drb, -X" do
    context "combined with --debug" do
      it "turns off the debugger if --drb is specified first" do
        expect(config_options_object("--drb", "--debug").drb_argv).not_to include("--debug")
        expect(config_options_object("--drb", "-d"     ).drb_argv).not_to include("--debug")
        expect(config_options_object("-X",    "--debug").drb_argv).not_to include("--debug")
        expect(config_options_object("-X",    "-d"     ).drb_argv).not_to include("--debug")
      end

      it "turns off the debugger option if --drb is specified later" do
        expect(config_options_object("--debug", "--drb").drb_argv).not_to include("--debug")
        expect(config_options_object("-d",      "--drb").drb_argv).not_to include("--debug")
        expect(config_options_object("--debug", "-X"   ).drb_argv).not_to include("--debug")
        expect(config_options_object("-d",      "-X"   ).drb_argv).not_to include("--debug")
      end

      it "turns off the debugger option if --drb is specified in the options file" do
        File.open("./.rspec", "w") {|f| f << "--drb"}
        expect(config_options_object("--debug").drb_argv).not_to include("--debug")
        expect(config_options_object("-d"     ).drb_argv).not_to include("--debug")
      end

      it "turns off the debugger option if --debug is specified in the options file" do
        File.open("./.rspec", "w") {|f| f << "--debug"}
        expect(config_options_object("--drb").drb_argv).not_to include("--debug")
        expect(config_options_object("-X"   ).drb_argv).not_to include("--debug")
      end
    end

    it "does not send --drb back to the parser after parsing options" do
      expect(config_options_object("--drb", "--color").drb_argv).not_to include("--drb")
    end

  end

  describe "--no-drb" do
    it "disables drb" do
      expect(parse_options("--no-drb")).to include(:drb => false)
    end

    it "overrides a previous drb => true" do
      expect(parse_options("--drb", "--no-drb")).to include(:drb => false)
    end

    it "gets overriden by a subsquent drb => true" do
      expect(parse_options("--no-drb", "--drb")).to include(:drb => true)
    end
  end


  describe "files_or_directories_to_run" do
    it "parses files from '-c file.rb dir/file.rb'" do
      expect(parse_options("-c", "file.rb", "dir/file.rb")).to include(
        :files_or_directories_to_run => ["file.rb", "dir/file.rb"]
      )
    end

    it "parses dir from 'dir'" do
      expect(parse_options("dir")).to include(:files_or_directories_to_run => ["dir"])
    end

    it "parses dir and files from 'spec/file1_spec.rb, spec/file2_spec.rb'" do
      expect(parse_options("dir", "spec/file1_spec.rb", "spec/file2_spec.rb")).to include(
        :files_or_directories_to_run => ["dir", "spec/file1_spec.rb", "spec/file2_spec.rb"]
      )
    end

    it "provides no files or directories if spec directory does not exist" do
      FileTest.stub(:directory?).with("spec").and_return false
      expect(parse_options()).to include(:files_or_directories_to_run => [])
    end
  end

  describe "default_path" do
    it "gets set before files_or_directories_to_run" do
      config = double("config").as_null_object
      config.should_receive(:force).with(:default_path => 'foo').ordered
      config.should_receive(:files_or_directories_to_run=).ordered
      opts = config_options_object("--default_path", "foo")
      opts.configure(config)
    end
  end

  describe "#filter_manager" do
    it "returns the same object as RSpec::configuration.filter_manager" do
      expect(config_options_object.filter_manager).to be(RSpec::configuration.filter_manager)
    end
  end

  describe "sources: ~/.rspec, ./.rspec, ./.rspec-local, custom, CLI, and SPEC_OPTS" do
    it "merges global, local, SPEC_OPTS, and CLI" do
      File.open("./.rspec", "w") {|f| f << "--line 37"}
      File.open("./.rspec-local", "w") {|f| f << "--format global"}
      File.open(File.expand_path("~/.rspec"), "w") {|f| f << "--color"}
      with_env_vars 'SPEC_OPTS' => "--debug --example 'foo bar'" do
        options = parse_options("--drb")
        expect(options[:color]).to be_true
        expect(options[:line_numbers]).to eq(["37"])
        expect(options[:debug]).to be_true
        expect(options[:full_description]).to eq([/foo\ bar/])
        expect(options[:drb]).to be_true
        expect(options[:formatters]).to eq([['global']])
      end
    end

    it "prefers SPEC_OPTS over CLI" do
      with_env_vars 'SPEC_OPTS' => "--format spec_opts" do
        expect(parse_options("--format", "cli")[:formatters]).to eq([['spec_opts']])
      end
    end

    it "prefers CLI over file options" do
      File.open("./.rspec", "w") {|f| f << "--format project"}
      File.open(File.expand_path("~/.rspec"), "w") {|f| f << "--format global"}
      expect(parse_options("--format", "cli")[:formatters]).to eq([['cli']])
    end

    it "prefers project file options over global file options" do
      File.open("./.rspec", "w") {|f| f << "--format project"}
      File.open(File.expand_path("~/.rspec"), "w") {|f| f << "--format global"}
      expect(parse_options[:formatters]).to eq([['project']])
    end

    it "prefers local file options over project file options" do
      File.open("./.rspec-local", "w") {|f| f << "--format local"}
      File.open("./.rspec", "w") {|f| f << "--format global"}
      expect(parse_options[:formatters]).to eq([['local']])
    end

    it "parses options file correctly if erb code has trimming options" do
      File.open("./.rspec", "w") do |f|
        f << "<% if true -%>\n"
        f << "--format local\n"
        f << "<%- end %>\n"
      end

      expect(parse_options[:formatters]).to eq([['local']])
    end

    context "with custom options file" do
      it "ignores project and global options files" do
        File.open("./.rspec", "w") {|f| f << "--format project"}
        File.open(File.expand_path("~/.rspec"), "w") {|f| f << "--format global"}
        File.open("./custom.opts", "w") {|f| f << "--color"}
        options = parse_options("-O", "./custom.opts")
        expect(options[:format]).to be_nil
        expect(options[:color]).to be_true
      end

      it "parses -e 'full spec description'" do
        File.open("./custom.opts", "w") {|f| f << "-e 'The quick brown fox jumps over the lazy dog'"}
        options = parse_options("-O", "./custom.opts")
        expect(options[:full_description]).to eq([/The\ quick\ brown\ fox\ jumps\ over\ the\ lazy\ dog/])
      end
    end
  end
end
