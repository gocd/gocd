require 'maven/ruby/maven'
require 'rake'

warn 'deprecated: maven rake tasks is enf of life'

module Maven
  class Tasks
    include Rake::DSL

    def install

      desc "Setup Maven instance."
      task :maven do
      end

      desc "Clean up the build directory."
      task :clean => :maven do
        maven.clean
      end

      desc "Run the java unit tests from src/test/java directory."
      task :junit => :maven do
        maven.exec( 'compile', 'resources:testResources', 'compiler:testCompile', 'surefire:test' )
      end

      desc "Build gem into the pkg directory."
      task :build => :maven do
        maven.package( '-Dmaven.test.skip' )
      end

      desc "Compile any java source configured - default java files are in src/main/java."
      task :compile => :maven do
        maven.compile
      end

      desc "Package jar-file with the compiled classes - default jar-file lib/{name}.jar"
      task :jar => :maven do
        maven.prepare_package( '-Dmaven.test.skip' )
      end

      desc "Push gem to rubygems.org"
      task :push => :maven do
        maven.deploy( '-Dmaven.test.skip' )
      end
    end
  end
  Tasks.new.install
end

def maven
  unless @__maven__
    @__maven__ = Maven::Ruby::Maven.new
    @__maven__.embedded = true
  end
  @__maven__
end
