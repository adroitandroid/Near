package com.adroitandroid.p2pchat

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArraySet
import androidx.recyclerview.widget.LinearLayoutManager
import com.adroitandroid.near.connect.NearConnect
import com.adroitandroid.near.discovery.NearDiscovery
import com.adroitandroid.near.model.Host
import com.adroitandroid.p2pchat.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var nearDiscovery: NearDiscovery
    private lateinit var nearConnect: NearConnect

    private lateinit var participantsAdapter: ParticipantsAdapter
    private var discoveryInProgressSnackbar: Snackbar? = null
    private var isDiscovering = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nearDiscovery = NearDiscovery.Builder()
                .setContext(this)
                .setDiscoverableTimeoutMillis(DISCOVERABLE_TIMEOUT_MILLIS)
                .setDiscoveryTimeoutMillis(DISCOVERY_TIMEOUT_MILLIS)
                .setDiscoverablePingIntervalMillis(DISCOVERABLE_PING_INTERVAL_MILLIS)
                .setDiscoveryListener(nearDiscoveryListener, Looper.getMainLooper())
                .build()

        nearConnect = NearConnect.Builder()
                .fromDiscovery(nearDiscovery)
                .setContext(this)
                .setListener(nearConnectListener, Looper.getMainLooper())
                .build()

        initViews()
    }

    private fun initViews() = with(binding) {
        startChattingBtn.setOnClickListener {
            if (isDiscovering) {
                stopDiscovery()
            } else {
                if (handleEt.text.isNotEmpty()) {
                    nearDiscovery.makeDiscoverable(handleEt.text.toString())
                    startDiscovery()
                    if (!nearConnect.isReceiving) {
                        nearConnect.startReceiving()
                    }
                } else {
                    Snackbar.make(root, "Please type in a handle first",
                            Snackbar.LENGTH_INDEFINITE).show()
                }
            }
        }

        participantsAdapter = ParticipantsAdapter { host: Host -> nearConnect.send(MESSAGE_REQUEST_START_CHAT.toByteArray(), host) }
        participantsRv.layoutManager = LinearLayoutManager(this@MainActivity)
        participantsRv.adapter = participantsAdapter
    }


    private val nearDiscoveryListener: NearDiscovery.Listener
        get() = object : NearDiscovery.Listener {
            override fun onPeersUpdate(host: Set<Host>) {
                participantsAdapter.setData(host)
            }

            override fun onDiscoveryTimeout() {
                Snackbar.make(binding.root,
                        "No other participants found",
                        Snackbar.LENGTH_LONG).show()
                binding.discoveryPb.visibility = View.GONE
                isDiscovering = false
                binding.startChattingBtn.text = "Start Chatting"
            }

            override fun onDiscoveryFailure(e: Throwable) {
                Snackbar.make(binding.root,
                        "Something went wrong while searching for participants",
                        Snackbar.LENGTH_LONG).show()
            }

            override fun onDiscoverableTimeout() {
                Toast.makeText(this@MainActivity, "You're not discoverable anymore", Toast.LENGTH_LONG).show()
            }
        }

    private val nearConnectListener: NearConnect.Listener
        get() = object : NearConnect.Listener {
            override fun onReceive(bytes: ByteArray, sender: Host) {
                when (String(bytes)) {
                    MESSAGE_REQUEST_START_CHAT -> AlertDialog.Builder(this@MainActivity)
                            .setMessage(sender.name + " would like to start chatting with you.")
                            .setPositiveButton("Start") { _: DialogInterface?, _: Int ->
                                nearConnect.send(MESSAGE_RESPONSE_ACCEPT_REQUEST.toByteArray(), sender)
                                stopNearServicesAndStartChat(sender)
                            }
                            .setNegativeButton("Cancel") { _: DialogInterface?, _: Int -> nearConnect.send(MESSAGE_RESPONSE_DECLINE_REQUEST.toByteArray(), sender) }.create().show()
                    MESSAGE_RESPONSE_DECLINE_REQUEST -> AlertDialog.Builder(this@MainActivity)
                            .setMessage(sender.name + " is busy at the moment.")
                            .setNeutralButton("Ok", null).create().show()
                    MESSAGE_RESPONSE_ACCEPT_REQUEST -> stopNearServicesAndStartChat(sender)
                }
            }

            override fun onSendComplete(jobId: Long) {}
            override fun onSendFailure(e: Throwable?, jobId: Long) {}
            override fun onStartListenFailure(e: Throwable?) {}
        }

    private fun stopNearServicesAndStartChat(sender: Host) {
        nearConnect.stopReceiving(true)
        nearDiscovery.stopDiscovery()
        ChatActivity.start(this@MainActivity, sender)
    }

    override fun onDestroy() {
        super.onDestroy()
        nearDiscovery.stopDiscovery()
        nearConnect.stopReceiving(true)
    }

    private fun stopDiscovery() = with(binding) {
        nearDiscovery.stopDiscovery()
        nearDiscovery.makeNonDiscoverable()
        isDiscovering = false
        discoveryInProgressSnackbar?.dismiss()
        participantsRv.visibility = View.GONE
        discoveryPb.visibility = View.GONE
        startChattingBtn.text = "Start Chatting"
    }

    private fun startDiscovery() = with(binding) {
        isDiscovering = true
        nearDiscovery.startDiscovery()
        discoveryInProgressSnackbar = Snackbar.make(root, "Looking for chat participants", Snackbar.LENGTH_INDEFINITE).apply { show() }
        participantsAdapter.setData(ArraySet())
        participantsRv.visibility = View.VISIBLE
        discoveryPb.visibility = View.VISIBLE
        startChattingBtn.text = "Stop Searching"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        nearConnect.startReceiving()
    }

    companion object {
        private const val DISCOVERABLE_TIMEOUT_MILLIS: Long = 60000
        private const val DISCOVERY_TIMEOUT_MILLIS: Long = 10000
        private const val DISCOVERABLE_PING_INTERVAL_MILLIS: Long = 5000
        const val MESSAGE_REQUEST_START_CHAT = "start_chat"
        const val MESSAGE_RESPONSE_DECLINE_REQUEST = "decline_request"
        const val MESSAGE_RESPONSE_ACCEPT_REQUEST = "accept_request"
    }
}