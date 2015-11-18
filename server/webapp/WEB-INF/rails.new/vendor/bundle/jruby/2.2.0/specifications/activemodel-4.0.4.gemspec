# -*- encoding: utf-8 -*-
# stub: activemodel 4.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "activemodel"
  s.version = "4.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2014-03-14"
  s.description = "A toolkit for building modeling frameworks like Active Record. Rich support for attributes, callbacks, validations, observers, serialization, internationalization, and testing."
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.licenses = ["MIT"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.4.8"
  s.summary = "A toolkit for building modeling frameworks (part of Rails)."

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<builder>, ["~> 3.1.0"])
    else
      s.add_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_dependency(%q<builder>, ["~> 3.1.0"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 4.0.4"])
    s.add_dependency(%q<builder>, ["~> 3.1.0"])
  end
end
