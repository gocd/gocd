module WebpackRails
  # :nodoc:
  class InstallGenerator < ::Rails::Generators::Base
    source_root File.expand_path("../../../../example", __FILE__)

    desc "Install everything you need for a basic webpack-rails integration"

    def add_foreman_to_gemfile
      gem 'foreman'
    end

    def copy_procfile
      copy_file "Procfile", "Procfile"
    end

    def copy_package_json
      copy_file "package.json", "package.json"
    end

    def copy_webpack_conf
      copy_file "webpack.config.js", "config/webpack.config.js"
    end

    def create_webpack_application_js
      empty_directory "webpack"
      create_file "webpack/application.js" do
        <<-EOF.strip_heredoc
        console.log("Hello world!");
        EOF
      end
    end

    def add_to_gitignore
      append_to_file ".gitignore" do
        <<-EOF.strip_heredoc
        # Added by webpack-rails
        /node_modules
        /public/webpack
        EOF
      end
    end

    def run_yarn_install
      run "yarn install" if yes?("Would you like us to run 'yarn install' for you?")
    end

    def run_bundle_install
      run "bundle install" if yes?("Would you like us to run 'bundle install' for you?")
    end

    def whats_next
      puts <<-EOF.strip_heredoc

        We've set up the basics of webpack-rails for you, but you'll still
        need to:

          1. Add the 'application' entry point in to your layout, and
          2. Run 'foreman start' to run the webpack-dev-server and rails server

        See the README.md for this gem at
        https://github.com/mipearson/webpack-rails/blob/master/README.md
        for more info.

        Thanks for using webpack-rails!

      EOF
    end
  end
end
