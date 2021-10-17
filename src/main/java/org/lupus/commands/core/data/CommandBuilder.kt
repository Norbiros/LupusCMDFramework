package org.lupus.commands.core.data

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
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
import org.lupus.commands.core.utils.LogUtil.outMsg
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.logging.Level

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
			field = value
			permission = getPerm()
		}
    var permission = getPerm()
	var description: String = ""
	val flags: MutableSet<CommandFlag> = hashSetOf()


	var method: Method? = null
		set(value) {
			if (value == null)
				return
			var first = true
			field = value
			for (parameter in value.parameters) {
				if (!ArgumentTypeList.contains(parameter.type)) {
					outMsg("[LCF] ERROR: ${value.name} Command argument isn't defined in ArgumentTypeList did you load your command arguments before scanning class?",
						Level.SEVERE)
					outMsg("If not use @NotCMD", Level.SEVERE)
					field = null
					return
				}
				if (first) {
					first = !first
					if (!CommandSender::class.java.isAssignableFrom(parameter.type)) {
						outMsg("[LCF] First argument of method ${value.name} is not Bukkit CommandSender aborting")
						field = null
						return
					}
					this.executorParameter = parameter
					continue
				}
				this.addParameter(parameter)
			}
			permission = getPerm()
		}

	val aliases: MutableList<String> = mutableListOf()
	var syntax = StringBuilder()

	val parameters: MutableList<ArgumentType> = mutableListOf()
	val subCommands: MutableList<CommandBuilder> = mutableListOf()




	var supCommand: CommandBuilder? = null
		set(it) {
			if (it == null)
				return
			field = it
			this.permission = getPerm()
			if (it.hasFlag(CommandFlag.NO_PERM))
				this.flags.add(CommandFlag.NO_PERM)
		}
	var executorParameter: Parameter? = null
	var paramModifiers: List<ParameterModifier> = mutableListOf()
	var anyModifiers: List<AnyModifier> = mutableListOf()
	val conditions: MutableList<ConditionFun> = mutableListOf()

	private fun hasFlag(flag: CommandFlag): Boolean {
		return flags.contains(flag)
	}
	private fun getPerm(): String {
		if(method != null)
			if (hasFlag(CommandFlag.NO_PERM))
				return ""
		var perm = plugin.name.lowercase()
		val supCommandPrefix = supCommand?.permission ?: ""
		// It's sure to be the last
		val methodName = getPermMethodPart()

		if (supCommandPrefix.isNotEmpty()) {
			perm = "$supCommandPrefix$methodName"
			return perm.lowercase()
		}
		val clazzPrefix = getPermClazzPrefixPart()
		perm += clazzPrefix

		return perm.lowercase()
	}
	private fun getPermClazzPrefixPart(): String {
		val clazzPrefix = declaringClazz
			.name
			.removePrefix("$packageName.")
			.replace(namingSchema, "")

		return ".${clazzPrefix}"
	}
	private fun getPermMethodPart(): String {
		if (method == null)
			return ""
		var methodName = method?.name ?: ""
		methodName = if(methodName.isNotEmpty()) ".$methodName" else ""
		return methodName
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
			?: throw IllegalArgumentException("clazz argument isn't in ArgumentTypes list")

		// It would be weird if this would be null whilst we're adding parameters
		if (method?.isAnnotationPresent(Syntax::class.java)!!) {
			return this
		}

		if (argumentType.argumentSpan > 1) {
			val argumentNames = argumentType.argumentName.split(',')

			this.parameters.add(argumentType)
			for (i in 0 until argumentType.argumentSpan) {
				syntax.append("[${parameterName}.${argumentNames[i]}] ")
			}

		}
		else {
			this.parameters.add(argumentType)

			syntax.append("[${parameterName}] ")
		}
		return this
	}

	fun build(previousNameSpace: String = ""): List<CommandLupi> {
		var previousNameSpace = previousNameSpace
		if (previousNameSpace.isEmpty())
			previousNameSpace = "$name"
		val subCommands = mutableListOf<CommandLupi>()
		for (subCommand in this.subCommands) {
			var nameSpace = previousNameSpace
			if (!hasFlag(CommandFlag.CONTINUOUS) && !subCommand.hasFlag(CommandFlag.CONTINUOUS))
				nameSpace +=
					"${this.syntax} ${subCommand.name} "
						// Replace double space if any exists
						.replace("  ", " ")
			subCommands.addAll(subCommand.build(nameSpace))
		}
		if (hasFlag(CommandFlag.CONTINUOUS))
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
			previousNameSpace,
			flags
		)
		outMsg(" ")
		outMsg(builtCommand.toString())
		outMsg(" ")
		return listOf(
			builtCommand
		)
	}

    fun addSubCommandPass(pass: String) {
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