# Typescript


## Typescript with React

1. Setup react using webpacker [react installer](#react). Then add required depedencies
for using typescript with React:

```bash
yarn add ts-loader typescript @types/react @types/react-dom
```

2. Add a `tsconfig.json` to project root:

``` json
{
  "compilerOptions": {
    "declaration": false,
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true,
    "lib": ["es6", "dom"],
    "module": "es6",
    "moduleResolution": "node",
    "sourceMap": true,
    "jsx": "react",
    "target": "es5"
  },
  "exclude": [
    "**/*.spec.ts",
    "node_modules",
    "vendor",
    "public"
  ],
  "compileOnSave": false
}
```

3. Finally add `.tsx` to the list of extensions in `config/webpacker.yml`
and rename your generated `hello_react.js` using react installer
to `hello_react.tsx` and make it valid typescript and now you can use
typescript, JSX with React.



## HTML templates with Typescript and Angular

After you have installed angular using `bundle exec rails webpacker:install:angular`
you would need to follow these steps to add HTML templates support:

1. Use `yarn` to add html-loader

```bash
yarn add html-loader
```

2. Add html-loader to `config/webpack/environment.js`

```js
environment.loaders.set('html', {
  test: /\.html$/,
  use: [{
    loader: 'html-loader',
    options: {
      minimize: true,
      removeAttributeQuotes: false,
      caseSensitive: true,
      customAttrSurround: [ [/#/, /(?:)/], [/\*/, /(?:)/], [/\[?\(?/, /(?:)/] ],
      customAttrAssign: [ /\)?\]?=/ ]
    }
  }]
})
```

3. Add `.html` to `config/webpacker.yml`

```yml
  extensions:
    - .elm
    - .coffee
    - .html
```

4. Setup a custom `d.ts` definition

```ts
// app/javascript/hello_angular/html.d.ts

declare module "*.html" {
  const content: string
  export default content
}
```

5. Add a template.html file relative to `app.component.ts`

```html
<h1>Hello {{name}}</h1>
```

6. Import template into `app.component.ts`

```ts
import { Component } from '@angular/core'
import templateString from './template.html'

@Component({
  selector: 'hello-angular',
  template: templateString
})

export class AppComponent {
  name = 'Angular!'
}
```

That's all. Voila!
