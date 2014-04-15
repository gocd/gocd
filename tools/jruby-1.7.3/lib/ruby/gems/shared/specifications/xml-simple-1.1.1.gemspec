# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = "xml-simple"
  s.version = "1.1.1"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Maik Schmidt"]
  s.date = "2011-10-11"
  s.email = "contact@maik-schmidt.de"
  s.homepage = "http://xml-simple.rubyforge.org"
  s.require_paths = ["lib"]
  s.rubyforge_project = "xml-simple"
  s.rubygems_version = "1.8.24"
  s.summary = "A simple API for XML processing."

  if s.respond_to? :specification_version then
    s.specification_version = 3

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
    else
    end
  else
  end
end
