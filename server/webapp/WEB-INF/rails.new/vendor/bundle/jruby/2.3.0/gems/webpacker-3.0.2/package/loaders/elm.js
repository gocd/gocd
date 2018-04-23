const { resolve } = require('path')

const elmSource = resolve(process.cwd())
const elmMake = `${elmSource}/node_modules/.bin/elm-make`
const elmDefaultOptions = `cwd=${elmSource}&pathToMake=${elmMake}`

const loaderOptions = () => {
  if (process.env.NODE_ENV === 'production') {
    return `elm-webpack-loader?${elmDefaultOptions}`
  }

  return `elm-hot-loader!elm-webpack-loader?${elmDefaultOptions}&verbose=true&warn=true&debug=true`
}

module.exports = {
  test: /\.elm(\.erb)?$/,
  exclude: [/elm-stuff/, /node_modules/],
  loader: loaderOptions()
}
