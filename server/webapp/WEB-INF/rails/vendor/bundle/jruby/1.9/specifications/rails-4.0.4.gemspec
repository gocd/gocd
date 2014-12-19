# -*- encoding: utf-8 -*-
# stub: rails 4.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "rails"
  s.version = "4.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.8.11") if s.respond_to? :required_rubygems_version=
  s.authors = ["David Heinemeier Hansson"]
  s.date = "2014-03-14"
  s.description = "Ruby on Rails is a full-stack web framework optimized for programmer happiness and sustainable productivity. It encourages beautiful code by favoring convention over configuration."
  s.email = "david@loudthinking.com"
  s.homepage = "http://www.rubyonrails.org"
  s.licenses = ["MIT"]
  s.require_paths = ["lib"]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3")
  s.rubygems_version = "2.1.9"
  s.summary = "Full-stack web application framework."

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<actionpack>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<activerecord>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<actionmailer>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<railties>, ["= 4.0.4"])
      s.add_runtime_dependency(%q<bundler>, ["< 2.0", ">= 1.3.0"])
      s.add_runtime_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
    else
      s.add_dependency(%q<activesupport>, ["= 4.0.4"])
      s.add_dependency(%q<actionpack>, ["= 4.0.4"])
      s.add_dependency(%q<activerecord>, ["= 4.0.4"])
      s.add_dependency(%q<actionmailer>, ["= 4.0.4"])
      s.add_dependency(%q<railties>, ["= 4.0.4"])
      s.add_dependency(%q<bundler>, ["< 2.0", ">= 1.3.0"])
      s.add_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
    end
  else
    s.add_dependency(%q<activesupport>, ["= 4.0.4"])
    s.add_dependency(%q<actionpack>, ["= 4.0.4"])
    s.add_dependency(%q<activerecord>, ["= 4.0.4"])
    s.add_dependency(%q<actionmailer>, ["= 4.0.4"])
    s.add_dependency(%q<railties>, ["= 4.0.4"])
    s.add_dependency(%q<bundler>, ["< 2.0", ">= 1.3.0"])
    s.add_dependency(%q<sprockets-rails>, ["~> 2.0.0"])
  end
end
