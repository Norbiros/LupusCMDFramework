package org.lupus.commands.core.data

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.plugin.java.JavaPlugin
import org.lupus.commands.core.annotations.general.NoPerm
import org.lupus.commands.core.annotations.method.CMDPass
import org.lupus.commands.core.annotations.method.Syntax
import org.lupus.commands.core.annotations.parameters.ParamName
import org.lupus.commands.core.arguments.ArgumentType
import org.lupus.commands.core.arguments.ArgumentTypeList
import org.lupus.commands.core.messages.I18n
import org.lupus.commands.core.scanner.ClazzScanner
import org.lupus.commands.core.scanner.modifiers.AnyModifier
import org.lupus.commands.core.scanner.modifiers.ParameterModifier
import java.lang.reflect.Method
import java.lang.reflect.Parameter

open class CommandBuilder(
	var plugin: JavaPlugin,
	var name: String,
	val packageName: String,
	val declaringClazz: Class<*>
) {
	private val pluginClazzLoader: ClassLoader = plugin::class.java.classLoader
	var noCMD: Boolean = false
	var namingSchema: Regex = Regex("Command|CMD")
		set(value) {
			permission = getPerm()
			field = value
		}
    var permission = getPerm()
	private var fullName = name
	var description: String = ""
	var method: Method? = null
	val aliases: MutableList<String> = mutableListOf()
	var syntax = StringBuilder()

	val parameters: MutableList<ArgumentType> = mutableListOf()
	val subCommands: MutableList<CommandBuilder> = mutableListOf()

	var help: Boolean = false
	var async: Boolean = false
	var continuous: Boolean = false


	var supCommand: CommandBuilder? = null
		set(it) {
			if (it == null)
				return
			field = it
			this.permission = getPerm()
		}
	var executorParameter: Parameter? = null
	var paramModifiers: List<ParameterModifier> = mutableListOf()
	var anyModifiers: List<AnyModifier> = mutableListOf()
	val conditions: MutableList<ConditionFun> = mutableListOf()

	private fun getPerm(): String {
		var perm = ""
		val supCommandPrefix = supCommand?.permission ?: ""
		// It's sure to be the last
		var methodName = method?.name ?: ""
		methodName = if(methodName != "") ".$methodName" else ""
		if (supCommandPrefix != "") {
			perm = "$supCommandPrefix$methodName"
			return perm
		}
		val clazzPrefix = declaringClazz.name.removePrefix("$packageName.").replace(namingSchema, "")
		if (perm == "") {
			perm = plugin.name
		}
		if (perm != ""){
			perm += ".$clazzPrefix"
		}
		if (method != null) {
			perm += ".${method!!.name}"
		}

		return perm
	}

	fun addParameter(parameter: Parameter): CommandBuilder {
		val clazz = parameter.type
		val parameterName = parameter.getAnnotation(ParamName::class.java)?.paramName ?: parameter.name

		for (paramModifier in paramModifiers) {
			val ann = parameter.getAnnotation(paramModifier.annotation) ?: continue
			paramModifier.modify(this, ann, parameter)
		}
		for (modifier in anyModifiers) {
			val ann = parameter.getAnnotation(modifier.annotation) ?: continue
			modifier.modify(this, ann, parameter)
		}

		val argumentType = ArgumentTypeList[clazz]
			?: throw IllegalArgumentException("clazz argument isn't ArgumentType")

		// It would be weird if this would be null whilst we're adding parameters
		if (method?.isAnnotationPresent(Syntax::class.java)!!) {
			return this
		}

		if (argumentType.argumentSpan > 1) {
			val argumentNames = argumentType.argumentName.split(',')

			for (i in 0..argumentType.argumentSpan) {
				this.parameters.add(argumentType)
				syntax.append("[${parameterName}.${argumentNames[i]}] ")
			}

		}
		else {
			this.parameters.add(argumentType)

			syntax.append("[${parameterName}] ")
		}
		return this
	}

	fun build(): List<CommandLupi> {

		val subCommands = mutableListOf<CommandLupi>()
		for (subCommand in this.subCommands) {
			if (!continuous && !subCommand.continuous)
				subCommand.fullName =
					"${this.fullName} ${this.syntax} ${subCommand.name}"
						// Replace double space
						.replace("  ", " ")
			subCommands.addAll(subCommand.build())
		}
		if (continuous)
			return subCommands
		var executor: ArgumentType? = null
		if (executorParameter != null)
			executor = ArgumentTypeList[executorParameter!!.type]
		if (subCommands.isNotEmpty()) {
			syntax.append(
				LegacyComponentSerializer
					.legacyAmpersand()
					.serialize(
						I18n[plugin, "sub-name"]
					)
			)
		}

		val builtCommand = CommandLupi(
			name,
			description,
			syntax.toString(),
			aliases,
			subCommands,
			method,
			declaringClazz,
			parameters,
			plugin,
			executor,
			conditions,
			permission,
			fullName,
			help,
			async
		)
		println(" ")
		println(builtCommand.toString())
		println(" ")
		return listOf(
			builtCommand
		)
	}

    fun addPass(pass: String) {
		val subCommand = getCommandPass(method) ?: return
		val cmd = ClazzScanner(plugin, packageName).scan(subCommand,true) ?: return
		cmd.supCommand = this
    	this.subCommands.add(cmd)
	}
	private fun getCommandPass(method: Method?): Class<*>? {
		if (method == null)
			return null
		val cmdPass = method.getAnnotation(CMDPass::class.java)?.commandPath ?: return null
		return pluginClazzLoader.loadClass("$packageName.$cmdPass")
	}

	fun addConditions(conditions: MutableList<ConditionFun>) {
		this.conditions.addAll(conditions)
	}


}