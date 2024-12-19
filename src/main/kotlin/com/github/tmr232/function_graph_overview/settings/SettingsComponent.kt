package com.github.tmr232.function_graph_overview.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel



/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class SettingsComponent {
    val panel: JPanel
    private val myFlatSwitch = JBCheckBox("Flat Switch")
    private val mySimplify = JBCheckBox("Simplify CFG")

    init {
        panel = FormBuilder.createFormBuilder()
            .addComponent(myFlatSwitch, 1)
            .addComponent(mySimplify, 1)
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
}