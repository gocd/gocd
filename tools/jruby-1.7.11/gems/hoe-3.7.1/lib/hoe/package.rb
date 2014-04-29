begin
  require 'rubygems/package_task'
rescue LoadError
  # rake/gempackagetask will go away some day
  require 'rake/gempackagetask'
  Gem::PackageTask = Rake::GemPackageTask
end

##
# Package plugin for hoe.
#
# === Tasks Provided:
#
# install_gem::        Install the package as a gem.
# prerelease::         Hook for pre-release actions like sanity checks.
# postrelease::        Hook for post-release actions like release announcements.
# release::            Package and upload the release.

module Hoe::Package
  ##
  # Optional: Should package create a tarball? [default: true]

  attr_accessor :need_tar

  ##
  # Optional: Should package create a zipfile? [default: false]

  attr_accessor :need_zip

  ##
  # Initialize variables for plugin.

  def initialize_package
    self.need_tar ||= false
    self.need_zip ||= false
  end

  ##
  # Define tasks for plugin.

  def define_package_tasks
    prerelease_version

    Gem::PackageTask.new spec do |pkg|
      pkg.need_tar = @need_tar
      pkg.need_zip = @need_zip
    end

    desc 'Install the package as a gem. (opt. NOSUDO=1)'
    task :install_gem => [:clean, :package, :check_extra_deps] do
      install_gem Dir['pkg/*.gem'].first
    end

    desc 'Package and upload; Requires VERSION=x.y.z (optional PRE=a.1)'
    task :release => [:prerelease, :release_to, :postrelease]

    # no doco, invisible hook
    task :prerelease do
      abort "Fix your version before you release" if spec.version =~ /borked/
    end

    # no doco, invisible hook
    task :release_to

    # no doco, invisible hook
    task :postrelease

    desc "Sanity checks for release"
    task :release_sanity do
      v = ENV["VERSION"] or abort "Must supply VERSION=x.y.z"

      pre = ENV['PRERELEASE'] || ENV['PRE']
      v += ".#{pre}" if pre

      abort "Versions don't match #{v} vs #{version}" if v != version
    end
  end

  ##
  # Returns the path used for packaging. Convenience method for those
  # that need to write a package hook.

  def pkg_path
    "pkg/#{spec.full_name}"
  end

  ##
  # Install the named gem.

  def install_gem name, version = nil, rdoc=true
    should_not_sudo = Hoe::WINDOZE || ENV["NOSUDO"] || File.writable?(Gem.dir)
    null_dev = Hoe::WINDOZE ? '> NUL 2>&1' : '> /dev/null 2>&1'

    gem_cmd = Gem.default_exec_format % 'gem'
    sudo    = 'sudo '                  unless should_not_sudo
    local   = '--local'                unless version
    version = "--version '#{version}'" if     version

    cmd  = "#{sudo}#{gem_cmd} install #{local} #{name} #{version}"
    cmd += " --no-rdoc --no-ri" unless rdoc
    cmd += " #{null_dev}" unless Rake.application.options.trace

    puts cmd if Rake.application.options.trace
    system cmd
  end

  def prerelease_version # :nodoc:
    pre = ENV['PRERELEASE'] || ENV['PRE']
    if pre then
      spec.version.version << "." << pre if pre

      abort "ERROR: You should format PRE like pre or alpha.1 or something" if
        (Gem::VERSION < "1.4"  and pre !~ /^[a-z]+(\.\d+)?$/) or
        (Gem::VERSION >= "1.4" and pre !~ /^[a-z]+(\.?\d+)?$/)
    end
  end
end
