# -*- encoding: utf-8 -*-
# stub: activemodel 4.1.1 ruby lib

Gem::Specification.new do |s|
  s.name = "activemodel"
  s.version = "4.1.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2014-05-06"
  s.description = "A toolkit for building modeling frameworks like Active Record. Rich support for attributes, callbacks, validations, serialization, internationalization, and testing."
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.1.9"
  s.summary = "A toolkit for building modeling frameworks (part of Rails)."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 4.1.1"])
      s.add_runtime_dependency(%q<builder>, ["~> 3.1"])
    else
      s.add_dependency(%q<activesupport>, ["= 4.1.1"])
      s.add_dependency(%q<builder>, ["~> 3.1"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 4.1.1"])
    s.add_dependency(%q<builder>, ["~> 3.1"])
  end
end
