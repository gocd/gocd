# -*- encoding: utf-8 -*-
# stub: atoulme-Antwrap 0.7.5 java lib

Gem::Specification.new do |s|
  s.name = "atoulme-Antwrap"
  s.version = "0.7.5"
  s.platform = "java"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Caleb Powell"]
  s.date = "2014-11-21"
  s.description = "\tA Ruby module that wraps the Apache Ant build tool. Antwrap can be used to invoke Ant Tasks from a Ruby or a JRuby script.\n\n== FEATURES/PROBLEMS:\n\n\tAntwrap runs on the native Ruby interpreter via the RJB (Ruby Java Bridge gem) and on the JRuby interpreter. Antwrap is compatible with Ant versions 1.5.4, \n\t1.6.5 and 1.7.0. For more information, \tsee the Project Info (http://rubyforge.org/projects/antwrap/) page. \n\t \n== SYNOPSIS:\n\n\tAntwrap is a Ruby library that can be used to invoke Ant tasks. It is being used in the Buildr (http://incubator.apache.org/buildr/) project to execute \n\tAnt (http://ant.apache.org/) tasks in a Java project. If you are tired of fighting with Ant or Maven XML files in your Java project, take some time to \n\tcheck out Buildr!"
  s.email = "caleb.powell@gmail.com"
  s.extra_rdoc_files = ["History.txt", "Manifest.txt", "README.txt"]
  s.files = ["History.txt", "Manifest.txt", "README.txt"]
  s.homepage = "http://rubyforge.org/projects/antwrap/"
  s.licenses = ["MIT"]
  s.rdoc_options = ["--main", "README.txt"]
  s.require_paths = ["lib"]
  s.rubygems_version = "2.1.9"
  s.summary = "A Ruby module that wraps the Apache Ant build tool. Antwrap can be used to invoke Ant Tasks from a Ruby or a JRuby script."

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_development_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_development_dependency(%q<hoe>, ["~> 3.13"])
    else
      s.add_dependency(%q<rdoc>, ["~> 4.0"])
      s.add_dependency(%q<hoe>, ["~> 3.13"])
    end
  else
    s.add_dependency(%q<rdoc>, ["~> 4.0"])
    s.add_dependency(%q<hoe>, ["~> 3.13"])
  end
end
