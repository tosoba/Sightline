package com.trm.sightline.core.datastore

import com.trm.sightline.core.model.UserPreferences

/**
 * Reads the proto3 `UserPreferences` message written by the pre-KMP app.
 *
 * The wire format is decoded by hand because the protobuf runtime is JVM-only and dropping it is
 * the point of the JSON store. Unknown fields are skipped so a partial upgrade cannot fail.
 */
internal object LegacyUserPreferencesReader {
  fun read(bytes: ByteArray): UserPreferences? =
    try {
      decode(bytes)
    } catch (_: IllegalArgumentException) {
      null
    }

  private fun decode(bytes: ByteArray): UserPreferences {
    val reader = ProtoReader(bytes)
    var preferences = UserPreferences()
    while (reader.hasMore) {
      val tag = reader.readVarint()
      val fieldNumber = (tag ushr 3).toInt()
      when (tag and WIRE_TYPE_MASK) {
        WIRE_TYPE_VARINT -> {
          val value = reader.readVarint()
          preferences =
            when (fieldNumber) {
              FIELD_USER_LOCATION_ENABLED ->
                preferences.copy(userLocationEnabled = value != 0L)
              FIELD_CUSTOM_LOCATION_TIMESTAMP ->
                preferences.copy(customLocationTimestampMillis = value)
              FIELD_SEARCH_RADIUS -> preferences.copy(searchRadiusMeters = value.toInt())
              else -> preferences
            }
        }
        WIRE_TYPE_FIXED_64 -> {
          val value = reader.readDouble()
          preferences =
            when (fieldNumber) {
              FIELD_CUSTOM_LOCATION_LATITUDE ->
                preferences.copy(customLocationLatitude = value)
              FIELD_CUSTOM_LOCATION_LONGITUDE ->
                preferences.copy(customLocationLongitude = value)
              FIELD_LAST_MAP_LATITUDE -> preferences.copy(lastMapLatitude = value)
              FIELD_LAST_MAP_LONGITUDE -> preferences.copy(lastMapLongitude = value)
              FIELD_LAST_MAP_ZOOM -> preferences.copy(lastMapZoom = value)
              FIELD_LAST_MAP_BEARING -> preferences.copy(lastMapBearing = value)
              FIELD_LAST_MAP_PITCH -> preferences.copy(lastMapPitch = value)
              else -> preferences
            }
        }
        WIRE_TYPE_LENGTH_DELIMITED -> {
          val value = reader.readString(reader.readVarint().toInt())
          preferences =
            if (fieldNumber == FIELD_CUSTOM_LOCATION_ADDRESS) {
              preferences.copy(customLocationAddress = value)
            } else {
              preferences
            }
        }
        WIRE_TYPE_FIXED_32 -> reader.skip(4)
        else -> throw IllegalArgumentException("Unsupported wire type for field $fieldNumber")
      }
    }
    return preferences
  }

  private class ProtoReader(private val bytes: ByteArray) {
    private var position = 0

    val hasMore: Boolean
      get() = position < bytes.size

    fun readVarint(): Long {
      var result = 0L
      var shift = 0
      while (shift <= MAX_SHIFT) {
        val byte = readByte().toInt()
        result = result or ((byte and VARINT_MASK).toLong() shl shift)
        if (byte and CONTINUATION_BIT == 0) return result
        shift += BITS_PER_VARINT_BYTE
      }
      throw IllegalArgumentException("Malformed varint")
    }

    fun readDouble(): Double = Double.fromBits(readLittleEndianLong())

    fun readString(length: Int): String {
      require(length >= 0) { "Negative length $length" }
      val end = position + length
      require(end <= bytes.size) { "Truncated string" }
      val value = bytes.decodeToString(startIndex = position, endIndex = end)
      position = end
      return value
    }

    fun skip(count: Int) {
      position += count
      require(position <= bytes.size) { "Truncated field" }
    }

    private fun readByte(): Byte {
      require(position < bytes.size) { "Truncated field" }
      return bytes[position++]
    }

    private fun readLittleEndianLong(): Long {
      var result = 0L
      repeat(Long.SIZE_BYTES) { index ->
        val byte = readByte().toLong() and BYTE_MASK
        result = result or (byte shl (index * Byte.SIZE_BITS))
      }
      return result
    }
  }

  private const val WIRE_TYPE_MASK = 0x7L
  private const val WIRE_TYPE_VARINT = 0L
  private const val WIRE_TYPE_FIXED_64 = 1L
  private const val WIRE_TYPE_LENGTH_DELIMITED = 2L
  private const val WIRE_TYPE_FIXED_32 = 5L

  private const val FIELD_USER_LOCATION_ENABLED = 1
  private const val FIELD_CUSTOM_LOCATION_LATITUDE = 2
  private const val FIELD_CUSTOM_LOCATION_LONGITUDE = 3
  private const val FIELD_CUSTOM_LOCATION_ADDRESS = 4
  private const val FIELD_CUSTOM_LOCATION_TIMESTAMP = 5
  private const val FIELD_LAST_MAP_LATITUDE = 6
  private const val FIELD_LAST_MAP_LONGITUDE = 7
  private const val FIELD_LAST_MAP_ZOOM = 8
  private const val FIELD_LAST_MAP_BEARING = 9
  private const val FIELD_LAST_MAP_PITCH = 10
  private const val FIELD_SEARCH_RADIUS = 11

  private const val CONTINUATION_BIT = 0x80
  private const val VARINT_MASK = 0x7F
  private const val BITS_PER_VARINT_BYTE = 7
  private const val MAX_SHIFT = 63
  private const val BYTE_MASK = 0xFFL
}
