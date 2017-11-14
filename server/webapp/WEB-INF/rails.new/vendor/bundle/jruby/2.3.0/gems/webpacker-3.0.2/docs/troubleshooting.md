# Troubleshooting


## ENOENT: no such file or directory - node-sass

*  If you get this error `ENOENT: no such file or directory - node-sass` on Heroku
or elsewhere during `assets:precompile` or `bundle exec rails webpacker:compile`
then you would need to rebuild node-sass. It's a bit of a weird error;
basically, it can't find the `node-sass` binary.
An easy solution is to create a postinstall hook - `npm rebuild node-sass` in
`package.json` and that will ensure `node-sass` is rebuilt whenever
you install any new modules.


## Can't find hello_react.js in manifest.json

* If you get this error `Can't find hello_react.js in manifest.json`
when loading a view in the browser it's because Webpack is still compiling packs.
Webpacker uses a `manifest.json` file to keep track of packs in all environments,
however since this file is generated after packs are compiled by webpack. So,
if you load a view in browser whilst webpack is compiling you will get this error.
Therefore, make sure webpack
(i.e `./bin/webpack-dev-server`) is running and has
completed the compilation successfully before loading a view.


## throw er; // Unhandled 'error' event

* If you get this error while trying to use Elm, try rebuilding Elm. You can do
  so with a postinstall hook in your `package.json`:

```
"scripts": {
  "postinstall": "npm rebuild elm"
}
```


## webpack or webpack-dev-server not found

* This could happen if  `webpacker:install` step is skipped. Please run `bundle exec rails webpacker:install` to fix the issue.

* If you encounter the above error on heroku after upgrading from Rails 4.x to 5.1.x, then the problem might be related to missing `yarn` binstub. Please run following commands to update/add binstubs:

```bash
bundle config --delete bin
./bin/rails app:update:bin # or rails app:update:bin
```


## Running Webpack on Windows

If you are running Webpack on Windows, your command shell may not be able to interpret the preferred interpreter
for the scripts generated in `bin/webpack` and `bin/webpack-dev-server`. Instead you'll want to run the scripts
manually with Ruby:

```
C:\path>ruby bin\webpack
C:\path>ruby bin\webpack-dev-server
```


## Invalid configuration object. Webpack has been initialised using a configuration object that does not match the API schema.

If you receive this error when running `$ ./bin/webpack-dev-server` ensure your configuration is correct; most likely the path to your "packs" folder is incorrect if you modified from the original "source_path" defined in `config/webpacker.yml`.
