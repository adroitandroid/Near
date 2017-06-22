package com.adroitandroid.p2pchat;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.adroitandroid.near.connect.NearConnect;
import com.adroitandroid.near.discovery.NearDiscovery;
import com.adroitandroid.near.model.Host;
import com.adroitandroid.p2pchat.databinding.ActivityMainBinding;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final long DISCOVERABLE_TIMEOUT_MILLIS = 60000;
    private static final long DISCOVERY_TIMEOUT_MILLIS = 10000;
    private static final long DISCOVERABLE_PING_INTERVAL_MILLIS = 5000;
    public static final String MESSAGE_REQUEST_START_CHAT = "start_chat";
    public static final String MESSAGE_RESPONSE_DECLINE_REQUEST = "decline_request";
    public static final String MESSAGE_RESPONSE_ACCEPT_REQUEST = "accept_request";
    private NearDiscovery mNearDiscovery;
    private NearConnect mNearConnect;
    private ActivityMainBinding binding;
    private Snackbar mDiscoveryInProgressSnackbar;
    private ParticipantsAdapter mParticipantsAdapter;
    private boolean mDiscovering;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mNearDiscovery = new NearDiscovery.Builder()
                .setContext(this)
                .setDiscoverableTimeoutMillis(DISCOVERABLE_TIMEOUT_MILLIS)
                .setDiscoveryTimeoutMillis(DISCOVERY_TIMEOUT_MILLIS)
                .setDiscoverablePingIntervalMillis(DISCOVERABLE_PING_INTERVAL_MILLIS)
                .setDiscoveryListener(getNearDiscoveryListener(), Looper.getMainLooper())
                .build();
        binding.startChattingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDiscovering) {
                    stopDiscovery();
                } else {
                    if (binding.handleEt.getText().length() > 0) {
                        mNearDiscovery.makeDiscoverable(binding.handleEt.getText().toString());
                        startDiscovery();
                    } else {
                        Snackbar.make(binding.getRoot(), "Please type in a handle first",
                                Snackbar.LENGTH_INDEFINITE).show();
                    }
                }
            }
        });

        mNearConnect = new NearConnect.Builder()
                .fromDiscovery(mNearDiscovery)
                .setContext(this).setListener(getNearConnectListener(), Looper.getMainLooper())
                .build();
        mNearConnect.startReceiving();

        mParticipantsAdapter = new ParticipantsAdapter(new ParticipantsAdapter.Listener() {
            @Override
            public void sendChatRequest(Host host) {
                mNearConnect.send(MESSAGE_REQUEST_START_CHAT.getBytes(), host);
            }
        });
        binding.participantsRv.setLayoutManager(new LinearLayoutManager(this));
        binding.participantsRv.setAdapter(mParticipantsAdapter);
    }

    @NonNull
    private NearDiscovery.Listener getNearDiscoveryListener() {
        return new NearDiscovery.Listener() {
            @Override
            public void onPeersUpdate(Set<Host> hosts) {
                mParticipantsAdapter.setData(hosts);
            }

            @Override
            public void onDiscoveryTimeout() {
                Snackbar.make(binding.getRoot(),
                        "No other participants found",
                        Snackbar.LENGTH_LONG).show();
                binding.discoveryPb.setVisibility(View.GONE);
                mDiscovering = false;
                binding.startChattingBtn.setText("Start Chatting");
            }

            @Override
            public void onDiscoveryFailure(Throwable e) {
                Snackbar.make(binding.getRoot(),
                        "Something went wrong while searching for participants",
                        Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onDiscoverableTimeout() {
                Toast.makeText(MainActivity.this, "You're not discoverable anymore", Toast.LENGTH_LONG).show();
            }
        };
    }

    @NonNull
    private NearConnect.Listener getNearConnectListener() {
        return new NearConnect.Listener() {
            @Override
            public void onReceive(byte[] bytes, final Host sender) {
                if (bytes != null) {
                    switch (new String(bytes)) {
                        case MESSAGE_REQUEST_START_CHAT:
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(sender.getName() + " would like to start chatting with you.")
                                    .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearDiscovery.stopDiscovery();
                                            mNearConnect.send(MESSAGE_RESPONSE_ACCEPT_REQUEST.getBytes(), sender);
                                            mNearConnect.stopReceiving(true);
                                            ChatActivity.start(MainActivity.this, sender);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearConnect.send(MESSAGE_RESPONSE_DECLINE_REQUEST.getBytes(), sender);
                                        }
                                    }).create().show();
                            break;
                        case MESSAGE_RESPONSE_DECLINE_REQUEST:
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(sender.getName() + " is busy at the moment.")
                                    .setNeutralButton("Ok", null).create().show();
                            break;
                        case MESSAGE_RESPONSE_ACCEPT_REQUEST:
                            mNearDiscovery.stopDiscovery();
                            mNearConnect.stopReceiving(true);
                            ChatActivity.start(MainActivity.this, sender);
                            break;
                    }
                }
            }

            @Override
            public void onSendComplete(long jobId) {

            }

            @Override
            public void onSendFailure(Throwable e, long jobId) {

            }

            @Override
            public void onStartListenFailure(Throwable e) {

            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNearDiscovery.stopDiscovery();
        mNearConnect.stopReceiving(true);
    }

    private void stopDiscovery() {
        mNearDiscovery.stopDiscovery();
        mNearDiscovery.makeNonDiscoverable();

        mDiscovering = false;

        mDiscoveryInProgressSnackbar.dismiss();
        binding.participantsRv.setVisibility(View.GONE);
        binding.discoveryPb.setVisibility(View.GONE);
        binding.startChattingBtn.setText("Start Chatting");
    }

    private void startDiscovery() {
        mDiscovering = true;
        mNearDiscovery.startDiscovery();
        mDiscoveryInProgressSnackbar = Snackbar.make(binding.getRoot(), "Looking for chat participants",
                Snackbar.LENGTH_INDEFINITE);
        mDiscoveryInProgressSnackbar.show();
        mParticipantsAdapter.setData(new ArraySet<Host>());
        binding.participantsRv.setVisibility(View.VISIBLE);
        binding.discoveryPb.setVisibility(View.VISIBLE);
        binding.startChattingBtn.setText("Stop Searching");
    }
}
