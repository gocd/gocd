Debugging Rails Applications
============================

This guide introduces techniques for debugging Ruby on Rails applications.

After reading this guide, you will know:

* The purpose of debugging.
* How to track down problems and issues in your application that your tests aren't identifying.
* The different ways of debugging.
* How to analyze the stack trace.

--------------------------------------------------------------------------------

View Helpers for Debugging
--------------------------

One common task is to inspect the contents of a variable. In Rails, you can do this with three methods:

* `debug`
* `to_yaml`
* `inspect`

### `debug`

The `debug` helper will return a \<pre> tag that renders the object using the YAML format. This will generate human-readable data from any object. For example, if you have this code in a view:

```html+erb
<%= debug @post %>
<p>
  <b>Title:</b>
  <%= @post.title %>
</p>
```

You'll see something like this:

```yaml
--- !ruby/object:Post
attributes:
  updated_at: 2008-09-05 22:55:47
  body: It's a very helpful guide for debugging your Rails app.
  title: Rails debugging guide
  published: t
  id: "1"
  created_at: 2008-09-05 22:55:47
attributes_cache: {}


Title: Rails debugging guide
```

### `to_yaml`

Displaying an instance variable, or any other object or method, in YAML format can be achieved this way:

```html+erb
<%= simple_format @post.to_yaml %>
<p>
  <b>Title:</b>
  <%= @post.title %>
</p>
```

The `to_yaml` method converts the method to YAML format leaving it more readable, and then the `simple_format` helper is used to render each line as in the console. This is how `debug` method does its magic.

As a result of this, you will have something like this in your view:

```yaml
--- !ruby/object:Post
attributes:
updated_at: 2008-09-05 22:55:47
body: It's a very helpful guide for debugging your Rails app.
title: Rails debugging guide
published: t
id: "1"
created_at: 2008-09-05 22:55:47
attributes_cache: {}

Title: Rails debugging guide
```

### `inspect`

Another useful method for displaying object values is `inspect`, especially when working with arrays or hashes. This will print the object value as a string. For example:

```html+erb
<%= [1, 2, 3, 4, 5].inspect %>
<p>
  <b>Title:</b>
  <%= @post.title %>
</p>
```

Will be rendered as follows:

```
[1, 2, 3, 4, 5]

Title: Rails debugging guide
```

The Logger
----------

It can also be useful to save information to log files at runtime. Rails maintains a separate log file for each runtime environment.

### What is the Logger?

Rails makes use of the `ActiveSupport::Logger` class to write log information. You can also substitute another logger such as `Log4r` if you wish.

You can specify an alternative logger in your `environment.rb` or any environment file:

```ruby
Rails.logger = Logger.new(STDOUT)
Rails.logger = Log4r::Logger.new("Application Log")
```

Or in the `Initializer` section, add _any_ of the following

```ruby
config.logger = Logger.new(STDOUT)
config.logger = Log4r::Logger.new("Application Log")
```

TIP: By default, each log is created under `Rails.root/log/` and the log file name is `environment_name.log`.

### Log Levels

When something is logged it's printed into the corresponding log if the log level of the message is equal or higher than the configured log level. If you want to know the current log level you can call the `Rails.logger.level` method.

The available log levels are: `:debug`, `:info`, `:warn`, `:error`, `:fatal`, and `:unknown`, corresponding to the log level numbers from 0 up to 5 respectively. To change the default log level, use

```ruby
config.log_level = :warn # In any environment initializer, or
Rails.logger.level = 0 # at any time
```

This is useful when you want to log under development or staging, but you don't want to flood your production log with unnecessary information.

TIP: The default Rails log level is `info` in production mode and `debug` in development and test mode.

### Sending Messages

To write in the current log use the `logger.(debug|info|warn|error|fatal)` method from within a controller, model or mailer:

```ruby
logger.debug "Person attributes hash: #{@person.attributes.inspect}"
logger.info "Processing the request..."
logger.fatal "Terminating application, raised unrecoverable error!!!"
```

Here's an example of a method instrumented with extra logging:

```ruby
class PostsController < ApplicationController
  # ...

  def create
    @post = Post.new(params[:post])
    logger.debug "New post: #{@post.attributes.inspect}"
    logger.debug "Post should be valid: #{@post.valid?}"

    if @post.save
      flash[:notice] = 'Post was successfully created.'
      logger.debug "The post was saved and now the user is going to be redirected..."
      redirect_to(@post)
    else
      render action: "new"
    end
  end

  # ...
end
```

