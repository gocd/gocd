require 'spec_helper'
require 'net/http'
require 'yaml'

if rails_available?
  describe 'A Rails app' do
    def bundle_install
      tries_remaining = 3
      while tries_remaining > 0
        puts `NOKOGIRI_USE_SYSTEM_LIBRARIES=true bundle install --path vendor;`
        if $?.success?
          tries_remaining = 0
        else
          tries_remaining -= 1
          puts "\n\nBundle failed, trying #{tries_remaining} more times\n\n"
        end
      end
    end

    before :all do
      temp_dir_before
      Dir::chdir @tmp

      `rails new rails-example --skip-bundle  --skip-active-record`
      Dir::chdir File.join(@tmp, 'rails-example')

      base = File.absolute_path(File.join(__FILE__, '../..'))

      # execjs v2.2.0 is broken in rbx, locking the version to 2.0.2 for now
      # see https://github.com/sstephenson/execjs/issues/148

      open('Gemfile', 'a') { |f|
        f.puts "gem 'jasmine', :path => '#{base}'"
        f.puts "gem 'jasmine-core', :github => 'pivotal/jasmine'"
        f.puts "gem 'thin'" unless RUBY_PLATFORM == 'java'
        f.puts "gem 'angularjs-rails'"
        f.puts "gem 'execjs', '2.0.2'"
        f.flush
      }

      Bundler.with_clean_env do
        bundle_install
        `bundle exec rails g jasmine:install`
        File.exists?('spec/javascripts/helpers/.gitkeep').should == true
        File.exists?('spec/javascripts/support/jasmine.yml').should == true
        `bundle exec rails g jasmine:examples`
        File.exists?('app/assets/javascripts/jasmine_examples/Player.js').should == true
        File.exists?('app/assets/javascripts/jasmine_examples/Song.js').should == true
        File.exists?('spec/javascripts/jasmine_examples/PlayerSpec.js').should == true
        File.exists?('spec/javascripts/helpers/jasmine_examples/SpecHelper.js').should == true
      end
    end

    after :all do
      temp_dir_after
    end

    it 'should have the jasmine & jasmine:ci rake task' do
      #See https://github.com/jimweirich/rake/issues/220 and https://github.com/jruby/activerecord-jdbc-adapter/pull/467
      #There's a workaround, but requires setting env vars & jruby opts (non-trivial when inside of a jruby process), so skip for now.
      Bundler.with_clean_env do
        output = `bundle exec rake -T`
        output.should include('jasmine ')
        output.should include('jasmine:ci')
      end
    end

    it "rake jasmine:ci runs and returns expected results" do
      Bundler.with_clean_env do
        output = `bundle exec rake jasmine:ci`
        output.should include('5 specs, 0 failures')
      end
    end

    it "rake jasmine:ci returns proper exit code when specs fail" do
      Bundler.with_clean_env do
        FileUtils.cp(File.join(@root, 'spec', 'fixture', 'failing_test.js'), File.join('spec', 'javascripts'))
        failing_yaml = custom_jasmine_config('failing') do |jasmine_config|
          jasmine_config['spec_files'] << 'failing_test.js'
        end
        output = `bundle exec rake jasmine:ci JASMINE_CONFIG_PATH=#{failing_yaml}`
        $?.should_not be_success
        output.should include('6 specs, 1 failure')
      end
    end

    it "rake jasmine:ci runs specs when an error occurs in the javascript" do
      Bundler.with_clean_env do
        FileUtils.cp(File.join(@root, 'spec', 'fixture', 'exception_test.js'), File.join('spec', 'javascripts'))
        exception_yaml = custom_jasmine_config('exception') do |jasmine_config|
          jasmine_config['spec_files'] << 'exception_test.js'
        end
        output = `bundle exec rake jasmine:ci JASMINE_CONFIG_PATH=#{exception_yaml}`
        $?.should be_success
        output.should include('5 specs, 0 failures')
      end
    end

    it "runs specs written in coffeescript" do
      coffee_yaml = custom_jasmine_config('coffee') do |jasmine_config|
        jasmine_config['spec_files'] << 'coffee_spec.coffee'
      end
      FileUtils.cp(File.join(@root, 'spec', 'fixture', 'coffee_spec.coffee'), File.join('spec', 'javascripts'))

      Bundler.with_clean_env do
        output = `bundle exec rake jasmine:ci JASMINE_CONFIG_PATH=#{coffee_yaml}`
        output.should include('6 specs, 0 failures')
      end
    end

    it "rake jasmine runs and serves the expected webpage when using asset pipeline" do
      open('app/assets/stylesheets/foo.css', 'w') { |f|
        f.puts "/* hi dere */"
        f.flush
      }

      open('spec/javascripts/helpers/angular_helper.js', 'w') { |f|
        f.puts "//= require angular-mocks"
        f.flush
      }

      css_yaml = custom_jasmine_config('css') do |jasmine_config|
        jasmine_config['src_files'] = %w[assets/application.js http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js]
        jasmine_config['stylesheets'] = ['assets/application.css']
      end

      run_jasmine_server("JASMINE_CONFIG_PATH=#{css_yaml}") do
        output = Net::HTTP.get(URI.parse('http://localhost:8888/'))
        output.should match(%r{script src.*/assets/jasmine_examples/Player\.js})
        output.should match(%r{script src=['"]http://ajax\.googleapis\.com/ajax/libs/jquery/1\.11\.0/jquery\.min\.js})
        output.should match(%r{script src.*/assets/jasmine_examples/Song\.js})
        output.should match(%r{script src.*angular_helper\.js})
        output.should match(%r{<link rel=.stylesheet.*?href=./assets/foo\.css\?.*?>})

        output = Net::HTTP.get(URI.parse('http://localhost:8888/__spec__/helpers/angular_helper.js'))
        output.should match(/angular\.mock/)
      end
    end

    it "sets assets_prefix when using sprockets" do
      open('app/assets/stylesheets/assets_prefix.js.erb', 'w') { |f|
        f.puts "<%= assets_prefix %>"
        f.flush
      }

      run_jasmine_server do
        output = Net::HTTP.get(URI.parse('http://localhost:8888/assets/assets_prefix.js'))
        output.should match("/assets")
      end
    end

    it "should load js files outside of the assets path too" do
      yaml = custom_jasmine_config('public-assets') do |jasmine_config|
        jasmine_config['src_files'] << 'public/javascripts/**/*.js'
        jasmine_config['spec_files'] = ['non_asset_pipeline_test.js']
      end
      FileUtils.mkdir_p(File.join('public', 'javascripts'))
      FileUtils.cp(File.join(@root, 'spec', 'fixture', 'non_asset_pipeline.js'), File.join('public', 'javascripts'))
      FileUtils.cp(File.join(@root, 'spec', 'fixture', 'non_asset_pipeline_test.js'), File.join('spec', 'javascripts'))

      Bundler.with_clean_env do
        output = `bundle exec rake jasmine:ci JASMINE_CONFIG_PATH=#{yaml}`
        output.should include('1 spec, 0 failures')
      end
    end

    it "should pass custom rack options from jasmine.yml" do
      pending "we're testing this with thin, which doesn't work in jruby" if RUBY_PLATFORM == 'java'
      rack_yaml = custom_jasmine_config('custom_rack') do |jasmine_config|
        jasmine_config['rack_options'] = { 'server' => 'webrick' }
      end

      Bundler.with_clean_env do
        default_output = `bundle exec rake jasmine:ci`
        default_output.should include('Thin web server')

        custom_output = `bundle exec rake jasmine:ci JASMINE_CONFIG_PATH=#{rack_yaml} 2>&1`
        custom_output.should include("WEBrick")
      end
    end

    def run_jasmine_server(options = "")
      Bundler.with_clean_env do
        begin
          pid = IO.popen("bundle exec rake jasmine #{options}").pid
          Jasmine::wait_for_listener(8888, 'jasmine server', 60)

          # if the process we started is not still running, it's very likely this test
          # will fail because another server is already running on port 8888
          # (kill -0 will check if you can send *ANY* signal to this process)
          # (( it does not actually kill the process, that happens below))
          `kill -0 #{pid}`
          unless $?.success?
            puts "someone else is running a server on port 8888"
            $?.should be_success
          end
        ensure
          Process.kill(:SIGINT, pid)
          begin
            Process.waitpid pid
          rescue Errno::ECHILD
          end
        end
      end
    end
  end
end
