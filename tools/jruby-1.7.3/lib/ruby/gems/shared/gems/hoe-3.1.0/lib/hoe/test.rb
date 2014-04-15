##
# Test plugin for hoe.
#
# === Tasks Provided:
#
# audit::              Run ZenTest against the package.
# default::            Run the default task(s).
# multi::              Run the test suite using multiruby.
# test::               Run the test suite.
# test_deps::          Show which test files fail when run alone.

module Hoe::Test
  ##
  # Configuration for the supported test frameworks for test task.

  SUPPORTED_TEST_FRAMEWORKS = {
    :testunit => "test/unit",
    :minitest => "minitest/autorun",
    :none     => nil,
  }

  ##
  # Used to add flags to test_unit (e.g., -n test_borked).
  #
  # eg FILTER="-n test_blah"

  FILTER = ENV['FILTER'] || ENV['TESTOPTS']

  ##
  # Optional: Array of incompatible versions for multiruby filtering.
  # Used as a regex.

  attr_accessor :multiruby_skip

  ##
  # Optional: What test library to require [default: :testunit]

  attr_accessor :testlib

  ##
  # Optional: Additional ruby to run before the test framework is loaded.

  attr_accessor :test_prelude

  ##
  # Optional: RSpec dirs. [default: %w(spec lib)]

  attr_accessor :rspec_dirs

  ##
  # Optional: RSpec options. [default: []]

  attr_accessor :rspec_options

  ##
  # Initialize variables for plugin.

  def initialize_test
    self.multiruby_skip ||= []
    self.testlib        ||= :testunit
    self.test_prelude   ||= nil
    self.rspec_dirs     ||= %w(spec lib)
    self.rspec_options  ||= []
  end

  ##
  # Define tasks for plugin.

  def define_test_tasks
    default_tasks = []

    if File.directory? "test" then
      desc 'Run the test suite. Use FILTER or TESTOPTS to add flags/args.'
      task :test do
        ruby make_test_cmd
      end

      desc 'Run the test suite using multiruby.'
      task :multi do
        ENV["EXCLUDED_VERSIONS"] = multiruby_skip.join(":")
        system "multiruby -S rake"
      end

      desc 'Show which test files fail when run alone.'
      task :test_deps do
        tests = Dir[*self.test_globs].uniq

        paths = ['bin', 'lib', 'test'].join(File::PATH_SEPARATOR)
        null_dev = Hoe::WINDOZE ? '> NUL 2>&1' : '&> /dev/null'

        tests.each do |test|
          if not system "ruby -I#{paths} #{test} #{null_dev}" then
            puts "Dependency Issues: #{test}"
          end
        end
      end

      default_tasks << :test
    end

    if File.directory? "spec" then
      found = try_loading_rspec2 || try_loading_rspec1

      if found then
        default_tasks << :spec
      else
        warn "Found spec dir, but couldn't load rspec (1 or 2) task. skipping."
      end
    end

    desc 'Run the default task(s).'
    task :default => default_tasks

    unless default_tasks.empty? then
      ##
      # This is for Erik Hollensbe's rubygems-test project. Hoe is
      # test-happy, so by using this plugin you're already testable. For
      # more information, see: <https://github.com/erikh/rubygems-test>
      # and/or <http://www.gem-testers.org/>

      gemtest = ".gemtest"

      gemtest.encode!(Encoding::UTF_8) if gemtest.respond_to?(:encoding)

      self.spec.files += [gemtest]

      pkg  = pkg_path
      turd = "#{pkg}/.gemtest"

      file turd => pkg_path do
        touch turd
      end

      file "#{pkg}.gem" => turd
    end

    desc 'Run ZenTest against the package.'
    task :audit do
      libs = %w(lib test ext).join(File::PATH_SEPARATOR)
      sh "zentest -I=#{libs} #{spec.files.grep(/^(lib|test)/).join(' ')}"
    end
  end

  ##
  # Generate the test command-line.

  def make_test_cmd
    unless SUPPORTED_TEST_FRAMEWORKS.has_key?(testlib)
      raise "unsupported test framework #{testlib}"
    end

    framework = SUPPORTED_TEST_FRAMEWORKS[testlib]

    tests = ["rubygems"]
    tests << framework if framework
    tests << test_globs.sort.map { |g| Dir.glob(g) }
    tests.flatten!
    tests.map! {|f| %(require "#{f}")}

    tests.insert 1, test_prelude if test_prelude

    "#{Hoe::RUBY_FLAGS} -e '#{tests.join("; ")}' -- #{FILTER}"
  end

  ##
  # Attempt to load RSpec 2, returning true if successful

  def try_loading_rspec2
    require 'rspec/core/rake_task'

    desc "Run all specifications"
    RSpec::Core::RakeTask.new(:spec) do |t|
      t.rspec_opts = self.rspec_options
      t.rspec_opts << "-I#{self.rspec_dirs.join(":")}" unless
      rspec_dirs.empty?
    end

    true
  rescue LoadError => err
    warn "%p while trying to load RSpec 2: %s" % [ err.class, err.message ]
    false
  end

  ##
  # Attempt to load RSpec 1, returning true if successful

  def try_loading_rspec1
    require 'spec/rake/spectask'

    desc "Run all specifications"
    Spec::Rake::SpecTask.new(:spec) do |t|
      t.libs = self.rspec_dirs
      t.spec_opts = self.rspec_options
    end
    true
  rescue LoadError => err
    warn "%p while trying to load RSpec 1: %s" % [ err.class, err.message ]
    false
  end

end
