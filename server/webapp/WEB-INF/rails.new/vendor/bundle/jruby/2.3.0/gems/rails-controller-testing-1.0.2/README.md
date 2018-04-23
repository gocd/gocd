# Rails::Controller::Testing

This gem brings back `assigns` to your controller tests as well as `assert_template`
to both controller and integration tests.

Note: This gem is only useful once `assigns` and `assert_template` have been
removed from Rails.

## Installation

Add this line to your application's Gemfile:

    gem 'rails-controller-testing'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install rails-controller-testing

### RSpec

See https://github.com/rspec/rspec-rails/issues/1393.

rspec-rails automatically integrates with this gem since version `3.5.0`.
Adding the gem to your `Gemfile` is sufficient.

If you use an older version of rspec-rails, you can manually include the
modules in your `rails_helper`.

```ruby
RSpec.configure do |config|
  [:controller, :view, :request].each do |type|
    config.include ::Rails::Controller::Testing::TestProcess, :type => type
    config.include ::Rails::Controller::Testing::TemplateAssertions, :type => type
    config.include ::Rails::Controller::Testing::Integration, :type => type
  end
end
```

## Outside Rails

For projects and gems using controller tests outside of a Rails application,
invoke the `Rails::Controller::Testing.install` method inside your test suite
setup to include the required modules on controller test cases.

```ruby
# test/test_helper.rb

require 'rails-controller-testing'
Rails::Controller::Testing.install
```

## Usage

### assigns

`assigns` allows you to access the instance variables that have been passed to
your views.

```ruby
class PostsController < ActionController::Base
  def index
    @posts = Post.all
  end
end

class PostControllerTest < ActionController::TestCase
  def test_index
    get :index
    assert_equal Post.all, assigns(:posts)
  end
end
```

### assert_template

`assert_template` allows to you assert that certain templates have been rendered.

```ruby
class PostControllerTest < ActionController::TestCase
  def test_index
    get :index
    assert_template 'posts/index'
  end
end
```

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
