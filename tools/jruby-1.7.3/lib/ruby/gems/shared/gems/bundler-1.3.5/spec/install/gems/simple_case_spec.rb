require "spec_helper"

describe "bundle install with gem sources" do
  describe "the simple case" do
    it "prints output and returns if no dependencies are specified" do
      gemfile <<-G
        source "file://#{gem_repo1}"
      G

      bundle :install
      expect(out).to match(/no dependencies/)
    end

    it "does not make a lockfile if the install fails" do
      install_gemfile <<-G, :expect_err => true
        raise StandardError, "FAIL"
      G

      expect(err).to match(/FAIL \(StandardError\)/)
      expect(bundled_app("Gemfile.lock")).not_to exist
    end

    it "creates a Gemfile.lock" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      expect(bundled_app('Gemfile.lock')).to exist
    end

    it "creates lock files based on the Gemfile name" do
      gemfile bundled_app('OmgFile'), <<-G
        source "file://#{gem_repo1}"
        gem "rack", "1.0"
      G

      bundle 'install --gemfile OmgFile'

      expect(bundled_app("OmgFile.lock")).to exist
    end

    it "doesn't delete the lockfile if one already exists" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      lockfile = File.read(bundled_app("Gemfile.lock"))

      install_gemfile <<-G, :expect_err => true
        raise StandardError, "FAIL"
      G

      expect(File.read(bundled_app("Gemfile.lock"))).to eq(lockfile)
    end

    it "does not touch the lockfile if nothing changed" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      expect { run '1' }.not_to change { File.mtime(bundled_app('Gemfile.lock')) }
    end

    it "fetches gems" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      expect(default_bundle_path("gems/rack-1.0.0")).to exist
      should_be_installed("rack 1.0.0")
    end

    it "fetches gems when multiple versions are specified" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack', "> 0.9", "< 1.0"
      G

      expect(default_bundle_path("gems/rack-0.9.1")).to exist
      should_be_installed("rack 0.9.1")
    end

    it "fetches gems when multiple versions are specified take 2" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack', "< 1.0", "> 0.9"
      G

      expect(default_bundle_path("gems/rack-0.9.1")).to exist
      should_be_installed("rack 0.9.1")
    end

    it "raises an appropriate error when gems are specified using symbols" do
      status = install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"
        gem :rack
      G
      expect(status).to eq(4)
    end

    it "pulls in dependencies" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rails"
      G

      should_be_installed "actionpack 2.3.2", "rails 2.3.2"
    end

    it "does the right version" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", "0.9.1"
      G

      should_be_installed "rack 0.9.1"
    end

    it "does not install the development dependency" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "with_development_dependency"
      G

      should_be_installed "with_development_dependency 1.0.0"
      should_not_be_installed "activesupport 2.3.5"
    end

    it "resolves correctly" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activemerchant"
        gem "rails"
      G

      should_be_installed "activemerchant 1.0", "activesupport 2.3.2", "actionpack 2.3.2"
    end

    it "activates gem correctly according to the resolved gems" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activesupport", "2.3.5"
      G

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "activemerchant"
        gem "rails"
      G

      should_be_installed "activemerchant 1.0", "activesupport 2.3.2", "actionpack 2.3.2"
    end

    it "does not reinstall any gem that is already available locally" do
      system_gems "activesupport-2.3.2"

      build_repo2 do
        build_gem "activesupport", "2.3.2" do |s|
          s.write "lib/activesupport.rb", "ACTIVESUPPORT = 'fail'"
        end
      end

      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "activerecord", "2.3.2"
      G

      should_be_installed "activesupport 2.3.2"
    end

    it "works when the gemfile specifies gems that only exist in the system" do
      build_gem "foo", :to_system => true
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
        gem "foo"
      G

      should_be_installed "rack 1.0.0", "foo 1.0.0"
    end

    it "prioritizes local gems over remote gems" do
      build_gem 'rack', '1.0.0', :to_system => true do |s|
        s.add_dependency "activesupport", "2.3.5"
      end

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G

      should_be_installed "rack 1.0.0", "activesupport 2.3.5"
    end

    describe "with a gem that installs multiple platforms" do
      it "installs gems for the local platform as first choice" do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 #{Gem::Platform.local}")
      end

      it "falls back on plain ruby" do
        simulate_platform "foo-bar-baz"
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 RUBY")
      end

      it "installs gems for java" do
        simulate_platform "java"
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 JAVA")
      end

      it "installs gems for windows" do
        simulate_platform mswin

        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "platform_specific"
        G

        run "require 'platform_specific' ; puts PLATFORM_SPECIFIC"
        expect(out).to eq("1.0.0 MSWIN")
      end
    end

    describe "doing bundle install foo" do
      before do
        gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack"
        G
      end

      it "works" do
        bundle "install --path vendor"
        should_be_installed "rack 1.0"
      end

      it "allows running bundle install --system without deleting foo" do
        bundle "install --path vendor"
        bundle "install --system"
        FileUtils.rm_rf(bundled_app("vendor"))
        should_be_installed "rack 1.0"
      end

      it "allows running bundle install --system after deleting foo" do
        bundle "install --path vendor"
        FileUtils.rm_rf(bundled_app("vendor"))
        bundle "install --system"
        should_be_installed "rack 1.0"
      end
    end

    it "finds gems in multiple sources" do
      build_repo2
      update_repo2

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        source "file://#{gem_repo2}"

        gem "activesupport", "1.2.3"
        gem "rack", "1.2"
      G

      should_be_installed "rack 1.2", "activesupport 1.2.3"
    end

    it "gives a useful error if no sources are set" do
      install_gemfile <<-G
        gem "rack"
      G

      bundle :install, :expect_err => true
      expect(out).to match(/Your Gemfile has no gem server sources/i)
    end

    it "creates a Gemfile.lock on a blank Gemfile" do
      install_gemfile <<-G
      G

      expect(File.exists?(bundled_app("Gemfile.lock"))).to be_true
    end

    it "gracefully handles error when rubygems server is unavailable" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        source "http://localhost:9384"

        gem 'foo'
      G

      bundle :install
      expect(out).to include("Could not fetch specs from http://localhost:9384/")
      expect(out).not_to include("file://")
    end

    it "doesn't blow up when the local .bundle/config is empty" do
      FileUtils.mkdir_p(bundled_app(".bundle"))
      FileUtils.touch(bundled_app(".bundle/config"))

      install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"

        gem 'foo'
      G
      expect(exitstatus).to eq(0)
    end

    it "doesn't blow up when the global .bundle/config is empty" do
      FileUtils.mkdir_p("#{Bundler.rubygems.user_home}/.bundle")
      FileUtils.touch("#{Bundler.rubygems.user_home}/.bundle/config")

      install_gemfile(<<-G, :exitstatus => true)
        source "file://#{gem_repo1}"

        gem 'foo'
      G
      expect(exitstatus).to eq(0)
    end
  end

  describe "when Bundler root contains regex chars" do
    before do
      root_dir = tmp("foo[]bar")

      FileUtils.mkdir_p(root_dir)
      in_app_root_custom(root_dir)
    end

    it "doesn't blow up" do
      build_lib "foo"
      gemfile = <<-G
        gem 'foo', :path => "#{lib_path('foo-1.0')}"
      G
      File.open('Gemfile', 'w') do |file|
        file.puts gemfile
      end

      bundle :install, :exitstatus => true

      expect(exitstatus).to eq(0)
    end
  end

  describe "when prerelease gems are available" do
    it "finds prereleases" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "not_released"
      G
      should_be_installed "not_released 1.0.pre"
    end

    it "uses regular releases if available" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "has_prerelease"
      G
      should_be_installed "has_prerelease 1.0"
    end

    it "uses prereleases if requested" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "has_prerelease", "1.1.pre"
      G
      should_be_installed "has_prerelease 1.1.pre"
    end
  end

  describe "when prerelease gems are not available" do
    it "still works" do
      build_repo3
      install_gemfile <<-G
        source "file://#{gem_repo3}"
        gem "rack"
      G

      should_be_installed "rack 1.0"
    end
  end

  describe "when BUNDLE_PATH or the global path config is set" do
    before :each do
      build_lib "rack", "1.0.0", :to_system => true do |s|
        s.write "lib/rack.rb", "raise 'FAIL'"
      end

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G
    end

    def set_bundle_path(type, location)
      if type == :env
        ENV["BUNDLE_PATH"] = location
      elsif type == :global
        bundle "config path #{location}", "no-color" => nil
      end
    end

    [:env, :global].each do |type|
      it "installs gems to a path if one is specified" do
        set_bundle_path(type, bundled_app("vendor2").to_s)
        bundle "install --path vendor/bundle"

        expect(vendored_gems("gems/rack-1.0.0")).to be_directory
        expect(bundled_app("vendor2")).not_to be_directory
        should_be_installed "rack 1.0.0"
      end

      it "installs gems to BUNDLE_PATH with #{type}" do
        set_bundle_path(type, bundled_app("vendor").to_s)

        bundle :install

        expect(bundled_app('vendor/gems/rack-1.0.0')).to be_directory
        should_be_installed "rack 1.0.0"
      end

      it "installs gems to BUNDLE_PATH relative to root when relative" do
        set_bundle_path(type, "vendor")

        FileUtils.mkdir_p bundled_app('lol')
        Dir.chdir(bundled_app('lol')) do
          bundle :install
        end

        expect(bundled_app('vendor/gems/rack-1.0.0')).to be_directory
        should_be_installed "rack 1.0.0"
      end
    end

    it "installs gems to BUNDLE_PATH from .bundle/config" do
      config "BUNDLE_PATH" => bundled_app("vendor/bundle").to_s

      bundle :install

      expect(vendored_gems('gems/rack-1.0.0')).to be_directory
      should_be_installed "rack 1.0.0"
    end

    it "sets BUNDLE_PATH as the first argument to bundle install" do
      bundle "install --path ./vendor/bundle"

      expect(vendored_gems('gems/rack-1.0.0')).to be_directory
      should_be_installed "rack 1.0.0"
    end

    it "disables system gems when passing a path to install" do
      # This is so that vendored gems can be distributed to others
      build_gem "rack", "1.1.0", :to_system => true
      bundle "install --path ./vendor/bundle"

      expect(vendored_gems('gems/rack-1.0.0')).to be_directory
      should_be_installed "rack 1.0.0"
    end
  end

  describe "when passing in a Gemfile via --gemfile" do
    it "finds the gemfile" do
      gemfile bundled_app("NotGemfile"), <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      bundle :install, :gemfile => bundled_app("NotGemfile")

      ENV['BUNDLE_GEMFILE'] = "NotGemfile"
      should_be_installed "rack 1.0.0"
    end
  end

  describe "when requesting a quiet install via --quiet" do
    it "should be quiet if there are no warnings" do
      gemfile <<-G
        source "file://#{gem_repo1}"
        gem 'rack'
      G

      bundle :install, :quiet => true
      expect(out).to eq("")
    end

    it "should still display warnings" do
      gemfile <<-G
        gem 'rack'
      G

      bundle :install, :quiet => true
      expect(out).to match(/Your Gemfile has no gem server sources/)
    end
  end

  describe "when disabling system gems" do
    before :each do
      build_gem "rack", "1.0.0", :to_system => true do |s|
        s.write "lib/rack.rb", "puts 'FAIL'"
      end

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G
    end

    it "behaves like bundle install vendor/bundle with --deployment" do
      bundle "install"
      bundle "install --deployment"
      expect(out).to include("It was installed into ./vendor/bundle")
      should_be_installed "rack 1.0.0"
      expect(bundled_app("vendor/bundle")).to exist
    end

    it "does not use available system gems with bundle --path vendor/bundle" do
      bundle "install --path vendor/bundle"
      should_be_installed "rack 1.0.0"
    end

    it "handles paths with regex characters in them" do
      dir = bundled_app("bun++dle")
      dir.mkpath

      Dir.chdir(dir) do
        bundle "install --path vendor/bundle"
        expect(out).to include("installed into ./vendor/bundle")
      end

      dir.rmtree
    end

    it "prints a warning to let the user know what has happened with bundle --path vendor/bundle" do
      bundle "install --path vendor/bundle"
      expect(out).to include("It was installed into ./vendor")
    end

    it "disallows --path vendor/bundle --system" do
      bundle "install --path vendor/bundle --system"
      expect(out).to include("Please choose.")
    end

    it "remembers to disable system gems after the first time with bundle --path vendor/bundle" do
      bundle "install --path vendor/bundle"
      FileUtils.rm_rf bundled_app('vendor')
      bundle "install"

      expect(vendored_gems('gems/rack-1.0.0')).to be_directory
      should_be_installed "rack 1.0.0"
    end
  end

  describe "when loading only the default group" do
    it "should not load all groups" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
        gem "activesupport", :groups => :development
      G

      ruby <<-R
        require "bundler"
        Bundler.setup :default
        Bundler.require :default
        puts RACK
        begin
          require "activesupport"
        rescue LoadError
          puts "no activesupport"
        end
      R

      expect(out).to include("1.0")
      expect(out).to include("no activesupport")
    end
  end

  describe "when a gem has a YAML gemspec" do
    before :each do
      build_repo2 do
        build_gem "yaml_spec", :gemspec => :yaml
      end
    end

    it "still installs correctly" do
      gemfile <<-G
        source "file://#{gem_repo2}"
        gem "yaml_spec"
      G
      bundle :install
      expect(err).to be_empty
    end

    it "still installs correctly when using path" do
      build_lib 'yaml_spec', :gemspec => :yaml

      install_gemfile <<-G
        gem 'yaml_spec', :path => "#{lib_path('yaml_spec-1.0')}"
      G
      expect(err).to eq("")
    end
  end

  describe "bundler dependencies" do
    before(:each) do
      build_repo2 do
        build_gem "rails", "3.0" do |s|
          s.add_dependency "bundler", ">= 0.9.0.pre"
        end
        build_gem "bundler", "0.9.1"
        build_gem "bundler", Bundler::VERSION
      end
    end

    it "are forced to the current bundler version" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rails", "3.0"
      G

      should_be_installed "bundler #{Bundler::VERSION}"
    end

    it "are not added if not already present" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack"
      G
      should_not_be_installed "bundler #{Bundler::VERSION}"
    end

    it "causes a conflict if explicitly requesting a different version" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rails", "3.0"
        gem "bundler", "0.9.2"
      G

      nice_error = <<-E.strip.gsub(/^ {8}/, '')
        Fetching source index from file:#{gem_repo2}/
        Resolving dependencies...
        Bundler could not find compatible versions for gem "bundler":
          In Gemfile:
            bundler (= 0.9.2) ruby

          Current Bundler version:
            bundler (#{Bundler::VERSION})
        E
      expect(out).to include(nice_error)
    end

    it "works for gems with multiple versions in its dependencies" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"

        gem "multiple_versioned_deps"
      G


      install_gemfile <<-G
        source "file://#{gem_repo2}"

        gem "multiple_versioned_deps"
        gem "rack"
      G

      should_be_installed "multiple_versioned_deps 1.0.0"
    end

    it "includes bundler in the bundle when it's a child dependency" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rails", "3.0"
      G

      run "begin; gem 'bundler'; puts 'WIN'; rescue Gem::LoadError; puts 'FAIL'; end"
      expect(out).to eq("WIN")
    end

    it "allows gem 'bundler' when Bundler is not in the Gemfile or its dependencies" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rack"
      G

      run "begin; gem 'bundler'; puts 'WIN'; rescue Gem::LoadError => e; puts e.backtrace; end"
      expect(out).to eq("WIN")
    end

    it "causes a conflict if child dependencies conflict" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "activemerchant"
        gem "rails_fail"
      G

      nice_error = <<-E.strip.gsub(/^ {8}/, '')
        Fetching source index from file:#{gem_repo2}/
        Resolving dependencies...
        Bundler could not find compatible versions for gem "activesupport":
          In Gemfile:
            activemerchant (>= 0) ruby depends on
              activesupport (>= 2.0.0) ruby

            rails_fail (>= 0) ruby depends on
              activesupport (1.2.3)
      E
      expect(out).to eq(nice_error)
    end

    it "causes a conflict if a child dependency conflicts with the Gemfile" do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rails_fail"
        gem "activesupport", "2.3.5"
      G

      nice_error = <<-E.strip.gsub(/^ {8}/, '')
        Fetching source index from file:#{gem_repo2}/
        Resolving dependencies...
        Bundler could not find compatible versions for gem "activesupport":
          In Gemfile:
            rails_fail (>= 0) ruby depends on
              activesupport (= 1.2.3) ruby

            activesupport (2.3.5)
      E
      expect(out).to eq(nice_error)
    end

    it "can install dependencies even if " do
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rails", "3.0"
      G

      simulate_bundler_version "10.0.0"
      #simulate_new_machine

      bundle "check"
      expect(out).to eq("The Gemfile's dependencies are satisfied")
    end
  end

  describe "when locked and installed with --without" do
    before(:each) do
      build_repo2
      system_gems "rack-0.9.1" do
        install_gemfile <<-G, :without => :rack
          source "file://#{gem_repo2}"
          gem "rack"

          group :rack do
            gem "rack_middleware"
          end
        G
      end
    end

    it "uses the correct versions even if --without was used on the original" do
      should_be_installed "rack 0.9.1"
      should_not_be_installed "rack_middleware 1.0"
      simulate_new_machine

      bundle :install

      should_be_installed "rack 0.9.1"
      should_be_installed "rack_middleware 1.0"
    end

    it "does not hit the remote a second time" do
      FileUtils.rm_rf gem_repo2
      bundle "install --without rack"
      expect(err).to be_empty
    end
  end

  describe "when system_bindir is set" do
    # On OS X, Gem.bindir defaults to /usr/bin, so system_bindir is useful if
    # you want to avoid sudo installs for system gems with OS X's default ruby
    it "overrides Gem.bindir" do
      expect(Pathname.new("/usr/bin")).not_to be_writable unless Process::euid == 0
      gemfile <<-G
        require 'rubygems'
        def Gem.bindir; "/usr/bin"; end
        source "file://#{gem_repo1}"
        gem "rack"
      G

      config "BUNDLE_SYSTEM_BINDIR" => system_gem_path('altbin').to_s
      bundle :install
      should_be_installed "rack 1.0.0"
      expect(system_gem_path("altbin/rackup")).to exist
    end
  end

end
