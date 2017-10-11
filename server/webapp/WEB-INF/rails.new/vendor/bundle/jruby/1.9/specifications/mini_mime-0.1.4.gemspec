# -*- encoding: utf-8 -*-
# stub: mini_mime 0.1.4 ruby lib

Gem::Specification.new do |s|
  s.name = "mini_mime"
  s.version = "0.1.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Sam Saffron"]
  s.bindir = "exe"
  s.date = "2017-08-11"
  s.description = "A lightweight mime type lookup toy"
  s.email = ["sam.saffron@gmail.com"]
  s.homepage = "https://github.com/discourse/mini_mime"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "A lightweight mime type lookup toy"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<bundler>, ["~> 1.13"])
      s.add_development_dependency(%q<rake>, ["~> 10.0"])
      s.add_development_dependency(%q<minitest>, ["~> 5.0"])
    else
      s.add_dependency(%q<bundler>, ["~> 1.13"])
      s.add_dependency(%q<rake>, ["~> 10.0"])
      s.add_dependency(%q<minitest>, ["~> 5.0"])
    end
  else
    s.add_dependency(%q<bundler>, ["~> 1.13"])
    s.add_dependency(%q<rake>, ["~> 10.0"])
    s.add_dependency(%q<minitest>, ["~> 5.0"])
  end
end
