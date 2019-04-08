package com.itis.android.firebasesimple.adapter

import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.model.Message
import com.itis.android.firebasesimple.utils.MESSAGE_URL
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_message.*

class MessageAdapter(
        query: Query
) : FirestoreAdapter<MessageAdapter.MessageViewHolder>(query) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MessageViewHolder(inflater.inflate(R.layout.item_message, parent, false))
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getSnapshot(position))
    }

    inner class MessageViewHolder(
            override val containerView: View
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(snapshot: DocumentSnapshot) {
            val message = snapshot.toObject(Message::class.java) ?: return
            message.let {
                showMessage(it)
                showMessenger(it)

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(it))
            }
        }

        private fun showMessenger(message: Message) {
            tv_messenger.text = message.name
            message.photoUrl?.let {
                Glide.with(itemView.context)
                        .load(it)
                        .into(iv_messenger)
            } ?: run {
                iv_messenger.setImageDrawable(ContextCompat.getDrawable(itemView.context,
                        R.drawable.ic_account_circle_black_36dp))
            }
        }

        private fun showMessage(message: Message) {
            if (message.text != null) {
                tv_message.text = message.text
                tv_message.visibility = TextView.VISIBLE
                iv_message.visibility = ImageView.GONE
            } else {
                message.imageUrl?.also { imageUrl ->
                    if (imageUrl.startsWith("https://firebasestorage")) {
                        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        storageReference.downloadUrl
                                .addOnSuccessListener {
                                    it?.let {
                                        Glide.with(itemView.context)
                                                .load(it)
                                                .into(iv_message)
                                    }
                                }
                                .addOnFailureListener {
                                    Log.w("Adapter", "Getting download url was not successful.", it)
                                }
                    } else {
                        Glide.with(itemView.context)
                                .load(message.imageUrl)
                                .into(iv_message)
                    }
                    iv_message.visibility = ImageView.VISIBLE
                    tv_message.visibility = TextView.GONE
                } ?: run {
                    iv_message.visibility = ImageView.GONE
                }
            }
        }

        private fun getMessageViewAction(message: Message): Action {
            return Action.Builder(Action.Builder.VIEW_ACTION)
                    .setObject(message.name ?: "EMPTY", MESSAGE_URL + message.id)
                    .setMetadata(Action.Metadata.Builder().setUpload(false))
                    .build()
        }
    }
}