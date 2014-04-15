= Hoe

* http://rubyforge.org/projects/seattlerb/
* http://seattlerb.rubyforge.org/hoe/
* mailto:ryand-ruby@zenspider.com

== DESCRIPTION:

Hoe is a simple rake/rubygems helper for project Rakefiles. It
generates all the usual tasks for projects including rdoc generation,
testing, packaging, and deployment.

Tasks Provided:

* announce          - Create news email file and post to rubyforge.
* audit             - Run ZenTest against the package.
* check_extra_deps  - Install missing dependencies.
* check_manifest    - Verify the manifest.
* clean             - Clean up all the extras.
* config_hoe        - Create a fresh ~/.hoerc file.
* debug_gem         - Show information about the gem.
* default           - Run the default task(s).
* deps:email        - Print a contact list for gems dependent on this gem
* deps:fetch        - Fetch all the dependent gems of this gem into tarballs
* deps:list         - List all the dependent gems of this gem
* docs              - Build the docs HTML Files
* email             - Generate email announcement file.
* flay              - Analyze for code duplication.
* flog              - Analyze code complexity.
* gem               - Build the gem file hoe-1.9.0.gem
* generate_key      - Generate a key for signing your gems.
* install_gem       - Install the package as a gem.
* multi             - Run the test suite using multiruby.
* package           - Build all the packages
* post_blog         - Post announcement to blog.
* post_news         - Post announcement to rubyforge.
* publish_docs      - Publish RDoc to RubyForge.
* rcov              - Analyze code coverage with tests
* release           - Package and upload the release to rubyforge.
* ridocs            - Generate ri locally for testing.
* tasks             - Generate a list of tasks for doco.
* test              - Run the test suite.
* test_deps         - Show which test files fail when run alone.

See class rdoc for help. Hint: ri Hoe

== FEATURES/PROBLEMS:

* Provides 'sow' for quick project directory creation.
* Make making and maintaining Rakefiles fun and easy.

== SYNOPSIS:

  % sow [group] project

or

  require 'hoe'
  
  Hoe.new(projectname, version) do |p|
    # ... project specific data ...
  end

  # ... project specific tasks ...

== REQUIREMENTS:

* rake
* rubyforge
* rubygems

== INSTALL:

* sudo gem install hoe

== LICENSE:

(The MIT License)

Copyright (c) Ryan Davis, Zen Spider Software

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
