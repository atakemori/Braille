package com.takemori.braille.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.takemori.braille.Braille
import com.takemori.braille.Letter
import java.util.Locale
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or
import kotlin.experimental.xor

class HomeViewModel : ViewModel() {


    private val _word = MutableLiveData<String>()
    val word: LiveData<String>
        get() = _word


    private val _brailleByte = MutableLiveData<Byte>()
    val brailleByte: LiveData<Byte>
        get() = _brailleByte

    val brailleToLetter = Transformations.map(brailleByte) { input: Byte? ->
        val i = input?.toInt()?: 0
        return@map Braille.getLetter(i) //TODO add logic to determin if it should show letter or abbrev etc.
    }

    private val _lettersList = MutableLiveData<MutableList<Letter>>()
    val lettersList: LiveData<MutableList<Letter>>
        get() = _lettersList


    val code001000: Byte = 60
    val numberCode: Byte = 60
    val letterCode: Byte = 48
    val capitalCode: Byte = 32

//    val code


    val lettersToString = Transformations.map(lettersList) { input: MutableList<Letter> ->
        val stringBuilder: StringBuilder  = StringBuilder()
        var mode: Byte = letterCode
        var capitalMode: Boolean = false
        var capitalNext: Boolean = false

        var i = 0
        var letter: Letter
        val inputSize = input.size

        // Evaluate the braille characters sequentially into a String
        while (i < inputSize) {
            letter = input[i]

            if (letter.code == numberCode) {
                // if the indicator is at the end of the string with nothing after it, display the indicator
                if (i == inputSize - 1) stringBuilder.append("NUM_START")
                mode = numberCode
                i++
                continue
            } else if (letter.code == letterCode) {
                // if the indicator is at the end of the string with nothing after it, display the indicator
                if (i == inputSize - 1) stringBuilder.append("LETTER_START")
                mode = letterCode
                i++
                continue
            } else if(letter.code == capitalCode) {
                // Two capital signs in a row, means CAPS
                if (capitalNext) capitalMode = true
                capitalNext = true
                i++
                continue
            }

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
                    i++
                    continue
                } else {
                    var nextStr = letter.letter

                    // Do Abbreviation evaluation


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

        fun checkIfPrevSpace(): Boolean {
            return i == 0 || input[i - 1].letter == "SPACE"
        }
        fun checkIfNextSpace(): Boolean {
            return i == inputSize - 1 || input[i+1].letter == "SPACE"
        }
        fun checkIfAlone(): Boolean {
            return checkIfPrevSpace() && checkIfNextSpace()
        }

        return@map stringBuilder.toString()
//        return@map "test"



    }





    init {
        _word.value = "la"
        _brailleByte.value = 0b0
        _lettersList.value = mutableListOf<Letter>()
    }


    /**
     *  Flip the viewModel's bit determined by the number
     *  button pressed (values 1 t0 6).
     */
    @ExperimentalStdlibApi
    fun buttonFlip(num: Int) {
        var mask: Byte = 1
        mask = mask.rotateLeft(num - 1)
        _brailleByte.value = _brailleByte.value?.xor(mask)
//        _brailleByte.value = _brailleByte.value?.plus(mask)?.toByte()
        Log.i("HomeViewModel.kt", "Button pressed: $num \nByte value: $brailleByte")
    }

    @ExperimentalStdlibApi
    fun buttonFlip(num: Int, on: Boolean) {
        var mask: Byte = 1
        mask =  mask.rotateLeft(num-1)
        if (on) {
            // Add the num bit to _brailleByte
            _brailleByte.value = _brailleByte.value?.or(mask)
        }
        else {
            // remove the num bit to _brailleByte
            _brailleByte.value = _brailleByte.value?.and(mask.inv())
        }


        val letter: Letter = Braille.getLetter(brailleByte.value!!.toInt())
        Log.i("HomeViewModel.kt", "Retrieved value :${letter.letter}")
    }

    /**
     * Function called from plus button, to take in the current displayed
     * braille byte and then store that letter in a local list to build a string.
     */
    public fun addInput() {
//        val letter: Letter = brailleToLetter.value?: Letter(-1, "ERROR", -1, null, null, null, null)
        val letter: Letter = Braille.getLetter(brailleByte.value?.toInt())

        //clearButtons()
        _lettersList.value?.add(letter)
        _lettersList.notifyObserver()
    }

    /**
     * Remove the last letter in _lettersList
     */
    public fun removeLetter() {
        if (_lettersList.value!!.isNotEmpty()) {
            _lettersList.value?.removeLast()
            _lettersList.notifyObserver()
        }
    }

}

/**
 * Extension function from s.o. https://stackoverflow.com/questions/47941537/notify-observer-when-item-is-added-to-list-of-livedata
 * to trigger a liveData update when lists are updated. Otherwise just changing
 * an item in a list will not create a new version number and observers are not notified.
 */
private fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}
