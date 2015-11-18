# -*- encoding: utf-8 -*-
# stub: ruby-maven 3.3.3 ruby lib

Gem::Specification.new do |s|
  s.name = "ruby-maven"
  s.version = "3.3.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Christian Meier"]
  s.date = "2015-06-23"
  s.description = "maven support for ruby DSL pom files. MRI needs java/javac installed."
  s.email = ["m.kristian@web.de"]
  s.executables = ["rmvn"]
  s.files = [".gitignore", ".mvn/extensions.xml", "Mavenfile", "bin/rmvn", "lib/maven/ruby.rb", "lib/maven/ruby/maven.rb", "lib/maven/ruby/tasks.rb", "lib/maven/ruby/version.rb", "lib/ruby-maven.rb", "lib/ruby_maven.rb", "ruby-maven.gemspec", "spec/ruby_maven_spec.rb", "spec/setup.rb"]
  s.homepage = "https://github.com/takari/ruby-maven"
  s.licenses = ["EPL"]
  s.rdoc_options = ["--main", "README.md"]
  s.rubygems_version = "2.4.6"
  s.summary = "maven support for ruby projects"

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<ruby-maven-libs>, ["~> 3.3.1"])
      s.add_development_dependency(%q<minitest>, ["~> 5.3"])
      s.add_development_dependency(%q<rake>, ["~> 10.3"])
    else
      s.add_dependency(%q<ruby-maven-libs>, ["~> 3.3.1"])
      s.add_dependency(%q<minitest>, ["~> 5.3"])
      s.add_dependency(%q<rake>, ["~> 10.3"])
    end
  else
    s.add_dependency(%q<ruby-maven-libs>, ["~> 3.3.1"])
    s.add_dependency(%q<minitest>, ["~> 5.3"])
    s.add_dependency(%q<rake>, ["~> 10.3"])
  end
end
