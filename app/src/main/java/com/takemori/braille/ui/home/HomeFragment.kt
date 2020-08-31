package com.takemori.braille.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.takemori.braille.R
import com.takemori.braille.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var binding: FragmentHomeBinding

    private lateinit var listOfBrailleButtons: List<ToggleButton>

    @ExperimentalStdlibApi
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_home,
                container,
                false
        )
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        // Set the viewmodel for databinding -this allows the bound layout access
        // to all the data in the ViewModel
        binding.homeFragmentViewModel = homeViewModel
        // Specify the fragment view as the lifecycle owner of the binding.
        // This is used so that the binding can observe LiveData updates
        binding.lifecycleOwner = viewLifecycleOwner

//        homeViewModel.brailleByte.observe(viewLifecycleOwner, Observer<Byte> { byte -> textView.text = byte.toString() })

        listOfBrailleButtons = listOf(
                binding.buttons.toggleButton1,
                binding.buttons.toggleButton4,
                binding.buttons.toggleButton2,
                binding.buttons.toggleButton5,
                binding.buttons.toggleButton3,
                binding.buttons.toggleButton6
        )


        Log.i("HomeFragment.kt", "Binding the buttons")
        binding.buttons.toggleButton1.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.i("HomeFragment.kt", "Button 1 pressed")
            homeViewModel.buttonFlip(1, isChecked)
        }

//        binding.buttonClear.setOnClickListener { clearButtons() }
        binding.buttonClear.setOnClickListener { removeLetter() }
        binding.addLetterButton.setOnClickListener { addLetter()}

        binding.buttons.toggleButton2.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(2, isChecked)}
        binding.buttons.toggleButton3.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(3, isChecked)}
        binding.buttons.toggleButton4.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(4, isChecked)}
        binding.buttons.toggleButton5.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(5, isChecked)}
        binding.buttons.toggleButton6.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(6, isChecked)}
        ///////////HOW DO TOGGLE BUTTONS HANDLE ON CLICK LISTENERS I THINK THIS IS THE WRONG WAY
        ///I SHOULD utilize the isCHecked portion to only add or only remove instead.
        return binding.root
    }


    public fun addLetter() {
        homeViewModel.addInput()
        clearButtons()
    }

    public fun clearButtons() {
        for (button:ToggleButton in listOfBrailleButtons) {
            //button.animate()
            button.isChecked = false
            //delay 20
        }
    }

    public fun removeLetter() {
        homeViewModel.removeLetter()
    }

}