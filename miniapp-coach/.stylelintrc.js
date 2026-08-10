module.exports = {
  extends: ['stylelint-config-standard-scss'],
  plugins: [],
  rules: {
    'selector-class-pattern': null,
    'scss/dollar-variable-pattern': null,
    'scss/at-mixin-pattern': null,
    'property-no-vendor-prefix': null,
    'value-no-vendor-prefix': null,
    'unit-no-unknown': [true, { ignoreUnits: ['rpx'] }],
    'color-hex-length': null,
    'color-function-notation': null,
    'alpha-value-notation': null,
    'scss/load-partial-extension': null,
    'selector-type-no-unknown': [true, { ignoreTypes: ['page'] }],
    'font-family-name-quotes': null,
    'scss/dollar-variable-empty-line-before': null
  }
}
