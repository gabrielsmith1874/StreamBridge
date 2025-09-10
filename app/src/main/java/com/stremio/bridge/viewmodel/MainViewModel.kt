package com.stremio.bridge.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stremio.bridge.model.RokuDevice
import com.stremio.bridge.service.RokuService
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    val rokuService = RokuService()
    
    private val _rokuDevices = MutableLiveData<List<RokuDevice>>()
    val rokuDevices: LiveData<List<RokuDevice>> = _rokuDevices
    
    private val _connectionStatus = MutableLiveData<String>()
    val connectionStatus: LiveData<String> = _connectionStatus
    
    private val _selectedDevice = MutableLiveData<RokuDevice?>()
    val selectedDevice: LiveData<RokuDevice?> = _selectedDevice
    
    fun startRokuDiscovery() {
        viewModelScope.launch {
            _connectionStatus.value = "Discovering Roku devices..."
            
            try {
                val devices = rokuService.discoverRokuDevices()
                _rokuDevices.value = devices
                
                if (devices.isEmpty()) {
                    _connectionStatus.value = "No Roku devices found"
                } else {
                    _connectionStatus.value = "Found ${devices.size} Roku device(s)"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "Discovery failed: ${e.message}"
                _rokuDevices.value = emptyList()
            }
        }
    }
    
    fun selectRokuDevice(device: RokuDevice) {
        _selectedDevice.value = device
        _connectionStatus.value = "Selected: ${device.name}"
    }
    
    fun addManualDevice(device: RokuDevice) {
        val currentDevices = _rokuDevices.value ?: emptyList()
        _rokuDevices.value = currentDevices + device
        selectRokuDevice(device)
    }
    
    fun sendVideoToRoku(
        videoUrl: String,
        title: String,
        format: String,
        callback: (Boolean) -> Unit
    ) {
        val device = _selectedDevice.value
        if (device == null) {
            _connectionStatus.value = "No Roku device selected"
            callback(false)
            return
        }
        
        viewModelScope.launch {
            _connectionStatus.value = "Sending video to ${device.name}..."
            
            try {
                val success = rokuService.sendVideoToRoku(device, videoUrl, title, format)
                
                if (success) {
                    _connectionStatus.value = "Video sent successfully!"
                } else {
                    _connectionStatus.value = "Failed to send video"
                }
                
                callback(success)
            } catch (e: Exception) {
                _connectionStatus.value = "Error: ${e.message}"
                callback(false)
            }
        }
    }
}
