package com.github.tmr232.function_graph_overview.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class SettingsComponent {
    val panel: JPanel
    private val myFlatSwitch = JBCheckBox("Flat switch")
    private val mySimplify = JBCheckBox("Simplify CFG")
    private val myHighlight = JBCheckBox("Highlight current node")
    private val myColorScheme = JBTextField("dark")

    init {
        panel =
            FormBuilder
                .createFormBuilder()
                .addComponent(myFlatSwitch, 1)
                .addComponent(mySimplify, 1)
                .addComponent(myHighlight, 1)
                .addLabeledComponent(JBLabel("Color scheme"), myColorScheme, 1, false)
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    val preferredFocusedComponent: JComponent
        get() = myFlatSwitch

    var flatSwitch: Boolean
        get() = myFlatSwitch.isSelected
        set(newStatus) {
            myFlatSwitch.isSelected = newStatus
        }

    var simplify: Boolean
        get() = mySimplify.isSelected
        set(newStatus) {
            mySimplify.isSelected = newStatus
        }
    var highlight: Boolean
        get() = myHighlight.isSelected
        set(newStatus) {
            myHighlight.isSelected = newStatus
        }
    var colorScheme: String
        get() = myColorScheme.text
        set(newText) {
            myColorScheme.text = newText
        }
}
