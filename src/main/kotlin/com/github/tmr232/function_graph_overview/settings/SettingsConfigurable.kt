package com.github.tmr232.function_graph_overview.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent


/**
 * Provides controller functionality for application settings.
 */
internal class SettingsConfigurable : Configurable {
    private var mySettingsComponent: SettingsComponent? = null

    // A default constructor with no arguments is required because
    // this implementation is registered as an applicationConfigurable
    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Function Graph Overview"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return mySettingsComponent?.preferredFocusedComponent
    }

    override fun createComponent(): JComponent? {
        mySettingsComponent = SettingsComponent()
        return mySettingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val state: Settings.State = Settings.instance.state
        return mySettingsComponent?.simplify != state.simplify ||
                mySettingsComponent?.flatSwitch != state.flatSwitch
    }

    override fun apply() {
        val state: Settings.State = Settings.instance.state
        state.simplify = mySettingsComponent?.simplify ?: state.simplify
        state.flatSwitch = mySettingsComponent?.flatSwitch ?: state.flatSwitch
    }

    override fun reset() {
        val state: Settings.State = Settings.instance.state
        mySettingsComponent?.simplify = state.simplify
        mySettingsComponent?.flatSwitch = state.flatSwitch
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}