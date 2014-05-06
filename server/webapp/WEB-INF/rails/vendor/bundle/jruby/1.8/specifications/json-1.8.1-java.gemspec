# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "json"
  s.version = "1.8.1"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Daniel Luz"]
  s.date = "2013-10-17"
  s.description = "A JSON implementation as a JRuby extension."
  s.email = "dev+ruby@mernen.com"
  s.homepage = "http://json-jruby.rubyforge.org/"
  s.licenses = ["Ruby"]
  s.require_paths = ["lib"]
  s.rubyforge_project = "json-jruby"
  s.rubygems_version = "1.8.24"
  s.summary = "JSON implementation for JRuby"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
