# -*- encoding: utf-8 -*-
# frozen_string_literal: true
$:.unshift File.expand_path("../lib", __FILE__)
require "shellwords"
require "benchmark"

RUBYGEMS_REPO = if `cd .. && git remote --verbose 2>/dev/null` =~ /rubygems/i
  File.expand_path("..")
else
  File.expand_path("tmp/rubygems")
end

BUNDLER_SPEC = Gem::Specification.load("bundler.gemspec")

def safe_task(&block)
  yield
  true
rescue
  false
end

# Benchmark task execution
module Rake
  class Task
    alias_method :real_invoke, :invoke

    def invoke(*args)
      time = Benchmark.measure(@name) do
        real_invoke(*args)
      end
      puts "#{@name} ran for #{time}"
    end
  end
end

namespace :spec do
  desc "Ensure spec dependencies are installed"
  task :deps do
    deps = Hash[BUNDLER_SPEC.development_dependencies.map do |d|
      [d.name, d.requirement.to_s]
    end]
    deps["rubocop"] ||= "= 0.45.0" if RUBY_VERSION >= "2.0.0" # can't go in the gemspec because of the ruby version requirement

    # JRuby can't build ronn or rdiscount, so we skip that
    if defined?(RUBY_ENGINE) && RUBY_ENGINE == "jruby"
      deps.delete("ronn")
      deps.delete("rdiscount")
    end

    gem_install_command = "install --no-ri --no-rdoc --conservative " + deps.sort_by {|name, _| name }.map do |name, version|
      "'#{name}:#{version}'"
    end.join(" ")
    sh %(#{Gem.ruby} -S gem #{gem_install_command})

    # Download and install gems used inside tests
    $LOAD_PATH.unshift("./spec")
    require "support/rubygems_ext"
    Spec::Rubygems.setup
  end

  namespace :travis do
    task :deps do
      # Give the travis user a name so that git won't fatally error
      system "sudo sed -i 's/1000::/1000:Travis:/g' /etc/passwd"
      # Strip secure_path so that RVM paths transmit through sudo -E
      system "sudo sed -i '/secure_path/d' /etc/sudoers"
      # Install groff so ronn can generate man/help pages
      sh "sudo apt-get install groff-base -y"
      # Install graphviz so that the viz specs can run
      sh "sudo apt-get install graphviz -y 2>&1 | tail -n 2"

      # Install the gems with a consistent version of RubyGems
      sh "gem update --system 2.6.4"

      $LOAD_PATH.unshift("./spec")
      require "support/rubygems_ext"
      Spec::Rubygems::DEPS["codeclimate-test-reporter"] = "~> 0.6.0" if RUBY_VERSION >= "2.2.0"

      # Install the other gem deps, etc
      Rake::Task["spec:deps"].invoke
    end
  end
end

begin
  rspec = BUNDLER_SPEC.development_dependencies.find {|d| d.name == "rspec" }
  gem "rspec", rspec.requirement.to_s
  require "rspec/core/rake_task"

  desc "Run specs"
  RSpec::Core::RakeTask.new
  task :spec => "man:build"

  if RUBY_VERSION >= "2.0.0"
    # can't go in the gemspec because of the ruby version requirement
    gem "rubocop", "= 0.45.0"
    require "rubocop/rake_task"
    RuboCop::RakeTask.new
  end

  namespace :spec do
    task :clean do
      rm_rf "tmp"
    end

    desc "Run the real-world spec suite (requires internet)"
    task :realworld => %w(set_realworld spec)

    task :set_realworld do
      ENV["BUNDLER_REALWORLD_TESTS"] = "1"
    end

    desc "Run the spec suite with the sudo tests"
    task :sudo => %w(set_sudo spec clean_sudo)

    task :set_sudo do
      ENV["BUNDLER_SUDO_TESTS"] = "1"
    end

    task :clean_sudo do
      puts "Cleaning up sudo test files..."
      system "sudo rm -rf #{File.expand_path("../tmp/sudo_gem_home", __FILE__)}"
    end

    # Rubygems specs by version
    namespace :rubygems do
      rubyopt = ENV["RUBYOPT"]
      # When editing this list, also edit .travis.yml!
      branches = %w(master)
      releases = %w(v1.3.6 v1.3.7 v1.4.2 v1.5.3 v1.6.2 v1.7.2 v1.8.29 v2.0.14 v2.1.11 v2.2.5 v2.4.8 v2.5.2 v2.6.8)
      (branches + releases).each do |rg|
        desc "Run specs with Rubygems #{rg}"
        RSpec::Core::RakeTask.new(rg) do |t|
          t.rspec_opts = %w(--format progress --color)
          t.ruby_opts  = %w(-w)
        end

        # Create tasks like spec:rubygems:v1.8.3:sudo to run the sudo specs
        namespace rg do
          task :sudo => ["set_sudo", rg, "clean_sudo"]
          task :realworld => ["set_realworld", rg]
        end

        task "clone_rubygems_#{rg}" do
          unless File.directory?(RUBYGEMS_REPO)
            system("git clone https://github.com/rubygems/rubygems.git tmp/rubygems")
          end
          hash = nil

          if RUBYGEMS_REPO.start_with?(Dir.pwd)
            Dir.chdir(RUBYGEMS_REPO) do
              system("git remote update")
              if rg == "master"
                system("git checkout origin/master")
              else
                system("git checkout #{rg}") || raise("Unknown Rubygems ref #{rg}")
              end
              hash = `git rev-parse HEAD`.chomp
            end
          elsif rg != "master"
            raise "need to be running against master with bundler as a submodule"
          end

          puts "Checked out rubygems '#{rg}' at #{hash}"
          ENV["RUBYOPT"] = "-I#{File.join(RUBYGEMS_REPO, "lib")} #{rubyopt}"
          puts "RUBYOPT=#{ENV["RUBYOPT"]}"
        end

        task rg => ["man:build", "clone_rubygems_#{rg}"]
        task "rubygems:all" => rg
      end

      desc "Run specs under a Rubygems checkout (set RG=path)"
      RSpec::Core::RakeTask.new("co") do |t|
        t.rspec_opts = %w(--format documentation --color)
        t.ruby_opts  = %w(-w)
      end

      task "setup_co" do
        rg = File.expand_path ENV["RG"]
        puts "Running specs against Rubygems in #{rg}..."
        ENV["RUBYOPT"] = "-I#{rg} #{rubyopt}"
      end

      task "co" => "setup_co"
      task "rubygems:all" => "co"
    end

    desc "Run the tests on Travis CI against a rubygem version (using ENV['RGV'])"
    task :travis do
      rg = ENV["RGV"] || raise("Rubygems version is required on Travis!")

      if RUBY_VERSION >= "2.0.0"
        puts "\n\e[1;33m[Travis CI] Running bundler linter\e[m\n\n"
        Rake::Task["rubocop"].invoke
      end

      puts "\n\e[1;33m[Travis CI] Running bundler specs against rubygems #{rg}\e[m\n\n"
      specs = safe_task { Rake::Task["spec:rubygems:#{rg}"].invoke }

      Rake::Task["spec:rubygems:#{rg}"].reenable

      puts "\n\e[1;33m[Travis CI] Running bundler sudo specs against rubygems #{rg}\e[m\n\n"
      sudos = system("sudo -E rake spec:rubygems:#{rg}:sudo")
      # clean up by chowning the newly root-owned tmp directory back to the travis user
      system("sudo chown -R #{ENV["USER"]} #{File.join(File.dirname(__FILE__), "tmp")}")

      Rake::Task["spec:rubygems:#{rg}"].reenable

      puts "\n\e[1;33m[Travis CI] Running bundler real world specs against rubygems #{rg}\e[m\n\n"
      realworld = safe_task { Rake::Task["spec:rubygems:#{rg}:realworld"].invoke }

      { "specs" => specs, "sudo" => sudos, "realworld" => realworld }.each do |name, passed|
        if passed
          puts "\e[0;32m[Travis CI] #{name} passed\e[m"
        else
          puts "\e[0;31m[Travis CI] #{name} failed\e[m"
        end
      end

      unless specs && sudos && realworld
        raise "Spec run failed, please review the log for more information"
      end
    end
  end

rescue LoadError
  task :spec do
    abort "Run `rake spec:deps` to be able to run the specs"
  end

  task :rubocop do
    abort "Run `rake spec:deps` to be able to run rubocop"
  end
end

begin
  require "ronn"

  namespace :man do
    directory "man"

    sources = Dir["man/*.ronn"].map {|f| File.basename(f, ".ronn") }
    sources.map do |basename|
      ronn = "man/#{basename}.ronn"
      manual_section = ".1" unless basename =~ /.*(\d+)\Z/
      roff = "man/#{basename}#{manual_section}"

      file roff => ["man", ronn] do
        sh "#{Gem.ruby} -S ronn --roff --pipe #{ronn} > #{roff}"
      end

      file "#{roff}.txt" => roff do
        sh "groff -Wall -mtty-char -mandoc -Tascii #{roff} | col -b > #{roff}.txt"
      end

      task :build_all_pages => "#{roff}.txt"
    end

    task :clean do
      leftovers = Dir["man/*"].reject do |f|
        File.extname(f) == ".ronn" || f == "man/index.txt"
      end
      rm leftovers if leftovers.any?
    end

    desc "Build the man pages"
    task :build => ["man:clean", "man:build_all_pages"]

    desc "Remove all built man pages"
    task :clobber do
      rm_rf "lib/bundler/man"
    end

    task(:require) {}
  end

rescue LoadError
  namespace :man do
    task(:require) { abort "Install the ronn gem to be able to release!" }
    task(:build) { warn "Install the ronn gem to build the help pages" }
  end
end

begin
  require "automatiek"

  Automatiek::RakeTask.new("molinillo") do |lib|
    lib.download = { :github => "https://github.com/CocoaPods/Molinillo" }
    lib.namespace = "Molinillo"
    lib.prefix = "Bundler"
    lib.vendor_lib = "lib/bundler/vendor/molinillo"
  end

  Automatiek::RakeTask.new("thor") do |lib|
    lib.download = { :github => "https://github.com/erikhuda/thor" }
    lib.namespace = "Thor"
    lib.prefix = "Bundler"
    lib.vendor_lib = "lib/bundler/vendor/thor"
  end

  Automatiek::RakeTask.new("postit") do |lib|
    lib.download = { :github => "https://github.com/bundler/postit" }
    lib.namespace = "PostIt"
    lib.prefix = "BundlerVendoredPostIt"
    lib.vendor_lib = "lib/bundler/vendor/postit"
  end

  Automatiek::RakeTask.new("net-http-persistent") do |lib|
    lib.download = { :github => "https://github.com/drbrain/net-http-persistent" }
    lib.namespace = "Net::HTTP::Persistent"
    lib.prefix = "Bundler::Persistent"
    lib.vendor_lib = "lib/bundler/vendor/net-http-persistent"

    mixin = Module.new do
      def namespace_files
        super
        require_target = vendor_lib.sub(%r{^(.+?/)?lib/}, "") << "/lib"
        relative_files = files.map {|f| Pathname.new(f).relative_path_from(Pathname.new(vendor_lib) / "lib").sub_ext("").to_s }
        process_files(/require (['"])(#{Regexp.union(relative_files)})/, "require \\1#{require_target}/\\2")
      end
    end
    lib.send(:extend, mixin)
  end
rescue LoadError
  namespace :vendor do
    task(:molinillo) { abort "Install the automatiek gem to be able to vendor gems." }
    task(:thor) { abort "Install the automatiek gem to be able to vendor gems." }
    task(:postit) { abort "Install the automatiek gem to be able to vendor gems." }
    task("net-http-persistent") { abort "Install the automatiek gem to be able to vendor gems." }
  end
end

desc "Update vendored SSL certs to match the certs vendored by Rubygems"
task :update_certs => "spec:rubygems:clone_rubygems_master" do
  require "bundler/ssl_certs/certificate_manager"
  Bundler::SSLCerts::CertificateManager.update_from!(RUBYGEMS_REPO)
end

require "bundler/gem_tasks"
task :build => ["man:build"]
task :release => ["man:require", "man:build"]

task :default => :spec
