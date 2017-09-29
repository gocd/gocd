require 'rubygems'
require 'rubygems/command'
require 'rubygems/dependency_installer'

begin
  Gem::Command.build_args = ARGV
rescue NoMethodError
  exit 1
end

installer = Gem::DependencyInstaller.new

begin
  if RUBY_PLATFORM =~ /mswin|cygwin|mingw/ && RUBY_VERSION.to_i < 2
    installer.install('windows-pr')
    installer.install('win32console')
  end
rescue
  exit 1
end

File.write(File.join(File.dirname(__FILE__), 'Rakefile'), "task :default\n")
