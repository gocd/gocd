require "spec_helper"

describe "bundle update" do
  describe "git sources" do
    it "floats on a branch when :branch is used" do
      build_git  "foo", "1.0"
      update_git "foo", :branch => "omg"

      install_gemfile <<-G
        git "#{lib_path('foo-1.0')}", :branch => "omg" do
          gem 'foo'
        end
      G

      update_git "foo", :branch => "omg" do |s|
        s.write "lib/foo.rb", "FOO = '1.1'"
      end

      bundle "update"

      should_be_installed "foo 1.1"
    end

    it "updates correctly when you have like craziness" do
      build_lib "activesupport", "3.0", :path => lib_path("rails/activesupport")
      build_git "rails", "3.0", :path => lib_path("rails") do |s|
        s.add_dependency "activesupport", "= 3.0"
      end

      install_gemfile <<-G
        gem "rails", :git => "#{lib_path('rails')}"
      G

      bundle "update rails"
      expect(out).to include("Using activesupport 3.0 from #{lib_path('rails')} (at master)")
      should_be_installed "rails 3.0", "activesupport 3.0"
    end

    it "floats on a branch when :branch is used and the source is specified in the update" do
      build_git  "foo", "1.0", :path => lib_path("foo")
      update_git "foo", :branch => "omg", :path => lib_path("foo")

      install_gemfile <<-G
        git "#{lib_path('foo')}", :branch => "omg" do
          gem 'foo'
        end
      G

      update_git "foo", :branch => "omg", :path => lib_path("foo") do |s|
        s.write "lib/foo.rb", "FOO = '1.1'"
      end

      bundle "update --source foo"

      should_be_installed "foo 1.1"
    end

    it "floats on master when updating all gems that are pinned to the source even if you have child dependencies" do
      build_git "foo", :path => lib_path('foo')
      build_gem "bar", :to_system => true do |s|
        s.add_dependency "foo"
      end

      install_gemfile <<-G
        gem "foo", :git => "#{lib_path('foo')}"
        gem "bar"
      G

      update_git "foo", :path => lib_path('foo') do |s|
        s.write "lib/foo.rb", "FOO = '1.1'"
      end

      bundle "update foo"

      should_be_installed "foo 1.1"
    end

    it "notices when you change the repo url in the Gemfile" do
      build_git "foo", :path => lib_path("foo_one")
      build_git "foo", :path => lib_path("foo_two")

      install_gemfile <<-G
        gem "foo", "1.0", :git => "#{lib_path('foo_one')}"
      G

      FileUtils.rm_rf lib_path("foo_one")

      install_gemfile <<-G
        gem "foo", "1.0", :git => "#{lib_path('foo_two')}"
      G

      expect(err).to be_empty
      expect(out).to include("Fetching #{lib_path}/foo_two")
      expect(out).to include("Your bundle is complete!")
    end


    it "fetches tags from the remote" do
      build_git "foo"
      @remote = build_git("bar", :bare => true)
      update_git "foo", :remote => @remote.path
      update_git "foo", :push => "master"

      install_gemfile <<-G
        gem 'foo', :git => "#{@remote.path}"
      G

      # Create a new tag on the remote that needs fetching
      update_git "foo", :tag => "fubar"
      update_git "foo", :push => "fubar"

      gemfile <<-G
        gem 'foo', :git => "#{@remote.path}", :tag => "fubar"
      G

      bundle "update", :exitstatus => true
      expect(exitstatus).to eq(0)
    end

    describe "with submodules" do
      before :each do
        build_gem "submodule", :to_system => true do |s|
          s.write "lib/submodule.rb", "puts 'GEM'"
        end

        build_git "submodule", "1.0" do |s|
          s.write "lib/submodule.rb", "puts 'GIT'"
        end

        build_git "has_submodule", "1.0" do |s|
          s.add_dependency "submodule"
        end

        Dir.chdir(lib_path('has_submodule-1.0')) do
          `git submodule add #{lib_path('submodule-1.0')} submodule-1.0`
          `git commit -m "submodulator"`
        end
      end

      it "it unlocks the source when submodules are added to a git source" do
        install_gemfile <<-G
          git "#{lib_path('has_submodule-1.0')}" do
            gem "has_submodule"
          end
        G

        run "require 'submodule'"
        expect(out).to eq('GEM')

        install_gemfile <<-G
          git "#{lib_path('has_submodule-1.0')}", :submodules => true do
            gem "has_submodule"
          end
        G

        run "require 'submodule'"
        expect(out).to eq('GIT')
      end

      it "it unlocks the source when submodules are removed from git source" do
        pending "This would require actually removing the submodule from the clone"
        install_gemfile <<-G
          git "#{lib_path('has_submodule-1.0')}", :submodules => true do
            gem "has_submodule"
          end
        G

        run "require 'submodule'"
        expect(out).to eq('GIT')

        install_gemfile <<-G
          git "#{lib_path('has_submodule-1.0')}" do
            gem "has_submodule"
          end
        G

        run "require 'submodule'"
        expect(out).to eq('GEM')
      end
    end

    it "errors with a message when the .git repo is gone" do
      build_git "foo", "1.0"

      install_gemfile <<-G
        gem "foo", :git => "#{lib_path('foo-1.0')}"
      G

      lib_path("foo-1.0").join(".git").rmtree

      bundle :update, :expect_err => true
      expect(out).to include(lib_path("foo-1.0").to_s)
    end

    it "should not explode on invalid revision on update of gem by name" do
      build_git "rack", "0.8"

      build_git "rack", "0.8", :path => lib_path('local-rack') do |s|
        s.write "lib/rack.rb", "puts :LOCAL"
      end

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :branch => "master"
      G

      bundle %|config local.rack #{lib_path('local-rack')}|
      bundle "update rack"
      expect(out).to include("Your bundle is updated!")
    end

    it "shows the previous version of the gem" do
      build_git "rails", "3.0", :path => lib_path("rails")

      install_gemfile <<-G
        gem "rails", :git => "#{lib_path('rails')}"
      G

      lockfile <<-G
        GIT
          remote: #{lib_path("rails")}
          specs:
            rails (2.3.2)

        PLATFORMS
          #{generic(Gem::Platform.local)}

        DEPENDENCIES
          rails!
      G

      bundle "update"
      expect(out).to include("Using rails 3.0 (was 2.3.2) from #{lib_path('rails')} (at master)")
    end
  end
end
