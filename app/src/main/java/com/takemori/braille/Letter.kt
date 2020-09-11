package com.takemori.braille

data class Letter(
        val code: Byte,
        val letter: String,
        val index: Int,
        val unicode_code: String,
        val unicode_dots: String,
        val initial_abbrev1: String?,
        val initial_abbrev2: String?,
        val initial_abbrev3: String?,
        val final_abbrev1: String?,
        val final_abbrev2: String?,
        val final_abbrev3: String?,
        val one_letter_contract: String?,
        val one_letter_affix: String?,
        val punctuation: String?,
        val number: String?
) {
}