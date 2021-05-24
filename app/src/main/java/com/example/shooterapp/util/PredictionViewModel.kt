package com.example.shooterapp.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PredictionViewModel: ViewModel() {
    private val _prediction = MutableLiveData<Prediction>()
    val prediction: LiveData<Prediction> = _prediction

    fun updateData(prediction: Prediction) {
        _prediction.postValue(prediction)
    }
}
data class Label (val name_display: String, val name: String)

data class Prediction (val label: Label, val confidence: Float)
