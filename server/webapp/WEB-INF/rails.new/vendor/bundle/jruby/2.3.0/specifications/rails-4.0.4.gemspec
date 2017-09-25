# -*- encoding: utf-8 -*-
# stub: rails 4.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "rails".freeze
  s.version = "4.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 1.8.11".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["David Heinemeier Hansson".freeze]
  s.date = "2014-03-14"
  s.description = "Ruby on Rails is a full-stack web framework optimized for programmer happiness and sustainable productivity. It encourages beautiful code by favoring convention over configuration.".freeze
  s.email = "david@loudthinking.com".freeze
  s.homepage = "http://www.rubyonrails.org".freeze
  s.licenses = ["MIT".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Full-stack web application framework.".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<actionpack>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<activerecord>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<actionmailer>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<railties>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<bundler>.freeze, ["< 2.0", ">= 1.3.0"])
      s.add_runtime_dependency(%q<sprockets-rails>.freeze, ["~> 2.0.0"])
    else
      s.add_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<actionpack>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<activerecord>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<actionmailer>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<railties>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<bundler>.freeze, ["< 2.0", ">= 1.3.0"])
      s.add_dependency(%q<sprockets-rails>.freeze, ["~> 2.0.0"])
    end
  else
    s.add_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<actionpack>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<activerecord>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<actionmailer>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<railties>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<bundler>.freeze, ["< 2.0", ">= 1.3.0"])
    s.add_dependency(%q<sprockets-rails>.freeze, ["~> 2.0.0"])
  end
end
