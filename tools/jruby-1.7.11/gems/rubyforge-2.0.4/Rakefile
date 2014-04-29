# -*- ruby -*-

require 'rubygems'
require 'hoe'
require 'json'

abort "you _must_ install this gem to release it" if
  ENV['VERSION'] && ENV['VERSION'] != RubyForge::VERSION

Hoe.plugin :email

Hoe.spec "rubyforge" do 
  developer 'Ryan Davis',   'ryand-ruby@zenspider.com'
  developer 'Eric Hodel',   'drbrain@segment7.net'
  developer 'Ara T Howard', 'ara.t.howard@gmail.com'
  developer 'Tom Copeland', 'tom@infoether.com'

  multiruby_skip << "rubinius"
  extra_deps << ["json_pure",">= 1.1.7"]
  self.rubyforge_name = "codeforpeople"
  self.need_tar       = false
end

task :postrelease => :announce

task :backup do
  Dir.chdir File.expand_path("~/.rubyforge") do
    cp "user-config.yml",  "user-config.yml.bak"
    cp "auto-config.yml",  "auto-config.yml.bak"
  end
end

task :restore do
  Dir.chdir File.expand_path("~/.rubyforge") do
    cp "user-config.yml.bak",  "user-config.yml"
    cp "auto-config.yml.bak",  "auto-config.yml"
  end
end

# vim:syntax=ruby
