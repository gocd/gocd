require "spec_helper"

describe "bundle gem" do
  before do
    @git_name = `git config --global user.name`.chomp
    `git config --global user.name "Bundler User"`
    @git_email = `git config --global user.email`.chomp
    `git config --global user.email user@example.com`
  end

  after do
    `git config --global user.name "#{@git_name}"`
    `git config --global user.email #{@git_email}`
  end

  shared_examples_for "git config is present" do
    context "git config user.{name,email} present" do
      it "sets gemspec author to git user.name if available" do
        expect(generated_gem.gemspec.authors.first).to eq("Bundler User")
      end

      it "sets gemspec email to git user.email if available" do
        expect(generated_gem.gemspec.email.first).to eq("user@example.com")
      end
    end
  end

  shared_examples_for "git config is absent" do |hoge|
    it "sets gemspec author to default message if git user.name is not set or empty" do
      expect(generated_gem.gemspec.authors.first).to eq("TODO: Write your name")
    end

    it "sets gemspec email to default message if git user.email is not set or empty" do
      expect(generated_gem.gemspec.email.first).to eq("TODO: Write your email address")
    end
  end

  context "gem naming with underscore" do
    let(:gem_name) { 'test_gem' }

    before do
      bundle "gem #{gem_name}"
      # reset gemspec cache for each test because of commit 3d4163a
      Bundler.clear_gemspec_cache
    end

    let(:generated_gem) { Bundler::GemHelper.new(bundled_app(gem_name).to_s) }

    it "generates a gem skeleton" do
      expect(bundled_app("test_gem/test_gem.gemspec")).to exist
      expect(bundled_app("test_gem/LICENSE.txt")).to exist
      expect(bundled_app("test_gem/Gemfile")).to exist
      expect(bundled_app("test_gem/Rakefile")).to exist
      expect(bundled_app("test_gem/lib/test_gem.rb")).to exist
      expect(bundled_app("test_gem/lib/test_gem/version.rb")).to exist
    end

    it "starts with version 0.0.1" do
      expect(bundled_app("test_gem/lib/test_gem/version.rb").read).to match(/VERSION = "0.0.1"/)
    end

    it "does not nest constants" do
      expect(bundled_app("test_gem/lib/test_gem/version.rb").read).to match(/module TestGem/)
      expect(bundled_app("test_gem/lib/test_gem.rb").read).to match(/module TestGem/)
    end

    it_should_behave_like "git config is present"

    context "git config user.{name,email} is not set" do
      before do
        `git config --global --unset user.name`
        `git config --global --unset user.email`
        reset!
        in_app_root
        bundle "gem #{gem_name}"
      end

      it_should_behave_like "git config is absent"
    end

    it "sets gemspec license to MIT by default" do
      expect(generated_gem.gemspec.license).to eq("MIT")
    end

    it "requires the version file" do
      expect(bundled_app("test_gem/lib/test_gem.rb").read).to match(/require "test_gem\/version"/)
    end

    it "runs rake without problems" do
      system_gems ["rake-10.0.2"]

      rakefile = strip_whitespace <<-RAKEFILE
        task :default do
          puts 'SUCCESS'
        end
      RAKEFILE
      File.open(bundled_app("test_gem/Rakefile"), 'w') do |file|
        file.puts rakefile
      end

      Dir.chdir(bundled_app(gem_name)) do
        sys_exec("rake")
        expect(out).to include("SUCCESS")
      end
    end

    context "--bin parameter set" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --bin"
      end

      it "builds bin skeleton" do
        expect(bundled_app("test_gem/bin/test_gem")).to exist
      end

      it "requires 'test-gem'" do
        expect(bundled_app("test_gem/bin/test_gem").read).to match(/require 'test_gem'/)
      end
    end

    context "no --test parameter" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name}"
      end

      it "doesn't create any spec/test file" do
        expect(bundled_app("test_gem/.rspec")).to_not exist
        expect(bundled_app("test_gem/spec/test_gem_spec.rb")).to_not exist
        expect(bundled_app("test_gem/spec/spec_helper.rb")).to_not exist
        expect(bundled_app("test_gem/test/test_test_gem.rb")).to_not exist
        expect(bundled_app("test_gem/test/minitest_helper.rb")).to_not exist
      end
    end

    context "--test parameter set to rspec" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test=rspec"
      end

      it "builds spec skeleton" do
        expect(bundled_app("test_gem/.rspec")).to exist
        expect(bundled_app("test_gem/spec/test_gem_spec.rb")).to exist
        expect(bundled_app("test_gem/spec/spec_helper.rb")).to exist
      end

      it "requires 'test-gem'" do
        expect(bundled_app("test_gem/spec/spec_helper.rb").read).to include("require 'test_gem'")
      end

      it "creates a default test which fails" do
        expect(bundled_app("test_gem/spec/test_gem_spec.rb").read).to include("expect(false).to eq(true)")
      end
    end

    context "--test parameter set to minitest" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test=minitest"
      end

      it "builds spec skeleton" do
        expect(bundled_app("test_gem/test/test_test_gem.rb")).to exist
        expect(bundled_app("test_gem/test/minitest_helper.rb")).to exist
      end

      it "requires 'test-gem'" do
        expect(bundled_app("test_gem/test/minitest_helper.rb").read).to include("require 'test_gem'")
      end

      it "requires 'minitest_helper'" do
        expect(bundled_app("test_gem/test/test_test_gem.rb").read).to include("require 'minitest_helper'")
      end

      it "creates a default test which fails" do
        expect(bundled_app("test_gem/test/test_test_gem.rb").read).to include("assert false")
      end
    end

    context "--test with no arguments" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test"
      end

      it "defaults to rspec" do
        expect(bundled_app("test_gem/spec/spec_helper.rb")).to exist
        expect(bundled_app("test_gem/test/minitest_helper.rb")).to_not exist
      end

      it "creates a .travis.yml file to test the library against the current Ruby version on Travis CI" do
        expect(bundled_app("test_gem/.travis.yml").read).to match(%r(- #{RUBY_VERSION}))
      end
    end

    context "--edit option" do
      it "opens the generated gemspec in the user's text editor" do
        reset!
        in_app_root
        output = bundle "gem #{gem_name} --edit=echo"
        gemspec_path = File.join(Dir.pwd, gem_name, "#{gem_name}.gemspec")
        expect(output).to include("echo \"#{gemspec_path}\"")
      end
    end
  end

  context "gem naming with dashed" do
    let(:gem_name) { 'test-gem' }

    before do
      bundle "gem #{gem_name}"
      # reset gemspec cache for each test because of commit 3d4163a
      Bundler.clear_gemspec_cache
    end

    let(:generated_gem) { Bundler::GemHelper.new(bundled_app(gem_name).to_s) }

    it "generates a gem skeleton" do
      expect(bundled_app("test-gem/test-gem.gemspec")).to exist
      expect(bundled_app("test-gem/LICENSE.txt")).to exist
      expect(bundled_app("test-gem/Gemfile")).to exist
      expect(bundled_app("test-gem/Rakefile")).to exist
      expect(bundled_app("test-gem/lib/test/gem.rb")).to exist
      expect(bundled_app("test-gem/lib/test/gem/version.rb")).to exist
    end

    it "starts with version 0.0.1" do
      expect(bundled_app("test-gem/lib/test/gem/version.rb").read).to match(/VERSION = "0.0.1"/)
    end

    it "nests constants so they work" do
      expect(bundled_app("test-gem/lib/test/gem/version.rb").read).to match(/module Test\n  module Gem/)
      expect(bundled_app("test-gem/lib/test/gem.rb").read).to match(/module Test\n  module Gem/)
    end

    it_should_behave_like "git config is present"

    context "git config user.{name,email} is not set" do
      before do
        `git config --global --unset user.name`
        `git config --global --unset user.email`
        reset!
        in_app_root
        bundle "gem #{gem_name}"
      end

      it_should_behave_like "git config is absent"
    end

    it "sets gemspec license to MIT by default" do
      expect(generated_gem.gemspec.license).to eq("MIT")
    end

    it "requires the version file" do
      expect(bundled_app("test-gem/lib/test/gem.rb").read).to match(/require "test\/gem\/version"/)
    end

    it "runs rake without problems" do
      system_gems ["rake-10.0.2"]

      rakefile = strip_whitespace <<-RAKEFILE
        task :default do
          puts 'SUCCESS'
        end
      RAKEFILE
      File.open(bundled_app("test-gem/Rakefile"), 'w') do |file|
        file.puts rakefile
      end

      Dir.chdir(bundled_app(gem_name)) do
        sys_exec("rake")
        expect(out).to include("SUCCESS")
      end
    end

    context "--bin parameter set" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --bin"
      end

      it "builds bin skeleton" do
        expect(bundled_app("test-gem/bin/test-gem")).to exist
      end

      it "requires 'test/gem'" do
        expect(bundled_app("test-gem/bin/test-gem").read).to match(/require 'test\/gem'/)
      end
    end

    context "no --test parameter" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name}"
      end

      it "doesn't create any spec/test file" do
        expect(bundled_app("test-gem/.rspec")).to_not exist
        expect(bundled_app("test-gem/spec/test/gem_spec.rb")).to_not exist
        expect(bundled_app("test-gem/spec/spec_helper.rb")).to_not exist
        expect(bundled_app("test-gem/test/test_test/gem.rb")).to_not exist
        expect(bundled_app("test-gem/test/minitest_helper.rb")).to_not exist
      end
    end

    context "--test parameter set to rspec" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test=rspec"
      end

      it "builds spec skeleton" do
        expect(bundled_app("test-gem/.rspec")).to exist
        expect(bundled_app("test-gem/spec/test/gem_spec.rb")).to exist
        expect(bundled_app("test-gem/spec/spec_helper.rb")).to exist
      end

      it "requires 'test/gem'" do
        expect(bundled_app("test-gem/spec/spec_helper.rb").read).to include("require 'test/gem'")
      end

      it "creates a default test which fails" do
        expect(bundled_app("test-gem/spec/test/gem_spec.rb").read).to include("expect(false).to eq(true)")
      end

      it "creates a default rake task to run the specs" do
        rakefile = strip_whitespace <<-RAKEFILE
          require "bundler/gem_tasks"
          require "rspec/core/rake_task"

          RSpec::Core::RakeTask.new(:spec)

          task :default => :spec

        RAKEFILE

        expect(bundled_app("test-gem/Rakefile").read).to eq(rakefile)
      end
    end

    context "--test parameter set to minitest" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test=minitest"
      end

      it "builds spec skeleton" do
        expect(bundled_app("test-gem/test/test_test/gem.rb")).to exist
        expect(bundled_app("test-gem/test/minitest_helper.rb")).to exist
      end

      it "requires 'test/gem'" do
        expect(bundled_app("test-gem/test/minitest_helper.rb").read).to match(/require 'test\/gem'/)
      end

      it "requires 'minitest_helper'" do
        expect(bundled_app("test-gem/test/test_test/gem.rb").read).to match(/require 'minitest_helper'/)
      end

      it "creates a default test which fails" do
        expect(bundled_app("test-gem/test/test_test/gem.rb").read).to match(/assert false/)
      end

      it "creates a default rake task to run the test suite" do
        rakefile = strip_whitespace <<-RAKEFILE
          require "bundler/gem_tasks"
          require "rake/testtask"

          Rake::TestTask.new(:test) do |t|
            t.libs << "test"
          end

          task :default => :test

        RAKEFILE

        expect(bundled_app("test-gem/Rakefile").read).to eq(rakefile)
      end
    end

    context "--test with no arguments" do
      before do
        reset!
        in_app_root
        bundle "gem #{gem_name} --test"
      end

      it "defaults to rspec" do
        expect(bundled_app("test-gem/spec/spec_helper.rb")).to exist
        expect(bundled_app("test-gem/test/minitest_helper.rb")).to_not exist
      end
    end

    context "--ext parameter set" do
      before do
        reset!
        in_app_root
        bundle "gem test_gem --ext"
      end

      it "builds ext skeleton" do
        expect(bundled_app("test_gem/ext/test_gem/extconf.rb")).to exist
        expect(bundled_app("test_gem/ext/test_gem/test_gem.h")).to exist
        expect(bundled_app("test_gem/ext/test_gem/test_gem.c")).to exist
      end

      it "includes rake-compiler" do
        expect(bundled_app("test_gem/test_gem.gemspec").read).to include('spec.add_development_dependency "rake-compiler"')
      end
    end
  end
end
