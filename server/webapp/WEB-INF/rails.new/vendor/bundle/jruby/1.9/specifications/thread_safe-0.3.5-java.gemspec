# -*- encoding: utf-8 -*-
# stub: thread_safe 0.3.5 java lib

Gem::Specification.new do |s|
  s.name = "thread_safe"
  s.version = "0.3.5"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Charles Oliver Nutter", "thedarkone"]
  s.date = "2015-03-11"
  s.description = "Thread-safe collections and utilities for Ruby"
  s.email = ["headius@headius.com", "thedarkone2@gmail.com"]
  s.homepage = "https://github.com/ruby-concurrency/thread_safe"
  s.licenses = ["Apache-2.0"]
  s.rubygems_version = "2.4.8"
  s.summary = "A collection of data structures and utilities to make thread-safe programming in Ruby easier"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<atomic>, ["= 1.1.16"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<minitest>, [">= 4"])
    else
      s.add_dependency(%q<atomic>, ["= 1.1.16"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<minitest>, [">= 4"])
    end
  else
    s.add_dependency(%q<atomic>, ["= 1.1.16"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<minitest>, [">= 4"])
  end
end
