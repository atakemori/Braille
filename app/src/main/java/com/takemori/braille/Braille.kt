package com.takemori.braille

import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

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

    }

}