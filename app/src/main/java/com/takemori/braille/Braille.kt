package com.takemori.braille

import android.content.res.XmlResourceParser
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import java.util.*
import kotlin.collections.ArrayList

class Braille {
    companion object {

        private val list: List<Letter>
    //    private val list: List<Letter?>


        init {
            list = loadData().toList()

        }


        private fun loadData(): MutableList<Letter> {
            val listOfLetter: MutableList<Letter> = ArrayList<Letter>(64)
            val xpp: XmlResourceParser = App.getContext().resources.getXml(R.xml.braille_v2)
            xpp.next()
            var eventType: Int = xpp.eventType

            /**
             * buildLetter gets called when the current xpp eventType is a START_TAG for "item"
             * This method handles the following nested XML entries and f
             */
            fun buildLetter(): Letter {
                var code: Byte = 0
                var letter: String = ""
                var index: Int = -1
                var unicodeCode: String = "U+283F"
                var unicodeDots: String = "⠿"
                var initialAbbrev1: String? = null
                var initialAbbrev2: String? = null
                var initialAbbrev3: String? = null
                var finalAbbrev1: String? = null
                var finalAbbrev2: String? = null
                var finalAbbrev3: String? = null
                var oneLetterContract: String? = null
                var oneLetterAffix: String? = null
                var punctuation: String? = null
                var number: String? = null

                fun readText(): String?{
                    var readText: String? = null
                    eventType = xpp.next()
                    if (eventType == XmlPullParser.TEXT) readText = xpp.text
                    xpp.next()
                    return readText
                }

                while ((eventType != XmlPullParser.END_TAG) || xpp.name.toString() != "item") {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (xpp.name) {
                            "code" -> {
                                code = readText()?.toByte(2)?:-1
                            }
                            "letter" -> {
                                letter = readText()?:""
                            }
                            "index" -> {
                                index = readText()?.toInt()?:-1
                            }
                            "unicode_code" -> {
                                unicodeCode = readText()?:""
                            }
                            "unicode_dots" -> {
                                unicodeDots = readText()?:""
                            }
                            "initial_abbrev1" -> {
                                initialAbbrev1 = readText()?:""
                            }
                            "initial_abbrev2" -> {
                                initialAbbrev2 = readText()?:""
                            }
                            "initial_abbrev3" -> {
                                initialAbbrev3 = readText()?:""
                            }
                            "final_abbrev1" -> {
                                finalAbbrev1 = readText()?:""
                            }
                            "final_abbrev2" -> {
                                finalAbbrev2 = readText()?:""
                            }
                            "final_abbrev3" -> {
                                finalAbbrev3 = readText()?:""
                            }
                            "one_letter_contract" -> {
                                oneLetterContract = readText()?:""
                            }
                            "one_letter_affix" -> {
                                oneLetterAffix = readText()?:""
                            }
                            "punctuation" -> {
                                punctuation = readText()?:""
                            }
                            "number" -> {
                                number = readText()?:""
                            }
                        }
                    }
                    eventType = xpp.next() //potentially another tag start
                }
                return Letter(code, letter, index, unicodeCode, unicodeDots, initialAbbrev1,
                        initialAbbrev2, initialAbbrev3, finalAbbrev1, finalAbbrev2, finalAbbrev3,
                        oneLetterContract, oneLetterAffix, punctuation, number)
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                } else if (eventType == XmlPullParser.START_TAG && xpp.name == "item") {
                    val letter:Letter = buildLetter()
                    listOfLetter.add(letter.index, letter)
                }// else if (eventType == XmlPullParser.)
                eventType = xpp.next()
            }

            return listOfLetter
        }


        val UNKNOWN_LETTER: Int = 0 // replace with a null letter index later
        /**
         * Return a Letter after xml data has been loaded.
         * The index is the braille dots Byte converted to
         * an integer.
         */
        public fun getLetter(i: Int?): Letter {
            return list.get(i?:UNKNOWN_LETTER)?: throw NullPointerException("Data not initialized")
        }


        val code001000: Byte = 60
        val numberCode: Byte = 60
        val letterCode: Byte = 48
        val capitalCode: Byte = 32
        val NOT_FOUND: String = "Not_Found"
