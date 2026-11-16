package com.musicplayer.promax.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class CustomEqProfile(
    val id: String,
    val name: String,
    val bandsMb: List<Int>,
    val bassStrength: Int,
    val virtualizerStrength: Int,
    val loudnessMb: Int,
    val stereoBalance: Float
)

object CustomEqProfileCodec {
    fun encodeBandLevels(levelsMb: List<Int>): String = levelsMb.joinToString(",")

    fun decodeBandLevels(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    fun encodeProfiles(profiles: List<CustomEqProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            val obj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("bandsMb", encodeBandLevels(profile.bandsMb))
                put("bassStrength", profile.bassStrength)
                put("virtualizerStrength", profile.virtualizerStrength)
                put("loudnessMb", profile.loudnessMb)
                put("stereoBalance", profile.stereoBalance.toDouble())
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeProfiles(raw: String?): List<CustomEqProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val bands = decodeBandLevels(obj.optString("bandsMb"))
                    add(
                        CustomEqProfile(
                            id = id,
                            name = name,
                            bandsMb = bands,
                            bassStrength = obj.optInt("bassStrength", 0).coerceIn(0, 1000),
                            virtualizerStrength = obj.optInt("virtualizerStrength", 0).coerceIn(0, 1000),
                            loudnessMb = obj.optInt("loudnessMb", 0).coerceIn(0, 1500),
                            stereoBalance = obj.optDouble("stereoBalance", 0.0).toFloat().coerceIn(-1f, 1f)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
