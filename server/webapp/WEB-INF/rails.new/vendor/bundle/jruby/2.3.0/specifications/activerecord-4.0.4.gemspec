# -*- encoding: utf-8 -*-
# stub: activerecord 4.0.4 ruby lib

Gem::Specification.new do |s|
  s.name = "activerecord".freeze
  s.version = "4.0.4"

  s.required_rubygems_version = Gem::Requirement.new(">= 0".freeze) if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib".freeze]
  s.authors = ["David Heinemeier Hansson".freeze]
  s.date = "2014-03-14"
  s.description = "Databases on Rails. Build a persistent domain model by mapping database tables to Ruby classes. Strong conventions for associations, validations, aggregations, migrations, and testing come baked-in.".freeze
  s.email = "david@loudthinking.com".freeze
  s.extra_rdoc_files = ["README.rdoc".freeze]
  s.files = ["README.rdoc".freeze]
  s.homepage = "http://www.rubyonrails.org".freeze
  s.licenses = ["MIT".freeze]
  s.rdoc_options = ["--main".freeze, "README.rdoc".freeze]
  s.required_ruby_version = Gem::Requirement.new(">= 1.9.3".freeze)
  s.rubygems_version = "2.6.13".freeze
  s.summary = "Object-relational mapper framework (part of Rails).".freeze

  s.installed_by_version = "2.6.13" if s.respond_to? :installed_by_version

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<activemodel>.freeze, ["= 4.0.4"])
      s.add_runtime_dependency(%q<arel>.freeze, ["~> 4.0.0"])
      s.add_runtime_dependency(%q<activerecord-deprecated_finders>.freeze, ["~> 1.0.2"])
    else
      s.add_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<activemodel>.freeze, ["= 4.0.4"])
      s.add_dependency(%q<arel>.freeze, ["~> 4.0.0"])
      s.add_dependency(%q<activerecord-deprecated_finders>.freeze, ["~> 1.0.2"])
    end
  else
    s.add_dependency(%q<activesupport>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<activemodel>.freeze, ["= 4.0.4"])
    s.add_dependency(%q<arel>.freeze, ["~> 4.0.0"])
    s.add_dependency(%q<activerecord-deprecated_finders>.freeze, ["~> 1.0.2"])
  end
end
