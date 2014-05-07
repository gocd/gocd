# rspec-rails [![Build Status](https://secure.travis-ci.org/rspec/rspec-rails.png?branch=master)](http://travis-ci.org/rspec/rspec-rails) [![Code Climate](https://codeclimate.com/github/rspec/rspec-rails.png)](https://codeclimate.com/github/rspec/rspec-rails)

**rspec-rails** is a testing framework for Rails 3.x and 4.x.

Use **[rspec-rails 1.x](http://github.com/dchelimsky/rspec-rails)** for Rails
2.x.

## Installation

Add `rspec-rails` to **both** the `:development` and `:test` groups in the
`Gemfile`:

```ruby
group :development, :test do
  gem 'rspec-rails', '~> 2.0'
end
```

Download and install by running:

```
bundle install
```

Initialize the `spec/` directory (where specs will reside) with:

```
rails generate rspec:install
```

To run your specs, use the `rspec` command:

```
bundle exec rspec

# Run only model specs
bundle exec rspec spec/models

# Run only specs for AccountsController
bundle exec rspec spec/controllers/accounts_controller_spec.rb
```

Specs can also be run via `rake spec`, though this command may be slower to
start than the `rspec` command.

In Rails 4, you may want to create a binstub for the `rspec` command so it can
be run via `bin/rspec`:

```
bundle binstubs rspec-core
```

### Generators

Once installed, RSpec will generate spec files instead of Test::Unit test files
when commands like `rails generate model` and `rails generate controller` are
used.

You may also invoke invoke RSpec generators independently. For instance,
running `rails generate rspec:model` will generate a model spec. For more
information, see [list of all
generators](https://www.relishapp.com/rspec/rspec-rails/docs/generators).

## Model Specs

Model specs reside in the `spec/models` folder. Use model specs to describe
behavior of models (usually ActiveRecord-based) in the application. For example:

```ruby
require "spec_helper"

describe User do
  it "orders by last name" do
    lindeman = User.create!(first_name: "Andy", last_name: "Lindeman")
    chelimsky = User.create!(first_name: "David", last_name: "Chelimsky")

    expect(User.ordered_by_last_name).to eq([chelimsky, lindeman])
  end
end
```

For more information, see [cucumber scenarios for model
specs](https://www.relishapp.com/rspec/rspec-rails/docs/model-specs).

## Controller Specs

Controller specs reside in the `spec/controllers` folder. Use controller specs
to describe behavior of Rails controllers. For example:

```ruby
require "spec_helper"

describe PostsController do
  describe "GET #index" do
    it "responds successfully with an HTTP 200 status code" do
      get :index
      expect(response).to be_success
      expect(response.status).to eq(200)
    end

    it "renders the index template" do
      get :index
      expect(response).to render_template("index")
    end

    it "loads all of the posts into @posts" do
      post1, post2 = Post.create!, Post.create!
      get :index

      expect(assigns(:posts)).to match_array([post1, post2])
    end
  end
end
```

For more information, see [cucumber scenarios for controller
specs](https://www.relishapp.com/rspec/rspec-rails/docs/controller-specs).

**Note:** To encourage more isolated testing, views are not rendered by default
in controller specs. If you wish to assert against the contents of the rendered
view in a controller spec, enable
[render\_views](https://www.relishapp.com/rspec/rspec-rails/docs/controller-specs/render-views)
or use a higher-level [request spec](#request-specs) or [feature
spec](#feature-specs).

## <a id="request-spec"></a>Request Specs

Request specs live in spec/requests, spec/api and
spec/integration, and mix in behavior
[ActionDispatch::Integration::Runner](http://api.rubyonrails.org/classes/ActionDispatch/Integration/Runner.html),
which is the basis for [Rails' integration
tests](http://guides.rubyonrails.org/testing.html#integration-testing).  The
intent is to specify one or more request/response cycles from end to end using
a black box approach.

```ruby
require 'spec_helper'
describe "home page" do
  it "displays the user's username after successful login" do
    user = User.create!(:username => "jdoe", :password => "secret")
    get "/login"
    assert_select "form.login" do
      assert_select "input[name=?]", "username"
      assert_select "input[name=?]", "password"
      assert_select "input[type=?]", "submit"
    end

    post "/login", :username => "jdoe", :password => "secret"
    assert_select ".header .username", :text => "jdoe"
  end
end
```

This example uses only standard Rails and RSpec API's, but many RSpec/Rails
users like to use extension libraries like
[FactoryGirl](https://github.com/thoughtbot/factory_girl) and
[Capybara](https://github.com/jnicklas/capybara):

```ruby
require 'spec_helper'
describe "home page" do
  it "displays the user's username after successful login" do
    user = FactoryGirl.create(:user, :username => "jdoe", :password => "secret")
    visit "/login"
    fill_in "Username", :with => "jdoe"
    fill_in "Password", :with => "secret"
    click_button "Log in"

    expect(page).to have_selector(".header .username", :text => "jdoe")
  end
end
```

FactoryGirl decouples this example from changes to validation requirements,
which can be encoded into the underlying factory definition without requiring
changes to this example.

Among other benefits, Capybara binds the form post to the generated HTML, which
means we don't need to specify them separately.  Note that Capybara's DSL as
shown is, by default, only available in specs in the spec/features directory.
For more information, see the [Capybara integration
docs](http://rubydoc.info/gems/rspec-rails/file/Capybara.md).

There are several other Ruby libs that implement the factory pattern or provide
a DSL for request specs (a.k.a. acceptance or integration specs), but
FactoryGirl and Capybara seem to be the most widely used. Whether you choose
these or other libs, we strongly recommend using something for each of these
roles.

# View specs

View specs live in spec/views, and mix in ActionView::TestCase::Behavior.

```ruby
require 'spec_helper'
describe "events/index" do
  it "renders _event partial for each event" do
    assign(:events, [stub_model(Event), stub_model(Event)])
    render
    expect(view).to render_template(:partial => "_event", :count => 2)
  end
end

describe "events/show" do
  it "displays the event location" do
    assign(:event, stub_model(Event,
      :location => "Chicago"
    ))
    render
    expect(rendered).to include("Chicago")
  end
end
```

View specs infer the controller name and path from the path to the view
template. e.g. if the template is "events/index.html.erb" then:

```ruby
controller.controller_path == "events"
controller.request.path_parameters[:controller] == "events"
```

This means that most of the time you don't need to set these values. When
spec'ing a partial that is included across different controllers, you _may_
need to override these values before rendering the view.

To provide a layout for the render, you'll need to specify _both_ the template
and the layout explicitly.  For example:

```ruby
render :template => "events/show", :layout => "layouts/application"
```

## `assign(key, val)`

Use this to assign values to instance variables in the view:

```ruby
assign(:widget, stub_model(Widget))
render
```

The code above assigns `stub_model(Widget)` to the `@widget` variable in the view, and then
renders the view.

Note that because view specs mix in `ActionView::TestCase` behavior, any
instance variables you set will be transparently propagated into your views
(similar to how instance variables you set in controller actions are made
available in views). For example:

```ruby
@widget = stub_model(Widget)
render # @widget is available inside the view
```

RSpec doesn't officially support this pattern, which only works as a
side-effect of the inclusion of `ActionView::TestCase`. Be aware that it may be
made unavailable in the future.

### Upgrade note

```ruby
# rspec-rails-1.x
assigns[key] = value

# rspec-rails-2.x
assign(key, value)
```

## `rendered`

This represents the rendered view.

```ruby
render
expect(rendered).to match /Some text expected to appear on the page/
```

### Upgrade note

```ruby
# rspec-rails-1.x
render
response.should xxx

# rspec-rails-2.x
render
rendered.should xxx

# rspec-rails-2.x with expect syntax
render
expect(rendered).to xxx
```

# Routing specs

Routing specs live in spec/routing.

```ruby
require 'spec_helper'
describe "routing to profiles" do
  it "routes /profile/:username to profile#show for username" do
    expect(:get => "/profiles/jsmith").to route_to(
      :controller => "profiles",
      :action => "show",
      :username => "jsmith"
    )
  end

  it "does not expose a list of profiles" do
    expect(:get => "/profiles").not_to be_routable
  end
end
```

### Upgrade note

`route_for` from rspec-rails-1.x is gone. Use `route_to` and `be_routable` instead.

# Helper specs

Helper specs live in spec/helpers, and mix in ActionView::TestCase::Behavior.

Provides a `helper` object which mixes in the helper module being spec'd, along
with `ApplicationHelper` (if present).

```ruby
require 'spec_helper'
describe EventsHelper do
  describe "#link_to_event" do
    it "displays the title, and formatted date" do
      event = Event.new("Ruby Kaigi", Date.new(2010, 8, 27))
      # helper is an instance of ActionView::Base configured with the
      # EventsHelper and all of Rails' built-in helpers
      expect(helper.link_to_event).to match /Ruby Kaigi, 27 Aug, 2010/
    end
  end
end
```

# Matchers

rspec-rails exposes domain-specific matchers to each of the example group types. Most
of them simply delegate to Rails' assertions.

## `be_a_new`
* Available in all specs.
* Primarily intended for controller specs

```ruby
expect(object).to be_a_new(Widget)
```


Passes if the object is a `Widget` and returns true for `new_record?`

## `render_template`
* Delegates to Rails' assert_template.
* Available in request, controller, and view specs.

In request and controller specs, apply to the response object:

```ruby
expect(response).to render_template("new")
```

In view specs, apply to the view object:

```ruby
expect(view).to render_template(:partial => "_form", :locals => { :widget => widget } )
```

## `redirect_to`
* Delegates to assert_redirect
* Available in request and controller specs.

```ruby
expect(response).to redirect_to(widgets_path)
```

## `route_to`

* Delegates to Rails' assert_routing.
* Available in routing and controller specs.

```ruby
expect(:get => "/widgets").to route_to(:controller => "widgets", :action => "index")
```

## `be_routable`

Passes if the path is recognized by Rails' routing. This is primarily intended
to be used with `not_to` to specify routes that should not be routable.

```ruby
expect(:get => "/widgets/1/edit").not_to be_routable
```

# `rake` tasks

`rspec-rails` defines rake tasks to run the entire test suite (`rake spec`)
and subsets of tests (e.g., `rake spec:models`).

A full list of the available rake tasks can be seen by running `rake -T | grep
spec`.

## Customizing `rake` tasks

If you want to customize the behavior of `rake spec`, you may [define your own
task in the `Rakefile` for your
project](https://www.relishapp.com/rspec/rspec-core/docs/command-line/rake-task).
However, you must first clear the task that rspec-rails defined:

```ruby
task("spec").clear
```

### Webrat and Capybara

You can choose between webrat or capybara for simulating a browser, automating
a browser, or setting expectations using the matchers they supply. Just add
your preference to the Gemfile:

```ruby
gem "webrat"
# ... or ...
gem "capybara"
```

See [http://rubydoc.info/gems/rspec-rails/file/Capybara.md](http://rubydoc.info/gems/rspec-rails/file/Capybara.md)
for more info on Capybara integration.

# Contribute

See [http://github.com/rspec/rspec-dev](http://github.com/rspec/rspec-dev).

For `rspec-rails`-specific development information, see
[DEV-README](https://github.com/rspec/rspec-rails/blob/master/DEV-README.md).

# Also see

* [http://github.com/rspec/rspec](http://github.com/rspec/rspec)
* [http://github.com/rspec/rspec-core](http://github.com/rspec/rspec-core)
* [http://github.com/rspec/rspec-expectations](http://github.com/rspec/rspec-expectations)
* [http://github.com/rspec/rspec-mocks](http://github.com/rspec/rspec-mocks)

## Feature Requests & Bugs

See <http://github.com/rspec/rspec-rails/issues>
