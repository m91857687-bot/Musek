package com.musicplayer.promax.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class CustomEqPreset(
    val id: String,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassStrength: Int,
    val virtualizerStrength: Int
)

data class AudioEffectsPreferences(
    val enabled: Boolean = true,
    val presetKey: String = PRESET_FLAT,
    val bandLevelsMb: List<Int> = emptyList(),
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val crossfadeSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val customPresets: List<CustomEqPreset> = emptyList()
) {
    companion object {
        const val PRESET_FLAT = "FLAT"
        const val PRESET_BASS = "BASS"
        const val PRESET_VOCAL = "VOCAL"
        const val PRESET_TREBLE = "TREBLE"
        const val PRESET_CUSTOM = "CUSTOM"

        fun encodeBandLevels(levels: List<Int>): String = levels.joinToString(",")

        fun decodeBandLevels(raw: String?): List<Int> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        fun encodeCustomPresets(presets: List<CustomEqPreset>): String {
            val array = JSONArray()
            presets.forEach { preset ->
                val obj = JSONObject()
                obj.put("id", preset.id)
                obj.put("name", preset.name)
                obj.put("bands", encodeBandLevels(preset.bandLevelsMb))
                obj.put("bass", preset.bassStrength)
                obj.put("virtualizer", preset.virtualizerStrength)
                array.put(obj)
            }
            return array.toString()
        }

        fun decodeCustomPresets(raw: String?): List<CustomEqPreset> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                        add(
                            CustomEqPreset(
                                id = id,
                                name = name,
                                bandLevelsMb = decodeBandLevels(obj.optString("bands")),
                                bassStrength = obj.optInt("bass", 0).coerceIn(0, 1000),
                                virtualizerStrength = obj.optInt("virtualizer", 0).coerceIn(0, 1000)
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
