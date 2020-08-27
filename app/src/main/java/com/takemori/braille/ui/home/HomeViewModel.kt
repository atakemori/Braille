package com.takemori.braille.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.takemori.braille.Braille
import com.takemori.braille.Letter
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


    init {
        _word.value = "la"
        _brailleByte.value = 0b0
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


}