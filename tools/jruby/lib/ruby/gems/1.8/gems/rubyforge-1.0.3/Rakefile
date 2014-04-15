# -*- ruby -*-

$:.unshift 'lib'
require 'rubyforge'

begin
  require 'hoe'
rescue LoadError
  abort "ERROR: This Rakefile is only useful with hoe installed.
       If you're trying to install the rubyforge library,
       please install it via rubygems."
end

abort "you _must_ install this gem to release it" if
  ENV['VERSION'] && ENV['VERSION'] != RubyForge::VERSION

Hoe.new("rubyforge", RubyForge::VERSION) do |rubyforge|
  rubyforge.rubyforge_name = "codeforpeople"
  rubyforge.need_tar = false

  rubyforge.developer('Ryan Davis', 'ryand-ruby@zenspider.com')
  rubyforge.developer('Eric Hodel', 'drbrain@segment7.net')
  rubyforge.developer('Ara T Howard', 'ara.t.howard@gmail.com')

  rubyforge.multiruby_skip << "rubinius"
end

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
