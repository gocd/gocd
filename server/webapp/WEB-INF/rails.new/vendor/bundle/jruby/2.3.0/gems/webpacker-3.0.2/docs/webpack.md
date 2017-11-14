# Webpack


## Configuration

Webpacker gives you a default set of configuration files for test, development and
production environments in `config/webpack/*.js`. You can configure each individual
environment in their respective files or configure them all in the base
`config/webpack/environment.js` file.

By default, you don't need to make any changes to `config/webpack/*.js`
files since it's all standard production-ready configuration. However,
if you do need to customize or add a new loader, this is where you would go.

Here is how you can modify webpack configuration:

```js
// config/webpack/custom.js
module.exports = {
  resolve: {
    alias: {
      jquery: 'jquery/src/jquery',
      vue: 'vue/dist/vue.js',
      React: 'react',
      ReactDOM: 'react-dom',
      vue_resource: 'vue-resource/dist/vue-resource',
    }
  }
}

// config/webpack/development.js
const merge = require('webpack-merge')
const environment = require('./environment')
const customConfig = require('./custom')

module.exports = merge(environment.toWebpackConfig(), customConfig)
```

If you need access to configs within Webpacker's configuration, you can import them like this:
```js
const config = require('@rails/webpacker/package/config');
const asset_host = require('@rails/webpacker/package/asset_host');

console.log(asset_host.publicPathWithHost);
```

**Note:** You will have to merge custom config to all env where you want that config
to be available. In above case, it will be applied to development environment.


## Loaders

You can add additional loaders beyond the base set that webpacker provides by
adding it to your environment. We'll use `json-loader` as an example:

```
yarn add json-loader
```

```js
// config/webpack/environment.js
const { environment } = require('@rails/webpacker')

environment.loaders.set('json', {
  test: /\.json$/,
  use: 'json-loader'
})

module.exports = environment
```

Finally add `.json` to the list of extensions in `config/webpacker.yml`. Now if you `import()` any `.json` files inside your javascript
they will be processed using `json-loader`. Voila!

You can also modify the loaders that webpacker pre-configures for you. We'll update
the `babel` loader as an example:

```js
// config/webpack/environment.js
const { environment } = require('@rails/webpacker')

// Update an option directly
const babelLoader = environment.loaders.get('babel')
babelLoader.options.cacheDirectory = false

module.exports = environment
```

### Overriding Loader Options in Webpack 3+ (for CSS Modules etc.)

In Webpack 3+, if you'd like to specify additional or different options for a loader, edit `config/webpack/environment.js` and provide an options object to override. This is similar to the technique shown above, but the following example shows specifically how to apply CSS Modules, which is what you may be looking for:

```javascript
const { environment } = require('@rails/webpacker')
const merge = require('webpack-merge')

const myCssLoaderOptions = {
  modules: true,
  sourceMap: true,
  localIdentName: '[name]__[local]___[hash:base64:5]'
}

const CSSLoader = environment.loaders.get('style').use.find(el => el.loader === 'css-loader')

CSSLoader.options = merge(CSSLoader.options, myCssLoaderOptions)

module.exports = environment
```

See [issue #756](https://github.com/rails/webpacker/issues/756#issuecomment-327148547) for additional discussion of this.

For this to work, don't forget to use the `stylesheet_pack_tag`, for example:

```
<%= stylesheet_pack_tag 'YOUR_PACK_NAME_HERE' %>
```

## Plugins

The process for adding or modifying webpack plugins is the same as the process
for loaders above:

```js
// config/webpack/environment.js
const { environment } = require('@rails/webpacker')
const webpack = require('webpack')

// Get a pre-configured plugin
environment.plugins.get('ExtractText') // Is an ExtractTextPlugin instance

// Add an additional plugin of your choosing : ProvidePlugin
environment.plugins.set(
  'Provide',
  new webpack.ProvidePlugin({
    $: 'jquery',
    jQuery: 'jquery',
    jquery: 'jquery',
    'window.Tether': 'tether',
    Popper: ['popper.js', 'default'],
    ActionCable: 'actioncable',
    Vue: 'vue',
    VueResource: 'vue-resource',
  })
)

module.exports = environment
```


### Add common chunks

The CommonsChunkPlugin is an opt-in feature that creates a separate file (known as a chunk), consisting of common modules shared between multiple entry points. By separating common modules from bundles, the resulting chunked file can be loaded once initially, and stored in the cache for later use. This results in page speed optimizations as the browser can quickly serve the shared code from the cache, rather than being forced to load a larger bundle whenever a new page is visited.

Add the plugins in `config/webpack/environment.js`:

```js
const webpack = require('webpack')

environment.plugins.set(
  'CommonsChunkVendor',
  new webpack.optimize.CommonsChunkPlugin({
    name: 'vendor',
    minChunks: (module) => {
      // this assumes your vendor imports exist in the node_modules directory
      return module.context && module.context.indexOf('node_modules') !== -1;
    }
  })
)

environment.plugins.set(
  'CommonsChunkManifest',
  new webpack.optimize.CommonsChunkPlugin({
    name: 'manifest',
    minChunks: Infinity
  })
)
```

Now, add these files to your `layouts/application.html.erb`:

```erb
<%# Head %>

<%= javascript_pack_tag "manifest" %>
<%= javascript_pack_tag "vendor" %>

<%# If importing any styles from node_modules in your JS app %>

<%= stylesheet_pack_tag "vendor" %>
```

More detailed guides available here: [Webpack guides](https://webpack.js.org/guides/)
