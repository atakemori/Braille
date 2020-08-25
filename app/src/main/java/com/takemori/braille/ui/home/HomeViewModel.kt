package com.takemori.braille.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
        val mask: Byte = 1
        mask.rotateLeft(num)
        _brailleByte.value?.xor(mask)
    }


}