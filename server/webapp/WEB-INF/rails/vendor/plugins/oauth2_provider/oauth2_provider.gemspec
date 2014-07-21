# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{oauth2_provider}
  s.version = "0.2.0"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["ThoughtWorks, Inc."]
  s.date = %q{2011-04-26}
  s.description = %q{A Rails plugin to OAuth v2.0 enable your rails application. This plugin implements v09 of the OAuth2 draft spec http://tools.ietf.org/html/draft-ietf-oauth-v2-09.}
  s.email = %q{ketan@thoughtworks.com}
  s.extra_rdoc_files = ["README.textile", "MIT-LICENSE.txt", "NOTICE.textile"]
  s.files = ["{app,config,generators,db,lib}/**/*", "CHANGELOG", "HACKING.textile", "lib", "MIT-LICENSE.txt", "NOTICE.textile", "oauth2_provider.gemspec", "README.textile", "WHAT_IS_OAUTH.textile"]
  s.homepage = %q{http://github.com/ThoughtWorksStudios/oauth2_provider}
  s.require_paths = ["lib"]
  s.rubygems_version = %q{1.3.6}
  s.summary = %q{A Rails plugin to OAuth v2.0 enable your rails application}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 3

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<validatable>, ["= 1.6.7"])
      s.add_development_dependency(%q<saikuro_treemap>, [">= 0"])
      s.add_development_dependency(%q<rcov>, ["= 0.9.8"])
    else
      s.add_dependency(%q<validatable>, ["= 1.6.7"])
      s.add_dependency(%q<saikuro_treemap>, [">= 0"])
      s.add_dependency(%q<rcov>, ["= 0.9.8"])
    end
  else
    s.add_dependency(%q<validatable>, ["= 1.6.7"])
    s.add_dependency(%q<saikuro_treemap>, [">= 0"])
    s.add_dependency(%q<rcov>, ["= 0.9.8"])
  end
end
