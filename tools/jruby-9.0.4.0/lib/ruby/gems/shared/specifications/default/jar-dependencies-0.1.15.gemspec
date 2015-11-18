# -*- encoding: utf-8 -*-
# stub: jar-dependencies 0.1.15 ruby lib

Gem::Specification.new do |s|
  s.name = "jar-dependencies"
  s.version = "0.1.15"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["christian meier"]
  s.date = "2015-06-22"
  s.description = "manage jar dependencies for gems and keep track which jar was already loaded using maven artifact coordinates. it warns on version conflicts and loads only ONE jar assuming the first one is compatible to the second one otherwise your project needs to lock down the right version by providing a Jars.lock file."
  s.email = ["mkristian@web.de"]
  s.files = ["MIT-LICENSE", "Mavenfile", "Rakefile", "Readme.md", "jar-dependencies.gemspec", "lib/jar-dependencies.rb", "lib/jar_dependencies.rb", "lib/jar_install_post_install_hook.rb", "lib/jar_installer.rb", "lib/jars/classpath.rb", "lib/jars/installer.rb", "lib/jars/jar_pom.rb", "lib/jars/jars_lock_pom.rb", "lib/jars/lock.rb", "lib/jars/maven_exec.rb", "lib/jars/post_install_hook.rb", "lib/jars/setup.rb", "lib/jars/version.rb", "lib/rubygems_plugin.rb"]
  s.homepage = "https://github.com/mkristian/jar-dependencies"
  s.licenses = ["MIT"]
  s.rubygems_version = "2.4.6"
  s.summary = "manage jar dependencies for gems"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<minitest>, ["~> 5.3"])
      s.add_development_dependency(%q<rake>, ["~> 10.2"])
      s.add_development_dependency(%q<ruby-maven>, ["~> 3.3.0"])
      s.add_development_dependency(%q<ruby-maven-libs>, ["~> 3.3.0"])
    else
      s.add_dependency(%q<minitest>, ["~> 5.3"])
      s.add_dependency(%q<rake>, ["~> 10.2"])
      s.add_dependency(%q<ruby-maven>, ["~> 3.3.0"])
      s.add_dependency(%q<ruby-maven-libs>, ["~> 3.3.0"])
    end
  else
    s.add_dependency(%q<minitest>, ["~> 5.3"])
    s.add_dependency(%q<rake>, ["~> 10.2"])
    s.add_dependency(%q<ruby-maven>, ["~> 3.3.0"])
    s.add_dependency(%q<ruby-maven-libs>, ["~> 3.3.0"])
  end
end
