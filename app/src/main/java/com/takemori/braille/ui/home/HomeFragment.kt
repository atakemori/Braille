package com.takemori.braille.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ComputableLiveData
import androidx.lifecycle.ViewModelProvider
import com.takemori.braille.R
import com.takemori.braille.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var binding: FragmentHomeBinding

//    @ExperimentalStdlibApi
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_home,
                container,
                false
        )
        //homeViewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Set the viewmodel for databinding -this allows the bound layout access
        // to all the data in the ViewModel
        binding.homeFragmentViewModel = homeViewModel

        // Specify the fragment view as the lifecycle owner of the binding.
        // This is used so that the binding can observe LiveData updates
        binding.lifecycleOwner = viewLifecycleOwner


//        val root = inflater.inflate(R.layout.fragment_home, container, false)
//        val textView = root.findViewById<TextView>(R.id.text_home)
//        homeViewModel.brailleByte.observe(viewLifecycleOwner, Observer<Byte> { byte -> textView.text = byte.toString() })

        Log.i("HomeFragment.kt", "Binding the buttons")
        binding.buttons.toggleButton1.setOnCheckedChangeListener { buttonView, isChecked ->
            Log.i("HomeFragment.kt", "Button 1 pressed")
            homeViewModel.buttonFlip(1)
        }
        binding.buttons.toggleButton2.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(2)}
        binding.buttons.toggleButton3.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(3)}
        binding.buttons.toggleButton4.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(4)}
        binding.buttons.toggleButton5.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(5)}
        binding.buttons.toggleButton6.setOnCheckedChangeListener { buttonView, isChecked -> homeViewModel.buttonFlip(6)}
        ///////////HOW DO TOGGLE BUTTONS HANDLE ON CLICK LISTENERS I THINK THIS IS THE WRONG WAY
//        binding.toggleButtonTEST.setOnCheckedChangeListener { buttonView, isChecked ->
//            Log.i("HomeFragment.kt", "Button TEST pressed")
//            homeViewModel.buttonFlip(1)
//        }
        val ccListener: CompoundButton.OnCheckedChangeListener = object:CompoundButton.OnCheckedChangeListener {
            /**
             * Called when the checked state of a compound button has changed.
             *
             * @param buttonView The compound button view whose state has changed.
             * @param isChecked  The new checked state of buttonView.
             */
            override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
                Log.i("HomeFragment.kt", "Button 1 pressed")
                homeViewModel.buttonFlip(1)
            }
        }
        //binding.toggleButtonTEST.setOnCheckedChangeListener(ccListener)
//        val testButton: ToggleButton? = view?.findViewById(R.id.toggleButtonTEST)
        val testButton: ToggleButton? = container?.findViewById(R.id.toggleButtonTEST)

    testButton?.setOnCheckedChangeListener(ccListener)
        binding.buttonPlainTEST.setOnClickListener { it: View? ->
            (it as Button).text = "pressed"
        }

        ///I SHOULD utilize the isCHecked portion to only add or only remove instead.
//        return root
        return binding.root
    }

    fun testClick(view:View) {
        homeViewModel.buttonFlip(1)
        Log.i("HomeFragment.kt", "Pressed the testClick function assigned within xml")
    }



}