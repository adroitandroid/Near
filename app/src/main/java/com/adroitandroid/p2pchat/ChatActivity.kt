package com.adroitandroid.p2pchat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArraySet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.adroitandroid.near.connect.NearConnect
import com.adroitandroid.near.model.Host
import com.adroitandroid.p2pchat.ChatActivity
import com.adroitandroid.p2pchat.databinding.ActivityChatBinding
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit

/**
 * Created by pv on 22/06/17.
 */
class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding

    private lateinit var nearConnect: NearConnect
    private var participant: Host? = null
    private var statusDisposable: Disposable? = null
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()

        participant = intent.getParcelableExtra(BUNDLE_PARTICIPANT)
        title = "Chat with ${participant?.name}"
        initNearConnect()
        initMessagingUi()
    }

    private fun initViews() = with(binding) {
        chatRv.layoutManager = LinearLayoutManager(this@ChatActivity)
        chatAdapter = ChatAdapter()
        chatRv.adapter = chatAdapter
        chatAdapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                chatRv.scrollToPosition(positionStart)
            }
        })
    }

    private fun initNearConnect() {
        val peers = ArraySet<Host>()
        peers.add(participant)
        nearConnect = NearConnect.Builder()
                .forPeers(peers)
                .setContext(this)
                .setListener(nearConnectListener, Looper.getMainLooper()).build()
        nearConnect.startReceiving()
    }

    //update UI with sent status if necessary
    private val nearConnectListener: NearConnect.Listener
        get() = object : NearConnect.Listener {
            override fun onReceive(bytes: ByteArray, sender: Host) = with(binding) {
                when (val data = String(bytes)) {
                    STATUS_TYPING -> {
                        statusTv.visibility = View.VISIBLE
                        statusDisposable!!.dispose()
                        statusDisposable = Observable.timer(1, TimeUnit.SECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe { statusTv.visibility = View.INVISIBLE }
                    }
                    STATUS_STOPPED_TYPING -> statusTv.visibility = View.INVISIBLE
                    STATUS_EXIT_CHAT -> {
                        statusTv.visibility = View.INVISIBLE
                        msgLl.visibility = View.GONE
                        AlertDialog.Builder(this@ChatActivity)
                                .setMessage(String.format("%s has left the chat.", sender.name))
                                .setNeutralButton("Ok") { _, _ ->
                                    nearConnect.stopReceiving(true)
                                    this@ChatActivity.setResult(RESULT_OK)
                                    finish()
                                }.create().show()
                    }
                    else -> if (data.startsWith(MSG_PREFIX)) {
                        chatAdapter.addMessage(data.substring(4), sender.name)
                    }
                }
            }

            override fun onSendComplete(jobId: Long) {
                //update UI with sent status if necessary
            }

            override fun onSendFailure(e: Throwable?, jobId: Long) {}
            override fun onStartListenFailure(e: Throwable?) {}
        }

    private fun initMessagingUi() = with(binding) {
        statusTv.text = String.format("%s is typing...", participant?.name)
        sendBtn.setOnClickListener {
            participant?.let { nearConnect.send(STATUS_STOPPED_TYPING.toByteArray(), it) }

            val message = msgEt.text.toString()
            participant?.let { nearConnect.send((MSG_PREFIX + message).toByteArray(), it) }
            chatAdapter.addMessage(message, "You")
            msgEt.setText("")
        }
        RxTextView.textChanges(msgEt)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribe(object : Observer<CharSequence> {
                    override fun onError(e: Throwable) {}
                    override fun onComplete() {}
                    override fun onSubscribe(d: Disposable) {}
                    override fun onNext(charSequence: CharSequence) {
                        if (charSequence.isNotEmpty()) {
                            participant?.let { nearConnect.send(STATUS_TYPING.toByteArray(), it) }
                        } else {
                            participant?.let { nearConnect.send(STATUS_STOPPED_TYPING.toByteArray(), it) }
                        }
                    }
                })
        statusDisposable = Observable.timer(0, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { statusTv.visibility = View.INVISIBLE }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        participant?.let { nearConnect.send(STATUS_EXIT_CHAT.toByteArray(), it) }
        nearConnect.stopReceiving(true)
    }

    companion object {
        const val BUNDLE_PARTICIPANT = "bundle_participant"
        const val MSG_PREFIX = "msg:"
        private const val STATUS_TYPING = "status:typing"
        private const val STATUS_STOPPED_TYPING = "status:stopped_typing"
        private const val STATUS_EXIT_CHAT = "status:exit_chat"
        fun start(activity: Activity, participant: Host?) {
            val intent = Intent(activity, ChatActivity::class.java)
            intent.putExtra(BUNDLE_PARTICIPANT, participant)
            activity.startActivityForResult(intent, 1234)
        }
    }
}