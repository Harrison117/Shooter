package com.example.shooterapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.shooterapp.R
import com.example.shooterapp.databinding.FragmentMenuBinding

class MenuFragment: Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentMenuBinding>(inflater, R.layout.fragment_menu, container, false)

        binding.menuButtonStart.setOnClickListener { view: View ->
            view.findNavController()
                    .navigate(MenuFragmentDirections.actionMenuFragmentToPermissionsFragment())
        }

        binding.testArStart.setOnClickListener { view: View ->
            view.findNavController()
                    .navigate(MenuFragmentDirections.actionMenuFragmentToArFragment())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }


}