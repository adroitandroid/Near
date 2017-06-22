package com.adroitandroid.p2pchat;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.adroitandroid.near.connect.NearConnect;
import com.adroitandroid.near.model.Host;
import com.adroitandroid.p2pchat.databinding.ActivityChatBinding;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by pv on 22/06/17.
 */

public class ChatActivity extends AppCompatActivity {

    public static final String BUNDLE_PARTICIPANT = "bundle_participant";
    public static final String MSG_PREFIX = "msg:";
    private static final String STATUS_TYPING = "status:typing";
    private static final String STATUS_STOPPED_TYPING = "status:stopped_typing";
    private static final String STATUS_EXIT_CHAT = "status:exit_chat";
    private ActivityChatBinding binding;
    private NearConnect mNearConnect;
    private Host mParticipant;
    private Disposable mStatusDisposable;
    private ChatAdapter mChatAdapter;

    public static void start(Activity activity, Host participant) {
        Intent intent = new Intent(activity, ChatActivity.class);
        intent.putExtra(BUNDLE_PARTICIPANT, participant);
        activity.startActivityForResult(intent, 1234);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        binding.chatRv.setLayoutManager(new LinearLayoutManager(this));
        mChatAdapter = new ChatAdapter();
        binding.chatRv.setAdapter(mChatAdapter);
        mChatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                binding.chatRv.scrollToPosition(positionStart);
            }
        });

        mParticipant = getIntent().getParcelableExtra(BUNDLE_PARTICIPANT);
        setTitle("Chat with " + mParticipant.getName());
        initNearConnect();
        initMessagingUi();
    }

    private void initNearConnect() {
        ArraySet<Host> peers = new ArraySet<>();
        peers.add(mParticipant);
        mNearConnect = new NearConnect.Builder()
                .forPeers(peers)
                .setContext(this)
                .setListener(getNearConnectListener(), Looper.getMainLooper()).build();
        mNearConnect.startReceiving();
    }

    @NonNull
    private NearConnect.Listener getNearConnectListener() {
        return new NearConnect.Listener() {
            @Override
            public void onReceive(byte[] bytes, Host sender) {
                if (bytes != null) {
                    String data = new String(bytes);
                    switch (data) {
                        case STATUS_TYPING:
                            binding.statusTv.setVisibility(View.VISIBLE);
                            mStatusDisposable.dispose();
                            mStatusDisposable = Observable.timer(1, TimeUnit.SECONDS)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Consumer<Long>() {
                                        @Override
                                        public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                                            binding.statusTv.setVisibility(View.INVISIBLE);
                                        }
                                    });
                            break;
                        case STATUS_STOPPED_TYPING:
                            binding.statusTv.setVisibility(View.INVISIBLE);
                            break;
                        case STATUS_EXIT_CHAT:
                            binding.statusTv.setVisibility(View.INVISIBLE);
                            binding.msgLl.setVisibility(View.GONE);
                            new AlertDialog.Builder(ChatActivity.this)
                                    .setMessage(String.format("%s has left the chat.", sender.getName()))
                                    .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearConnect.stopReceiving(true);
                                            ChatActivity.this.setResult(RESULT_OK);
                                            ChatActivity.this.finish();
                                        }
                                    }).create().show();
                            break;
                        default:
                            if (data.startsWith(MSG_PREFIX)) {
                                mChatAdapter.addMessage(data.substring(4), sender.getName());
                            }
                            break;
                    }
                }
            }

            @Override
            public void onSendComplete(long jobId) {
//                update UI with sent status if necessary
            }

            @Override
            public void onSendFailure(Throwable e, long jobId) {

            }

            @Override
            public void onStartListenFailure(Throwable e) {

            }
        };
    }

    private void initMessagingUi() {
        binding.statusTv.setText(String.format("%s is typing...", mParticipant.getName()));
        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNearConnect.send(STATUS_STOPPED_TYPING.getBytes(), mParticipant);
                String message = binding.msgEt.getText().toString();
                mNearConnect.send((MSG_PREFIX + message).getBytes(), mParticipant);
                mChatAdapter.addMessage(message, "You");
                binding.msgEt.setText("");
            }
        });
        RxTextView.textChanges(binding.msgEt)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<CharSequence>() {
                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CharSequence charSequence) {
                        if (charSequence.length() > 0) {
                            mNearConnect.send(STATUS_TYPING.getBytes(), mParticipant);
                        } else {
                            mNearConnect.send(STATUS_STOPPED_TYPING.getBytes(), mParticipant);
                        }
                    }
                });
        mStatusDisposable = Observable.timer(0, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                        binding.statusTv.setVisibility(View.INVISIBLE);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mNearConnect.send(STATUS_EXIT_CHAT.getBytes(), mParticipant);
        mNearConnect.stopReceiving(true);
    }
}
