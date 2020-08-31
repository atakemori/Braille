package com.takemori.braille

import android.content.res.Resources
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
        val xpp: XmlResourceParser = App.getContext().resources.getXml(R.xml.braille)
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
            var punctuation: String? = null
            var abrevSolo: String? = null
            var abrev1: String? = null
            var abrev2: String? = null

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
                        "letter" -> {
                            letter = readText()?:""
                        }
                        "index" -> {
                            index = readText()?.toInt()?:-1
                        }
                        "code" -> {
                            code = readText()?.toByte(2)?:-1
                        }
                        "punctuation" -> {
                            punctuation = readText()?:""
                        }
                        "abrevSolo" -> {
                            abrevSolo = readText()?:""
                        }
                        "abrev1" -> {
                            abrev1 = readText()?:""
                        }
                        "abrev2" -> {
                            abrev2 = readText()?:""
                        }
                    }
                }
                eventType = xpp.next() //potentially another tag start
            }
            return Letter(code, letter, index, punctuation, abrevSolo, abrev1, abrev2)
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