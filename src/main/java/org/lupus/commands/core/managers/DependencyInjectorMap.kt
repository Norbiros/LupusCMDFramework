package org.lupus.commands.core.managers

import org.bukkit.plugin.java.JavaPlugin

object DependencyInjectorMap : HashMap<JavaPlugin, HashMap<Class<out Any>, Any>>() {
	fun addDependency(plugin: JavaPlugin, clazz: Class<*>, obj: Any) {
		if(this[plugin] == null)
			this[plugin] = hashMapOf()
		this[plugin]!![clazz] = obj
	}
}
