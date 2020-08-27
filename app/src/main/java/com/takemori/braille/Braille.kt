package com.takemori.braille

import android.content.res.Resources
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser

object Braille {

    private val list: List<Letter>

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

            while (eventType != XmlPullParser.END_TAG && xpp.name != "item") {
                if (eventType == XmlPullParser.START_TAG) {
                    when (xpp.name) {
                        "letter" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) letter = xpp.text
                            xpp.next()
                        }
                        "index" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) index = xpp.text.toInt()
                            xpp.next()
                        }
                        "code" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) code = xpp.text.toByte()
                            xpp.next()
                        }
                        "punctuation" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) punctuation = xpp.text
                            xpp.next()
                        }
                        "abrevSolo" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) abrevSolo = xpp.text
                            xpp.next()
                        }
                        "abrev1" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) abrev1 = xpp.text
                            xpp.next()
                        }
                        "abrev2" -> {
                            eventType = xpp.next()
                            if (eventType == XmlPullParser.TEXT) abrev2 = xpp.text
                            xpp.next()
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




}