# -*- encoding: utf-8 -*-
# stub: hike 1.2.3 ruby lib

Gem::Specification.new do |s|
  s.name = "hike"
  s.version = "1.2.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Sam Stephenson"]
  s.date = "2013-06-04"
  s.description = "A Ruby library for finding files in a set of paths."
  s.email = ["sstephenson@gmail.com"]
  s.homepage = "http://github.com/sstephenson/hike"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "Find files in a set of paths"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rake>, [">= 0"])
    else
      s.add_dependency(%q<rake>, [">= 0"])
    end
  else
    s.add_dependency(%q<rake>, [">= 0"])
  end
end
