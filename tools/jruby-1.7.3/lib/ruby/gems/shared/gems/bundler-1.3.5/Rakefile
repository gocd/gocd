# -*- encoding: utf-8 -*-
$:.unshift File.expand_path("../lib", __FILE__)
require 'bundler/gem_tasks'
require 'rubygems'
require 'shellwords'
require 'benchmark'

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
    {"rdiscount" => "~> 1.6", "ronn" => "~> 0.7.3", "rspec" => "~> 2.13"}.each do |name, version|
      sh "#{Gem.ruby} -S gem list #{name} -v '#{version}' | grep '#{name}' -q || " \
         "#{Gem.ruby} -S gem install #{name} -v '#{version}' --no-ri --no-rdoc"
    end
  end

  namespace :travis do
    task :deps do
      # Give the travis user a name so that git won't fatally error
      system("sudo sed -i 's/1000::/1000:Travis:/g' /etc/passwd")
      # Strip secure_path so that RVM paths transmit through sudo -E
      system("sudo sed -i '/secure_path/d' /etc/sudoers")
      # Install groff for the ronn gem
      system("sudo apt-get install groff -y")
      # Install the other gem deps, etc.
      Rake::Task["spec:deps"].invoke
    end
  end
end

begin
  # running the specs needs both rspec and ronn
  require 'rspec/core/rake_task'
  require 'ronn'

  desc "Run specs"
  RSpec::Core::RakeTask.new do |t|
    t.rspec_opts = %w(-fs --color)
    t.ruby_opts  = %w(-w)
  end
  task :spec => "man:build"

  namespace :spec do
    task :clean do
      rm_rf 'tmp'
    end

    desc "Run the real-world spec suite (requires internet)"
    task :realworld => ["set_realworld", "spec"]

    task :set_realworld do
      ENV['BUNDLER_REALWORLD_TESTS'] = '1'
    end

    desc "Run the spec suite with the sudo tests"
    task :sudo => ["set_sudo", "spec", "clean_sudo"]

    task :set_sudo do
      ENV['BUNDLER_SUDO_TESTS'] = '1'
    end

    task :clean_sudo do
      puts "Cleaning up sudo test files..."
      system "sudo rm -rf #{File.expand_path('../tmp/sudo_gem_home', __FILE__)}"
    end

    namespace :rubygems do
      # Rubygems specs by version
      rubyopt = ENV["RUBYOPT"]
      %w(master v1.3.6 v1.3.7 v1.4.2 v1.5.3 v1.6.2 v1.7.2 v1.8.25 v2.0.2).each do |rg|
        desc "Run specs with Rubygems #{rg}"
        RSpec::Core::RakeTask.new(rg) do |t|
          t.rspec_opts = %w(-fs --color)
          t.ruby_opts  = %w(-w)
        end

        # Create tasks like spec:rubygems:v1.8.3:sudo to run the sudo specs
        namespace rg do
          task :sudo => ["set_sudo", rg, "clean_sudo"]
          task :realworld => ["set_realworld", rg]
        end

        task "clone_rubygems_#{rg}" do
          unless File.directory?("tmp/rubygems")
            system("git clone git://github.com/rubygems/rubygems.git tmp/rubygems")
          end
          hash = nil

          Dir.chdir("tmp/rubygems") do
            system("git remote update")
            if rg == "master"
              system("git checkout origin/master")
            else
              system("git checkout #{rg}")
            end
            hash = `git rev-parse HEAD`.chomp
          end

          puts "Checked out rubygems '#{rg}' at #{hash}"
          ENV["RUBYOPT"] = "-I#{File.expand_path("tmp/rubygems/lib")} #{rubyopt}"
          puts "RUBYOPT=#{ENV['RUBYOPT']}"
        end

        task rg => ["clone_rubygems_#{rg}", "man:build"]
        task "rubygems:all" => rg
      end

      desc "Run specs under a Rubygems checkout (set RG=path)"
      RSpec::Core::RakeTask.new("co") do |t|
        t.rspec_opts = %w(-fs --color)
        t.ruby_opts  = %w(-w)
      end

      task "setup_co" do
        ENV["RUBYOPT"] = "-I#{File.expand_path ENV['RG']} #{rubyopt}"
      end

      task "co" => "setup_co"
      task "rubygems:all" => "co"
    end

    desc "Run the tests on Travis CI against a rubygem version (using ENV['RGV'])"
    task :travis do
      rg = ENV['RGV'] || 'v1.8.24'

      puts "\n\e[1;33m[Travis CI] Running bundler specs against rubygems #{rg}\e[m\n\n"
      specs = safe_task { Rake::Task["spec:rubygems:#{rg}"].invoke }

      Rake::Task["spec:rubygems:#{rg}"].reenable

      puts "\n\e[1;33m[Travis CI] Running bundler sudo specs against rubygems #{rg}\e[m\n\n"
      sudos = system("sudo -E rake spec:rubygems:#{rg}:sudo")
      # clean up by chowning the newly root-owned tmp directory back to the travis user
      system("sudo chown -R #{ENV['USER']} #{File.join(File.dirname(__FILE__), 'tmp')}")

      Rake::Task["spec:rubygems:#{rg}"].reenable

      puts "\n\e[1;33m[Travis CI] Running bundler real world specs against rubygems #{rg}\e[m\n\n"
      realworld = safe_task { Rake::Task["spec:rubygems:#{rg}:realworld"].invoke }

      {"specs" => specs, "sudo" => sudos, "realworld" => realworld}.each do |name, passed|
        if passed
          puts "\e[0;32m[Travis CI] #{name} passed\e[m"
        else
          puts "\e[0;31m[Travis CI] #{name} failed\e[m"
        end
      end

      unless specs && sudos && realworld
        fail "Spec run failed, please review the log for more information"
      end
    end
  end

rescue LoadError
  task :spec do
    abort "Run `rake spec:deps` to be able to run the specs"
  end
end

begin
  require 'ronn'

  namespace :man do
    directory "lib/bundler/man"

    Dir["man/*.ronn"].each do |ronn|
      basename = File.basename(ronn, ".ronn")
      roff = "lib/bundler/man/#{basename}"

      file roff => ["lib/bundler/man", ronn] do
        sh "#{Gem.ruby} -S ronn --roff --pipe #{ronn} > #{roff}"
      end

      file "#{roff}.txt" => roff do
        sh "groff -Wall -mtty-char -mandoc -Tascii #{roff} | col -b > #{roff}.txt"
      end

      task :build_all_pages => "#{roff}.txt"
    end

    desc "Build the man pages"
    task :build => "man:build_all_pages"

    desc "Clean up from the built man pages"
    task :clean do
      rm_rf "lib/bundler/man"
    end
  end

rescue LoadError
  namespace :man do
    task(:build) { abort "Install the ronn gem to be able to release!" }
    task(:clean) { abort "Install the ronn gem to be able to release!" }
  end
end

task :build => ["man:clean", "man:build"]
task :release => ["man:clean", "man:build"]

namespace :vendor do
  desc "Build the vendor dir"
  task :build => :clean do
    sh "git clone git://github.com/wycats/thor.git lib/bundler/vendor/tmp"
    sh "mv lib/bundler/vendor/tmp/lib/* lib/bundler/vendor/"
    rm_rf "lib/bundler/vendor/tmp"
  end

  desc "Clean the vendor dir"
  task :clean do
    rm_rf "lib/bundler/vendor"
  end
end

task :default => :spec
