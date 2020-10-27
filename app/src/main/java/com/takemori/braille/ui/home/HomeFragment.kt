package com.takemori.braille.ui.home

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.*
import android.view.animation.*
import android.widget.ImageView
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.takemori.braille.R
import com.takemori.braille.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var binding: FragmentHomeBinding

    private lateinit var listOfBrailleButtons: List<ToggleButton>

    lateinit var sharedPreferences: SharedPreferences
    var optionPlaySounds: Boolean = false
    var optionPlayAnimations: Boolean = false

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


    var buttonX: Float = 100f
    var buttonY: Float = 100f
    var mainTextRect: Rect = Rect()
    val buttonsLayoutRect: Rect = Rect()



    @ExperimentalStdlibApi
    override fun onStart() {
        super.onStart()
//        sharedPreferences = this.activity?.getPreferences(Context.MODE_PRIVATE)?: return
//        sharedPreferences = activity?.getPreferences(Context.MODE_PRIVATE)?: return
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var sharedPreferencesToString = sharedPreferences.all.toString()
        Log.i("HomeFragment.kt", "onStart sharedPreferences: $sharedPreferencesToString")
//        sharedPreferences.all.toString()
        optionPlaySounds = sharedPreferences.getBoolean("sound", false)
        optionPlayAnimations = sharedPreferences.getBoolean("animate", false)
        Log.i("HomeFragment.kt", "onStart boolean from sharedPreferences doPlaySounds: $optionPlaySounds")

        soundPlayer = SoundPool(3, AudioManager.USE_DEFAULT_STREAM_TYPE, 0)
        soundIdToggleOn = soundPlayer.load(this.context, R.raw.blip_fs5, 1)
        soundIdToggleOff = soundPlayer.load(this.context, R.raw.blip_d5, 1)
    }





    @ExperimentalStdlibApi
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val buttonBounceAnimation: Animation = AnimationUtils.loadAnimation(this.context, R.anim.button_bounce_v2)

        // Assign the bindings and animations to all buttons
        Log.i("HomeFragment.kt", "Binding the buttons")
        for (toggleButton: ToggleButton in listOfBrailleButtons) {
            // Use the button's xml assigned tag as a reference for functions requiring a unique int
            val buttonNum: Int = toggleButton.tag.toString().toInt()

            // For starting a drag
            //toggleButton.setOnLongClickListener(longClickListener)
            toggleButton.setOnTouchListener(onTouchListener)

            // For responding to a drag entering the button
            toggleButton.setOnDragListener(dragListener)

            // A view that gets animated over the button along a path to the main text
            val movingView:ImageView = ImageView(this.context).apply {
                visibility = View.GONE
                isClickable = false
                id = buttonNum + 5000
                setImageResource(R.drawable.dot_off)
                adjustViewBounds = true
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
                Log.i("HomeFragment.kt", "movingView initial X: " + toggleButton.x +", " + buttonsLayoutRect.left)
            }
            binding.homeMainLayout.addView(movingView)




            Log.i("HomeFragment.kt", "onStart location when binding: " + toggleButton.x +", " + buttonsLayoutRect.left)

            // Add OnChecked listeners for updating viewModel data and animations once the buttons are toggled
            toggleButton.setOnCheckedChangeListener { view, isChecked ->
                homeViewModel.buttonFlip(buttonNum, isChecked)


                // Do the work regarding view and button coordinates for animation paths.
                // Attempting to do in the onCreate methods did not work since the Views
                // had no position components and were not shown yet.
                binding.mainTextFrame.getGlobalVisibleRect(mainTextRect)
                binding.buttons.getGlobalVisibleRect(buttonsLayoutRect)
                Log.i("HomeFragment.kt", "mainTextRect rectangles in onStart$mainTextRect")
                Log.i("HomeFragment.kt", "buttonsLayoutRect rectangles in onStart$buttonsLayoutRect")

                val globalOffset: Point = Point()
                val tempMainLayoutRect: Rect = Rect()
                binding.homeMainLayout.getGlobalVisibleRect(tempMainLayoutRect, globalOffset)
                Log.i("HomeFragment.kt", "MAIN_LAYOUT_RECT: $tempMainLayoutRect")
                Log.i("HomeFragment.kt", "MAIN_LAYOUT_OFFSET: $globalOffset")

                val buttonX = view.x + buttonsLayoutRect.left
                val buttonY = view.y + buttonsLayoutRect.top  - globalOffset.y

                //Animate the button check
                if (isChecked) {
                    (AnimatorInflater.loadAnimator(this.context, R.animator.button_bounce_checkon_property_animator) as AnimatorSet).apply {
                        setTarget(toggleButton)
                        start()
                    }
                } else {
                    (AnimatorInflater.loadAnimator(this.context, R.animator.button_bounce_checkoff_property_animator) as AnimatorSet).apply {
                        setTarget(toggleButton)
                        start()
                    }
                }

                // send a path animation if the button is toggled to off without pressing that button
                if (optionPlayAnimations && (!isChecked && !view.isPressed)) {
                    // Reset the movingView
                    movingView.scaleX = 1.0f
                    movingView.scaleY = 1.0f
                    movingView.x = buttonX
                    movingView.y = buttonY

                    movingView.visibility = View.VISIBLE

                    Log.i("HomeFragment.kt", "buttonX:" + buttonX + ", buttonY: " + buttonY)
                    Log.i("HomeFragment.kt", "ToggleButton relative Coords:" + toggleButton.x + ", " + toggleButton.y)
                    // Sets the path the movingView will follow upwards
                    val path = Path().apply {
                        moveTo(buttonX, buttonY)
                        quadTo(1000f, 300f, mainTextRect.exactCenterX() - (toggleButton.width / 2), mainTextRect.exactCenterY() - (toggleButton.height / 2))
                    }


                    val viewMask: ImageView = binding.homeMainLayout.findViewById(buttonNum + 5000)

                    val pathAnimator = ObjectAnimator.ofFloat(viewMask, View.X, View.Y, path)
                    pathAnimator.interpolator = DecelerateInterpolator(1.5f)

                    val scaleXAnimator = ObjectAnimator.ofFloat(viewMask, View.SCALE_X, 0.0f)
                    val scaleYAnimator = ObjectAnimator.ofFloat(viewMask, View.SCALE_Y, 0.0f)
                    scaleXAnimator.interpolator = AnticipateInterpolator(0.20f)
                    scaleYAnimator.interpolator = AnticipateInterpolator(0.20f) //scaleXAnimator.interpolator

                    //val toWhiteAnimator = ObjectAnimator.ofObject(viewMask, "backgroundColor", ArgbEvaluator(), 0xFFFFFFFF, 0xFFFFFFFF)

                    val animatorSet: AnimatorSet = AnimatorSet()
                    animatorSet.playTogether(pathAnimator, scaleXAnimator, scaleYAnimator)
                    animatorSet.duration = 350
                    animatorSet.doOnEnd { viewMask.visibility = View.GONE }
                    animatorSet.start()

                }

            }
        }
    }

    private fun playSound(soundId: Int) {
        if (optionPlaySounds) {
            soundPlayer.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    private fun playSoundButtonToggle(on: Boolean) {
        if (on) {
            playSound(soundIdToggleOn)
        } else {
            playSound(soundIdToggleOff)
        }
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
                binding.buttonsInclude.toggleButton1,
                binding.buttonsInclude.toggleButton4,
                binding.buttonsInclude.toggleButton2,
                binding.buttonsInclude.toggleButton5,
                binding.buttonsInclude.toggleButton3,
                binding.buttonsInclude.toggleButton6
        )


        // Bind additional buttons to add and remove letters from main string list
        binding.buttonClear.setOnClickListener { removeLetter() }
        binding.addLetterButton.setOnClickListener { addLetter()}
        binding.toggleButtonShowAsDots.setOnCheckedChangeListener { buttonView, isChecked ->  homeViewModel.setShowAsDots(isChecked)}
        binding.clearCellsButton.setOnClickListener { clearButtons() }
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
                // From extras, get whether the drag event is to toggle the buttons
                //  On or Off, then set their checked status correspondingly.
                val extras: PersistableBundle = event.clipDescription.extras
                var dragExtraBool: Boolean = extras.getBoolean("checkedStatus")
                if (v.tag.toString().toInt() != extras.getInt("buttonStartedDrag")) {
                    if (dragExtraBool != v.isChecked) {
                        // Set the button
                        v.isChecked = extras.getBoolean("checkedStatus") //TODO use something else to drop the api level perhaps
                        // Only play a sound if the checked change was triggered by pressing the button.
                        //  This way adding or clearing the cells will not play several sounds
                        playSoundButtonToggle(dragExtraBool)
                    }
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
                playSoundButtonToggle(isChecked)
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