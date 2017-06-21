package com.adroitandroid.p2pchat;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.adroitandroid.near.discovery.NearDiscovery;
import com.adroitandroid.near.model.Host;
import com.adroitandroid.p2pchat.databinding.ActivityMainBinding;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final long DISCOVERABLE_TIMEOUT_MILLIS = 60000;
    private static final long DISCOVERY_TIMEOUT_MILLIS = 60000;
    private static final long DISCOVERABLE_PING_INTERVAL_MILLIS = 5000;
    private NearDiscovery mNearDiscovery;
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
                .setDiscoveryListener(new NearDiscovery.Listener() {
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
                }, Looper.getMainLooper()).build();
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
        mParticipantsAdapter = new ParticipantsAdapter();
        binding.participantsRv.setLayoutManager(new LinearLayoutManager(this));
        binding.participantsRv.setAdapter(mParticipantsAdapter);
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
