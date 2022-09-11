package io.github.nbcss.wynnlib.data

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.MathHelper

class CharacterProfile {
    companion object {
        private val client = MinecraftClient.getInstance()
        private val apArray = arrayOf(0,1,2,2,3,3,4,4,5,5,6,6,7,8,8,9,9,10,11,11,
            12,12,13,14,14,15,16,16,17,17,18,18,19,19,20,20,20,21,21,22,22,23,23,
            23,24,24,25,25,26,26,27,27,28,28,29,29,30,30,31,31,32,32,33,33,34,34,
            34,35,35,35,36,36,36,37,37,37,38,38,38,38,39,39,39,39,40,40,40,40,41,
            41,41,41,42,42,42,42,43,43,43,43,44,44,44,44,45,45,45)
        private var profile: CharacterProfile? = CharacterProfile()
        fun getCurrentProfile(): CharacterProfile? = profile
    }

    fun getLevel(): Int {
        return client.player?.experienceLevel ?: 0
    }

    fun getMaxAP(): Int {
        val i = getLevel()
        return if (i in apArray.indices) apArray[i] else 0
    }

    fun getMaxSP(): Int {
        val i = getLevel()
        return MathHelper.clamp((i - 1) * 2, 0, 200)
    }
}