Here's an example of the log generated when this controller action is executed:

```
Processing PostsController#create (for 127.0.0.1 at 2008-09-08 11:52:54) [POST]
  Session ID: BAh7BzoMY3NyZl9pZCIlMDY5MWU1M2I1ZDRjODBlMzkyMWI1OTg2NWQyNzViZjYiCmZsYXNoSUM6J0FjdGl
vbkNvbnRyb2xsZXI6OkZsYXNoOjpGbGFzaEhhc2h7AAY6CkB1c2VkewA=--b18cd92fba90eacf8137e5f6b3b06c4d724596a4
  Parameters: {"commit"=>"Create", "post"=>{"title"=>"Debugging Rails",
 "body"=>"I'm learning how to print in logs!!!", "published"=>"0"},
 "authenticity_token"=>"2059c1286e93402e389127b1153204e0d1e275dd", "action"=>"create", "controller"=>"posts"}
New post: {"updated_at"=>nil, "title"=>"Debugging Rails", "body"=>"I'm learning how to print in logs!!!",
 "published"=>false, "created_at"=>nil}
Post should be valid: true
  Post Create (0.000443)   INSERT INTO "posts" ("updated_at", "title", "body", "published",
 "created_at") VALUES('2008-09-08 14:52:54', 'Debugging Rails',
 'I''m learning how to print in logs!!!', 'f', '2008-09-08 14:52:54')
The post was saved and now the user is going to be redirected...
Redirected to #<Post:0x20af760>
Completed in 0.01224 (81 reqs/sec) | DB: 0.00044 (3%) | 302 Found [http://localhost/posts]
```

Adding extra logging like this makes it easy to search for unexpected or unusual behavior in your logs. If you add extra logging, be sure to make sensible use of log levels to avoid filling your production logs with useless trivia.

### Tagged Logging

When running multi-user, multi-account applications, it’s often useful
to be able to filter the logs using some custom rules. `TaggedLogging`
in Active Support helps in doing exactly that by stamping log lines with subdomains, request ids, and anything else to aid debugging such applications.

```ruby
logger = ActiveSupport::TaggedLogging.new(Logger.new(STDOUT))
logger.tagged("BCX") { logger.info "Stuff" }                            # Logs "[BCX] Stuff"
logger.tagged("BCX", "Jason") { logger.info "Stuff" }                   # Logs "[BCX] [Jason] Stuff"
logger.tagged("BCX") { logger.tagged("Jason") { logger.info "Stuff" } } # Logs "[BCX] [Jason] Stuff"
```

Debugging with the `debugger` gem
---------------------------------

When your code is behaving in unexpected ways, you can try printing to logs or the console to diagnose the problem. Unfortunately, there are times when this sort of error tracking is not effective in finding the root cause of a problem. When you actually need to journey into your running source code, the debugger is your best companion.

The debugger can also help you if you want to learn about the Rails source code but don't know where to start. Just debug any request to your application and use this guide to learn how to move from the code you have written deeper into Rails code.

### Setup

You can use the `debugger` gem to set breakpoints and step through live code in Rails. To install it, just run:

```bash
$ gem install debugger
```

Rails has had built-in support for debugging since Rails 2.0. Inside any Rails application you can invoke the debugger by calling the `debugger` method.

Here's an example:

```ruby
class PeopleController < ApplicationController
  def new
    debugger
    @person = Person.new
  end
end
```

If you see this message in the console or logs:

```
***** Debugger requested, but was not available: Start server with --debugger to enable *****
```

Make sure you have started your web server with the option `--debugger`:

```bash
$ rails server --debugger
=> Booting WEBrick
=> Rails 4.0.0 application starting on http://0.0.0.0:3000
=> Debugger enabled
...
```

TIP: In development mode, you can dynamically `require \'debugger\'` instead of restarting the server, even if it was started without `--debugger`.

### The Shell

As soon as your application calls the `debugger` method, the debugger will be started in a debugger shell inside the terminal window where you launched your application server, and you will be placed at the debugger's prompt `(rdb:n)`. The _n_ is the thread number. The prompt will also show you the next line of code that is waiting to run.

