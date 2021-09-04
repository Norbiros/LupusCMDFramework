package org.lupus.commands.core.scanner.modifiers.clazz

import org.lupus.commands.core.annotations.clazz.HelpCMD
import org.lupus.commands.core.data.CommandBuilder
import org.lupus.commands.core.scanner.modifiers.ClazzModifier

object HelpMod : ClazzModifier(HelpCMD::class.java) {
    override fun modify(cmdBuilder: CommandBuilder, annotation: Annotation, objModified: Class<out Any>) {
        isThisAnnotationValid(annotation)
        cmdBuilder.help = true
    }
}