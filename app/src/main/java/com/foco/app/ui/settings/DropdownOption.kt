package com.foco.app.ui.settings

/** One option of a dropdown: its label and what picking it does. */
data class DropdownOption(
    val label: String,
    val onSelect: () -> Unit,
)
