package org.lupus.commands.core.scanner

import org.lupus.commands.core.scanner.modifiers.FieldsModifier
import org.lupus.commands.core.scanner.modifiers.ParameterModifier
import org.lupus.commands.core.scanner.modifiers.any.*
import org.lupus.commands.core.scanner.modifiers.clazz.ContinuousMod
import org.lupus.commands.core.scanner.modifiers.clazz.HelpMod
import org.lupus.commands.core.scanner.modifiers.fields.DependencyModifier
import org.lupus.commands.core.scanner.modifiers.method.CMDPassMod
import org.lupus.commands.core.scanner.modifiers.method.DefaultMod
import org.lupus.commands.core.scanner.modifiers.method.FilterPassMod
import org.lupus.commands.core.scanner.modifiers.method.NotCMDMod

object DefaultModifiers {
    val clazzMods = mutableListOf(ContinuousMod, HelpMod)
    val methodMods = mutableListOf(CMDPassMod, DefaultMod, NotCMDMod, DefaultMod, FilterPassMod)
    val anyMods = mutableListOf(
        AliasesMod,
        AsyncMod,
        ConditionsMod,
        CooldownMod,
        CMDNameMod,
        DescMod,
        PermModifier,
        NoPermModifier,
        SyntaxMod,
    )
    val paramModifiers = mutableListOf<ParameterModifier>()
	val fieldsModifier = mutableListOf<FieldsModifier>(DependencyModifier)
}
