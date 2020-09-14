package com.takemori.braille.ui.home

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.*
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.takemori.braille.R
import com.takemori.braille.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var binding: FragmentHomeBinding

    private lateinit var listOfBrailleButtons: List<ToggleButton>

    private final lateinit var soundPlayer:  SoundPool
    private var soundIdToggleOn: Int = 0
    private var soundIdToggleOff: Int = 0


    override fun onStop() {
        super.onStop()
        // Docu recommends to set SoundPool to null after release, but I would rather
        // not since kotlin will require safe calls throughout usage
        soundPlayer.release()
        //soundPlayer = null
    }

    override fun onStart() {
        super.onStart()
        soundPlayer = SoundPool(3, AudioManager.USE_DEFAULT_STREAM_TYPE, 0)
        soundIdToggleOn = soundPlayer.load(this.context, R.raw.blip_fs5, 1)
        soundIdToggleOff = soundPlayer.load(this.context, R.raw.blip_d5, 1)
    }

    @RequiresApi(Build.VERSION_CODES.N)
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

        // Put all bindings into a list for easily assigning them listeners
        listOfBrailleButtons = listOf(
                binding.buttons.toggleButton1,
                binding.buttons.toggleButton4,
                binding.buttons.toggleButton2,
                binding.buttons.toggleButton5,
                binding.buttons.toggleButton3,
                binding.buttons.toggleButton6
        )
        Log.i("HomeFragment.kt", "Binding the buttons")
        for (toggleButton: ToggleButton in listOfBrailleButtons) {
            val buttonNum: Int = toggleButton.tag.toString().toInt()
            // Add listeners for updating viewModel data once the buttons are toggled
            toggleButton.setOnCheckedChangeListener { view, isChecked ->
                homeViewModel.buttonFlip(buttonNum, isChecked)
                if (isChecked) {soundPlayer.play(soundIdToggleOn, 0.5f, 1f, 0, 0, 1f)}
                else {soundPlayer.play(soundIdToggleOff, 1f, 0.5f, 0, 0, 1f)}
            }
            // For starting a drag
            //toggleButton.setOnLongClickListener(longClickListener)
            toggleButton.setOnTouchListener(onTouchListener)
            // For responding to a drag entering the button
            toggleButton.setOnDragListener(dragListener)
        }

        // Bind additional buttons to add and remove letters from main string list
        binding.buttonClear.setOnClickListener { removeLetter() }
        binding.addLetterButton.setOnClickListener { addLetter()}
        binding.toggleButtonShowAsDots.setOnCheckedChangeListener { buttonView, isChecked ->  homeViewModel.setShowAsDots(isChecked)}

//        binding.buttons.toggleButton6.isHapticFeedbackEnabled = true
//        binding.buttons.toggleButton6.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) { buttonView.performHapticFeedback(LONG_PRESS) }
//            else { buttonView.performHapticFeedback(LONG_PRESS)}
//            homeViewModel.buttonFlip(6, isChecked)
//        }

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


    @RequiresApi(Build.VERSION_CODES.N)
    private val dragListener: View.OnDragListener = View.OnDragListener { v, event ->
        v as ToggleButton
        var check: Boolean = v.isChecked
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED -> {
                val extras: PersistableBundle = event.clipDescription.extras
                if (v.tag.toString().toInt() != extras.getInt("buttonStartedDrag")) {
                    v.isChecked = extras.getBoolean("checkedStatus") //TODO use something else to drop the api level perhaps
                }
                Log.i("HomeFragment.kt", "View Entered")
                true
            }
            DragEvent.ACTION_DRAG_STARTED -> {
                Log.i("HomeFragment.kt", "Drag Event Started, Button returns true")
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> true
            DragEvent.ACTION_DROP -> true
            DragEvent.ACTION_DRAG_ENDED -> {true}
        }
        return@OnDragListener true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    val longClickListener: View.OnLongClickListener = View.OnLongClickListener { it: View ->
        it as ToggleButton
        it.toggle()
        val isChecked: Boolean = it.isChecked
        val buttonStartedDrag: Int = (it.tag as String).toInt()


        // Create a new ClipData.
        // This is done in two steps to provide clarity. The convenience method
        // ClipData.newPlainText() can create a plain text ClipData in one step.

        // Create a new ClipData.Item from the ImageView object's tag
        val item = ClipData.Item(it.tag as? CharSequence)

        // Create a new ClipData using the tag as a label, the plain text MIME type, and
        // the already-created item. This will create a new ClipDescription object within the
        // ClipData, and set its MIME type entry to "text/plain"
        val dData = ClipData("", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
        val bundle = PersistableBundle()
        bundle.putInt("buttonStartedDrag", buttonStartedDrag)
        bundle.putBoolean("checkedStatus", isChecked)
        dData.description.extras = bundle

        // Instantiates the drag shadow builder.
        val myShadow = MyDragShadowBuilder(it)

        // Starts the drag
        it.startDrag(
                dData,
                myShadow,
                null,
                0
        )

        return@OnLongClickListener true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("ClickableViewAccessibility")
    val onTouchListener = View.OnTouchListener { it: View, event: MotionEvent ->
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                it as ToggleButton
                it.toggle()
                val isChecked: Boolean = it.isChecked
                val buttonStartedDrag: Int = (it.tag as String).toInt()


                // Create a new ClipData.
                // This is done in two steps to provide clarity. The convenience method
                // ClipData.newPlainText() can create a plain text ClipData in one step.

                // Create a new ClipData.Item from the ImageView object's tag
                val item = ClipData.Item(it.tag as? CharSequence)

                // Create a new ClipData using the tag as a label, the plain text MIME type, and
                // the already-created item. This will create a new ClipDescription object within the
                // ClipData, and set its MIME type entry to "text/plain"
                val dData = ClipData("", arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)
                val bundle = PersistableBundle()
                bundle.putInt("buttonStartedDrag", buttonStartedDrag)
                bundle.putBoolean("checkedStatus", isChecked)
                dData.description.extras = bundle

                // Instantiates the drag shadow builder.
                val myShadow = MyDragShadowBuilder(it)

                // Starts the drag
                it.startDrag(
                        dData,
                        myShadow,
                        null,
                        0
                )

                return@OnTouchListener true
            }
            else -> { return@OnTouchListener true }
        }
    }

    private class MyDragShadowBuilder(v: View) : View.DragShadowBuilder(v) {

        private val shadow = ColorDrawable(Color.LTGRAY)
        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
            // Sets the width of the shadow to half the width of the original View
            val width: Int = view.width / 2

            // Sets the height of the shadow to half the height of the original View
            val height: Int = view.height / 2

            // The drag shadow is a ColorDrawable. This sets its dimensions to be the same as the
            // Canvas that the system will provide. As a result, the drag shadow will fill the
            // Canvas.
            shadow.setBounds(0, 0, width, height)

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height)

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2)
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system constructs
        // from the dimensions passed in onProvideShadowMetrics().
        override fun onDrawShadow(canvas: Canvas) {
            // Draws the ColorDrawable in the Canvas passed in from the system.
            //shadow.draw(canvas)
        }

    }

}