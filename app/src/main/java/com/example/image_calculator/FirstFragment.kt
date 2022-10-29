package com.example.image_calculator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.example.image_calculator.databinding.FragmentFirstBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import permissions.dispatcher.ktx.PermissionsRequester
import permissions.dispatcher.ktx.constructPermissionsRequest


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

@RequiresApi(Build.VERSION_CODES.P)
class FirstFragment : Fragment() {

    private lateinit var getPhoto: PermissionsRequester

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getPhoto = constructPermissionsRequest(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            requiresPermission = ::processClick
        )
    }

    private var _binding: FragmentFirstBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                if (BuildConfig.BUILD_TYPE == CAMERA) {
                    (result.data!!.extras!!["data"] as Bitmap?)?.let {
                        readText(it)
                    }
                } else {
                    val uri: Uri = result.data?.data!!
                    (MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri))?.let {
                        readText(it)
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            binding.total.text = ""
            binding.inputDetails.text = ""

            getPhoto.launch()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun processClick() {
        if (BuildConfig.BUILD_TYPE == CAMERA) {
            openCamera()
        } else {
            selectPhoto()
        }
    }

    private fun openCamera() {
        val cameraIntent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activityResultLauncher.launch(cameraIntent)
    }

    private fun selectPhoto() {
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        activityResultLauncher.launch(intent)
    }

    // Use the text recognition library to extract the texts from the image
    private fun readText(image: Bitmap) {
        binding.image.setImageBitmap(image)
        val inputImage = InputImage.fromBitmap(image, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer
            .process(inputImage)
            .addOnSuccessListener {
                processText(it)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Invalid image", Toast.LENGTH_LONG).show()
            }
    }

    // Process the text extracted from the image
    private fun processText(text: Text) {
        var stringImageText = ""
        if (text.textBlocks.size == 0) {
            return
        }

        for (blockText in text.textBlocks) {
            for (line in blockText.lines) {
                for (elements in line.elements) {
                    stringImageText += " ${elements.text}"
                }
            }
        }

        checkOperation(stringImageText)
    }

    // Identify the operation to be used
    private fun checkOperation(inputString: String) {
        var operation = ' '
        run checking@ {
            inputString.forEachIndexed { _, c ->
                if ((c == MULTIPLICATION_OPTION1) ||
                    (c == MULTIPLICATION_OPTION2) ||
                    (c == DIVISION_OPTION1) ||
                    (c == DIVISION_OPTION2) ||
                    (c == ADDITION) ||
                    (c == SUBTRACTION)
                ){
                    operation = c
                    return@checking
                }
            }
        }

        checkStringForOperation(inputString, operation)
    }

    // Parse the first and second number to be solved
    private fun checkStringForOperation(input: String, operation: Char): Boolean {
        val firstNumber = input.substringBefore(operation).trim().substringAfterLast(" ").trim()
        val secondNumber = input.substringAfter(operation).trim().substringBefore(" ").trim()
        return if ((firstNumber.toIntOrNull() != null) && (secondNumber.toIntOrNull() != null)) {
            val value = performArithmetic(firstNumber.toInt(), secondNumber.toInt(),  operation)
            // Display the results if valid
            if (value != null) {
                binding.total.text = value.toString()
                binding.inputDetails.text = "$firstNumber $operation $secondNumber"
            } else {
                binding.total.text = "Invalid image"
                binding.inputDetails.text = "Invalid image"
            }
            true
        } else {
            false
        }
    }

    // Get the result value after identifying the numbers and the operation
    private fun performArithmetic( firstNumber: Int, secondNumber: Int, method: Char): Int? {
        when(method) {
            MULTIPLICATION_OPTION1 -> {
                return firstNumber * secondNumber
            }

            MULTIPLICATION_OPTION2 -> {
                return firstNumber * secondNumber
            }

            DIVISION_OPTION1 -> {
                return firstNumber / secondNumber
            }

            DIVISION_OPTION2 -> {
                return firstNumber / secondNumber
            }

            ADDITION -> {
                return firstNumber + secondNumber
            }

            SUBTRACTION -> {
                return firstNumber - secondNumber
            }

            else ->{
                return null
            }
        }
    }

    companion object {
        private const val MULTIPLICATION_OPTION1 = 'x'
        private const val MULTIPLICATION_OPTION2 = '*'
        private const val DIVISION_OPTION1 = '/'
        private const val DIVISION_OPTION2 = '%'
        private const val ADDITION = '+'
        private const val SUBTRACTION = '-'
        private const val FILESYSTEM = "fileSystem"
        private const val CAMERA = "camera"
    }
}
