# -*- encoding: utf-8 -*-
# stub: ruby-debug-ide 0.6.0 ruby lib
# stub: ext/mkrf_conf.rb

Gem::Specification.new do |s|
  s.name = "ruby-debug-ide"
  s.version = "0.6.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Markus Barchfeld, Martin Krauskopf, Mark Moseley, JetBrains RubyMine Team"]
  s.date = "2015-08-18"
  s.description = "An interface which glues ruby-debug to IDEs like Eclipse (RDT), NetBeans and RubyMine.\n"
  s.email = "rubymine-feedback@jetbrains.com"
  s.executables = ["rdebug-ide"]
  s.extensions = ["ext/mkrf_conf.rb"]
  s.files = ["bin/rdebug-ide", "ext/mkrf_conf.rb"]
  s.homepage = "https://github.com/ruby-debug/ruby-debug-ide"
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.2")
  s.rubyforge_project = "debug-commons"
  s.rubygems_version = "2.4.8"
  s.summary = "IDE interface for ruby-debug."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rake>, [">= 0.8.1"])
    else
      s.add_dependency(%q<rake>, [">= 0.8.1"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0.8.1"])
  end
end
