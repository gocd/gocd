module Bundler
  class CLI::Gem
    attr_reader :options, :gem_name, :thor
    def initialize(options, gem_name, thor)
      @options = options
      @gem_name = gem_name
      @thor = thor
    end

    def run
      if options[:ext] && gem_name.index('-')
        Bundler.ui.error "You have specified a gem name which does not conform to the \n" \
                         "naming guidelines for C extensions. For more information, \n" \
                         "see the 'Extension Naming' section at the following URL:\n" \
                         "http://guides.rubygems.org/gems-with-extensions/\n"
        exit 1
      end

      name = gem_name.chomp("/") # remove trailing slash if present
      underscored_name = name.tr('-', '_')
      namespaced_path = name.tr('-', '/')
      target = File.join(Dir.pwd, name)
      constant_name = name.split('_').map{|p| p[0..0].upcase + p[1..-1] }.join
      constant_name = constant_name.split('-').map{|q| q[0..0].upcase + q[1..-1] }.join('::') if constant_name =~ /-/
      constant_array = constant_name.split('::')
      git_user_name = `git config user.name`.chomp
      git_user_email = `git config user.email`.chomp
      opts = {
        :name             => name,
        :underscored_name => underscored_name,
        :namespaced_path  => namespaced_path,
        :constant_name    => constant_name,
        :constant_array   => constant_array,
        :author           => git_user_name.empty? ? "TODO: Write your name" : git_user_name,
        :email            => git_user_email.empty? ? "TODO: Write your email address" : git_user_email,
        :test             => options[:test],
        :ext              => options[:ext]
      }
      gemspec_dest = File.join(target, "#{name}.gemspec")
      thor.template(File.join("newgem/Gemfile.tt"),               File.join(target, "Gemfile"),                             opts)
      thor.template(File.join("newgem/Rakefile.tt"),              File.join(target, "Rakefile"),                            opts)
      thor.template(File.join("newgem/LICENSE.txt.tt"),           File.join(target, "LICENSE.txt"),                         opts)
      thor.template(File.join("newgem/README.md.tt"),             File.join(target, "README.md"),                           opts)
      thor.template(File.join("newgem/gitignore.tt"),             File.join(target, ".gitignore"),                          opts)
      thor.template(File.join("newgem/newgem.gemspec.tt"),        gemspec_dest,                                             opts)
      thor.template(File.join("newgem/lib/newgem.rb.tt"),         File.join(target, "lib/#{namespaced_path}.rb"),           opts)
      thor.template(File.join("newgem/lib/newgem/version.rb.tt"), File.join(target, "lib/#{namespaced_path}/version.rb"),   opts)
      if options[:bin]
        thor.template(File.join("newgem/bin/newgem.tt"),          File.join(target, 'bin', name),                           opts)
      end
      case options[:test]
      when 'rspec'
        thor.template(File.join("newgem/rspec.tt"),               File.join(target, ".rspec"),                              opts)
        thor.template(File.join("newgem/spec/spec_helper.rb.tt"), File.join(target, "spec/spec_helper.rb"),                 opts)
        thor.template(File.join("newgem/spec/newgem_spec.rb.tt"), File.join(target, "spec/#{namespaced_path}_spec.rb"),     opts)
      when 'minitest'
        thor.template(File.join("newgem/test/minitest_helper.rb.tt"), File.join(target, "test/minitest_helper.rb"),         opts)
        thor.template(File.join("newgem/test/test_newgem.rb.tt"),     File.join(target, "test/test_#{namespaced_path}.rb"), opts)
      end
      if options[:test]
        thor.template(File.join("newgem/.travis.yml.tt"),         File.join(target, ".travis.yml"),            opts)
      end
      if options[:ext]
        thor.template(File.join("newgem/ext/newgem/extconf.rb.tt"), File.join(target, "ext/#{name}/extconf.rb"), opts)
        thor.template(File.join("newgem/ext/newgem/newgem.h.tt"), File.join(target, "ext/#{name}/#{underscored_name}.h"), opts)
        thor.template(File.join("newgem/ext/newgem/newgem.c.tt"), File.join(target, "ext/#{name}/#{underscored_name}.c"), opts)
      end
      Bundler.ui.info "Initializing git repo in #{target}"
      Dir.chdir(target) { `git init`; `git add .` }

      if options[:edit]
        thor.run("#{options["edit"]} \"#{gemspec_dest}\"")  # Open gemspec in editor
      end
    end

  end
end
