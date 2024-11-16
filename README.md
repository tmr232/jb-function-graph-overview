# Function Graph Overview (for JetBrains IDEs)

![Build](https://github.com/tmr232/jb-sandbox/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/25676.svg)](https://plugins.jetbrains.com/plugin/25676)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25676.svg)](https://plugins.jetbrains.com/plugin/25676)

<!--
## Template ToDo list
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
-->

<!-- Plugin description -->
See live control-flow-graphs of your code!

This plugin adds a CFG ([Control-Flow-Graph](https://en.wikipedia.org/wiki/Control-flow_graph))
view for the current function.

Before installing, you can also try an [interactive demo](https://tmr232.github.io/function-graph-overview/).

The plugin currently supports Python, C, and Go. 

<!-- Plugin description end -->


| ![A docked CFG view](/media/screenshot-docked.png) | ![An undocked CFG view](/media/screenshot-undocked.png) |
|----------------------------------------------------|---------------------------------------------------------|

This is a port of the [Function-Graph-Overview](https://github.com/tmr232/function-graph-overview/) to JetBrains IDEs.


## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Function Graph Overview"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/25676) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/25676/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