//        val NON_LETTER_LIST: MutableList<Letter> = mutableListOf(
//                getLetter(0),
//                getLetter(4),
//                getLetter(8),
//                getLetter(16),
//                getLetter(24),
//                getLetter(32),
//                getLetter(36),
//                getLetter(38),
//                getLetter(40),
//                getLetter(48),
//                getLetter(52),
//                getLetter(56),
//                getLetter(60),
//        )
        val ALLOWS_ONE_LETTER_ABBREV: List<Letter> = listOf(
            getLetter(0),
        getLetter(32),
        getLetter(38),
        getLetter(52),
        //??????? maybe this shouldntbe allowed if it conflicts with checking finalAbbr2
        getLetter(48)
)

        fun translateLetters(input: MutableList<Letter>): String {
            val stringBuilder: StringBuilder  = StringBuilder()
            var mode: Byte = letterCode
            var capitalMode: Boolean = false
            var capitalNext: Boolean = false

            var i = 0
            var letter: Letter
            val inputSize = input.size


            fun checkIfPrevSpace(): Boolean {
//                return i == 0 || input[i-1] in ALLOWS_ONE_LETTER_ABBREV
                return i == 0 || input[i - 1].letter == "SPACE"

            }
            fun checkIfNextSpace(): Boolean {
                return i == inputSize - 1 || input[i+1].letter == "SPACE"
            }
            fun checkIfKSpace(k: Int): Boolean {
                if (i + k < 0 || i+k > inputSize - 1) return true
                else {
                    return input[i+k].letter == "SPACE"
                }
            }
            fun checkIfAlone(): Boolean {
                return checkIfPrevSpace() && checkIfNextSpace()
            }
            // Grabs the next Letter if there is one, and increments index by 1
            fun grabNextBraille(): Letter? {
                if (i < inputSize - 1) {
                    i++
                    return input[i]
                }
                return null
            }
            // Peek k Letters ahead, but do not incrememnt the count
            fun peekNextBraille(k: Int): Letter? {
                if (i + k in 0 until inputSize) {
                    return input [i+k]
                }
                return null
            }


            // Evaluate the braille characters sequentially into a String
            translate@ while (i < inputSize) {
                letter = input[i]

                if (letter.code == numberCode) {
                    // if the indicator is at the end of the string with nothing after it, display the indicator
                    if (i == inputSize - 1) stringBuilder.append("NUM_START")
                    mode = numberCode
                    i++
                    continue
                } else if (mode == numberCode && letter.code == letterCode) {
                    // if the indicator is at the end of the string with nothing after it, display the indicator
                    if (i == inputSize - 1) stringBuilder.append("LETTER_START")
                    mode = letterCode
                    i++
                    continue
                }
                // Handle the capitalization later instead
//                } else if(letter.code == capitalCode) {
//                    // Two capital signs in a row, means CAPS
//                    if (capitalNext) capitalMode = true
//                    capitalNext = true
//                    i++
//                    continue
//                }

                if (mode == numberCode) {
                    if (letter.number != null) {
                        stringBuilder.append(letter.number!!)
                        i++
                        continue
                    } else {
                        mode = letterCode
                        continue
                    }
                }
                if (mode == letterCode) {
                    if (letter.letter == "SPACE") {
                        stringBuilder.append("_")
                        capitalMode = false
                        capitalNext = false
                        i++
                        continue
                    } else {
                        // Basic version, get the next character's letter
                        var nextStr = letter.letter

                        // Then if the letter is a meta character, do Abbreviation evaluation
                        when (letter.letter) {
                            "INITIAL ABBR. 1" -> {
                                val afterLetter: Letter? = grabNextBraille()
                                // If there is nothing after the abbr sign, print the abbrev text "INITIAL ABBR. 1"
                                nextStr = if (afterLetter == null) letter.letter
                                else {
                                    // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
                                    afterLetter.initial_abbrev1
                                            ?: letter.unicode_dots + afterLetter.letter
                                }
                            }
                            "INITIAL ABBR. 2" -> {
                                val afterLetter: Letter? = grabNextBraille()
                                // If there is nothing after the abbr sign, print the abbrev text "INITIAL ABBR. 2"
                                nextStr = if (afterLetter == null) letter.letter
                                else {
                                    // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
                                    afterLetter.initial_abbrev2
                                            ?: letter.unicode_dots + afterLetter.letter
                                }
                            }
                            // hmmmmmmmmmmmmmmmmmmmm
                            "INITIAL ABBR. 3" -> {
                                val afterLetter: Letter? = grabNextBraille()
                                // If there is nothing after the abbr sign, print the abbrev text "INITIAL ABBR. 3"
                                nextStr = if (afterLetter == null) "Capitalization" //letter.letter
                                else {
                                    // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
                                    afterLetter.initial_abbrev3
                                            ?: letter.unicode_dots + afterLetter.letter
                                }
                            }
                            "FINAL ABBR. 1" -> {
                                val afterLetter: Letter? = grabNextBraille()
                                // If there is nothing after the abbr sign, print the abbrev text "FINAL ABBR. 1"
                                nextStr = if (afterLetter == null) letter.letter
                                else {
                                    // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
                                    afterLetter.final_abbrev1
                                            ?: letter.unicode_dots + afterLetter.letter
                                }
                            }
                            "FINAL ABBR. 2" -> {
                                val afterLetter: Letter? = grabNextBraille()
                                // If there is nothing after the abbr sign, print the abbrev text "FINAL ABBR. 2"
                                nextStr = if (afterLetter == null) letter.letter
                                else {
                                    // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
                                    afterLetter.final_abbrev2
                                            ?: letter.unicode_dots + afterLetter.letter
                                }
                            }
                            // Special handling to be sure that the final abbrev comes at the end
                            // of a word with a space after to act as a suffix
                            "CAPS OR FINAL ABBR. 3" -> {
                                // See if this is an end_abbrev where it is preceded by letters and then
                                // Y or N and then another space.
                                if ((!checkIfPrevSpace()) && (checkIfKSpace(2))) {
                                    val afterLetter: Letter? = peekNextBraille(1)
                                    Log.i("Braille.kt", "Peeked Letter" + afterLetter?.letter)
                                    if (afterLetter?.final_abbrev3 != null) {
                                        i++ //incrememnt after the peeked Letter
                                        nextStr = afterLetter.final_abbrev3
                                    } else {
                                        if (capitalNext) capitalMode = true
                                        else capitalNext = true
                                        i++
                                        continue
                                    }
                                } else {
                                    if (capitalNext) capitalMode = true
                                    else capitalNext = true
                                    i++
                                    continue
                                }
                            }
//                                val afterLetter: Letter? = grabNextBraille()
//                                // Preceding letters indicate abbreviation
//                                if (! checkIfPrevSpace()) {
//                                    // If there is nothing after the abbr sign, print the abbrev text "FINAL ABBR. 3"
//                                    nextStr = if (afterLetter == null) "Final Abbr. 3"//letter.letter
//                                    else {
//                                        // Otherwise if there is an abbrev, make it that and if not, print the braille dots and the next letter.
//                                        afterLetter.final_abbrev3
//                                                ?: letter.unicode_dots + afterLetter.letter
//                                    }
//                                }
                        }
                        // Check if the character is isolated from other letters as a one letter contraction or affix
//                        if (check)

                        // Capitalization handling
                        if (capitalMode) {
                            nextStr = nextStr.toUpperCase(Locale.ROOT)
                        }else if (capitalNext) {
                            nextStr = nextStr.capitalize(Locale.ROOT)
                            capitalNext = false
                        }

                        // Add the processed string to the final string
                        stringBuilder.append(nextStr)
                        i++
                        continue
                    }
                }
            }
        return stringBuilder.toString()
        }

    }
}