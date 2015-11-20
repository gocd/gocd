## Rails 4.0.4 (March 14, 2014) ##

*   Added Thor-action for creation of migrations.

    Fixes #13588 and #12674.

    *Gert Goet*

*   Add `ENV['DATABASE_URL']` support in `rails dbconsole`. Fixes #13320.

    *Huiming Teo*

*   Fix default `config/application.rb` template to honor the RAILS_GROUPS env variable.

    *Guillermo Iguaran*

*   Fix default `config/application.rb` template to remove unused `config.assets.enabled` variable.

    *Guillermo Iguaran*


## Rails 4.0.3 (February 18, 2014) ##

*No changes*


## Rails 4.0.2 (December 02, 2013) ##

*No changes*


## Rails 4.0.1 (November 01, 2013) ##

*   Fix the event name of action_dispatch requests.

    *Rafael Mendonça França*

*   Make `config.log_level` work with custom loggers.

    *Max Shytikov*

*   Fix `rake environment` to do not eager load modules

    *Paul Nikitochkin*

*   Fix `rails plugin --help` command.

    *Richard Schneeman*

*   Omit turbolinks configuration completely on skip_javascript generator option.

    *Nikita Fedyashev*


## Rails 4.0.0 (June 25, 2013) ##

*   Clearing autoloaded constants triggers routes reloading [Fixes #10685].

    *Xavier Noria*

*   Fixes bug with scaffold generator with `--assets=false --resource-route=false`.
    Fixes #9525.

    *Arun Agrawal*

*   Move rails.png into a data-uri. One less file to get generated into a new
    application. This is also consistent with the removal of index.html.

    *Steve Klabnik*

*   The application rake task `doc:rails` generates now an API like the
    official one (except for the links to GitHub).

    *Xavier Noria*

*   Allow vanilla apps to render CoffeeScript templates in production

    Vanilla apps already render CoffeeScript templates in development and test
    environments.  With this change, the production behavior matches that of
    the other environments.

    Effectively, this meant moving coffee-rails (and the JavaScript runtime on
    which it is dependent) from the :assets group to the top-level of the
    generated Gemfile.

    *Gabe Kopley*

*   Don't generate a scaffold.css when --no-assets is specified

    *Kevin Glowacz*

*   Add support for generate scaffold password:digest

    * adds password_digest attribute to the migration
    * adds has_secure_password to the model
    * adds password and password_confirmation password_fields to _form.html
    * omits password from index.html and show.html
    * adds password and password_confirmation to the controller
    * adds unencrypted password and password_confirmation to the controller test
    * adds encrypted password_digest to the fixture

    *Sam Ruby*

*   Improved `rake test` command for running tests

    To run all tests:

        $ rake test

    To run a test suite

        $ rake test:[models,helpers,units,controllers,mailers,...]

    To run a selected test file(s):

        $ rake test test/unit/foo_test.rb [test/unit/bar_test.rb ...]

    To run a single test from a test file

        $ rake test test/unit/foo_test.rb TESTOPTS='-n test_the_truth'

*   Improve service pages with new layout (404, etc).

    *Stanislav Sobolev*

*   Improve `rake stats` for JavaScript and CoffeeScript: ignore block comments
    and calculates number of functions.

    *Hendy Tanata*

*   Ability to use a custom builder by passing `--builder` (or `-b`) has been removed.
    Consider using application template instead. See this guide for more detail:
    http://guides.rubyonrails.org/rails_application_templates.html

    *Prem Sichanugrist*

*   Fix `rake db:*` tasks to work with `DATABASE_URL` and without `config/database.yml`.

    *Terence Lee*

*   Add notice message for destroy action in scaffold generator.

    *Rahul P. Chaudhari*

*   Add two new test rake tasks to speed up full test runs.

    * `test:all`: run tests quickly by merging all types and not resetting db.
    * `test:all:db`: run tests quickly, but also reset db.

    *Ryan Davis*

*   Add `--rc` option to support the load of a custom rc file during the generation of a new app.

    *Amparo Luna*

*   Add `--no-rc` option to skip the loading of railsrc file during the generation of a new app.

    *Amparo Luna*

*   Fixes database.yml when creating a new rails application with '.'
    Fixes #8304.

    *Jeremy W. Rowe*

*   Restore Rails::Engine::Railties#engines with deprecation to ensure
    compatibility with gems such as Thinking Sphinx
    Fixes #8551.

    *Tim Raymond*

*   Specify which logs to clear when using the `rake log:clear` task.
    (e.g. rake log:clear LOGS=test,staging)

    *Matt Bridges*

*   Allow a `:dirs` key in the `SourceAnnotationExtractor.enumerate` options
    to explicitly set the directories to be traversed so it's easier to define
    custom rake tasks.

    *Brian D. Burns*

*   Deprecate `Rails::Generators::ActiveModel#update_attributes` in favor of `#update`.

    ORMs that implement `Generators::ActiveModel#update_attributes` should change
    to `#update`. Scaffold controller generators should change calls like:

        @orm_instance.update_attributes(...)

    to:

        @orm_instance.update(...)

    This goes along with the addition of `ActiveRecord::Base#update`.

    *Carlos Antonio da Silva*

*   Include `jbuilder` by default and rely on its scaffold generator to show json API.
    Check https://github.com/rails/jbuilder for more info and examples.

    *DHH*

*   Scaffold now generates HTML-only controller by default.

    *DHH + Pavel Pravosud*

*   The generated `README.rdoc` for new applications invites the user to
    document the necessary steps to get the application up and running.

    *Xavier Noria*

*   Generated applications no longer get `doc/README_FOR_APP`. In consequence,
    the `doc` directory is created on demand by documentation tasks rather than
    generated by default.

    *Xavier Noria*

*   App executables now live in the `bin/` directory: `bin/bundle`,
    `bin/rails`, `bin/rake`. Run `rake rails:update:bin` to add these
    executables to your own app. `script/rails` is gone from new apps.

    Running executables within your app ensures they use your app's Ruby
    version and its bundled gems, and it ensures your production deployment
    tools only need to execute a single script. No more having to carefully
    `cd` to the app dir and run `bundle exec ...`.

    Rather than treating `bin/` as a junk drawer for generated "binstubs",
    bundler 1.3 adds support for generating stubs for just the executables
    you actually use: `bundle binstubs unicorn` generates `bin/unicorn`.
    Add that executable to git and version it just like any other app code.

    *Jeremy Kemper*

*   `config.assets.enabled` is now true by default. If you're upgrading from a Rails 3.x app
    that does not use the asset pipeline, you'll be required to add `config.assets.enabled = false`
    to your application.rb. If you don't want the asset pipeline on a new app use `--skip-sprockets`

    *DHH*

*   Environment name can be a start substring of the default environment names
    (production, development, test). For example: tes, pro, prod, dev, devel.
    Fixes #8628.

    *Mykola Kyryk*

*   Add `-B` alias for `--skip-bundle` option in the rails new generators.

    *Jiri Pospisil*

*   Quote column names in generates fixture files. This prevents
    conflicts with reserved YAML keywords such as 'yes' and 'no'
    Fixes #8612.

    *Yves Senn*

*   Explicit options have precedence over `~/.railsrc` on the `rails new` command.

    *Rafael Mendonça França*

*   Generated migrations now always use the `change` method.

    *Marc-André Lafortune*

*   Add `app/models/concerns` and `app/controllers/concerns` to the default directory structure and load path.
    See http://37signals.com/svn/posts/3372-put-chubby-models-on-a-diet-with-concerns for usage instructions.

    *DHH*

*   The `rails/info/routes` now correctly formats routing output as an html table.

    *Richard Schneeman*

*   The `public/index.html` is no longer generated for new projects.
    Page is replaced by internal `welcome_controller` inside of railties.

    *Richard Schneeman*

*   Add `ENV['RACK_ENV']` support to `rails runner/console/server`.

    *kennyj*

*   Add `db` to list of folders included by `rake notes` and `rake notes:custom`. *Antonio Cangiano*

*   Engines with a dummy app include the rake tasks of dependencies in the app namespace.
    Fixes #8229.

    *Yves Senn*

*   Add `sqlserver.yml` template file to satisfy `-d sqlserver` being passed to `rails new`.
    Fixes #6882.

    *Robert Nesius*

*   Rake test:uncommitted finds git directory in ancestors *Nicolas Despres*

*   Add dummy app Rake tasks when `--skip-test-unit` and `--dummy-path` is passed to the plugin generator.
    Fixes #8121.

    *Yves Senn*

*   Add `.rake` to list of file extensions included by `rake notes` and `rake notes:custom`. *Brent J. Nordquist*

*   New test locations `test/models`, `test/helpers`, `test/controllers`, and
    `test/mailers`. Corresponding rake tasks added as well. *Mike Moore*

*   Set a different cache per environment for assets pipeline
    through `config.assets.cache`.

    *Guillermo Iguaran*

*   `Rails.public_path` now returns a Pathname object. *Prem Sichanugrist*

*   Remove highly uncommon `config.assets.manifest` option for moving the manifest path.
    This option is now unsupported in sprockets-rails.

    *Guillermo Iguaran & Dmitry Vorotilin*

*   Add `config.action_controller.permit_all_parameters` to disable
    StrongParameters protection, it's false by default.

    *Guillermo Iguaran*

*   Remove `config.active_record.whitelist_attributes` and
    `config.active_record.mass_assignment_sanitizer` from new applications since
    MassAssignmentSecurity has been extracted from Rails.

    *Guillermo Iguaran*

*   Change `rails new` and `rails plugin new` generators to name the `.gitkeep` files
    as `.keep` in a more SCM-agnostic way.

    Change `--skip-git` option to only skip the `.gitignore` file and still generate
    the `.keep` files.

    Add `--skip-keeps` option to skip the `.keep` files.

    *Derek Prior & Francesco Rodriguez*

*   Fixed support for `DATABASE_URL` environment variable for rake db tasks.

    *Grace Liu*

*   `rails dbconsole` now can use SSL for MySQL. The `database.yml` options sslca, sslcert, sslcapath, sslcipher
    and sslkey now affect `rails dbconsole`.

    *Jim Kingdon and Lars Petrus*

*   Correctly handle SCRIPT_NAME when generating routes to engine in application
    that's mounted at a sub-uri. With this behavior, you *should not* use
    `default_url_options[:script_name]` to set proper application's mount point by
    yourself.

     *Piotr Sarnacki*

*   `config.threadsafe!` is deprecated in favor of `config.eager_load` which provides a more fine grained control on what is eager loaded .

    *José Valim*

*   The migration generator will now produce AddXXXToYYY/RemoveXXXFromYYY migrations with references statements, for instance

        rails g migration AddReferencesToProducts user:references supplier:references{polymorphic}

    will generate the migration with:

        add_reference :products, :user, index: true
        add_reference :products, :supplier, polymorphic: true, index: true

    *Aleksey Magusev*

*   Allow scaffold/model/migration generators to accept a `polymorphic` modifier
    for `references`/`belongs_to`, for instance

        rails g model Product supplier:references{polymorphic}

    will generate the model with `belongs_to :supplier, polymorphic: true`
    association and appropriate migration.

    *Aleksey Magusev*

*   Set `config.active_record.migration_error` to `:page_load` for development.

     *Richard Schneeman*

*   Add runner to `Rails::Railtie` as a hook called just after runner starts.

     *José Valim & kennyj*

*   Add `/rails/info/routes` path, displays same information as `rake routes` .

     *Richard Schneeman & Andrew White*

*   Improved `rake routes` output for redirects.

     *Łukasz Strzałkowski & Andrew White*

*   Load all environments available in `config.paths["config/environments"]`.

     *Piotr Sarnacki*

*   Remove `Rack::SSL` in favour of `ActionDispatch::SSL`.

     *Rafael Mendonça França*

*   Remove Active Resource from Rails framework.

     *Prem Sichangrist*

*   Allow to set class that will be used to run as a console, other than IRB, with `Rails.application.config.console=`. It's best to add it to `console` block.

    Example:

        # it can be added to config/application.rb
        console do
          # this block is called only when running console,
          # so we can safely require pry here
          require "pry"
          config.console = Pry
        end

    *Piotr Sarnacki*

*   Add convenience `hide!` method to Rails generators to hide current generator
    namespace from showing when running `rails generate`.

     *Carlos Antonio da Silva*

*   Rails::Plugin has gone. Instead of adding plugins to vendor/plugins use gems or bundler with path or git dependencies.

    *Santiago Pastorino*

*   Set config.action_mailer.async = true to turn on asynchronous
    message delivery.

     *Brian Cardarella*

Please check [3-2-stable](https://github.com/rails/rails/blob/3-2-stable/railties/CHANGELOG.md) for previous changes.
