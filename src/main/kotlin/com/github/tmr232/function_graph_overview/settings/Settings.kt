// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.github.tmr232.function_graph_overview.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.NonNls

interface SettingsListener {
    fun settingsChanged()

    companion object {
        val TOPIC: Topic<SettingsListener> = Topic.create("FunctionGraphOverview settings changed", SettingsListener::class.java)
    }
}

@State(name = "org.intellij.sdk.settings.AppSettings", storages = [Storage("FunctionGraphOverview.xml")])
internal class Settings : PersistentStateComponent<Settings.State> {
    internal class State {
        var flatSwitch: Boolean = false
        var simplify: Boolean = true
        var highlight: Boolean = true
        var colorScheme: @NonNls String = ""
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: Settings
            get() =
                ApplicationManager
                    .getApplication()
                    .getService(Settings::class.java)

        val flatSwitch: Boolean
            get() = instance.state.flatSwitch
        val simplify: Boolean
            get() = instance.state.simplify
        val highlight: Boolean
            get() = instance.state.highlight
        val colorScheme: String
            get() = instance.state.colorScheme
    }
}
