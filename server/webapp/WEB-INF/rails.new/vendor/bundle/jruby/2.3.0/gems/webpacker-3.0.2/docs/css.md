# CSS, SASS and SCSS


Webpacker supports importing css, sass and scss files directly into your javascript files.


## Import styles into your JS app

```sass
// app/javascript/hello_react/styles/hello-react.sass

.hello-react
  padding: 20px
  font-size: 12px
```

```js
// React component example
// app/javascripts/packs/hello_react.jsx

import React from 'react'
import helloIcon from '../hello_react/images/icon.png'
import '../hello_react/styles/hello-react'

const Hello = props => (
  <div className="hello-react">
    <img src={helloIcon} alt="hello-icon" />
    <p>Hello {props.name}!</p>
  </div>
)
```


## Link styles from your Rails views

Under the hood webpack uses
[extract-text-webpack-plugin](https://github.com/webpack-contrib/extract-text-webpack-plugin) plugin to extract all the referenced styles within your app and compile it into
a separate `[pack_name].css` bundle so that in your view you can use the
`stylesheet_pack_tag` helper.

```erb
<%= stylesheet_pack_tag 'hello_react' %>
```


## Add bootstrap

You can use yarn to add bootstrap or any other modules available on npm:

```bash
yarn add bootstrap
```

Import Bootstrap and theme (optional) CSS in your app/javascript/packs/app.js file:

```js
import 'bootstrap/dist/css/bootstrap'
import 'bootstrap/dist/css/bootstrap-theme'
```

Or in your app/javascript/app.sass file:

```sass
// ~ to tell that this is not a relative import

@import '~bootstrap/dist/css/bootstrap'
@import '~bootstrap/dist/css/bootstrap-theme'
```


## Post-Processing CSS

Webpacker out-of-the-box provides CSS post-processing using
[postcss-loader](https://github.com/postcss/postcss-loader)
and the installer sets up a standard `.postcssrc.yml`
file in your app root with standard plugins.

```yml
plugins:
  postcss-smart-import: {}
  postcss-cssnext: {}
```
