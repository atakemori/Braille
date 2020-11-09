package com.takemori.braille.ui.home

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.*
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
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.takemori.braille.R
import com.takemori.braille.databinding.FragmentHomeBinding
import kotlin.random.Random

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    private lateinit var binding: FragmentHomeBinding

    private lateinit var listOfBrailleButtons: List<ToggleButton>

    lateinit var sharedPreferences: SharedPreferences
    var optionPlaySounds: Boolean = false
    var optionPlayAnimations: Boolean = false
    var globalOffset: Point = Point()

    private final lateinit var soundPlayer:  SoundPool
    private var soundIdToggleOn: Int = 0
    private var soundIdToggleOff: Int = 0

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
//        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java) original
//        homeViewModel = ViewModelProvider(this.context).get(HomeViewModel::class.java)
        homeViewModel = ViewModelProvider(this.requireActivity()).get(HomeViewModel::class.java)

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
        binding.copyButton.setOnClickListener { copyMainText() }
//        binding.buttons.toggleButton6.isHapticFeedbackEnabled = true
//        binding.buttons.toggleButton6.setOnCheckedChangeListener { buttonView, isChecked ->
//            if (isChecked) { buttonView.performHapticFeedback(LONG_PRESS) }
//            else { buttonView.performHapticFeedback(LONG_PRESS)}
//            homeViewModel.buttonFlip(6, isChecked)
//        }

        return binding.root
    }

    @ExperimentalStdlibApi
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Posts a runnable on the UI thread to notify and queue assigning functions
        // to the Braille buttons only after the views are laid out.
        //   Doing this before IN ANY OF THE GODDAMN ONCREATE FUNCTIONS IN ANDROID will cause views to have no position.
        val mainLayout: ConstraintLayout = binding.homeMainLayout
        mainLayout.post { ->
            assignButtonFunctions()
        }
    }

    override fun onStop() {
        super.onStop()
        // Docu recommends to set SoundPool to null after release, but I would rather
        // not since kotlin will require safe calls throughout usage
        soundPlayer.release()
        //soundPlayer = null
    }

    var mainTextRect: Rect = Rect()
    val buttonsLayoutRect: Rect = Rect()

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


    @RequiresApi(Build.VERSION_CODES.N)
    @ExperimentalStdlibApi
    private fun assignButtonFunctions() {
        // Do the work regarding view and button coordinates for animation paths.
        // Attempting to do in the onCreate methods did not work since the Views
        // had no position components and were not shown yet.
        binding.mainTextFrame.getGlobalVisibleRect(mainTextRect)
        binding.buttons.getGlobalVisibleRect(buttonsLayoutRect)
        Log.i("HomeFragment.kt", "HERE ${buttonsLayoutRect.toShortString()}")
        val tempMainLayoutRect: Rect = Rect()
        binding.homeMainLayout.getGlobalVisibleRect(tempMainLayoutRect, globalOffset)

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
                isFocusable = false
                isEnabled = false
                id = buttonNum + 5000
                x = toggleButton.x
                y = toggleButton.y
                setImageResource(R.drawable.dot_ghost)
                setColorFilter(Color.parseColor("#B0E4E2E2"))
                adjustViewBounds = true
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
                Log.i("HomeFragment.kt", "movingView initial X: " + toggleButton.x +", " + buttonsLayoutRect.left)
            }
            binding.homeMainLayout.addView(movingView)

            // Get the animator set for sending the masks to the text box.
            val viewMask: ImageView = binding.homeMainLayout.findViewById(buttonNum + 5000)
            val animatorSet: AnimatorSet = generateAnimatorSet(toggleButton, viewMask)

            // Add OnChecked listeners for updating viewModel data and animations once the buttons are toggled
            toggleButton.setOnCheckedChangeListener { view, isChecked ->
                homeViewModel.buttonFlip(buttonNum, isChecked)

                // Animate the button check
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
                    animatorSet.start()
                }
            }
        }
    }

    private fun generateAnimatorSet(buttonView: ToggleButton, targetView: View): AnimatorSet {
        val buttonX = buttonView.x + buttonsLayoutRect.left
        val buttonY = buttonView.y + buttonsLayoutRect.top  - globalOffset.y

        // Define animator set
        val animatorSet: AnimatorSet = AnimatorSet()

        // Reset View properties
        animatorSet.doOnStart {
            targetView.visibility = View.VISIBLE
//            targetView.scaleX = 1.0f
//            targetView.scaleY = 1.0f
            targetView.alpha = 1.0f
            targetView.x = buttonX
            targetView.y = buttonY
        }

        // Create the path
        val path = Path().apply {
            moveTo(buttonX, buttonY)
            // Not buttonView.height/2 since the scaling is not using a center pivot point
            quadTo(1000f, 300f, mainTextRect.exactCenterX() - (buttonView.width / 2), mainTextRect.exactCenterY() - (3*buttonView.height/4))
        }

        // DEBUG
        val textBoxX = mainTextRect.exactCenterX() - (buttonView.width / 2)
        val textBoxY = mainTextRect.exactCenterY() - (buttonView.height / 2)
        Log.i("HomeFragment.kt", "\n buttonXY: ($buttonX,$buttonY)\n    |\n    |\n    V\n")
        Log.i("HomeFragment.kt", "textBoxXY: ($textBoxX, $textBoxY)")


        // Create animator object to move View X and Y
        val pathAnimator = ObjectAnimator.ofFloat(targetView, View.X, View.Y, path)
        pathAnimator.interpolator = DecelerateInterpolator(1.5f)

        // Repeat for scale, tint, etc...
        val scaleXAnimator = ObjectAnimator.ofFloat(targetView, View.SCALE_X, 1.0f, 0.0f, 0.0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(targetView, View.SCALE_Y, 1.0f, 0.0f, 0.0f)
        scaleXAnimator.interpolator = AnticipateInterpolator(0.20f)
        scaleYAnimator.interpolator = AnticipateInterpolator(0.20f)
        val alphaAnimator = ObjectAnimator.ofFloat(targetView, View.ALPHA, 1.0f, 0.5f, 0.0f)

        // Add to animator set, for set duration, remove View visibility on finish
        animatorSet.playTogether(pathAnimator, scaleXAnimator, scaleYAnimator, alphaAnimator)
        animatorSet.duration = 350
        animatorSet.startDelay = buttonView.tag.toString().toLong() * 25
        animatorSet.doOnEnd {
            // Reset the animated View and disappear
            targetView.visibility = View.GONE
        }

        // Assign animator set to targetView, return.
        return animatorSet
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


    public fun copyMainText() {
        val clipboard: ClipboardManager? = getSystemService(this.requireContext(), ClipboardManager::class.java)
        val clip: ClipData = ClipData.newPlainText("MainText", homeViewModel.lettersToString.value)
        // Add the text to the clipboard. If not found, then pop an error toast.
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this.requireContext(), "Copied to clipboard.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this.requireContext(), "Cannot set clipboard text.", Toast.LENGTH_SHORT).show()
        }
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