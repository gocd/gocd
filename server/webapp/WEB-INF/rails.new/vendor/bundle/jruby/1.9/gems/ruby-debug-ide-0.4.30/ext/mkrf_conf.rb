jruby = defined?(JRUBY_VERSION) || (defined?(RUBY_ENGINE) && 'jruby' == RUBY_ENGINE)
rbx = defined?(RUBY_ENGINE) && 'rbx' == RUBY_ENGINE

def already_installed(dep)
  !Gem::DependencyInstaller.new(:domain => :local).find_gems_with_sources(dep).empty? ||
  !Gem::DependencyInstaller.new(:domain => :local,:prerelease => true).find_gems_with_sources(dep).empty?    
end

unless jruby || rbx
  require 'rubygems'
  require 'rubygems/command.rb'
  require 'rubygems/dependency.rb'
  require 'rubygems/dependency_installer.rb'

  begin
    Gem::Command.build_args = ARGV
    rescue NoMethodError
  end

  if RUBY_VERSION < "1.9"
    dep = Gem::Dependency.new("ruby-debug-base", '>=0.10.4')
  elsif RUBY_VERSION < '2.0'
    dep = Gem::Dependency.new("ruby-debug-base19x", '>=0.11.30.pre15')
  else    
    dep = Gem::Dependency.new("debase", '> 0')
  end

  begin
    puts "Installing base gem"
    inst = Gem::DependencyInstaller.new :prerelease => dep.prerelease?
    inst.install dep
  rescue
    begin
      inst = Gem::DependencyInstaller.new(:prerelease => true)
      inst.install dep
    rescue Exception => e
      puts e
      puts e.backtrace.join "\n  "
      exit(1)
    end
  end unless dep.nil? || already_installed(dep)
end

# create dummy rakefile to indicate success
f = File.open(File.join(File.dirname(__FILE__), "Rakefile"), "w")
f.write("task :default\n")
f.close