If you got there by a browser request, the browser tab containing the request will be hung until the debugger has finished and the trace has finished processing the entire request.

For example:

```bash
@posts = Post.all
(rdb:7)
```

Now it's time to explore and dig into your application. A good place to start is by asking the debugger for help. Type: `help`

```
(rdb:7) help
ruby-debug help v0.10.2
Type 'help <command-name>' for help on a specific command

Available commands:
backtrace  delete   enable  help    next  quit     show    trace
break      disable  eval    info    p     reload   source  undisplay
catch      display  exit    irb     pp    restart  step    up
condition  down     finish  list    ps    save     thread  var
continue   edit     frame   method  putl  set      tmate   where
```

TIP: To view the help menu for any command use `help <command-name>` at the debugger prompt. For example: _`help var`_

The next command to learn is one of the most useful: `list`. You can abbreviate any debugging command by supplying just enough letters to distinguish them from other commands, so you can also use `l` for the `list` command.

This command shows you where you are in the code by printing 10 lines centered around the current line; the current line in this particular case is line 6 and is marked by `=>`.

```
(rdb:7) list
[1, 10] in /PathTo/project/app/controllers/posts_controller.rb
   1  class PostsController < ApplicationController
   2    # GET /posts
   3    # GET /posts.json
   4    def index
   5      debugger
=> 6      @posts = Post.all
   7
   8      respond_to do |format|
   9        format.html # index.html.erb
   10        format.json { render :json => @posts }
```

If you repeat the `list` command, this time using just `l`, the next ten lines of the file will be printed out.

```
(rdb:7) l
[11, 20] in /PathTo/project/app/controllers/posts_controller.rb
   11      end
   12    end
   13
   14    # GET /posts/1
   15    # GET /posts/1.json
   16    def show
   17      @post = Post.find(params[:id])
   18
   19      respond_to do |format|
   20        format.html # show.html.erb
```

And so on until the end of the current file. When the end of file is reached, the `list` command will start again from the beginning of the file and continue again up to the end, treating the file as a circular buffer.

On the other hand, to see the previous ten lines you should type `list-` (or `l-`)

```
(rdb:7) l-
[1, 10] in /PathTo/project/app/controllers/posts_controller.rb
   1  class PostsController < ApplicationController
   2    # GET /posts
   3    # GET /posts.json
   4    def index
   5      debugger
   6      @posts = Post.all
   7
   8      respond_to do |format|
   9        format.html # index.html.erb
   10        format.json { render :json => @posts }
```

This way you can move inside the file, being able to see the code above and over the line you added the `debugger`.
Finally, to see where you are in the code again you can type `list=`

```
(rdb:7) list=
[1, 10] in /PathTo/project/app/controllers/posts_controller.rb
   1  class PostsController < ApplicationController
   2    # GET /posts
   3    # GET /posts.json
   4    def index
   5      debugger
=> 6      @posts = Post.all
   7
   8      respond_to do |format|
   9        format.html # index.html.erb
   10        format.json { render :json => @posts }
```

### The Context

When you start debugging your application, you will be placed in different contexts as you go through the different parts of the stack.

The debugger creates a context when a stopping point or an event is reached. The context has information about the suspended program which enables a debugger to inspect the frame stack, evaluate variables from the perspective of the debugged program, and contains information about the place where the debugged program is stopped.

At any time you can call the `backtrace` command (or its alias `where`) to print the backtrace of the application. This can be very helpful to know how you got where you are. If you ever wondered about how you got somewhere in your code, then `backtrace` will supply the answer.

```
(rdb:5) where
    #0 PostsController.index
       at line /PathTo/project/app/controllers/posts_controller.rb:6
    #1 Kernel.send
       at line /PathTo/project/vendor/rails/actionpack/lib/action_controller/base.rb:1175
    #2 ActionController::Base.perform_action_without_filters
       at line /PathTo/project/vendor/rails/actionpack/lib/action_controller/base.rb:1175
    #3 ActionController::Filters::InstanceMethods.call_filters(chain#ActionController::Fil...,...)
       at line /PathTo/project/vendor/rails/actionpack/lib/action_controller/filters.rb:617
...
```

