package com.example.meishizukan.dto

data class Photo(
    val id:Int,
    val hashedBinary:ByteArray,
    val binary:ByteArray,
    val createdOn:String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Photo

        if (id != other.id) return false
        if (!hashedBinary.contentEquals(other.hashedBinary)) return false
        if (!binary.contentEquals(other.binary)) return false
        if (createdOn != other.createdOn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + hashedBinary.contentHashCode()
        result = 31 * result + binary.contentHashCode()
        result = 31 * result + createdOn.hashCode()
        return result
    }
}