const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlInlineScriptPlugin = require('html-inline-script-webpack-plugin');
const path = require('path');

module.exports = (env, argv) => {
  const mode = argv.mode || 'development';

  return [
    // UI bundle
    {
      name: 'ui',
      mode,
      entry: './src/ui/index.tsx',
      output: {
        filename: 'ui.js',
        path: path.resolve(__dirname, 'dist'),
      },
      module: {
        rules: [
          {
            test: /\.tsx?$/,
            use: 'ts-loader',
            exclude: /node_modules/,
          },
          {
            test: /\.css$/,
            use: [
              'style-loader',
              'css-loader',
              'postcss-loader',
            ],
          },
        ],
      },
      resolve: {
        extensions: ['.tsx', '.ts', '.js', '.jsx'],
        alias: {
          '@': path.resolve(__dirname, 'src'),
        },
      },
      plugins: [
        new HtmlWebpackPlugin({
          template: './src/ui/index.html',
          filename: 'ui.html',
          inject: 'body',
        }),
        new HtmlInlineScriptPlugin({
          htmlMatchPattern: [/ui.html$/],
        }),
      ],
      devtool: mode === 'production' ? false : 'inline-source-map',
    },
    // Plugin code bundle
    {
      name: 'plugin',
      mode,
      entry: './src/plugin/code.ts',
      output: {
        filename: 'code.js',
        path: path.resolve(__dirname, 'dist'),
      },
      module: {
        rules: [
          {
            test: /\.tsx?$/,
            use: {
              loader: 'ts-loader',
              options: {
                configFile: 'tsconfig.code.json',
              },
            },
            exclude: /node_modules/,
          },
        ],
      },
      resolve: {
        extensions: ['.tsx', '.ts', '.js'],
        alias: {
          '@': path.resolve(__dirname, 'src'),
        },
      },
      devtool: mode === 'production' ? false : 'inline-source-map',
    },
  ];
};
