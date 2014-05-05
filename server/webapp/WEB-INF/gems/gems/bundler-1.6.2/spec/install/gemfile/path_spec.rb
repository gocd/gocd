require "spec_helper"

describe "bundle install with explicit source paths" do
  it "fetches gems" do
    build_lib "foo"

    install_gemfile <<-G
      path "#{lib_path('foo-1.0')}"
      gem 'foo'
    G

    should_be_installed("foo 1.0")
  end

  it "supports pinned paths" do
    build_lib "foo"

    install_gemfile <<-G
      gem 'foo', :path => "#{lib_path('foo-1.0')}"
    G

    should_be_installed("foo 1.0")
  end

  it "supports relative paths" do
    build_lib "foo"

    relative_path = lib_path('foo-1.0').relative_path_from(Pathname.new(Dir.pwd))

    install_gemfile <<-G
      gem 'foo', :path => "#{relative_path}"
    G

    should_be_installed("foo 1.0")
  end

  it "expands paths" do
    build_lib "foo"

    relative_path = lib_path('foo-1.0').relative_path_from(Pathname.new('~').expand_path)

    install_gemfile <<-G
      gem 'foo', :path => "~/#{relative_path}"
    G

    should_be_installed("foo 1.0")
  end

  it "expands paths raise error with not existing user's home dir" do
    build_lib "foo"
    username = 'some_unexisting_user'
    relative_path = lib_path('foo-1.0').relative_path_from(Pathname.new("/home/#{username}").expand_path)

    install_gemfile <<-G
      gem 'foo', :path => "~#{username}/#{relative_path}"
    G
    expect(out).to match("There was an error while trying to use the path `~#{username}/#{relative_path}`.")
    expect(out).to match("user #{username} doesn't exist")
  end

  it "expands paths relative to Bundler.root" do
    build_lib "foo", :path => bundled_app("foo-1.0")

    install_gemfile <<-G
      gem 'foo', :path => "./foo-1.0"
    G

    bundled_app("subdir").mkpath
    Dir.chdir(bundled_app("subdir")) do
      should_be_installed("foo 1.0")
    end
  end

  it "expands paths when comparing locked paths to Gemfile paths" do
    build_lib "foo", :path => bundled_app("foo-1.0")

    install_gemfile <<-G
      gem 'foo', :path => File.expand_path("../foo-1.0", __FILE__)
    G

    bundle "install --frozen", :exitstatus => true
    expect(exitstatus).to eq(0)
  end

  it "installs dependencies from the path even if a newer gem is available elsewhere" do
    system_gems "rack-1.0.0"

    build_lib "rack", "1.0", :path => lib_path('nested/bar') do |s|
      s.write "lib/rack.rb", "puts 'WIN OVERRIDE'"
    end

    build_lib "foo", :path => lib_path('nested') do |s|
      s.add_dependency "rack", "= 1.0"
    end

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "foo", :path => "#{lib_path('nested')}"
    G

    run "require 'rack'"
    expect(out).to eq('WIN OVERRIDE')
  end

  it "works" do
    build_gem "foo", "1.0.0", :to_system => true do |s|
      s.write "lib/foo.rb", "puts 'FAIL'"
    end

    build_lib "omg", "1.0", :path => lib_path("omg") do |s|
      s.add_dependency "foo"
    end

    build_lib "foo", "1.0.0", :path => lib_path("omg/foo")

    install_gemfile <<-G
      gem "omg", :path => "#{lib_path('omg')}"
    G

    should_be_installed "foo 1.0"
  end

  it "supports gemspec syntax" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.add_dependency "rack", "1.0"
    end

    gemfile = <<-G
      source "file://#{gem_repo1}"
      gemspec
    G

    File.open(lib_path("foo/Gemfile"), "w") {|f| f.puts gemfile }

    Dir.chdir(lib_path("foo")) do
      bundle "install"
      should_be_installed "foo 1.0"
      should_be_installed "rack 1.0"
    end
  end

  it "supports gemspec syntax with an alternative path" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.add_dependency "rack", "1.0"
    end

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gemspec :path => "#{lib_path("foo")}"
    G

    should_be_installed "foo 1.0"
    should_be_installed "rack 1.0"
  end

  it "doesn't automatically unlock dependencies when using the gemspec syntax" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.add_dependency "rack", ">= 1.0"
    end

    Dir.chdir lib_path("foo")

    install_gemfile lib_path("foo/Gemfile"), <<-G
      source "file://#{gem_repo1}"
      gemspec
    G

    build_gem "rack", "1.0.1", :to_system => true

    bundle "install"

    should_be_installed "foo 1.0"
    should_be_installed "rack 1.0"
  end

  it "doesn't automatically unlock dependencies when using the gemspec syntax and the gem has development dependencies" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.add_dependency "rack", ">= 1.0"
      s.add_development_dependency "activesupport"
    end

    Dir.chdir lib_path("foo")

    install_gemfile lib_path("foo/Gemfile"), <<-G
      source "file://#{gem_repo1}"
      gemspec
    G

    build_gem "rack", "1.0.1", :to_system => true

    bundle "install"

    should_be_installed "foo 1.0"
    should_be_installed "rack 1.0"
  end

  it "raises if there are multiple gemspecs" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.write "bar.gemspec"
    end

    install_gemfile <<-G, :exitstatus => true
      gemspec :path => "#{lib_path("foo")}"
    G

    expect(exitstatus).to eq(15)
    expect(out).to match(/There are multiple gemspecs/)
  end

  it "allows :name to be specified to resolve ambiguity" do
    build_lib "foo", "1.0", :path => lib_path("foo") do |s|
      s.write "bar.gemspec"
    end

    install_gemfile <<-G, :exitstatus => true
      gemspec :path => "#{lib_path("foo")}", :name => "foo"
    G

    should_be_installed "foo 1.0"
  end

  it "sets up executables" do
    pending_jruby_shebang_fix

    build_lib "foo" do |s|
      s.executables = "foobar"
    end

    install_gemfile <<-G
      path "#{lib_path('foo-1.0')}"
      gem 'foo'
    G

    bundle "exec foobar"
    expect(out).to eq("1.0")
  end

  it "handles directories in bin/" do
    build_lib "foo"
    lib_path("foo-1.0").join("foo.gemspec").rmtree
    lib_path("foo-1.0").join("bin/performance").mkpath

    install_gemfile <<-G
      gem 'foo', '1.0', :path => "#{lib_path('foo-1.0')}"
    G
    expect(err).to eq("")
  end

  it "removes the .gem file after installing" do
    build_lib "foo"

    install_gemfile <<-G
      gem 'foo', :path => "#{lib_path('foo-1.0')}"
    G

    expect(lib_path('foo-1.0').join('foo-1.0.gem')).not_to exist
  end

  describe "block syntax" do
    it "pulls all gems from a path block" do
      build_lib "omg"
      build_lib "hi2u"

      install_gemfile <<-G
        path "#{lib_path}" do
          gem "omg"
          gem "hi2u"
        end
      G

      should_be_installed "omg 1.0", "hi2u 1.0"
    end
  end

  it "keeps source pinning" do
    build_lib "foo", "1.0", :path => lib_path('foo')
    build_lib "omg", "1.0", :path => lib_path('omg')
    build_lib "foo", "1.0", :path => lib_path('omg/foo') do |s|
      s.write "lib/foo.rb", "puts 'FAIL'"
    end

    install_gemfile <<-G
      gem "foo", :path => "#{lib_path('foo')}"
      gem "omg", :path => "#{lib_path('omg')}"
    G

    should_be_installed "foo 1.0"
  end

  it "works when the path does not have a gemspec" do
    build_lib "foo", :gemspec => false

    gemfile <<-G
      gem "foo", "1.0", :path => "#{lib_path('foo-1.0')}"
    G

    should_be_installed "foo 1.0"

    should_be_installed "foo 1.0"
  end

  it "installs executable stubs" do
    build_lib "foo" do |s|
      s.executables = ['foo']
    end

    install_gemfile <<-G
      gem "foo", :path => "#{lib_path('foo-1.0')}"
    G

    bundle "exec foo"
    expect(out).to eq("1.0")
  end

  describe "when the gem version in the path is updated" do
    before :each do
      build_lib "foo", "1.0", :path => lib_path("foo") do |s|
        s.add_dependency "bar"
      end
      build_lib "bar", "1.0", :path => lib_path("foo/bar")

      install_gemfile <<-G
        gem "foo", :path => "#{lib_path('foo')}"
      G
    end

    it "unlocks all gems when the top level gem is updated" do
      build_lib "foo", "2.0", :path => lib_path("foo") do |s|
        s.add_dependency "bar"
      end

      bundle "install"

      should_be_installed "foo 2.0", "bar 1.0"
    end

    it "unlocks all gems when a child dependency gem is updated" do
      build_lib "bar", "2.0", :path => lib_path("foo/bar")

      bundle "install"

      should_be_installed "foo 1.0", "bar 2.0"
    end
  end

  describe "when dependencies in the path are updated" do
    before :each do
      build_lib "foo", "1.0", :path => lib_path("foo")

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "foo", :path => "#{lib_path('foo')}"
      G
    end

    it "gets dependencies that are updated in the path" do
      build_lib "foo", "1.0", :path => lib_path("foo") do |s|
        s.add_dependency "rack"
      end

      bundle "install"

      should_be_installed "rack 1.0.0"
    end
  end

  describe "switching sources" do
    it "doesn't switch pinned git sources to rubygems when pinning the parent gem to a path source" do
      build_gem "foo", "1.0", :to_system => true do |s|
        s.write "lib/foo.rb", "raise 'fail'"
      end
      build_lib "foo", "1.0", :path => lib_path('bar/foo')
      build_git "bar", "1.0", :path => lib_path('bar') do |s|
        s.add_dependency 'foo'
      end

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "bar", :git => "#{lib_path('bar')}"
      G

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "bar", :path => "#{lib_path('bar')}"
      G

      should_be_installed "foo 1.0", "bar 1.0"
    end

    it "switches the source when the gem existed in rubygems and the path was already being used for another gem" do
      build_lib "foo", "1.0", :path => lib_path("foo")
      build_gem "bar", "1.0", :to_system => true do |s|
        s.write "lib/bar.rb", "raise 'fail'"
      end

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        gem "bar"
        path "#{lib_path('foo')}" do
          gem "foo"
        end
      G

      build_lib "bar", "1.0", :path => lib_path("foo/bar")

      install_gemfile <<-G
        source "file://#{gem_repo1}"
        path "#{lib_path('foo')}" do
          gem "foo"
          gem "bar"
        end
      G

      should_be_installed "bar 1.0"
    end
  end

  describe "gem install hooks" do
    it "runs pre-install hooks" do
      build_git "foo"
      gemfile <<-G
        gem "foo", :git => "#{lib_path('foo-1.0')}"
      G

      File.open(lib_path("install_hooks.rb"), "w") do |h|
        h.write <<-H
          require 'rubygems'
          Gem.pre_install_hooks << lambda do |inst|
            STDERR.puts "Ran pre-install hook: \#{inst.spec.full_name}"
          end
        H
      end

      bundle :install, :expect_err => true,
        :requires => [lib_path('install_hooks.rb')]
      expect(err).to eq("Ran pre-install hook: foo-1.0")
    end

    it "runs post-install hooks" do
      build_git "foo"
      gemfile <<-G
        gem "foo", :git => "#{lib_path('foo-1.0')}"
      G

      File.open(lib_path("install_hooks.rb"), "w") do |h|
        h.write <<-H
          require 'rubygems'
          Gem.post_install_hooks << lambda do |inst|
            STDERR.puts "Ran post-install hook: \#{inst.spec.full_name}"
          end
        H
      end

      bundle :install, :expect_err => true,
        :requires => [lib_path('install_hooks.rb')]
      expect(err).to eq("Ran post-install hook: foo-1.0")
    end

    it "complains if the install hook fails" do
      build_git "foo"
      gemfile <<-G
        gem "foo", :git => "#{lib_path('foo-1.0')}"
      G

      File.open(lib_path("install_hooks.rb"), "w") do |h|
        h.write <<-H
          require 'rubygems'
          Gem.pre_install_hooks << lambda do |inst|
            false
          end
        H
      end

      bundle :install, :expect_err => true,
        :requires => [lib_path('install_hooks.rb')]
      expect(out).to include("failed for foo-1.0")
    end
  end

end
