# -*- encoding: utf-8 -*-
# stub: rspec-collection_matchers 1.1.3 ruby lib

Gem::Specification.new do |s|
  s.name = "rspec-collection_matchers"
  s.version = "1.1.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Hugo Bara\u{fa}na"]
  s.date = "2016-12-21"
  s.description = "Collection cardinality matchers, extracted from rspec-expectations"
  s.email = ["hugo.barauna@plataformatec.com.br"]
  s.homepage = "https://github.com/rspec/rspec-collection_matchers"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.8"
  s.summary = "rspec-collection_matchers-1.1.3"

  s.installed_by_version = "2.4.8" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rspec-expectations>, [">= 2.99.0.beta1"])
      s.add_development_dependency(%q<bundler>, ["~> 1.3"])
      s.add_development_dependency(%q<activemodel>, [">= 3.0"])
    else
      s.add_dependency(%q<rspec-expectations>, [">= 2.99.0.beta1"])
      s.add_dependency(%q<bundler>, ["~> 1.3"])
      s.add_dependency(%q<activemodel>, [">= 3.0"])
    end
  else
    s.add_dependency(%q<rspec-expectations>, [">= 2.99.0.beta1"])
    s.add_dependency(%q<bundler>, ["~> 1.3"])
    s.add_dependency(%q<activemodel>, [">= 3.0"])
  end
end
