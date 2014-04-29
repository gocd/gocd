require "spec_helper"

describe "Bundler.setup" do
  describe "with no arguments" do
    it "makes all groups available" do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :group => :test
      G

      ruby <<-RUBY
        require 'rubygems'
        require 'bundler'
        Bundler.setup

        require 'rack'
        puts RACK
      RUBY
      expect(err).to eq("")
      expect(out).to eq("1.0.0")
    end
  end

  describe "when called with groups" do
    before(:each) do
      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "yard"
        gem "rack", :group => :test
      G
    end

    it "doesn't make all groups available" do
      ruby <<-RUBY
        require 'rubygems'
        require 'bundler'
        Bundler.setup(:default)

        begin
          require 'rack'
        rescue LoadError
          puts "WIN"
        end
      RUBY
      expect(err).to eq("")
      expect(out).to eq("WIN")
    end

    it "accepts string for group name" do
      ruby <<-RUBY
        require 'rubygems'
        require 'bundler'
        Bundler.setup(:default, 'test')

        require 'rack'
        puts RACK
      RUBY
      expect(err).to eq("")
      expect(out).to eq("1.0.0")
    end

    it "leaves all groups available if they were already" do
      ruby <<-RUBY
        require 'rubygems'
        require 'bundler'
        Bundler.setup
        Bundler.setup(:default)

        require 'rack'
        puts RACK
      RUBY
      expect(err).to eq("")
      expect(out).to eq("1.0.0")
    end

    it "leaves :default available if setup is called twice" do
      ruby <<-RUBY
        require 'rubygems'
        require 'bundler'
        Bundler.setup(:default)
        Bundler.setup(:default, :test)

        begin
          require 'yard'
          puts "WIN"
        rescue LoadError
          puts "FAIL"
        end
      RUBY
      expect(err).to eq("")
      expect(out).to match("WIN")
    end
  end

  it "raises if the Gemfile was not yet installed" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    ruby <<-R
      require 'rubygems'
      require 'bundler'

      begin
        Bundler.setup
        puts "FAIL"
      rescue Bundler::GemNotFound
        puts "WIN"
      end
    R

    expect(out).to eq("WIN")
  end

  it "doesn't create a Gemfile.lock if the setup fails" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    ruby <<-R, :expect_err => true
      require 'rubygems'
      require 'bundler'

      Bundler.setup
    R

    expect(bundled_app("Gemfile.lock")).not_to exist
  end

  it "doesn't change the Gemfile.lock if the setup fails" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    lockfile = File.read(bundled_app("Gemfile.lock"))

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "nosuchgem", "10.0"
    G

    ruby <<-R, :expect_err => true
      require 'rubygems'
      require 'bundler'

      Bundler.setup
    R

    expect(File.read(bundled_app("Gemfile.lock"))).to eq(lockfile)
  end

  it "makes a Gemfile.lock if setup succeeds" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    File.read(bundled_app("Gemfile.lock"))

    FileUtils.rm(bundled_app("Gemfile.lock"))

    run "1"
    expect(bundled_app("Gemfile.lock")).to exist
  end

  it "uses BUNDLE_GEMFILE to locate the gemfile if present" do
    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    gemfile bundled_app('4realz'), <<-G
      source "file://#{gem_repo1}"
      gem "activesupport", "2.3.5"
    G

    ENV['BUNDLE_GEMFILE'] = bundled_app('4realz').to_s
    bundle :install

    should_be_installed "activesupport 2.3.5"
  end

  it "prioritizes gems in BUNDLE_PATH over gems in GEM_HOME" do
    ENV['BUNDLE_PATH'] = bundled_app('.bundle').to_s
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", "1.0.0"
    G

    build_gem "rack", "1.0", :to_system => true do |s|
      s.write "lib/rack.rb", "RACK = 'FAIL'"
    end

    should_be_installed "rack 1.0.0"
  end

  describe "integrate with rubygems" do
    describe "by replacing #gem" do
      before :each do
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "rack", "0.9.1"
        G
      end

      it "replaces #gem but raises when the gem is missing" do
        run <<-R
          begin
            gem "activesupport"
            puts "FAIL"
          rescue LoadError
            puts "WIN"
          end
        R

        expect(out).to eq("WIN")
      end

      it "version_requirement is now deprecated in rubygems 1.4.0+ when gem is missing" do
        run <<-R, :expect_err => true
          begin
            gem "activesupport"
            puts "FAIL"
          rescue LoadError
            puts "WIN"
          end
        R

        expect(err).to be_empty
      end

      it "replaces #gem but raises when the version is wrong" do
        run <<-R
          begin
            gem "rack", "1.0.0"
            puts "FAIL"
          rescue LoadError
            puts "WIN"
          end
        R

        expect(out).to eq("WIN")
      end

      it "version_requirement is now deprecated in rubygems 1.4.0+ when the version is wrong" do
        run <<-R, :expect_err => true
          begin
            gem "rack", "1.0.0"
            puts "FAIL"
          rescue LoadError
            puts "WIN"
          end
        R

        expect(err).to be_empty
      end
    end

    describe "by hiding system gems" do
      before :each do
        system_gems "activesupport-2.3.5"
        install_gemfile <<-G
          source "file://#{gem_repo1}"
          gem "yard"
        G
      end

      it "removes system gems from Gem.source_index" do
        run "require 'yard'"
        expect(out).to eq("bundler-#{Bundler::VERSION}\nyard-1.0")
      end

      context "when the ruby stdlib is a substring of Gem.path" do
        it "does not reject the stdlib from $LOAD_PATH" do
          substring = "/" + $LOAD_PATH.find{|p| p =~ /vendor_ruby/ }.split("/")[2]
          run "puts 'worked!'", :env => {"GEM_PATH" => substring}
          expect(out).to eq("worked!")
        end
      end
    end
  end

  describe "with paths" do
    it "activates the gems in the path source" do
      system_gems "rack-1.0.0"

      build_lib "rack", "1.0.0" do |s|
        s.write "lib/rack.rb", "puts 'WIN'"
      end

      gemfile <<-G
        path "#{lib_path('rack-1.0.0')}"
        source "file://#{gem_repo1}"
        gem "rack"
      G

      run "require 'rack'"
      expect(out).to eq("WIN")
    end
  end

  describe "with git" do
    before do
      build_git "rack", "1.0.0"

      gemfile <<-G
        gem "rack", :git => "#{lib_path('rack-1.0.0')}"
      G
    end

    it "provides a useful exception when the git repo is not checked out yet" do
      run "1", :expect_err => true
      expect(err).to match(/the git source #{lib_path('rack-1.0.0')} is not yet checked out. Please run `bundle install`/i)
    end

    it "does not hit the git binary if the lockfile is available and up to date" do
      bundle "install"

      break_git!

      ruby <<-R
        require 'rubygems'
        require 'bundler'

        begin
          Bundler.setup
          puts "WIN"
        rescue Exception => e
          puts "FAIL"
        end
      R

      expect(out).to eq("WIN")
    end

    it "provides a good exception if the lockfile is unavailable" do
      bundle "install"

      FileUtils.rm(bundled_app("Gemfile.lock"))

      break_git!

      ruby <<-R
        require "rubygems"
        require "bundler"

        begin
          Bundler.setup
          puts "FAIL"
        rescue Bundler::GitError => e
          puts e.message
        end
      R

      run "puts 'FAIL'", :expect_err => true

      expect(err).not_to include "This is not the git you are looking for"
    end

    it "works even when the cache directory has been deleted" do
      bundle "install --path vendor/bundle"
      FileUtils.rm_rf vendored_gems('cache')
      should_be_installed "rack 1.0.0"
    end

    it "does not randomly change the path when specifying --path and the bundle directory becomes read only" do
      begin
        bundle "install --path vendor/bundle"

        Dir["**/*"].each do |f|
          File.directory?(f) ?
            File.chmod(0555, f) :
            File.chmod(0444, f)
        end
        should_be_installed "rack 1.0.0"
      ensure
        Dir["**/*"].each do |f|
          File.directory?(f) ?
            File.chmod(0755, f) :
            File.chmod(0644, f)
        end
      end
    end
  end

  describe "when specifying local override" do
    it "explodes if given path does not exist on runtime" do
      build_git "rack", "0.8"

      FileUtils.cp_r("#{lib_path('rack-0.8')}/.", lib_path('local-rack'))

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :branch => "master"
      G

      bundle %|config local.rack #{lib_path('local-rack')}|
      bundle :install
      expect(out).to match(/at #{lib_path('local-rack')}/)

      FileUtils.rm_rf(lib_path('local-rack'))
      run "require 'rack'", :expect_err => true
      expect(err).to match(/Cannot use local override for rack-0.8 because #{Regexp.escape(lib_path('local-rack').to_s)} does not exist/)
    end

    it "explodes if branch is not given on runtime" do
      build_git "rack", "0.8"

      FileUtils.cp_r("#{lib_path('rack-0.8')}/.", lib_path('local-rack'))

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :branch => "master"
      G

      bundle %|config local.rack #{lib_path('local-rack')}|
      bundle :install
      expect(out).to match(/at #{lib_path('local-rack')}/)

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}"
      G

      run "require 'rack'", :expect_err => true
      expect(err).to match(/because :branch is not specified in Gemfile/)
    end

    it "explodes on different branches on runtime" do
      build_git "rack", "0.8"

      FileUtils.cp_r("#{lib_path('rack-0.8')}/.", lib_path('local-rack'))

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :branch => "master"
      G

      bundle %|config local.rack #{lib_path('local-rack')}|
      bundle :install
      expect(out).to match(/at #{lib_path('local-rack')}/)

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :branch => "changed"
      G

      run "require 'rack'", :expect_err => true
      expect(err).to match(/is using branch master but Gemfile specifies changed/)
    end

    it "explodes on refs with different branches on runtime" do
      build_git "rack", "0.8"

      FileUtils.cp_r("#{lib_path('rack-0.8')}/.", lib_path('local-rack'))

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :ref => "master", :branch => "master"
      G

      gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", :git => "#{lib_path('rack-0.8')}", :ref => "master", :branch => "nonexistant"
      G

      bundle %|config local.rack #{lib_path('local-rack')}|
      run "require 'rack'", :expect_err => true
      expect(err).to match(/is using branch master but Gemfile specifies nonexistant/)
    end
  end

  describe "when excluding groups" do
    it "doesn't change the resolve if --without is used" do
      install_gemfile <<-G, :without => :rails
        source "file://#{gem_repo1}"
        gem "activesupport"

        group :rails do
          gem "rails", "2.3.2"
        end
      G

      install_gems "activesupport-2.3.5"

      should_be_installed "activesupport 2.3.2", :groups => :default
    end

    it "remembers --without and does not bail on bare Bundler.setup" do
      install_gemfile <<-G, :without => :rails
        source "file://#{gem_repo1}"
        gem "activesupport"

        group :rails do
          gem "rails", "2.3.2"
        end
      G

      install_gems "activesupport-2.3.5"

      should_be_installed "activesupport 2.3.2"
    end

    it "remembers --without and does not include groups passed to Bundler.setup" do
      install_gemfile <<-G, :without => :rails
        source "file://#{gem_repo1}"
        gem "activesupport"

        group :rack do
          gem "rack"
        end

        group :rails do
          gem "rails", "2.3.2"
        end
      G

      should_not_be_installed "activesupport 2.3.2", :groups => :rack
      should_be_installed "rack 1.0.0", :groups => :rack
    end
  end

  # Unfortunately, gem_prelude does not record the information about
  # activated gems, so this test cannot work on 1.9 :(
  if RUBY_VERSION < "1.9"
    describe "preactivated gems" do
      it "raises an exception if a pre activated gem conflicts with the bundle" do
        system_gems "thin-1.0", "rack-1.0.0"
        build_gem "thin", "1.1", :to_system => true do |s|
          s.add_dependency "rack"
        end

        gemfile <<-G
          gem "thin", "1.0"
        G

        ruby <<-R
          require 'rubygems'
          gem "thin"
          require 'bundler'
          begin
            Bundler.setup
            puts "FAIL"
          rescue Gem::LoadError => e
            puts e.message
          end
        R

        expect(out).to eq("You have already activated thin 1.1, but your Gemfile requires thin 1.0. Prepending `bundle exec` to your command may solve this.")
      end

      it "version_requirement is now deprecated in rubygems 1.4.0+" do
        system_gems "thin-1.0", "rack-1.0.0"
        build_gem "thin", "1.1", :to_system => true do |s|
          s.add_dependency "rack"
        end

        gemfile <<-G
          gem "thin", "1.0"
        G

        ruby <<-R, :expect_err => true
          require 'rubygems'
          gem "thin"
          require 'bundler'
          begin
            Bundler.setup
            puts "FAIL"
          rescue Gem::LoadError => e
            puts e.message
          end
        R

        expect(err).to be_empty
      end
    end
  end

  # Rubygems returns loaded_from as a string
  it "has loaded_from as a string on all specs" do
    build_git "foo"
    build_git "no-gemspec", :gemspec => false

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "foo", :git => "#{lib_path('foo-1.0')}"
      gem "no-gemspec", "1.0", :git => "#{lib_path('no-gemspec-1.0')}"
    G

    run <<-R
      Gem.loaded_specs.each do |n, s|
        puts "FAIL" unless s.loaded_from.is_a?(String)
      end
    R

    expect(out).to be_empty
  end

  it "ignores empty gem paths" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    ENV["GEM_HOME"] = ""
    bundle %{exec ruby -e "require 'set'"}

    expect(err).to be_empty
  end

  it "should prepend gemspec require paths to $LOAD_PATH in order" do
    update_repo2 do
      build_gem("requirepaths") do |s|
        s.write("lib/rq.rb", "puts 'yay'")
        s.write("src/rq.rb", "puts 'nooo'")
        s.require_paths = ["lib", "src"]
      end
    end

    install_gemfile <<-G
      source "file://#{gem_repo2}"
      gem "requirepaths", :require => nil
    G

    run "require 'rq'"
    expect(out).to eq("yay")
  end

  it "stubs out Gem.refresh so it does not reveal system gems" do
    system_gems "rack-1.0.0"

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "activesupport"
    G

    run <<-R
      puts Bundler.rubygems.find_name("rack").inspect
      Gem.refresh
      puts Bundler.rubygems.find_name("rack").inspect
    R

    expect(out).to eq("[]\n[]")
  end

  describe "when a vendored gem specification uses the :path option" do
    it "should resolve paths relative to the Gemfile" do
      path = bundled_app(File.join('vendor', 'foo'))
      build_lib "foo", :path => path

      # If the .gemspec exists, then Bundler handles the path differently.
      # See Source::Path.load_spec_files for details.
      FileUtils.rm(File.join(path, 'foo.gemspec'))

      install_gemfile <<-G
        gem 'foo', '1.2.3', :path => 'vendor/foo'
      G

      Dir.chdir(bundled_app.parent) do
        run <<-R, :env => {"BUNDLE_GEMFILE" => bundled_app('Gemfile')}
          require 'foo'
        R
      end
      expect(err).to eq("")
    end

    it "should make sure the Bundler.root is really included in the path relative to the Gemfile" do
      relative_path = File.join('vendor', Dir.pwd[1..-1], 'foo')
      absolute_path = bundled_app(relative_path)
      FileUtils.mkdir_p(absolute_path)
      build_lib "foo", :path => absolute_path

      # If the .gemspec exists, then Bundler handles the path differently.
      # See Source::Path.load_spec_files for details.
      FileUtils.rm(File.join(absolute_path, 'foo.gemspec'))

      gemfile <<-G
        gem 'foo', '1.2.3', :path => '#{relative_path}'
      G

      bundle :install

      Dir.chdir(bundled_app.parent) do
        run <<-R, :env => {"BUNDLE_GEMFILE" => bundled_app('Gemfile')}
          require 'foo'
        R
      end

      expect(err).to eq("")
    end
  end

  describe "with git gems that don't have gemspecs" do
    before :each do
      build_git "no-gemspec", :gemspec => false

      install_gemfile <<-G
        gem "no-gemspec", "1.0", :git => "#{lib_path('no-gemspec-1.0')}"
      G
    end

    it "loads the library via a virtual spec" do
      run <<-R
        require 'no-gemspec'
        puts NOGEMSPEC
      R

      expect(out).to eq("1.0")
    end
  end

  describe "with bundled and system gems" do
    before :each do
      system_gems "rack-1.0.0"

      install_gemfile <<-G
        source "file://#{gem_repo1}"

        gem "activesupport", "2.3.5"
      G
    end

    it "does not pull in system gems" do
      run <<-R
        require 'rubygems'

        begin;
          require 'rack'
        rescue LoadError
          puts 'WIN'
        end
      R

      expect(out).to eq("WIN")
    end

    it "provides a gem method" do
      run <<-R
        gem 'activesupport'
        require 'activesupport'
        puts ACTIVESUPPORT
      R

      expect(out).to eq("2.3.5")
    end

    it "raises an exception if gem is used to invoke a system gem not in the bundle" do
      run <<-R
        begin
          gem 'rack'
        rescue LoadError => e
          puts e.message
        end
      R

      expect(out).to eq("rack is not part of the bundle. Add it to Gemfile.")
    end

    it "sets GEM_HOME appropriately" do
      run "puts ENV['GEM_HOME']"
      expect(out).to eq(default_bundle_path.to_s)
    end
  end

  describe "with system gems in the bundle" do
    before :each do
      system_gems "rack-1.0.0"

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "rack", "1.0.0"
        gem "activesupport", "2.3.5"
      G
    end

    it "sets GEM_PATH appropriately" do
      run "puts Gem.path"
      paths = out.split("\n")
      expect(paths).to include(system_gem_path.to_s)
      expect(paths).to include(default_bundle_path.to_s)
    end
  end

  describe "with a gemspec that requires other files" do
    before :each do
      build_git "bar", :gemspec => false do |s|
        s.write "lib/bar/version.rb", %{BAR_VERSION = '1.0'}
        s.write "bar.gemspec", <<-G
          lib = File.expand_path('../lib/', __FILE__)
          $:.unshift lib unless $:.include?(lib)
          require 'bar/version'

          Gem::Specification.new do |s|
            s.name        = 'bar'
            s.version     = BAR_VERSION
            s.summary     = 'Bar'
            s.files       = Dir["lib/**/*.rb"]
          end
        G
      end

      gemfile <<-G
        gem "bar", :git => "#{lib_path('bar-1.0')}"
      G
    end

    it "evals each gemspec in the context of its parent directory" do
      bundle :install
      run "require 'bar'; puts BAR"
      expect(out).to eq("1.0")
    end

    it "error intelligently if the gemspec has a LoadError" do
      update_git "bar", :gemspec => false do |s|
        s.write "bar.gemspec", "require 'foobarbaz'"
      end
      bundle :install
      expect(out).to include("was a LoadError while loading bar.gemspec")
      expect(out).to include("foobarbaz")
      expect(out).to include("bar.gemspec:1")
      expect(out).to include("try to require a relative path") if RUBY_VERSION >= "1.9"
    end

    it "evals each gemspec with a binding from the top level" do
      bundle "install"

      ruby <<-RUBY
        require 'bundler'
        def Bundler.require(path)
          raise "LOSE"
        end
        Bundler.load
      RUBY

      expect(err).to eq("")
      expect(out).to eq("")
    end
  end

  describe "when Bundler is bundled" do
    it "doesn't blow up" do
      install_gemfile <<-G
        gem "bundler", :path => "#{File.expand_path("..", lib)}"
      G

      bundle %|exec ruby -e "require 'bundler'; Bundler.setup"|
      expect(err).to be_empty
    end
  end

end
