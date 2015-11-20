Getting Started with Rails
==========================

This guide covers getting up and running with Ruby on Rails.

After reading this guide, you will know:

* How to install Rails, create a new Rails application, and connect your
  application to a database.
* The general layout of a Rails application.
* The basic principles of MVC (Model, View, Controller) and RESTful design.
* How to quickly generate the starting pieces of a Rails application.

--------------------------------------------------------------------------------

Guide Assumptions
-----------------

This guide is designed for beginners who want to get started with a Rails
application from scratch. It does not assume that you have any prior experience
with Rails. However, to get the most out of it, you need to have some
prerequisites installed:

* The [Ruby](http://www.ruby-lang.org/en/downloads) language version 1.9.3 or newer
* The [RubyGems](http://rubygems.org/) packaging system
    * To learn more about RubyGems, please read the [RubyGems User Guide](http://docs.rubygems.org/read/book/1)
* A working installation of the [SQLite3 Database](http://www.sqlite.org)

Rails is a web application framework running on the Ruby programming language.
If you have no prior experience with Ruby, you will find a very steep learning
curve diving straight into Rails. There are some good free resources on the
internet for learning Ruby, including:

* [Mr. Neighborly's Humble Little Ruby Book](http://www.humblelittlerubybook.com)
* [Programming Ruby](http://www.ruby-doc.org/docs/ProgrammingRuby/)
* [Why's (Poignant) Guide to Ruby](http://mislav.uniqpath.com/poignant-guide/)

What is Rails?
--------------

Rails is a web application development framework written in the Ruby language.
It is designed to make programming web applications easier by making assumptions
about what every developer needs to get started. It allows you to write less
code while accomplishing more than many other languages and frameworks.
Experienced Rails developers also report that it makes web application
development more fun.

Rails is opinionated software. It makes the assumption that there is the "best"
way to do things, and it's designed to encourage that way - and in some cases to
discourage alternatives. If you learn "The Rails Way" you'll probably discover a
tremendous increase in productivity. If you persist in bringing old habits from
other languages to your Rails development, and trying to use patterns you
learned elsewhere, you may have a less happy experience.

The Rails philosophy includes two major guiding principles:

* DRY - "Don't Repeat Yourself" - suggests that writing the same code over and over again is a bad thing.
* Convention Over Configuration - means that Rails makes assumptions about what you want to do and how you're going to
do it, rather than requiring you to specify every little thing through endless configuration files.

Creating a New Rails Project
----------------------------

The best way to use this guide is to follow each step as it happens, no code or
step needed to make this example application has been left out, so you can
literally follow along step by step. You can get the complete code
[here](https://github.com/rails/docrails/tree/master/guides/code/getting_started).

By following along with this guide, you'll create a Rails project called
`blog`, a
(very) simple weblog. Before you can start building the application, you need to
make sure that you have Rails itself installed.

TIP: The examples below use `#` and `$` to denote superuser and regular
user terminal prompts respectively in a UNIX-like OS. If you are using
Windows, your prompt will look something like `c:\source_code>`

### Installing Rails

Open up a command line prompt. On Mac OS X open Terminal.app, on Windows choose
"Run" from your Start menu and type 'cmd.exe'. Any commands prefaced with a
dollar sign `$` should be run in the command line. Verify that you have a
current version of Ruby installed:

```bash
$ ruby -v
ruby 1.9.3p385
```

To install Rails, use the `gem install` command provided by RubyGems:

```bash
$ gem install rails
```

TIP. A number of tools exist to help you quickly install Ruby and Ruby
on Rails on your system. Windows users can use [Rails Installer](http://railsinstaller.org), while Mac OS X users can use
[Rails One Click](http://railsoneclick.com).

To verify that you have everything installed correctly, you should be able to run the following:

```bash
$ rails --version
```

If it says something like "Rails 4.0.0", you are ready to continue.

### Creating the Blog Application

Rails comes with a number of scripts called generators that are designed to make your development life easier by creating everything that's necessary to start working on a particular task. One of these is the new application generator, which will provide you with the foundation of a fresh Rails application so that you don't have to write it yourself.

To use this generator, open a terminal, navigate to a directory where you have rights to create files, and type:

```bash
$ rails new blog
```

This will create a Rails application called Blog in a directory called blog and install the gem dependencies that are already mentioned in `Gemfile` using `bundle install`.

TIP: You can see all of the command line options that the Rails
application builder accepts by running `rails new -h`.

After you create the blog application, switch to its folder to continue work directly in that application:

```bash
$ cd blog
```

The `rails new blog` command we ran above created a folder in your
working directory called `blog`. The `blog` directory has a number of
auto-generated files and folders that make up the structure of a Rails
application. Most of the work in this tutorial will happen in the `app/` folder, but here's a basic rundown on the function of each of the files and folders that Rails created by default:

| File/Folder | Purpose |
| ----------- | ------- |
|app/|Contains the controllers, models, views, helpers, mailers and assets for your application. You'll focus on this folder for the remainder of this guide.|
|bin/|Contains the rails script that starts your app and can contain other scripts you use to deploy or run your application.|
|config/|Configure your application's runtime rules, routes, database, and more.  This is covered in more detail in [Configuring Rails Applications](configuring.html)|
|config.ru|Rack configuration for Rack based servers used to start the application.|
|db/|Contains your current database schema, as well as the database migrations.|
|Gemfile<br />Gemfile.lock|These files allow you to specify what gem dependencies are needed for your Rails application. These files are used by the Bundler gem. For more information about Bundler, see [the Bundler website](http://gembundler.com) |
|lib/|Extended modules for your application.|
|log/|Application log files.|
|public/|The only folder seen to the world as-is. Contains the static files and compiled assets.|
|Rakefile|This file locates and loads tasks that can be run from the command line. The task definitions are defined throughout the components of Rails. Rather than changing Rakefile, you should add your own tasks by adding files to the lib/tasks directory of your application.|
|README.rdoc|This is a brief instruction manual for your application. You should edit this file to tell others what your application does, how to set it up, and so on.|
|test/|Unit tests, fixtures, and other test apparatus. These are covered in [Testing Rails Applications](testing.html)|
|tmp/|Temporary files (like cache, pid and session files)|
|vendor/|A place for all third-party code. In a typical Rails application, this includes Ruby Gems and the Rails source code (if you optionally install it into your project).|

Hello, Rails!
-------------

To begin with, let's get some text up on screen quickly. To do this, you need to get your Rails application server running.

### Starting up the Web Server

You actually have a functional Rails application already. To see it, you need to start a web server on your development machine. You can do this by running the following in the root directory of your rails application:

```bash
$ rails server
```

TIP: Compiling CoffeeScript to JavaScript requires a JavaScript runtime and the absence of a runtime will give you an `execjs` error. Usually Mac OS X and Windows come with a JavaScript runtime installed. Rails adds the `therubyracer` gem to Gemfile in a commented line for new apps and you can uncomment if you need it. `therubyrhino` is the recommended runtime for JRuby users and is added by default to Gemfile in apps generated under JRuby. You can investigate about all the supported runtimes at [ExecJS](https://github.com/sstephenson/execjs#readme).

This will fire up WEBrick, a webserver built into Ruby by default. To see your application in action, open a browser window and navigate to <http://localhost:3000>. You should see the Rails default information page:

![Welcome Aboard screenshot](images/getting_started/rails_welcome.png)

TIP: To stop the web server, hit Ctrl+C in the terminal window where it's running. To verify the server has stopped you should see your command prompt cursor again. For most UNIX-like systems including Mac OS X this will be a dollar sign `$`. In development mode, Rails does not generally require you to restart the server; changes you make in files will be automatically picked up by the server.

The "Welcome Aboard" page is the _smoke test_ for a new Rails application: it makes sure that you have your software configured correctly enough to serve a page. You can also click on the _About your application’s environment_ link to see a summary of your application's environment.

### Say "Hello", Rails

To get Rails saying "Hello", you need to create at minimum a _controller_ and a _view_.

A controller's purpose is to receive specific requests for the application. _Routing_ decides which controller receives which requests. Often, there is more than one route to each controller, and different routes can be served by different _actions_. Each action's purpose is to collect information to provide it to a view.

A view's purpose is to display this information in a human readable format. An important distinction to make is that it is the _controller_, not the view, where information is collected. The view should just display that information. By default, view templates are written in a language called ERB (Embedded Ruby) which is converted by the request cycle in Rails before being sent to the user.

To create a new controller, you will need to run the "controller" generator and tell it you want a controller called "welcome" with an action called "index", just like this:

```bash
$ rails generate controller welcome index
```

Rails will create several files and a route for you.

```bash
create  app/controllers/welcome_controller.rb
 route  get "welcome/index"
invoke  erb
create    app/views/welcome
create    app/views/welcome/index.html.erb
invoke  test_unit
create    test/controllers/welcome_controller_test.rb
invoke  helper
create    app/helpers/welcome_helper.rb
invoke    test_unit
create      test/helpers/welcome_helper_test.rb
invoke  assets
invoke    coffee
create      app/assets/javascripts/welcome.js.coffee
invoke    scss
create      app/assets/stylesheets/welcome.css.scss
```

Most important of these are of course the controller, located at `app/controllers/welcome_controller.rb` and the view, located at `app/views/welcome/index.html.erb`.

Open the `app/views/welcome/index.html.erb` file in your text editor. Delete all of the existing code in the file, and replace it with the following single line of code:

```html
<h1>Hello, Rails!</h1>
```

### Setting the Application Home Page

Now that we have made the controller and view, we need to tell Rails when we want Hello Rails! to show up. In our case, we want it to show up when we navigate to the root URL of our site, <http://localhost:3000>. At the moment, "Welcome Aboard" is occupying that spot.

Next, you have to tell Rails where your actual home page is located.

Open the file `config/routes.rb` in your editor.

```ruby
Blog::Application.routes.draw do
  get "welcome/index"

  # The priority is based upon order of creation:
  # first created -> highest priority.
  # ...
  # You can have the root of your site routed with "root"
  # root to: "welcome#index"
```

This is your application's _routing file_ which holds entries in a special DSL (domain-specific language) that tells Rails how to connect incoming requests to controllers and actions. This file contains many sample routes on commented lines, and one of them actually shows you how to connect the root of your site to a specific controller and action. Find the line beginning with `root` and uncomment it. It should look something like the following:

```ruby
root "welcome#index"
```

The `root "welcome#index"` tells Rails to map requests to the root of the application to the welcome controller's index action and `get "welcome/index"` tells Rails to map requests to <http://localhost:3000/welcome/index> to the welcome controller's index action. This was created earlier when you ran the controller generator (`rails generate controller welcome index`).

If you navigate to <http://localhost:3000> in your browser, you'll see the `Hello, Rails!` message you put into `app/views/welcome/index.html.erb`, indicating that this new route is indeed going to `WelcomeController`'s `index` action and is rendering the view correctly.

TIP: For more information about routing, refer to [Rails Routing from the Outside In](routing.html).

Getting Up and Running
----------------------

Now that you've seen how to create a controller, an action and a view, let's create something with a bit more substance.

In the Blog application, you will now create a new _resource_. A resource is the term used for a collection of similar objects, such as posts, people or animals. You can create, read, update and destroy items for a resource and these operations are referred to as _CRUD_ operations.

Rails provides a `resources` method which can be used to declare a
standard REST resource. Here's how `config/routes.rb` will look like.

```ruby
Blog::Application.routes.draw do

  resources :posts

  root to: "welcome#index"
end
```

If you run `rake routes`, you'll see that all the routes for the
standard RESTful actions.

```bash
$ rake routes
    posts GET    /posts(.:format)          posts#index
          POST   /posts(.:format)          posts#create
 new_post GET    /posts/new(.:format)      posts#new
edit_post GET    /posts/:id/edit(.:format) posts#edit
     post GET    /posts/:id(.:format)      posts#show
          PATCH  /posts/:id(.:format)      posts#update
          PUT    /posts/:id(.:format)      posts#update
          DELETE /posts/:id(.:format)      posts#destroy
     root        /                         welcome#index
```

In the next section, you will add the ability to create new posts in your application and be able to view them. This is the "C" and the "R" from CRUD: creation and reading. The form for doing this will look like this:

![The new post form](images/getting_started/new_post.png)

It will look a little basic for now, but that's ok. We'll look at improving the styling for it afterwards.

### Laying down the ground work

The first thing that you are going to need to create a new post within the application is a place to do that. A great place for that would be at `/posts/new`.  With the route already defined, requests can now be made to `/posts/new` in the application. Navigate to <http://localhost:3000/posts/new> and you'll see a routing error:

![Another routing error, uninitialized constant PostsController](images/getting_started/routing_error_no_controller.png)

This error occurs because the route needs to have a controller defined in order to serve the request. The solution to this particular problem is simple: create a controller called `PostsController`. You can do this by running this command:

```bash
$ rails g controller posts
```

If you open up the newly generated `app/controllers/posts_controller.rb` you'll see a fairly empty controller:

```ruby
class PostsController < ApplicationController
end
```

A controller is simply a class that is defined to inherit from `ApplicationController`. It's inside this class that you'll define methods that will become the actions for this controller. These actions will perform CRUD operations on the posts within our system.

NOTE: There are `public`, `private` and `protected` methods in `Ruby`
(for more details you can check on [Programming Ruby](http://www.ruby-doc.org/docs/ProgrammingRuby/)).
But only `public` methods can be actions for controllers.

If you refresh <http://localhost:3000/posts/new> now, you'll get a new error:

![Unknown action new for PostsController!](images/getting_started/unknown_action_new_for_posts.png)

This error indicates that Rails cannot find the `new` action inside the `PostsController` that you just generated. This is because when controllers are generated in Rails they are empty by default, unless you tell it you wanted actions during the generation process.

To manually define an action inside a controller, all you need to do is to define a new method inside the controller. Open `app/controllers/posts_controller.rb` and inside the `PostsController` class, define a `new` method like this:

```ruby
def new
end
```

With the `new` method defined in `PostsController`, if you refresh <http://localhost:3000/posts/new> you'll see another error:

![Template is missing for posts/new](images/getting_started/template_is_missing_posts_new.png)

You're getting this error now because Rails expects plain actions like this one to have views associated with them to display their information. With no view available, Rails errors out.

In the above image, the bottom line has been truncated. Let's see what the full thing looks like:

<blockquote>
Missing template posts/new, application/new with {locale:[:en], formats:[:html], handlers:[:erb, :builder, :coffee]}. Searched in: * "/path/to/blog/app/views"
</blockquote>

That's quite a lot of text! Let's quickly go through and understand what each part of it does.

The first part identifies what template is missing. In this case, it's the `posts/new` template. Rails will first look for this template. If not found, then it will attempt to load a template called `application/new`. It looks for one here because the `PostsController` inherits from `ApplicationController`.

The next part of the message contains a hash. The `:locale` key in this hash simply indicates what spoken language template should be retrieved. By default, this is the English — or "en" — template. The next key, `:formats` specifies the format of template to be served in response. The default format is `:html`, and so Rails is looking for an HTML template. The final key, `:handlers`, is telling us what _template handlers_ could be used to render our template. `:erb` is most commonly used for HTML templates, `:builder` is used for XML templates, and `:coffee` uses CoffeeScript to build JavaScript templates.

The final part of this message tells us where Rails has looked for the templates. Templates within a basic Rails application like this are kept in a single location, but in more complex applications it could be many different paths.

The simplest template that would work in this case would be one located at `app/views/posts/new.html.erb`. The extension of this file name is key: the first extension is the _format_ of the template, and the second extension is the _handler_ that will be used. Rails is attempting to find a template called `posts/new` within `app/views` for the application. The format for this template can only be `html` and the handler must be one of `erb`, `builder` or `coffee`. Because you want to create a new HTML form, you will be using the `ERB` language. Therefore the file should be called `posts/new.html.erb` and needs to be located inside the `app/views` directory of the application.

Go ahead now and create a new file at `app/views/posts/new.html.erb` and write this content in it:

```html
<h1>New Post</h1>
```

When you refresh <http://localhost:3000/posts/new> you'll now see that the page has a title. The route, controller, action and view are now working harmoniously! It's time to create the form for a new post.

### The first form

To create a form within this template, you will use a <em>form
builder</em>. The primary form builder for Rails is provided by a helper
method called `form_for`. To use this method, add this code into `app/views/posts/new.html.erb`:

```html+erb
<%= form_for :post do |f| %>
  <p>
    <%= f.label :title %><br>
    <%= f.text_field :title %>
  </p>

  <p>
    <%= f.label :text %><br>
    <%= f.text_area :text %>
  </p>

  <p>
    <%= f.submit %>
  </p>
<% end %>
```

If you refresh the page now, you'll see the exact same form as in the example. Building forms in Rails is really just that easy!

When you call `form_for`, you pass it an identifying object for this
form. In this case, it's the symbol `:post`. This tells the `form_for`
helper what this form is for. Inside the block for this method, the
`FormBuilder` object — represented by `f` — is used to build two labels and two text fields, one each for the title and text of a post. Finally, a call to `submit` on the `f` object will create a submit button for the form.

There's one problem with this form though. If you inspect the HTML that is generated, by viewing the source of the page, you will see that the `action` attribute for the form is pointing at `/posts/new`. This is a problem because this route goes to the very page that you're on right at the moment, and that route should only be used to display the form for a new post.

The form needs to use a different URL in order to go somewhere else.
This can be done quite simply with the `:url` option of `form_for`.
Typically in Rails, the action that is used for new form submissions
like this is called "create", and so the form should be pointed to that action.

Edit the `form_for` line inside `app/views/posts/new.html.erb` to look like this:

```html+erb
<%= form_for :post, url: posts_path do |f| %>
```

In this example, the `posts_path` helper is passed to the `:url` option. What Rails will do with this is that it will point the form to the `create` action of the current controller, the `PostsController`, and will send a `POST` request to that route.

By using the `post` method rather than the `get` method, Rails will define a route that will only respond to POST methods. The POST method is the typical method used by forms all over the web.

With the form and its associated route defined, you will be able to fill in the form and then click the submit button to begin the process of creating a new post, so go ahead and do that. When you submit the form, you should see a familiar error:

![Unknown action create for PostsController](images/getting_started/unknown_action_create_for_posts.png)

You now need to create the `create` action within the `PostsController` for this to work.

### Creating posts

To make the "Unknown action" go away, you can define a `create` action within the `PostsController` class in `app/controllers/posts_controller.rb`, underneath the `new` action:

```ruby
class PostsController < ApplicationController
  def new
  end

  def create
  end
end
```

If you re-submit the form now, you'll see another familiar error: a template is missing. That's ok, we can ignore that for now. What the `create` action should be doing is saving our new post to a database.

When a form is submitted, the fields of the form are sent to Rails as _parameters_. These parameters can then be referenced inside the controller actions, typically to perform a particular task. To see what these parameters look like, change the `create` action to this:

```ruby
def create
  render text: params[:post].inspect
end
```

The `render` method here is taking a very simple hash with a key of `text` and value of `params[:post].inspect`. The `params` method is the object which represents the parameters (or fields) coming in from the form. The `params` method returns an `ActiveSupport::HashWithIndifferentAccess` object, which allows you to access the keys of the hash using either strings or symbols. In this situation, the only parameters that matter are the ones from the form.

If you re-submit the form one more time you'll now no longer get the missing template error. Instead, you'll see something that looks like the following:

```ruby
{"title"=>"First post!", "text"=>"This is my first post."}
```

This action is now displaying the parameters for the post that are coming in from the form. However, this isn't really all that helpful. Yes, you can see the parameters but nothing in particular is being done with them.

### Creating the Post model

Models in Rails use a singular name, and their corresponding database tables use
a plural name. Rails provides a generator for creating models, which
most Rails developers tend to use when creating new models.
To create the new model, run this command in your terminal:

```bash
$ rails generate model Post title:string text:text
```

With that command we told Rails that we want a `Post` model, together
with a _title_ attribute of type string, and a _text_ attribute
of type text. Those attributes are automatically added to the `posts`
table in the database and mapped to the `Post` model.

Rails responded by creating a bunch of files. For
now, we're only interested in `app/models/post.rb` and
`db/migrate/20120419084633_create_posts.rb` (your name could be a bit
different). The latter is responsible
for creating the database structure, which is what we'll look at next.

TIP: Active Record is smart enough to automatically map column names to
model attributes, which means you don't have to declare attributes
inside Rails models, as that will be done automatically by Active
Record.

### Running a Migration

As we've just seen, `rails generate model` created a _database
migration_ file inside the `db/migrate` directory.
Migrations are Ruby classes that are designed to make it simple to
create and modify database tables. Rails uses rake commands to run migrations,
and it's possible to undo a migration after it's been applied to your database.
Migration filenames include a timestamp to ensure that they're processed in the
order that they were created.

If you look in the `db/migrate/20120419084633_create_posts.rb` file (remember,
yours will have a slightly different name), here's what you'll find:

```ruby
class CreatePosts < ActiveRecord::Migration
  def change
    create_table :posts do |t|
      t.string :title
      t.text :text

      t.timestamps
    end
  end
end
```

The above migration creates a method named `change` which will be called when you
run this migration. The action defined in this method is also reversible, which
means Rails knows how to reverse the change made by this migration, in case you
want to reverse it later. When you run this migration it will create a
`posts` table with one string column and a text column. It also creates two
timestamp fields to allow Rails to track post creation and update times.

TIP: For more information about migrations, refer to [Rails Database
Migrations](migrations.html).

At this point, you can use a rake command to run the migration:

```bash
$ rake db:migrate
```

Rails will execute this migration command and tell you it created the Posts
table.

```bash
==  CreatePosts: migrating ====================================================
-- create_table(:posts)
   -> 0.0019s
==  CreatePosts: migrated (0.0020s) ===========================================
```

NOTE. Because you're working in the development environment by default, this
command will apply to the database defined in the `development` section of your
`config/database.yml` file. If you would like to execute migrations in another
environment, for instance in production, you must explicitly pass it when
invoking the command: `rake db:migrate RAILS_ENV=production`.

### Saving data in the controller

Back in `posts_controller`, we need to change the `create` action
to use the new `Post` model to save the data in the database. Open `app/controllers/posts_controller.rb`
and change the `create` action to look like this:

```ruby
def create
  @post = Post.new(params[:post])
  @post.save
  redirect_to @post
end
```

Here's what's going on: every Rails model can be initialized with its
respective attributes, which are automatically mapped to the respective
database columns. In the first line we do just that (remember that
`params[:post]` contains the attributes we're interested in). Then,
`@post.save` is responsible for saving the model in the database.
Finally, we redirect the user to the `show` action,
which we'll define later.

TIP: As we'll see later, `@post.save` returns a boolean indicating
whether the model was saved or not.

If you now go to
<http://localhost:3000/posts/new> you'll *almost* be able to create a post. Try
it! You should get an error that looks like this:

![Forbidden attributes for new post](images/getting_started/forbidden_attributes_for_new_post.png)

Rails has several security features that help you write secure applications,
and you're running into one of them now. This one is called
`strong_parameters`, which requires us to tell Rails exactly which parameters
we want to accept in our controllers. In this case, we want to allow the
`title` and `text` parameters, so change your `create` controller action to
look like this:

```ruby
def create
  @post = Post.new(post_params)

  @post.save
  redirect_to @post
end

private
  def post_params
    params.require(:post).permit(:title, :text)
  end
```

See the `permit`? It allows us to accept both `title` and `text` in this
action.

TIP: Note that `def post_params` is private. This new approach prevents an attacker from
setting the model's attributes by manipulating the hash passed to the model.
For more information, refer to
[this blog post about Strong Parameters](http://weblog.rubyonrails.org/2012/3/21/strong-parameters/).

### Showing Posts

If you submit the form again now, Rails will complain about not finding
the `show` action. That's not very useful though, so let's add the
`show` action before proceeding.

As we have seen in the output of `rake routes`, the route for `show` action is
as follows:

```
post GET    /posts/:id(.:format)      posts#show
```

The special syntax `:id` tells rails that this route expects an `:id`
parameter, which in our case will be the id of the post.

As we did before, we need to add the `show` action in
`app/controllers/posts_controller.rb` and its respective view.

```ruby
def show
  @post = Post.find(params[:id])
end
```

A couple of things to note. We use `Post.find` to find the post we're
interested in. We also use an instance variable (prefixed by `@`) to
hold a reference to the post object. We do this because Rails will pass all instance
variables to the view.

Now, create a new file `app/views/posts/show.html.erb` with the following
content:

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>
```

With this change, you should finally be able to create new posts.
Visit <http://localhost:3000/posts/new> and give it a try!

![Show action for posts](images/getting_started/show_action_for_posts.png)

### Listing all posts

We still need a way to list all our posts, so let's do that.
We'll use a specific route from `config/routes.rb`:

```ruby
posts GET    /posts(.:format)          posts#index
```

And an action for that route inside the `PostsController` in the `app/controllers/posts_controller.rb` file:

```ruby
def index
  @posts = Post.all
end
```

And then finally a view for this action, located at `app/views/posts/index.html.erb`:

```html+erb
<h1>Listing posts</h1>

<table>
  <tr>
    <th>Title</th>
    <th>Text</th>
  </tr>

  <% @posts.each do |post| %>
    <tr>
      <td><%= post.title %></td>
      <td><%= post.text %></td>
    </tr>
  <% end %>
</table>
```

Now if you go to `http://localhost:3000/posts` you will see a list of all the posts that you have created.

### Adding links

You can now create, show, and list posts. Now let's add some links to
navigate through pages.

Open `app/views/welcome/index.html.erb` and modify it as follows:

```html+erb
<h1>Hello, Rails!</h1>
<%= link_to "My Blog", controller: "posts" %>
```

The `link_to` method is one of Rails' built-in view helpers. It creates a
hyperlink based on text to display and where to go - in this case, to the path
for posts.

Let's add links to the other views as well, starting with adding this "New Post" link to `app/views/posts/index.html.erb`, placing it above the `<table>` tag:

```erb
<%= link_to 'New post', new_post_path %>
```

This link will allow you to bring up the form that lets you create a new post. You should also add a link to this template — `app/views/posts/new.html.erb` — to go back to the `index` action. Do this by adding this underneath the form in this template:

```erb
<%= form_for :post do |f| %>
  ...
<% end %>

<%= link_to 'Back', posts_path %>
```

Finally, add another link to the `app/views/posts/show.html.erb` template to go back to the `index` action as well, so that people who are viewing a single post can go back and view the whole list again:

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>

<%= link_to 'Back', posts_path %>
```

TIP: If you want to link to an action in the same controller, you don't
need to specify the `:controller` option, as Rails will use the current
controller by default.

TIP: In development mode (which is what you're working in by default), Rails
reloads your application with every browser request, so there's no need to stop
and restart the web server when a change is made.

### Allowing the update of fields

The model file, `app/models/post.rb` is about as simple as it can get:

```ruby
class Post < ActiveRecord::Base
end
```

There isn't much to this file - but note that the `Post` class inherits from
`ActiveRecord::Base`. Active Record supplies a great deal of functionality to
your Rails models for free, including basic database CRUD (Create, Read, Update,
Destroy) operations, data validation, as well as sophisticated search support
and the ability to relate multiple models to one another.

### Adding Some Validation

Rails includes methods to help you validate the data that you send to models.
Open the `app/models/post.rb` file and edit it:

```ruby
class Post < ActiveRecord::Base
  validates :title, presence: true,
                    length: { minimum: 5 }
end
```

These changes will ensure that all posts have a title that is at least five
characters long.  Rails can validate a variety of conditions in a model,
including the presence or uniqueness of columns, their format, and the
existence of associated objects. Validations are covered in detail in [Active
Record Validations](active_record_validations.html)

With the validation now in place, when you call `@post.save` on an invalid
post, it will return `false`. If you open `app/controllers/posts_controller.rb`
again, you'll notice that we don't check the result of calling `@post.save`
inside the `create` action. If `@post.save` fails in this situation, we need to
show the form back to the user. To do this, change the `new` and `create`
actions inside `app/controllers/posts_controller.rb` to these:

```ruby
def new
  @post = Post.new
end

def create
  @post = Post.new(params[:post].permit(:title, :text))

  if @post.save
    redirect_to @post
  else
    render 'new'
  end
end
```

The `new` action is now creating a new instance variable called `@post`, and
you'll see why that is in just a few moments.

Notice that inside the `create` action we use `render` instead of `redirect_to` when `save`
returns `false`. The `render` method is used so that the `@post` object is passed back to the `new` template when it is rendered. This rendering is done within the same request as the form submission, whereas the `redirect_to` will tell the browser to issue another request.

If you reload
<http://localhost:3000/posts/new> and
try to save a post without a title, Rails will send you back to the
form, but that's not very useful. You need to tell the user that
something went wrong. To do that, you'll modify
`app/views/posts/new.html.erb` to check for error messages:

```html+erb
<%= form_for :post, url: posts_path do |f| %>
  <% if @post.errors.any? %>
  <div id="error_explanation">
    <h2><%= pluralize(@post.errors.count, "error") %> prohibited
      this post from being saved:</h2>
    <ul>
    <% @post.errors.full_messages.each do |msg| %>
      <li><%= msg %></li>
    <% end %>
    </ul>
  </div>
  <% end %>
  <p>
    <%= f.label :title %><br>
    <%= f.text_field :title %>
  </p>

  <p>
    <%= f.label :text %><br>
    <%= f.text_area :text %>
  </p>

  <p>
    <%= f.submit %>
  </p>
<% end %>

<%= link_to 'Back', posts_path %>
```

A few things are going on. We check if there are any errors with
`@post.errors.any?`, and in that case we show a list of all
errors with `@post.errors.full_messages`.

`pluralize` is a rails helper that takes a number and a string as its
arguments. If the number is greater than one, the string will be automatically pluralized.

The reason why we added `@post = Post.new` in `posts_controller` is that
otherwise `@post` would be `nil` in our view, and calling
`@post.errors.any?` would throw an error.

TIP: Rails automatically wraps fields that contain an error with a div
with class `field_with_errors`. You can define a css rule to make them
standout.

Now you'll get a nice error message when saving a post without title when you
attempt to do just that on the new post form [(http://localhost:3000/posts/new)](http://localhost:3000/posts/new).

![Form With Errors](images/getting_started/form_with_errors.png)

### Updating Posts

We've covered the "CR" part of CRUD. Now let's focus on the "U" part, updating posts.

The first step we'll take is adding an `edit` action to `posts_controller`.

```ruby
def edit
  @post = Post.find(params[:id])
end
```

The view will contain a form similar to the one we used when creating
new posts. Create a file called `app/views/posts/edit.html.erb` and make
it look as follows:

```html+erb
<h1>Editing post</h1>

<%= form_for :post, url: post_path(@post), method: :patch do |f| %>
  <% if @post.errors.any? %>
  <div id="error_explanation">
    <h2><%= pluralize(@post.errors.count, "error") %> prohibited
      this post from being saved:</h2>
    <ul>
    <% @post.errors.full_messages.each do |msg| %>
      <li><%= msg %></li>
    <% end %>
    </ul>
  </div>
  <% end %>
  <p>
    <%= f.label :title %><br>
    <%= f.text_field :title %>
  </p>

  <p>
    <%= f.label :text %><br>
    <%= f.text_area :text %>
  </p>

  <p>
    <%= f.submit %>
  </p>
<% end %>

<%= link_to 'Back', posts_path %>
```

This time we point the form to the `update` action, which is not defined yet
but will be very soon.

The `method: :patch` option tells Rails that we want this form to be submitted
via the `PATCH` HTTP method which is the HTTP method you're expected to use to
**update** resources according to the REST protocol.

TIP: By default forms built with the _form_for_ helper are sent via `POST`.

Next we need to create the `update` action in `app/controllers/posts_controller.rb`:

```ruby
def update
  @post = Post.find(params[:id])

  if @post.update(params[:post].permit(:title, :text))
    redirect_to @post
  else
    render 'edit'
  end
end
```

The new method, `update`, is used when you want to update a record
that already exists, and it accepts a hash containing the attributes
that you want to update. As before, if there was an error updating the
post we want to show the form back to the user.

TIP: You don't need to pass all attributes to `update`. For
example, if you'd call `@post.update(title: 'A new title')`
Rails would only update the `title` attribute, leaving all other
attributes untouched.

Finally, we want to show a link to the `edit` action in the list of all the
posts, so let's add that now to `app/views/posts/index.html.erb` to make it
appear next to the "Show" link:

```html+erb
<table>
  <tr>
    <th>Title</th>
    <th>Text</th>
    <th></th>
    <th></th>
  </tr>

<% @posts.each do |post| %>
  <tr>
    <td><%= post.title %></td>
    <td><%= post.text %></td>
    <td><%= link_to 'Show', post %></td>
    <td><%= link_to 'Edit', edit_post_path(post) %></td>
  </tr>
<% end %>
</table>
```

And we'll also add one to the `app/views/posts/show.html.erb` template as well,
so that there's also an "Edit" link on a post's page. Add this at the bottom of
the template:

```html+erb
...

<%= link_to 'Back', posts_path %>
| <%= link_to 'Edit', edit_post_path(@post) %>
```

And here's how our app looks so far:

![Index action with edit link](images/getting_started/index_action_with_edit_link.png)

### Using partials to clean up duplication in views

Our `edit` page looks very similar to the `new` page, in fact they
both share the same code for displaying the form. Let's remove some duplication
by using a view partial. By convention, partial files are prefixed by an
underscore.

TIP: You can read more about partials in the
[Layouts and Rendering in Rails](layouts_and_rendering.html) guide.

Create a new file `app/views/posts/_form.html.erb` with the following
content:

```html+erb
<%= form_for @post do |f| %>
  <% if @post.errors.any? %>
  <div id="error_explanation">
    <h2><%= pluralize(@post.errors.count, "error") %> prohibited
      this post from being saved:</h2>
    <ul>
    <% @post.errors.full_messages.each do |msg| %>
      <li><%= msg %></li>
    <% end %>
    </ul>
  </div>
  <% end %>
  <p>
    <%= f.label :title %><br>
    <%= f.text_field :title %>
  </p>

  <p>
    <%= f.label :text %><br>
    <%= f.text_area :text %>
  </p>

  <p>
    <%= f.submit %>
  </p>
<% end %>
```

Everything except for the `form_for` declaration remained the same.
How `form_for` can figure out the right `action` and `method` attributes
when building the form will be explained in just a moment. For now, let's update the
`app/views/posts/new.html.erb` view to use this new partial, rewriting it
completely:

```html+erb
<h1>New post</h1>

<%= render 'form' %>

<%= link_to 'Back', posts_path %>
```

Then do the same for the `app/views/posts/edit.html.erb` view:

```html+erb
<h1>Edit post</h1>

<%= render 'form' %>

<%= link_to 'Back', posts_path %>
```

### Deleting Posts

We're now ready to cover the "D" part of CRUD, deleting posts from the
database. Following the REST convention, the route for
deleting posts in the `config/routes.rb` is:

```ruby
DELETE /posts/:id(.:format)      posts#destroy
```

The `delete` routing method should be used for routes that destroy
resources. If this was left as a typical `get` route, it could be possible for
people to craft malicious URLs like this:

```html
<a href='http://example.com/posts/1/destroy'>look at this cat!</a>
```

We use the `delete` method for destroying resources, and this route is mapped to
the `destroy` action inside `app/controllers/posts_controller.rb`, which doesn't exist yet, but is
provided below:

```ruby
def destroy
  @post = Post.find(params[:id])
  @post.destroy

  redirect_to posts_path
end
```

You can call `destroy` on Active Record objects when you want to delete
them from the database. Note that we don't need to add a view for this
action since we're redirecting to the `index` action.

Finally, add a 'destroy' link to your `index` action template
(`app/views/posts/index.html.erb`) to wrap everything
together.

```html+erb
<h1>Listing Posts</h1>
<%= link_to 'New post', new_post_path %>
<table>
  <tr>
    <th>Title</th>
    <th>Text</th>
    <th></th>
    <th></th>
    <th></th>
  </tr>

<% @posts.each do |post| %>
  <tr>
    <td><%= post.title %></td>
    <td><%= post.text %></td>
    <td><%= link_to 'Show', post_path(post) %></td>
    <td><%= link_to 'Edit', edit_post_path(post) %></td>
    <td><%= link_to 'Destroy', post_path(post),
                    method: :delete, data: { confirm: 'Are you sure?' } %></td>
  </tr>
<% end %>
</table>
```

Here we're using `link_to` in a different way. We pass the named route as the first argument,
and then the final two keys as another argument. The `:method` and `:'data-confirm'`
options are used as HTML5 attributes so that when the link is clicked,
Rails will first show a confirm dialog to the user, and then submit the link with method `delete`.
This is done via the JavaScript file `jquery_ujs` which is automatically included
into your application's layout (`app/views/layouts/application.html.erb`) when you
generated the application. Without this file, the confirmation dialog box wouldn't appear.

![Confirm Dialog](images/getting_started/confirm_dialog.png)

Congratulations, you can now create, show, list, update and destroy
posts.

TIP: In general, Rails encourages the use of resources objects in place
of declaring routes manually.
For more information about routing, see
[Rails Routing from the Outside In](routing.html).

Adding a Second Model
---------------------

It's time to add a second model to the application. The second model will handle comments on
posts.

### Generating a Model

We're going to see the same generator that we used before when creating
the `Post` model. This time we'll create a `Comment` model to hold
reference of post comments. Run this command in your terminal:

```bash
$ rails generate model Comment commenter:string body:text post:references
```

This command will generate four files:

| File                                         | Purpose                                                                                                |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| db/migrate/20100207235629_create_comments.rb | Migration to create the comments table in your database (your name will include a different timestamp) |
| app/models/comment.rb                        | The Comment model                                                                                      |
| test/models/comment_test.rb                  | Testing harness for the comments model                                                                 |
| test/fixtures/comments.yml                   | Sample comments for use in testing                                                                     |

First, take a look at `app/models/comment.rb`:

```ruby
class Comment < ActiveRecord::Base
  belongs_to :post
end
```

This is very similar to the `post.rb` model that you saw earlier. The difference
is the line `belongs_to :post`, which sets up an Active Record _association_.
You'll learn a little about associations in the next section of this guide.

In addition to the model, Rails has also made a migration to create the
corresponding database table:

```ruby
class CreateComments < ActiveRecord::Migration
  def change
    create_table :comments do |t|
      t.string :commenter
      t.text :body
      t.references :post

      t.timestamps
    end

    add_index :comments, :post_id
  end
end
```

The `t.references` line sets up a foreign key column for the association between
the two models. And the `add_index` line sets up an index for this association
column. Go ahead and run the migration:

```bash
$ rake db:migrate
```

Rails is smart enough to only execute the migrations that have not already been
run against the current database, so in this case you will just see:

```bash
==  CreateComments: migrating =================================================
-- create_table(:comments)
   -> 0.0008s
-- add_index(:comments, :post_id)
   -> 0.0003s
==  CreateComments: migrated (0.0012s) ========================================
```

### Associating Models

Active Record associations let you easily declare the relationship between two
models. In the case of comments and posts, you could write out the relationships
this way:

* Each comment belongs to one post.
* One post can have many comments.

In fact, this is very close to the syntax that Rails uses to declare this
association. You've already seen the line of code inside the `Comment` model (app/models/comment.rb) that
makes each comment belong to a Post:

```ruby
class Comment < ActiveRecord::Base
  belongs_to :post
end
```

You'll need to edit `app/models/post.rb` to add the other side of the association:

```ruby
class Post < ActiveRecord::Base
  has_many :comments
  validates :title, presence: true,
                    length: { minimum: 5 }
  [...]
end
```

These two declarations enable a good bit of automatic behavior. For example, if
you have an instance variable `@post` containing a post, you can retrieve all
the comments belonging to that post as an array using `@post.comments`.

TIP: For more information on Active Record associations, see the [Active Record
Associations](association_basics.html) guide.

### Adding a Route for Comments

As with the `welcome` controller, we will need to add a route so that Rails knows
where we would like to navigate to see `comments`. Open up the
`config/routes.rb` file again, and edit it as follows:

```ruby
resources :posts do
  resources :comments
end
```

This creates `comments` as a _nested resource_ within `posts`. This is another
part of capturing the hierarchical relationship that exists between posts and
comments.

TIP: For more information on routing, see the [Rails Routing](routing.html) guide.

### Generating a Controller

With the model in hand, you can turn your attention to creating a matching
controller. Again, we'll use the same generator we used before:

```bash
$ rails generate controller Comments
```

This creates six files and one empty directory:

| File/Directory                               | Purpose                                  |
| -------------------------------------------- | ---------------------------------------- |
| app/controllers/comments_controller.rb       | The Comments controller                  |
| app/views/comments/                          | Views of the controller are stored here  |
| test/controllers/comments_controller_test.rb | The test for the controller              |
| app/helpers/comments_helper.rb               | A view helper file                       |
| test/helpers/comments_helper_test.rb         | The test for the helper                  |
| app/assets/javascripts/comment.js.coffee     | CoffeeScript for the controller          |
| app/assets/stylesheets/comment.css.scss      | Cascading style sheet for the controller |

Like with any blog, our readers will create their comments directly after
reading the post, and once they have added their comment, will be sent back to
the post show page to see their comment now listed. Due to this, our
`CommentsController` is there to provide a method to create comments and delete
spam comments when they arrive.

So first, we'll wire up the Post show template
(`app/views/posts/show.html.erb`) to let us make a new comment:

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>

<h2>Add a comment:</h2>
<%= form_for([@post, @post.comments.build]) do |f| %>
  <p>
    <%= f.label :commenter %><br />
    <%= f.text_field :commenter %>
  </p>
  <p>
    <%= f.label :body %><br />
    <%= f.text_area :body %>
  </p>
  <p>
    <%= f.submit %>
  </p>
<% end %>

<%= link_to 'Back', posts_path %>
| <%= link_to 'Edit', edit_post_path(@post) %>
```

This adds a form on the `Post` show page that creates a new comment by
calling the `CommentsController` `create` action. The `form_for` call here uses
an array, which will build a nested route, such as `/posts/1/comments`.

Let's wire up the `create` in `app/controllers/comments_controller.rb`:

```ruby
class CommentsController < ApplicationController
  def create
    @post = Post.find(params[:post_id])
    @comment = @post.comments.create(params[:comment].permit(:commenter, :body))
    redirect_to post_path(@post)
  end
end
```

You'll see a bit more complexity here than you did in the controller for posts.
That's a side-effect of the nesting that you've set up. Each request for a
comment has to keep track of the post to which the comment is attached, thus the
initial call to the `find` method of the `Post` model to get the post in question.

In addition, the code takes advantage of some of the methods available for an
association. We use the `create` method on `@post.comments` to create and save
the comment. This will automatically link the comment so that it belongs to that
particular post.

Once we have made the new comment, we send the user back to the original post
using the `post_path(@post)` helper. As we have already seen, this calls the
`show` action of the `PostsController` which in turn renders the `show.html.erb`
template. This is where we want the comment to show, so let's add that to the
`app/views/posts/show.html.erb`.

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>

<h2>Comments</h2>
<% @post.comments.each do |comment| %>
  <p>
    <strong>Commenter:</strong>
    <%= comment.commenter %>
  </p>

  <p>
    <strong>Comment:</strong>
    <%= comment.body %>
  </p>
<% end %>

<h2>Add a comment:</h2>
<%= form_for([@post, @post.comments.build]) do |f| %>
  <p>
    <%= f.label :commenter %><br />
    <%= f.text_field :commenter %>
  </p>
  <p>
    <%= f.label :body %><br />
    <%= f.text_area :body %>
  </p>
  <p>
    <%= f.submit %>
  </p>
<% end %>

<%= link_to 'Edit Post', edit_post_path(@post) %> |
<%= link_to 'Back to Posts', posts_path %>
```

Now you can add posts and comments to your blog and have them show up in the
right places.

![Post with Comments](images/getting_started/post_with_comments.png)

Refactoring
-----------

Now that we have posts and comments working, take a look at the
`app/views/posts/show.html.erb` template. It is getting long and awkward. We can
use partials to clean it up.

### Rendering Partial Collections

First, we will make a comment partial to extract showing all the comments for the
post. Create the file `app/views/comments/_comment.html.erb` and put the
following into it:

```html+erb
<p>
  <strong>Commenter:</strong>
  <%= comment.commenter %>
</p>

<p>
  <strong>Comment:</strong>
  <%= comment.body %>
</p>
```

Then you can change `app/views/posts/show.html.erb` to look like the
following:

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>

<h2>Comments</h2>
<%= render @post.comments %>

<h2>Add a comment:</h2>
<%= form_for([@post, @post.comments.build]) do |f| %>
  <p>
    <%= f.label :commenter %><br />
    <%= f.text_field :commenter %>
  </p>
  <p>
    <%= f.label :body %><br />
    <%= f.text_area :body %>
  </p>
  <p>
    <%= f.submit %>
  </p>
<% end %>

<%= link_to 'Edit Post', edit_post_path(@post) %> |
<%= link_to 'Back to Posts', posts_path %>
```

This will now render the partial in `app/views/comments/_comment.html.erb` once
for each comment that is in the `@post.comments` collection. As the `render`
method iterates over the `@post.comments` collection, it assigns each
comment to a local variable named the same as the partial, in this case
`comment` which is then available in the partial for us to show.

### Rendering a Partial Form

Let us also move that new comment section out to its own partial. Again, you
create a file `app/views/comments/_form.html.erb` containing:

```html+erb
<%= form_for([@post, @post.comments.build]) do |f| %>
  <p>
    <%= f.label :commenter %><br />
    <%= f.text_field :commenter %>
  </p>
  <p>
    <%= f.label :body %><br />
    <%= f.text_area :body %>
  </p>
  <p>
    <%= f.submit %>
  </p>
<% end %>
```

Then you make the `app/views/posts/show.html.erb` look like the following:

```html+erb
<p>
  <strong>Title:</strong>
  <%= @post.title %>
</p>

<p>
  <strong>Text:</strong>
  <%= @post.text %>
</p>

<h2>Comments</h2>
<%= render @post.comments %>

<h2>Add a comment:</h2>
<%= render "comments/form" %>

<%= link_to 'Edit Post', edit_post_path(@post) %> |
<%= link_to 'Back to Posts', posts_path %>
```

The second render just defines the partial template we want to render,
`comments/form`. Rails is smart enough to spot the forward slash in that
string and realize that you want to render the `_form.html.erb` file in
the `app/views/comments` directory.

The `@post` object is available to any partials rendered in the view because we
defined it as an instance variable.

Deleting Comments
-----------------

Another important feature of a blog is being able to delete spam comments. To do
this, we need to implement a link of some sort in the view and a `DELETE` action
in the `CommentsController`.

So first, let's add the delete link in the
`app/views/comments/_comment.html.erb` partial:

```html+erb
<p>
  <strong>Commenter:</strong>
  <%= comment.commenter %>
</p>

<p>
  <strong>Comment:</strong>
  <%= comment.body %>
</p>

<p>
  <%= link_to 'Destroy Comment', [comment.post, comment],
               method: :delete,
               data: { confirm: 'Are you sure?' } %>
</p>
```

Clicking this new "Destroy Comment" link will fire off a `DELETE
/posts/:post_id/comments/:id` to our `CommentsController`, which can then use
this to find the comment we want to delete, so let's add a destroy action to our
controller (`app/controllers/comments_controller.rb`):

```ruby
class CommentsController < ApplicationController

  def create
    @post = Post.find(params[:post_id])
    @comment = @post.comments.create(params[:comment])
    redirect_to post_path(@post)
  end

  def destroy
    @post = Post.find(params[:post_id])
    @comment = @post.comments.find(params[:id])
    @comment.destroy
    redirect_to post_path(@post)
  end

end
```

The `destroy` action will find the post we are looking at, locate the comment
within the `@post.comments` collection, and then remove it from the
database and send us back to the show action for the post.


### Deleting Associated Objects

If you delete a post then its associated comments will also need to be deleted.
Otherwise they would simply occupy space in the database. Rails allows you to
use the `dependent` option of an association to achieve this. Modify the Post
model, `app/models/post.rb`, as follows:

```ruby
class Post < ActiveRecord::Base
  has_many :comments, dependent: :destroy
  validates :title, presence: true,
                    length: { minimum: 5 }
  [...]
end
```

Security
--------

If you were to publish your blog online, anybody would be able to add, edit and
delete posts or delete comments.

Rails provides a very simple HTTP authentication system that will work nicely in
this situation.

In the `PostsController` we need to have a way to block access to the various
actions if the person is not authenticated, here we can use the Rails
`http_basic_authenticate_with` method, allowing access to the requested
action if that method allows it.

To use the authentication system, we specify it at the top of our
`PostsController`, in this case, we want the user to be authenticated on every
action, except for `index` and `show`, so we write that in `app/controllers/posts_controller.rb`:

```ruby
class PostsController < ApplicationController

  http_basic_authenticate_with name: "dhh", password: "secret", except: [:index, :show]

  def index
    @posts = Post.all
  end

  # snipped for brevity
```

We also only want to allow authenticated users to delete comments, so in the
`CommentsController` (`app/controllers/comments_controller.rb`) we write:

```ruby
class CommentsController < ApplicationController

  http_basic_authenticate_with name: "dhh", password: "secret", only: :destroy

  def create
    @post = Post.find(params[:post_id])
    ...
  end
  # snipped for brevity
```

Now if you try to create a new post, you will be greeted with a basic HTTP
Authentication challenge

![Basic HTTP Authentication Challenge](images/getting_started/challenge.png)

What's Next?
------------

Now that you've seen your first Rails application, you should feel free to
update it and experiment on your own. But you don't have to do everything
without help. As you need assistance getting up and running with Rails, feel
free to consult these support resources:

* The [Ruby on Rails guides](index.html)
* The [Ruby on Rails Tutorial](http://railstutorial.org/book)
* The [Ruby on Rails mailing list](http://groups.google.com/group/rubyonrails-talk)
* The [#rubyonrails](irc://irc.freenode.net/#rubyonrails) channel on irc.freenode.net

Rails also comes with built-in help that you can generate using the rake command-line utility:

* Running `rake doc:guides` will put a full copy of the Rails Guides in the `doc/guides` folder of your application. Open `doc/guides/index.html` in your web browser to explore the Guides.
* Running `rake doc:rails` will put a full copy of the API documentation for Rails in the `doc/api` folder of your application. Open `doc/api/index.html` in your web browser to explore the API documentation.

TIP: To be able to generate the Rails Guides locally with the `doc:guides` rake task you need to install the RedCloth gem. Add it to your `Gemfile` and run `bundle install` and you're ready to go.

Configuration Gotchas
---------------------

The easiest way to work with Rails is to store all external data as UTF-8. If
you don't, Ruby libraries and Rails will often be able to convert your native
data into UTF-8, but this doesn't always work reliably, so you're better off
ensuring that all external data is UTF-8.

If you have made a mistake in this area, the most common symptom is a black
diamond with a question mark inside appearing in the browser. Another common
symptom is characters like "Ã¼" appearing instead of "ü". Rails takes a number
of internal steps to mitigate common causes of these problems that can be
automatically detected and corrected. However, if you have external data that is
not stored as UTF-8, it can occasionally result in these kinds of issues that
cannot be automatically detected by Rails and corrected.

Two very common sources of data that are not UTF-8:

* Your text editor: Most text editors (such as TextMate), default to saving files as
  UTF-8. If your text editor does not, this can result in special characters that you
  enter in your templates (such as é) to appear as a diamond with a question mark inside
  in the browser. This also applies to your i18n translation files.
  Most editors that do not already default to UTF-8 (such as some versions of
  Dreamweaver) offer a way to change the default to UTF-8. Do so.
* Your database. Rails defaults to converting data from your database into UTF-8 at
  the boundary. However, if your database is not using UTF-8 internally, it may not
  be able to store all characters that your users enter. For instance, if your database
  is using Latin-1 internally, and your user enters a Russian, Hebrew, or Japanese
  character, the data will be lost forever once it enters the database. If possible,
  use UTF-8 as the internal storage of your database.
