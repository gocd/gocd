# -*- encoding: utf-8 -*-
# stub: thread_safe 0.3.4 java lib

Gem::Specification.new do |s|
  s.name = "thread_safe"
  s.version = "0.3.4"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Charles Oliver Nutter", "thedarkone"]
  s.date = "2014-05-27"
  s.description = "Thread-safe collections and utilities for Ruby"
  s.email = ["headius@headius.com", "thedarkone2@gmail.com"]
  s.homepage = "https://github.com/headius/thread_safe"
  s.licenses = ["Apache-2.0"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "A collection of data structures and utilities to make thread-safe programming in Ruby easier"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<atomic>, ["< 2", ">= 1.1.7"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<minitest>, [">= 4"])
    else
      s.add_dependency(%q<atomic>, ["< 2", ">= 1.1.7"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<minitest>, [">= 4"])
    end
  else
    s.add_dependency(%q<atomic>, ["< 2", ">= 1.1.7"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<minitest>, [">= 4"])
  end
end
