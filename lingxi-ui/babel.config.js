module.exports = {
  presets: [
    ['@vue/cli-plugin-babel/preset', {
      useBuiltIns: 'entry',
      corejs: 3
    }]
  ],
  plugins: [
    ['transform-imports', {
      'lodash-es': {
        transform: 'lodash-es/${member}',
        preventFullImport: true
      }
    }]
  ],
  'env': {
    'development': {
      'plugins': ['dynamic-import-node']
    }
  }
}