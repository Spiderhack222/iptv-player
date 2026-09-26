package com.example.iptvplayer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.iptvplayer.data.DeviceProfileStorage
import com.example.iptvplayer.databinding.ActivityMacActivationBinding
import com.example.iptvplayer.model.DeviceProfile

class MacActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacActivationBinding
    private lateinit var storage: DeviceProfileStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        storage = DeviceProfileStorage(this)

        val existing = storage.getProfile()
        val fallbackMac = intent.getStringExtra(EXTRA_DEFAULT_MAC).orEmpty()

        binding.editPortal.setText(existing?.portalUrl.orEmpty())
        binding.editMac.setText(existing?.macAddress?.ifBlank { fallbackMac } ?: fallbackMac)
        binding.editCode.setText(existing?.accessCode.orEmpty())
        binding.editName.setText(existing?.profileName.orEmpty())
        binding.switchReconnect.isChecked = existing?.autoReconnect ?: true

        binding.btnSaveConnect.setOnClickListener {
            val portal = binding.editPortal.text.toString().trim()
            val mac = binding.editMac.text.toString().trim().uppercase()
            val code = binding.editCode.text.toString().trim()
            val profileName = binding.editName.text.toString().trim().ifBlank {
                getString(R.string.mac_default_profile_name)
            }

            if (portal.isBlank() || mac.isBlank()) {
                Toast.makeText(this, R.string.mac_required_fields, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val profile = DeviceProfile(
                portalUrl = portal,
                macAddress = mac,
                accessCode = code,
                profileName = profileName,
                autoReconnect = binding.switchReconnect.isChecked,
                lastConnectedAt = System.currentTimeMillis()
            )

            storage.saveProfile(profile)
            setResult(RESULT_OK)
            finish()
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_DEFAULT_MAC = "extra_default_mac"
    }
}
