# Deployment


Webpacker hooks up a new `webpacker:compile` task to `assets:precompile`, which gets run whenever you run `assets:precompile`. If you are not using sprockets you
can manually trigger `bundle exec rails webpacker:compile` during your app deploy.

The `javascript_pack_tag` and `stylesheet_pack_tag` helper method will automatically insert the correct HTML tag for compiled pack. Just like the asset pipeline does it.

By default the output will look like this in different environments:

```html
  <!-- In development mode with webpack-dev-server -->
  <script src="http://localhost:8080/calendar-0bd141f6d9360cf4a7f5.js"></script>
  <link rel="stylesheet" media="screen" href="http://localhost:8080/calendar-dc02976b5f94b507e3b6.css">
  <!-- In production or development mode -->
  <script src="/packs/calendar-0bd141f6d9360cf4a7f5.js"></script>
  <link rel="stylesheet" media="screen" href="/packs/calendar-dc02976b5f94b507e3b6.css">
```


## Heroku

Heroku installs yarn and node by default if you deploy a rails app with
Webpacker so all you would need to do:

```bash
heroku create shiny-webpacker-app
heroku addons:create heroku-postgresql:hobby-dev
git push heroku master
```


## CDN

Webpacker out-of-the-box provides CDN support using your Rails app `config.action_controller.asset_host` setting. If you already have [CDN](http://guides.rubyonrails.org/asset_pipeline.html#cdns) added in your rails app
you don't need to do anything extra for webpacker, it just works.
