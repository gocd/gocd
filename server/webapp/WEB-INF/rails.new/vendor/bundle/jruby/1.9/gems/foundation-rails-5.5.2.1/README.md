# Foundation::Rails

Foundation::Rails is a gem to make it super easy to use Foundation in your upcoming Rails project. You can start using Foundation::Rails in your projects by following the instructions below.

## Installation

Add this line to your application's Gemfile:

    $ gem 'foundation-rails'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install foundation-rails

### Configuring Foundation

You can run the following command to add Foundation:

    $ rails g foundation:install

## Manual Installation

### Add Foundation to your CSS

Append the following line to your `app/assets/stylesheets/application.css` file:

    /*= require foundation */

If you're planning on using Sass, then you'll want to rename `application.css` to `application.scss`. That file should then look like:

    @import "foundation_and_overrides";
    /* Add imports of custom sass/scss files here */

### Add Foundation to your JS

Append the following lines to your `app/assets/javascripts/application.js` file:

    //= require foundation
    $(document).foundation();

### Add Modernizr

Make sure that Modernizr is included in the `<head>` of your page layout:

    javascript_include_tag "vendor/modernizr"

### Set Viewport Width

Add the following line to the `head` of your page layout:

    <meta name="viewport" content="width=device-width, initial-scale=1.0" />

## Usage

Run the generator to add foundation to the asset pipeline:

    rails g foundation:install [layout_name] [options]
    
    Options:
      [--haml]  # Generate HAML layout instead of erb
      [--slim]  # Generate Slim layout instead of erb
    Runtime options:
      -f, [--force]    # Overwrite files that already exist
      -p, [--pretend]  # Run but do not make any changes
      -q, [--quiet]    # Suppress status output
      -s, [--skip]     # Skip files that already exist

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

## Resources

* [Foundation Docs](http://foundation.zurb.com/docs/)
* [Foundation Forum](http://foundation.zurb.com/forum)
* [Foundation Training](http://foundation.zurb.com/learn/training.html)