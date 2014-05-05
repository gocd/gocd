require "spec_helper"

describe "the lockfile format" do
  include Bundler::GemHelpers

  it "generates a simple lockfile for a single source, gem" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rack
    G
  end

  it "generates a simple lockfile for a single source, gem with dependencies" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack-obama"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)
          rack-obama (1.0)
            rack

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rack-obama
    G
  end

  it "generates a simple lockfile for a single source, gem with a version requirement" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack-obama", ">= 1.0"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)
          rack-obama (1.0)
            rack

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rack-obama (>= 1.0)
    G
  end

  it "generates lockfiles with multiple requirements" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "net-sftp"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          net-sftp (1.1.1)
            net-ssh (>= 1.0.0, < 1.99.0)
          net-ssh (1.0)

      PLATFORMS
        ruby

      DEPENDENCIES
        net-sftp
    G

    should_be_installed "net-sftp 1.1.1", "net-ssh 1.0.0"
  end

  it "generates a simple lockfile for a single pinned source, gem with a version requirement" do
    git = build_git "foo"

    install_gemfile <<-G
      gem "foo", :git => "#{lib_path("foo-1.0")}"
    G

    lockfile_should_be <<-G
      GIT
        remote: #{lib_path("foo-1.0")}
        revision: #{git.ref_for('master')}
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "does not assplode when a platform specific dependency is present and the Gemfile has not been resolved on that platform" do
    build_lib "omg", :path => lib_path('omg')

    gemfile <<-G
      source "file://#{gem_repo1}"

      platforms :#{not_local_tag} do
        gem "omg", :path => "#{lib_path('omg')}"
      end

      gem "rack"
    G

    lockfile <<-L
      GIT
        remote: git://github.com/nex3/haml.git
        revision: 8a2271f
        specs:

      GEM
        remote: file://#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        #{not_local}

      DEPENDENCIES
        omg!
        rack
    L

    bundle "install"
    should_be_installed "rack 1.0.0"
  end

  it "serializes global git sources" do
    git = build_git "foo"

    install_gemfile <<-G
      git "#{lib_path('foo-1.0')}" do
        gem "foo"
      end
    G

    lockfile_should_be <<-G
      GIT
        remote: #{lib_path('foo-1.0')}
        revision: #{git.ref_for('master')}
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "generates a lockfile with a ref for a single pinned source, git gem with a branch requirement" do
    git = build_git "foo"
    update_git "foo", :branch => "omg"

    install_gemfile <<-G
      gem "foo", :git => "#{lib_path("foo-1.0")}", :branch => "omg"
    G

    lockfile_should_be <<-G
      GIT
        remote: #{lib_path("foo-1.0")}
        revision: #{git.ref_for('omg')}
        branch: omg
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "generates a lockfile with a ref for a single pinned source, git gem with a tag requirement" do
    git = build_git "foo"
    update_git "foo", :tag => "omg"

    install_gemfile <<-G
      gem "foo", :git => "#{lib_path("foo-1.0")}", :tag => "omg"
    G

    lockfile_should_be <<-G
      GIT
        remote: #{lib_path("foo-1.0")}
        revision: #{git.ref_for('omg')}
        tag: omg
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "serializes pinned path sources to the lockfile" do
    build_lib "foo"

    install_gemfile <<-G
      gem "foo", :path => "#{lib_path("foo-1.0")}"
    G

    lockfile_should_be <<-G
      PATH
        remote: #{lib_path("foo-1.0")}
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "lists gems alphabetically" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "thin"
      gem "actionpack"
      gem "rack-obama"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          actionpack (2.3.2)
            activesupport (= 2.3.2)
          activesupport (2.3.2)
          rack (1.0.0)
          rack-obama (1.0)
            rack
          thin (1.0)
            rack

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        actionpack
        rack-obama
        thin
    G
  end

  it "orders dependencies' dependencies in alphabetical order" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rails"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          actionmailer (2.3.2)
            activesupport (= 2.3.2)
          actionpack (2.3.2)
            activesupport (= 2.3.2)
          activerecord (2.3.2)
            activesupport (= 2.3.2)
          activeresource (2.3.2)
            activesupport (= 2.3.2)
          activesupport (2.3.2)
          rails (2.3.2)
            actionmailer (= 2.3.2)
            actionpack (= 2.3.2)
            activerecord (= 2.3.2)
            activeresource (= 2.3.2)
            rake (= 10.0.2)
          rake (10.0.2)

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rails
    G
  end

  it "orders dependencies by version" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem 'double_deps'
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          double_deps (1.0)
            net-ssh
            net-ssh (>= 1.0.0)
          net-ssh (1.0)

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        double_deps
    G
  end

  it "does not add the :require option to the lockfile" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack-obama", ">= 1.0", :require => "rack/obama"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)
          rack-obama (1.0)
            rack

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rack-obama (>= 1.0)
    G
  end

  it "does not add the :group option to the lockfile" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack-obama", ">= 1.0", :group => :test
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)
          rack-obama (1.0)
            rack

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        rack-obama (>= 1.0)
    G
  end

  it "stores relative paths when the path is provided in a relative fashion and in Gemfile dir" do
    build_lib "foo", :path => bundled_app('foo')

    install_gemfile <<-G
      path "foo"
      gem "foo"
    G

    lockfile_should_be <<-G
      PATH
        remote: foo
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo
    G
  end

  it "stores relative paths when the path is provided in a relative fashion and is above Gemfile dir" do
    build_lib "foo", :path => bundled_app(File.join('..', 'foo'))

    install_gemfile <<-G
      path "../foo"
      gem "foo"
    G

    lockfile_should_be <<-G
      PATH
        remote: ../foo
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo
    G
  end

  it "stores relative paths when the path is provided in an absolute fashion but is relative" do
    build_lib "foo", :path => bundled_app('foo')

    install_gemfile <<-G
      path File.expand_path("../foo", __FILE__)
      gem "foo"
    G

    lockfile_should_be <<-G
      PATH
        remote: foo
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo
    G
  end

  it "stores relative paths when the path is provided for gemspec" do
    build_lib("foo", :path => tmp.join("foo"))

    install_gemfile <<-G
      gemspec :path => "../foo"
    G

    lockfile_should_be <<-G
      PATH
        remote: ../foo
        specs:
          foo (1.0)

      GEM
        specs:

      PLATFORMS
        #{generic(Gem::Platform.local)}

      DEPENDENCIES
        foo!
    G
  end

  it "keeps existing platforms in the lockfile" do
    lockfile <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        java

      DEPENDENCIES
        rack
    G

    install_gemfile <<-G
      source "file://#{gem_repo1}"

      gem "rack"
    G

    platforms = ['java', generic(Gem::Platform.local).to_s].sort

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        #{platforms[0]}
        #{platforms[1]}

      DEPENDENCIES
        rack
    G
  end

  it "persists the spec's platform to the lockfile" do
    build_gem "platform_specific", "1.0.0", :to_system => true do |s|
      s.platform = Gem::Platform.new('universal-java-16')
    end

    simulate_platform "universal-java-16"

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "platform_specific"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          platform_specific (1.0-java)

      PLATFORMS
        java

      DEPENDENCIES
        platform_specific
    G
  end

  it "does not add duplicate gems" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
    G

    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "activesupport"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          activesupport (2.3.5)
          rack (1.0.0)

      PLATFORMS
        ruby

      DEPENDENCIES
        activesupport
        rack
    G
  end

  it "does not add duplicate dependencies" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "rack"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        ruby

      DEPENDENCIES
        rack
    G
  end

  it "does not add duplicate dependencies with versions" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", "1.0"
      gem "rack", "1.0"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        ruby

      DEPENDENCIES
        rack (= 1.0)
    G
  end

  it "does not add duplicate dependencies in different groups" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", "1.0", :group => :one
      gem "rack", "1.0", :group => :two
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (1.0.0)

      PLATFORMS
        ruby

      DEPENDENCIES
        rack (= 1.0)
    G
  end

  it "raises if two different versions are used" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", "1.0"
      gem "rack", "1.1"
    G

    expect(bundled_app("Gemfile.lock")).not_to exist
    expect(out).to include "rack (= 1.0) and rack (= 1.1)"
  end


  it "raises if two different sources are used" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack"
      gem "rack", :git => "git://hubz.com"
    G

    expect(bundled_app("Gemfile.lock")).not_to exist
    expect(out).to include "rack (>= 0) should come from an unspecified source and git://hubz.com (at master)"
  end

  it "works correctly with multiple version dependencies" do
    install_gemfile <<-G
      source "file://#{gem_repo1}"
      gem "rack", "> 0.9", "< 1.0"
    G

    lockfile_should_be <<-G
      GEM
        remote: file:#{gem_repo1}/
        specs:
          rack (0.9.1)

      PLATFORMS
        ruby

      DEPENDENCIES
        rack (> 0.9, < 1.0)
    G

  end

  # Some versions of the Bundler 1.1 RC series introduced corrupted
  # lockfiles. There were two major problems:
  #
  # * multiple copies of the same GIT section appeared in the lockfile
  # * when this happened, those sections got multiple copies of gems
  #   in those sections.
  it "fixes corrupted lockfiles" do
    build_git "omg", :path => lib_path('omg')
    revision = revision_for(lib_path('omg'))

    gemfile <<-G
      source "file://#{gem_repo1}"
      gem "omg", :git => "#{lib_path('omg')}", :branch => 'master'
    G

    bundle "install --path vendor"
    should_be_installed "omg 1.0"

    # Create a Gemfile.lock that has duplicate GIT sections
    lockfile <<-L
      GIT
        remote: #{lib_path('omg')}
        revision: #{revision}
        branch: master
        specs:
          omg (1.0)

      GIT
        remote: #{lib_path('omg')}
        revision: #{revision}
        branch: master
        specs:
          omg (1.0)

      GEM
        remote: file:#{gem_repo1}/
        specs:

      PLATFORMS
        #{local}

      DEPENDENCIES
        omg!
    L

    FileUtils.rm_rf(bundled_app('vendor'))
    bundle "install"
    should_be_installed "omg 1.0"

    # Confirm that duplicate specs do not appear
    expect(File.read(bundled_app('Gemfile.lock'))).to eq(strip_whitespace(<<-L))
      GIT
        remote: #{lib_path('omg')}
        revision: #{revision}
        branch: master
        specs:
          omg (1.0)

      GEM
        remote: file:#{gem_repo1}/
        specs:

      PLATFORMS
        #{local}

      DEPENDENCIES
        omg!
    L
  end

  describe "a line ending" do
    def set_lockfile_mtime_to_known_value
      time = Time.local(2000, 1, 1, 0, 0, 0)
      File.utime(time, time, bundled_app('Gemfile.lock'))
    end
    before(:each) do
      build_repo2
      install_gemfile <<-G
        source "file://#{gem_repo2}"
        gem "rack"
      G
      set_lockfile_mtime_to_known_value
    end

    it "generates Gemfile.lock with \\n line endings" do
      expect(File.read(bundled_app("Gemfile.lock"))).not_to match("\r\n")
      should_be_installed "rack 1.0"
    end

    context "during updates" do

      it "preserves Gemfile.lock \\n line endings" do
        update_repo2

        expect { bundle "update" }.to change { File.mtime(bundled_app('Gemfile.lock')) }
        expect(File.read(bundled_app("Gemfile.lock"))).not_to match("\r\n")
        should_be_installed "rack 1.2"
      end

      it "preserves Gemfile.lock \\n\\r line endings" do
        update_repo2
        win_lock = File.read(bundled_app("Gemfile.lock")).gsub(/\n/, "\r\n")
        File.open(bundled_app("Gemfile.lock"), "wb"){|f| f.puts(win_lock) }
        set_lockfile_mtime_to_known_value

        expect { bundle "update" }.to change { File.mtime(bundled_app('Gemfile.lock')) }
        expect(File.read(bundled_app("Gemfile.lock"))).to match("\r\n")
        should_be_installed "rack 1.2"
      end
    end

    context "when nothing changes" do

      it "preserves Gemfile.lock \\n line endings" do
        expect { ruby <<-RUBY
                   require 'rubygems'
                   require 'bundler'
                   Bundler.setup
                 RUBY
               }.not_to change { File.mtime(bundled_app('Gemfile.lock')) }
      end

      it "preserves Gemfile.lock \\n\\r line endings" do
        win_lock = File.read(bundled_app("Gemfile.lock")).gsub(/\n/, "\r\n")
        File.open(bundled_app("Gemfile.lock"), "wb"){|f| f.puts(win_lock) }
        set_lockfile_mtime_to_known_value

        expect { ruby <<-RUBY
                   require 'rubygems'
                   require 'bundler'
                   Bundler.setup
                 RUBY
               }.not_to change { File.mtime(bundled_app('Gemfile.lock')) }
      end
    end
  end

  it "refuses to install if Gemfile.lock contains conflict markers" do
    lockfile <<-L
      GEM
        remote: file://#{gem_repo1}/
        specs:
      <<<<<<<
          rack (1.0.0)
      =======
          rack (1.0.1)
      >>>>>>>

      PLATFORMS
        ruby

      DEPENDENCIES
        rack
    L

    error = install_gemfile(<<-G, :expect_err => true)
      source "file://#{gem_repo1}"
      gem "rack"
    G

    expect(error).to match(/your Gemfile.lock contains merge conflicts/i)
    expect(error).to match(/git checkout HEAD -- Gemfile.lock/i)
  end
end
