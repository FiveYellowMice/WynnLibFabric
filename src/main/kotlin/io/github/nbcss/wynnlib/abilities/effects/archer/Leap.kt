package io.github.nbcss.wynnlib.abilities.effects.archer

import com.google.gson.JsonObject
import io.github.nbcss.wynnlib.abilities.Ability
import io.github.nbcss.wynnlib.abilities.display.CooldownTooltip
import io.github.nbcss.wynnlib.abilities.display.EffectTooltip
import io.github.nbcss.wynnlib.abilities.effects.AbilityEffect
import io.github.nbcss.wynnlib.abilities.effects.BaseEffect
import io.github.nbcss.wynnlib.abilities.properties.legacy.CooldownProperty

open class Leap(parent: Ability, json: JsonObject): BaseEffect(parent, json), CooldownProperty {
    companion object: AbilityEffect.Factory {
        override fun create(parent: Ability, properties: JsonObject): Leap {
            return Leap(parent, properties)
        }
    }
    private val cooldown: Double = CooldownProperty.read(json)

    override fun getCooldown(): Double = cooldown

    override fun getTooltipItems(): List<EffectTooltip> {
        return listOf(CooldownTooltip)
    }
}