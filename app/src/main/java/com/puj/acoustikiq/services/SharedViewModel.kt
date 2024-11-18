package com.puj.acoustikiq.services

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.puj.acoustikiq.model.Concert

class SharedViewModel : ViewModel() {
    val concerts: MutableLiveData<HashMap<String, Concert>> = MutableLiveData()
}