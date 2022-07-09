package io.github.nbcss.wynnlib.abilities.properties.general

import com.google.gson.JsonElement
import io.github.nbcss.wynnlib.abilities.Ability
import io.github.nbcss.wynnlib.abilities.properties.AbilityProperty
import io.github.nbcss.wynnlib.i18n.Translations
import io.github.nbcss.wynnlib.i18n.Translations.TOOLTIP_ABILITY_MAX
import io.github.nbcss.wynnlib.utils.Symbol
import io.github.nbcss.wynnlib.utils.signed
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class GrowingEffectProperty(ability: Ability,
                            bonus: EffectBonus,
                            private val max: Int): BonusEffectProperty(ability, bonus) {
    companion object: Type {
        override fun create(ability: Ability, data: JsonElement): AbilityProperty {
            val json = data.asJsonObject
            val max = if (json.has(MAX_KEY)) json[MAX_KEY].asInt else 0
            val bonus = EffectBonus(json)
            return GrowingEffectProperty(ability, bonus, max)
        }
        override fun getKey(): String = "grow_effect"
        private const val MAX_KEY = "max"
    }

    fun getMaxModifier(): Int = max

    override fun getTooltip(): List<Text> {
        val modifier = getEffectBonus().getEffectModifier()
        val text = Symbol.EFFECT.asText().append(" ")
            .append(Translations.TOOLTIP_ABILITY_EFFECT.formatted(Formatting.GRAY).append(": "))
        if (modifier != null){
            text.append(Translations.TOOLTIP_SUFFIX_PER_S.formatted(Formatting.WHITE,
                null, "${signed(modifier)}%")).append(" ")
                .append(getEffectBonus().getEffectType().formatted(Formatting.GRAY))
            return listOf(text, LiteralText("   (").formatted(Formatting.DARK_GRAY)
                .append(Symbol.MAX.asText()).append(" ")
                .append(TOOLTIP_ABILITY_MAX.formatted(Formatting.DARK_GRAY))
                .append(": ${signed(max)}%)"))
        }else{
            //it should not happen...right?
            text.append(getEffectBonus().getEffectType().formatted(Formatting.WHITE))
        }
        return listOf(text)
    }
}