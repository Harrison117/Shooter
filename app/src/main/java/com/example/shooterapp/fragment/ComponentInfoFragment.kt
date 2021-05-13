package com.example.shooterapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil

import androidx.navigation.fragment.navArgs

import com.example.shooterapp.R
import com.example.shooterapp.databinding.FragmentComponentInfoBinding

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ComponentInfoFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentComponentInfoBinding

    private val args: ArFragmentArgs by navArgs()

    private lateinit var componentNameTextView: TextView
    private lateinit var componentDescTextView: TextView
    private lateinit var componentProbTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_component_info,
            container,
            false
        )

        componentNameTextView = binding.componentName
        componentDescTextView = binding.componentDesc
        componentProbTextView = binding.componentProb

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        componentNameTextView.text = args.componentDisplay
        when(args.component) {
            "power" -> {
                componentDescTextView.text = getString(R.string.power_component_desc)
                componentProbTextView.text = getString(R.string.power_component_prob)
            }
            "mboard" -> {
                componentDescTextView.text = getString(R.string.mboard_component_desc)
                componentProbTextView.text = getString(R.string.mboard_component_prob)
            }
            "hdrive" -> {
                componentDescTextView.text = getString(R.string.hdrive_component_desc)
                componentProbTextView.text = getString(R.string.hdrive_component_prob)
            }
            else -> {
                componentDescTextView.text = getString(R.string.cpu_component_desc)
                componentProbTextView.text = getString(R.string.cpu_component_prob)
            }
        }
    }
}