You move anywhere you want in this trace (thus changing the context) by using the `frame _n_` command, where _n_ is the specified frame number.

```
(rdb:5) frame 2
#2 ActionController::Base.perform_action_without_filters
       at line /PathTo/project/vendor/rails/actionpack/lib/action_controller/base.rb:1175
```

The available variables are the same as if you were running the code line by line. After all, that's what debugging is.

Moving up and down the stack frame: You can use `up [n]` (`u` for abbreviated) and `down [n]` commands in order to change the context _n_ frames up or down the stack respectively. _n_ defaults to one. Up in this case is towards higher-numbered stack frames, and down is towards lower-numbered stack frames.

### Threads

The debugger can list, stop, resume and switch between running threads by using the command `thread` (or the abbreviated `th`). This command has a handful of options:

* `thread` shows the current thread.
* `thread list` is used to list all threads and their statuses. The plus + character and the number indicates the current thread of execution.
* `thread stop _n_` stop thread _n_.
* `thread resume _n_` resumes thread _n_.
* `thread switch _n_` switches the current thread context to _n_.

This command is very helpful, among other occasions, when you are debugging concurrent threads and need to verify that there are no race conditions in your code.

### Inspecting Variables

Any expression can be evaluated in the current context. To evaluate an expression, just type it!

This example shows how you can print the instance_variables defined within the current context:

```
@posts = Post.all
(rdb:11) instance_variables
["@_response", "@action_name", "@url", "@_session", "@_cookies", "@performed_render", "@_flash", "@template", "@_params", "@before_filter_chain_aborted", "@request_origin", "@_headers", "@performed_redirect", "@_request"]
```

