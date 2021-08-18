package org.lupus.commands.core.annotations

/**
 * Add alias to command
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Aliases(val aliases: String)
