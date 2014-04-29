# -*- encoding: utf-8 -*-
# stub: bundler 1.6.2 ruby lib

Gem::Specification.new do |s|
  s.name = "bundler"
  s.version = "1.6.2"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.3.6") if s.respond_to? :required_rubygems_version=
  s.authors = ["Andr\u{e9} Arko", "Terence Lee", "Carl Lerche", "Yehuda Katz"]
  s.date = "2014-04-14"
  s.description = "Bundler manages an application's dependencies through its entire life, across many machines, systematically and repeatably"
  s.email = ["andre@arko.net"]
  s.executables = ["bundle", "bundler"]
  s.files = ["bin/bundle", "bin/bundler"]
  s.homepage = "http://bundler.io"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.8.7")
  s.rubygems_version = "2.1.9"
  s.summary = "The best way to manage your application's dependencies"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<ronn>, ["~> 0.7.3"])
      s.add_development_dependency(%q<rspec>, ["~> 2.99.0.beta1"])
    else
      s.add_dependency(%q<ronn>, ["~> 0.7.3"])
      s.add_dependency(%q<rspec>, ["~> 2.99.0.beta1"])
    end
  else
    s.add_dependency(%q<ronn>, ["~> 0.7.3"])
    s.add_dependency(%q<rspec>, ["~> 2.99.0.beta1"])
  end
end