As you may have figured out, all of the variables that you can access from a controller are displayed. This list is dynamically updated as you execute code. For example, run the next line using `next` (you'll learn more about this command later in this guide).

```
(rdb:11) next
Processing PostsController#index (for 127.0.0.1 at 2008-09-04 19:51:34) [GET]
  Session ID: BAh7BiIKZmxhc2hJQzonQWN0aW9uQ29udHJvbGxlcjo6Rmxhc2g6OkZsYXNoSGFzaHsABjoKQHVzZWR7AA==--b16e91b992453a8cc201694d660147bba8b0fd0e
  Parameters: {"action"=>"index", "controller"=>"posts"}
/PathToProject/posts_controller.rb:8
respond_to do |format|
```

And then ask again for the instance_variables:

```
(rdb:11) instance_variables.include? "@posts"
true
```

Now `@posts` is included in the instance variables, because the line defining it was executed.

TIP: You can also step into **irb** mode with the command `irb` (of course!). This way an irb session will be started within the context you invoked it. But be warned: this is an experimental feature.

The `var` method is the most convenient way to show variables and their values:

```
var
(rdb:1) v[ar] const <object>            show constants of object
(rdb:1) v[ar] g[lobal]                  show global variables
(rdb:1) v[ar] i[nstance] <object>       show instance variables of object
(rdb:1) v[ar] l[ocal]                   show local variables
```

This is a great way to inspect the values of the current context variables. For example:

```
(rdb:9) var local
  __dbg_verbose_save => false
```

You can also inspect for an object method this way:

```
(rdb:9) var instance Post.new
@attributes = {"updated_at"=>nil, "body"=>nil, "title"=>nil, "published"=>nil, "created_at"...
@attributes_cache = {}
@new_record = true
```

TIP: The commands `p` (print) and `pp` (pretty print) can be used to evaluate Ruby expressions and display the value of variables to the console.

You can use also `display` to start watching variables. This is a good way of tracking the values of a variable while the execution goes on.

```
(rdb:1) display @recent_comments
1: @recent_comments =
```

The variables inside the displaying list will be printed with their values after you move in the stack. To stop displaying a variable use `undisplay _n_` where _n_ is the variable number (1 in the last example).

### Step by Step

Now you should know where you are in the running trace and be able to print the available variables. But lets continue and move on with the application execution.

Use `step` (abbreviated `s`) to continue running your program until the next logical stopping point and return control to the debugger.

TIP: You can also use `step+ n` and `step- n` to move forward or backward `n` steps respectively.

You may also use `next` which is similar to step, but function or method calls that appear within the line of code are executed without stopping. As with step, you may use plus sign to move _n_ steps.

The difference between `next` and `step` is that `step` stops at the next line of code executed, doing just a single step, while `next` moves to the next line without descending inside methods.

For example, consider this block of code with an included `debugger` statement:

```ruby
class Author < ActiveRecord::Base
  has_one :editorial
  has_many :comments

  def find_recent_comments(limit = 10)
    debugger
    @recent_comments ||= comments.where("created_at > ?", 1.week.ago).limit(limit)
  end
end
```

TIP: You can use the debugger while using `rails console`. Just remember to `require "debugger"` before calling the `debugger` method.

```
$ rails console
Loading development environment (Rails 4.0.0)
>> require "debugger"
=> []
>> author = Author.first
=> #<Author id: 1, first_name: "Bob", last_name: "Smith", created_at: "2008-07-31 12:46:10", updated_at: "2008-07-31 12:46:10">
>> author.find_recent_comments
/PathTo/project/app/models/author.rb:11
)
```

With the code stopped, take a look around:

```
(rdb:1) list
[2, 9] in /PathTo/project/app/models/author.rb
   2    has_one :editorial
   3    has_many :comments
   4
   5    def find_recent_comments(limit = 10)
   6      debugger
=> 7      @recent_comments ||= comments.where("created_at > ?", 1.week.ago).limit(limit)
   8    end
   9  end
```

You are at the end of the line, but... was this line executed? You can inspect the instance variables.

```
(rdb:1) var instance
@attributes = {"updated_at"=>"2008-07-31 12:46:10", "id"=>"1", "first_name"=>"Bob", "las...
@attributes_cache = {}
```

`@recent_comments` hasn't been defined yet, so it's clear that this line hasn't been executed yet. Use the `next` command to move on in the code:

```
(rdb:1) next
/PathTo/project/app/models/author.rb:12
@recent_comments
(rdb:1) var instance
@attributes = {"updated_at"=>"2008-07-31 12:46:10", "id"=>"1", "first_name"=>"Bob", "las...
@attributes_cache = {}
@comments = []
@recent_comments = []
```

Now you can see that the `@comments` relationship was loaded and @recent_comments defined because the line was executed.

If you want to go deeper into the stack trace you can move single `steps`, through your calling methods and into Rails code. This is one of the best ways to find bugs in your code, or perhaps in Ruby or Rails.

### Breakpoints

A breakpoint makes your application stop whenever a certain point in the program is reached. The debugger shell is invoked in that line.

You can add breakpoints dynamically with the command `break` (or just `b`). There are 3 possible ways of adding breakpoints manually:

* `break line`: set breakpoint in the _line_ in the current source file.
* `break file:line [if expression]`: set breakpoint in the _line_ number inside the _file_. If an _expression_ is given it must evaluated to _true_ to fire up the debugger.
* `break class(.|\#)method [if expression]`: set breakpoint in _method_ (. and \# for class and instance method respectively) defined in _class_. The _expression_ works the same way as with file:line.

```
(rdb:5) break 10
Breakpoint 1 file /PathTo/project/vendor/rails/actionpack/lib/action_controller/filters.rb, line 10
```

Use `info breakpoints _n_` or `info break _n_` to list breakpoints. If you supply a number, it lists that breakpoint. Otherwise it lists all breakpoints.

```
(rdb:5) info breakpoints
Num Enb What
  1 y   at filters.rb:10
```

To delete breakpoints: use the command `delete _n_` to remove the breakpoint number _n_. If no number is specified, it deletes all breakpoints that are currently active..

```
(rdb:5) delete 1
(rdb:5) info breakpoints
No breakpoints.
```

You can also enable or disable breakpoints:

* `enable breakpoints`: allow a list _breakpoints_ or all of them if no list is specified, to stop your program. This is the default state when you create a breakpoint.
* `disable breakpoints`: the _breakpoints_ will have no effect on your program.

### Catching Exceptions

The command `catch exception-name` (or just `cat exception-name`) can be used to intercept an exception of type _exception-name_ when there would otherwise be is no handler for it.

To list all active catchpoints use `catch`.

### Resuming Execution

There are two ways to resume execution of an application that is stopped in the debugger:

* `continue` [line-specification] \(or `c`): resume program execution, at the address where your script last stopped; any breakpoints set at that address are bypassed. The optional argument line-specification allows you to specify a line number to set a one-time breakpoint which is deleted when that breakpoint is reached.
* `finish` [frame-number] \(or `fin`): execute until the selected stack frame returns. If no frame number is given, the application will run until the currently selected frame returns. The currently selected frame starts out the most-recent frame or 0 if no frame positioning (e.g up, down or frame) has been performed. If a frame number is given it will run until the specified frame returns.

### Editing

Two commands allow you to open code from the debugger into an editor:

* `edit [file:line]`: edit _file_ using the editor specified by the EDITOR environment variable. A specific _line_ can also be given.
* `tmate _n_` (abbreviated `tm`): open the current file in TextMate. It uses n-th frame if _n_ is specified.

### Quitting

To exit the debugger, use the `quit` command (abbreviated `q`), or its alias `exit`.

A simple quit tries to terminate all threads in effect. Therefore your server will be stopped and you will have to start it again.

### Settings

The `debugger` gem can automatically show the code you're stepping through and reload it when you change it in an editor. Here are a few of the available options:

* `set reload`: Reload source code when changed.
* `set autolist`: Execute `list` command on every breakpoint.
* `set listsize _n_`: Set number of source lines to list by default to _n_.
* `set forcestep`: Make sure the `next` and `step` commands always move to a new line

You can see the full list by using `help set`. Use `help set _subcommand_` to learn about a particular `set` command.

TIP: You can save these settings in an `.rdebugrc` file in your home directory. The debugger reads these global settings when it starts.

Here's a good start for an `.rdebugrc`:

```bash
set autolist
set forcestep
set listsize 25
```

Debugging Memory Leaks
----------------------

A Ruby application (on Rails or not), can leak memory - either in the Ruby code or at the C code level.

In this section, you will learn how to find and fix such leaks by using tool such as Valgrind.

### Valgrind

[Valgrind](http://valgrind.org/) is a Linux-only application for detecting C-based memory leaks and race conditions.

There are Valgrind tools that can automatically detect many memory management and threading bugs, and profile your programs in detail. For example, a C extension in the interpreter calls `malloc()` but is doesn't properly call `free()`, this memory won't be available until the app terminates.

For further information on how to install Valgrind and use with Ruby, refer to [Valgrind and Ruby](http://blog.evanweaver.com/articles/2008/02/05/valgrind-and-ruby/) by Evan Weaver.

Plugins for Debugging
---------------------

There are some Rails plugins to help you to find errors and debug your application. Here is a list of useful plugins for debugging:

* [Footnotes](https://github.com/josevalim/rails-footnotes) Every Rails page has footnotes that give request information and link back to your source via TextMate.
* [Query Trace](https://github.com/ntalbott/query_trace/tree/master) Adds query origin tracing to your logs.
* [Query Reviewer](https://github.com/nesquena/query_reviewer) This rails plugin not only runs "EXPLAIN" before each of your select queries in development, but provides a small DIV in the rendered output of each page with the summary of warnings for each query that it analyzed.
* [Exception Notifier](https://github.com/smartinez87/exception_notification/tree/master) Provides a mailer object and a default set of templates for sending email notifications when errors occur in a Rails application.
* [Better Errors](https://github.com/charliesome/better_errors) Replaces the standard Rails error page with a new one containing more contextual information, like source code and variable inspection.
* [RailsPanel](https://github.com/dejan/rails_panel) Chrome extension for Rails development that will end your tailing of development.log. Have all information about your Rails app requests in the browser - in the Developer Tools panel. Provides insight to db/rendering/total times, parameter list, rendered views and more.

References
----------

* [ruby-debug Homepage](http://bashdb.sourceforge.net/ruby-debug/home-page.html)
* [debugger Homepage](https://github.com/cldwalker/debugger)
* [Article: Debugging a Rails application with ruby-debug](http://www.sitepoint.com/debug-rails-app-ruby-debug/)
* [Ryan Bates' debugging ruby (revised) screencast](http://railscasts.com/episodes/54-debugging-ruby-revised)
* [Ryan Bates' stack trace screencast](http://railscasts.com/episodes/24-the-stack-trace)
* [Ryan Bates' logger screencast](http://railscasts.com/episodes/56-the-logger)
* [Debugging with ruby-debug](http://bashdb.sourceforge.net/ruby-debug.html)
