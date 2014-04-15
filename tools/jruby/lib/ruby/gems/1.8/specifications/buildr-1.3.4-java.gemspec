# -*- encoding: utf-8 -*-

Gem::Specification.new do |s|
  s.name = %q{buildr}
  s.version = "1.3.4"
  s.platform = %q{java}

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.authors = ["Apache Buildr"]
  s.date = %q{2009-04-21}
  s.default_executable = %q{buildr}
  s.description = %q{Apache Buildr is a build system for Java-based applications, including support for Scala, Groovy and a growing number of JVM languages and tools.  We wanted something that's simple and intuitive to use, so we only need to tell it what to do, and it takes care of the rest.  But also something we can easily extend for those one-off tasks, with a language that's a joy to use.}
  s.email = %q{users@buildr.apache.org}
  s.executables = ["buildr"]
  s.extra_rdoc_files = ["README.rdoc", "CHANGELOG", "LICENSE", "NOTICE"]
  s.files = ["addon/buildr", "addon/buildr/jetty.rb", "addon/buildr/jdepend.rb", "addon/buildr/org", "addon/buildr/org/apache", "addon/buildr/org/apache/buildr", "addon/buildr/org/apache/buildr/BuildrNail.java", "addon/buildr/org/apache/buildr/JettyWrapper.class", "addon/buildr/org/apache/buildr/BuildrNail.class", "addon/buildr/org/apache/buildr/BuildrNail$Main.class", "addon/buildr/org/apache/buildr/JettyWrapper.java", "addon/buildr/org/apache/buildr/JettyWrapper$1.class", "addon/buildr/org/apache/buildr/JettyWrapper$BuildrHandler.class", "addon/buildr/antlr.rb", "addon/buildr/nailgun.rb", "addon/buildr/drb.rb", "addon/buildr/javacc.rb", "addon/buildr/openjpa.rb", "addon/buildr/jibx.rb", "addon/buildr/xmlbeans.rb", "addon/buildr/cobertura.rb", "addon/buildr/hibernate.rb", "addon/buildr/emma.rb", "bin/buildr", "doc/packaging.textile", "doc/css", "doc/css/print.css", "doc/css/syntax.css", "doc/css/default.css", "doc/testing.textile", "doc/settings_profiles.textile", "doc/images", "doc/images/favicon.png", "doc/images/asf-logo.gif", "doc/images/buildr.png", "doc/images/growl-icon.tiff", "doc/images/project-structure.png", "doc/images/tip.png", "doc/images/note.png", "doc/images/zbuildr.tif", "doc/images/buildr-hires.png", "doc/preface.textile", "doc/projects.textile", "doc/getting_started.textile", "doc/scripts", "doc/scripts/install-linux.sh", "doc/scripts/install-jruby.sh", "doc/scripts/gitflow.rb", "doc/scripts/install-osx.sh", "doc/scripts/buildr-git.rb", "doc/building.textile", "doc/index.textile", "doc/pages", "doc/artifacts.textile", "doc/more_stuff.textile", "doc/_layouts", "doc/_layouts/preface.html", "doc/_layouts/default.html", "doc/extending.textile", "doc/download.textile", "doc/contributing.textile", "doc/languages.textile", "doc/mailing_lists.textile", "etc/KEYS", "lib/buildr", "lib/buildr/java", "lib/buildr/java/org", "lib/buildr/java/org/apache", "lib/buildr/java/org/apache/buildr", "lib/buildr/java/org/apache/buildr/JavaTestFilter.java", "lib/buildr/java/org/apache/buildr/JavaTestFilter.class", "lib/buildr/java/packaging.rb", "lib/buildr/java/ant.rb", "lib/buildr/java/commands.rb", "lib/buildr/java/pom.rb", "lib/buildr/java/jtestr_runner.rb.erb", "lib/buildr/java/compiler.rb", "lib/buildr/java/test_result.rb", "lib/buildr/java/rjb.rb", "lib/buildr/java/cobertura.rb", "lib/buildr/java/version_requirement.rb", "lib/buildr/java/jruby.rb", "lib/buildr/java/tests.rb", "lib/buildr/java/emma.rb", "lib/buildr/java/bdd.rb", "lib/buildr/java/deprecated.rb", "lib/buildr/groovy.rb", "lib/buildr/packaging.rb", "lib/buildr/scala.rb", "lib/buildr/core.rb", "lib/buildr/scala", "lib/buildr/scala/compiler.rb", "lib/buildr/scala/tests.rb", "lib/buildr/scala/bdd.rb", "lib/buildr/groovy", "lib/buildr/groovy/compiler.rb", "lib/buildr/groovy/bdd.rb", "lib/buildr/tasks", "lib/buildr/ide.rb", "lib/buildr/java.rb", "lib/buildr/core", "lib/buildr/core/checks.rb", "lib/buildr/core/transports.rb", "lib/buildr/core/environment.rb", "lib/buildr/core/help.rb", "lib/buildr/core/progressbar.rb", "lib/buildr/core/compile.rb", "lib/buildr/core/generate.rb", "lib/buildr/core/util.rb", "lib/buildr/core/build.rb", "lib/buildr/core/osx.rb", "lib/buildr/core/filter.rb", "lib/buildr/core/project.rb", "lib/buildr/core/application.rb", "lib/buildr/core/test.rb", "lib/buildr/core/common.rb", "lib/buildr/resources", "lib/buildr/resources/buildr.icns", "lib/buildr/ide", "lib/buildr/ide/idea.rb", "lib/buildr/ide/idea7x.rb", "lib/buildr/ide/idea.ipr.template", "lib/buildr/ide/idea7x.ipr.template", "lib/buildr/ide/eclipse.rb", "lib/buildr/packaging", "lib/buildr/packaging/ziptask.rb", "lib/buildr/packaging/gems.rb", "lib/buildr/packaging/zip.rb", "lib/buildr/packaging/artifact.rb", "lib/buildr/packaging/version_requirement.rb", "lib/buildr/packaging/artifact_search.rb", "lib/buildr/packaging/artifact_namespace.rb", "lib/buildr/packaging/tar.rb", "lib/buildr/packaging/archive.rb", "lib/buildr/packaging/package.rb", "lib/buildr.rb", "rakelib/package.rake", "rakelib/jekylltask.rb", "rakelib/stage.rake", "rakelib/rspec.rake", "rakelib/checks.rake", "rakelib/setup.rake", "rakelib/release.rake", "rakelib/doc.rake", "spec/java", "spec/java/java_spec.rb", "spec/java/ant.rb", "spec/java/compiler_spec.rb", "spec/java/emma_spec.rb", "spec/java/packaging_spec.rb", "spec/java/bdd_spec.rb", "spec/java/cobertura_spec.rb", "spec/java/test_coverage_spec.rb", "spec/java/tests_spec.rb", "spec/version_requirement_spec.rb", "spec/addon", "spec/addon/drb_spec.rb", "spec/scala", "spec/scala/scala.rb", "spec/scala/compiler_spec.rb", "spec/scala/bdd_spec.rb", "spec/scala/tests_spec.rb", "spec/groovy", "spec/groovy/compiler_spec.rb", "spec/groovy/bdd_spec.rb", "spec/spec_helpers.rb", "spec/core", "spec/core/test_spec.rb", "spec/core/checks_spec.rb", "spec/core/util_spec.rb", "spec/core/build_spec.rb", "spec/core/common_spec.rb", "spec/core/transport_spec.rb", "spec/core/compile_spec.rb", "spec/core/project_spec.rb", "spec/core/application_spec.rb", "spec/core/generate_spec.rb", "spec/sandbox.rb", "spec/ide", "spec/ide/eclipse_spec.rb", "spec/ide/idea7x_spec.rb", "spec/packaging", "spec/packaging/artifact_namespace_spec.rb", "spec/packaging/packaging_helper.rb", "spec/packaging/packaging_spec.rb", "spec/packaging/archive_spec.rb", "spec/packaging/artifact_spec.rb", "buildr.gemspec", "buildr.buildfile", "LICENSE", "NOTICE", "CHANGELOG", "README.rdoc", "Rakefile", "_buildr", "_jbuildr"]
  s.homepage = %q{http://buildr.apache.org/}
  s.post_install_message = %q{To get started run buildr --help}
  s.rdoc_options = ["--title", "Buildr", "--main", "README.rdoc", "--webcvs", "http://svn.apache.org/repos/asf/buildr/trunk/"]
  s.require_paths = ["lib", "addon"]
  s.rubyforge_project = %q{buildr}
  s.rubygems_version = %q{1.3.3}
  s.summary = %q{A build system that doesn't suck}

  if s.respond_to? :specification_version then
    current_version = Gem::Specification::CURRENT_SPECIFICATION_VERSION
    s.specification_version = 2

    if Gem::Version.new(Gem::RubyGemsVersion) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<rake>, ["= 0.8.4"])
      s.add_runtime_dependency(%q<builder>, ["= 2.1.2"])
      s.add_runtime_dependency(%q<net-ssh>, ["= 2.0.11"])
      s.add_runtime_dependency(%q<net-sftp>, ["= 2.0.2"])
      s.add_runtime_dependency(%q<rubyzip>, ["= 0.9.1"])
      s.add_runtime_dependency(%q<highline>, ["= 1.5.0"])
      s.add_runtime_dependency(%q<rubyforge>, ["= 1.0.3"])
      s.add_runtime_dependency(%q<hoe>, ["= 1.11.0"])
      s.add_runtime_dependency(%q<Antwrap>, ["= 0.7.0"])
      s.add_runtime_dependency(%q<rspec>, ["= 1.2.2"])
      s.add_runtime_dependency(%q<xml-simple>, ["= 1.0.12"])
      s.add_runtime_dependency(%q<archive-tar-minitar>, ["= 0.5.2"])
      s.add_runtime_dependency(%q<jruby-openssl>, ["= 0.3"])
    else
      s.add_dependency(%q<rake>, ["= 0.8.4"])
      s.add_dependency(%q<builder>, ["= 2.1.2"])
      s.add_dependency(%q<net-ssh>, ["= 2.0.11"])
      s.add_dependency(%q<net-sftp>, ["= 2.0.2"])
      s.add_dependency(%q<rubyzip>, ["= 0.9.1"])
      s.add_dependency(%q<highline>, ["= 1.5.0"])
      s.add_dependency(%q<rubyforge>, ["= 1.0.3"])
      s.add_dependency(%q<hoe>, ["= 1.11.0"])
      s.add_dependency(%q<Antwrap>, ["= 0.7.0"])
      s.add_dependency(%q<rspec>, ["= 1.2.2"])
      s.add_dependency(%q<xml-simple>, ["= 1.0.12"])
      s.add_dependency(%q<archive-tar-minitar>, ["= 0.5.2"])
      s.add_dependency(%q<jruby-openssl>, ["= 0.3"])
    end
  else
    s.add_dependency(%q<rake>, ["= 0.8.4"])
    s.add_dependency(%q<builder>, ["= 2.1.2"])
    s.add_dependency(%q<net-ssh>, ["= 2.0.11"])
    s.add_dependency(%q<net-sftp>, ["= 2.0.2"])
    s.add_dependency(%q<rubyzip>, ["= 0.9.1"])
    s.add_dependency(%q<highline>, ["= 1.5.0"])
    s.add_dependency(%q<rubyforge>, ["= 1.0.3"])
    s.add_dependency(%q<hoe>, ["= 1.11.0"])
    s.add_dependency(%q<Antwrap>, ["= 0.7.0"])
    s.add_dependency(%q<rspec>, ["= 1.2.2"])
    s.add_dependency(%q<xml-simple>, ["= 1.0.12"])
    s.add_dependency(%q<archive-tar-minitar>, ["= 0.5.2"])
    s.add_dependency(%q<jruby-openssl>, ["= 0.3"])
  end
end
