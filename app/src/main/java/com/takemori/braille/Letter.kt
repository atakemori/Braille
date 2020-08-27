package com.takemori.braille

data class Letter(
        val code: Byte,
        val letter: String,
        val index: Int,
        val punctuation: String?,
        val abrevSolo: String?,
        val abrev1: String?,
        val abrev2: String?
) {
}