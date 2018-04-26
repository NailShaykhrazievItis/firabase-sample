/**
 * Copyright Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itis.android.firebasesimple.activity

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.database.SnapshotParser
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.appindexing.Indexable
import com.google.firebase.appindexing.builders.Indexables
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.model.Message
import com.itis.android.firebasesimple.utils.FRIENDLY_MSG_LENGTH
import de.hdodenhof.circleimageview.CircleImageView
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.adView
import kotlinx.android.synthetic.main.activity_main.addMessageImageView
import kotlinx.android.synthetic.main.activity_main.messageEditText
import kotlinx.android.synthetic.main.activity_main.messageRecyclerView
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_main.sendButton
import kotlinx.android.synthetic.main.item_message.view.messageImageView
import kotlinx.android.synthetic.main.item_message.view.messageTextView
import kotlinx.android.synthetic.main.item_message.view.messengerImageView
import kotlinx.android.synthetic.main.item_message.view.messengerTextView
import java.util.HashMap

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private var linearLayoutManager: LinearLayoutManager? = null

    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var firebaseRemoteConfig: FirebaseRemoteConfig? = null
    private var googleApiClient: GoogleApiClient? = null

    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseUser: FirebaseUser? = null

    private var firebaseDatabaseReference: DatabaseReference? = null
    private var firebaseAdapter: FirebaseRecyclerAdapter<Message, MessageViewHolder>? = null

    private var username: String = ""
    private var photoUrl: String = ""
    private var sharedPreferences: SharedPreferences? = null

    class MessageViewHolder internal constructor(v: View) : RecyclerView.ViewHolder(v) {
        internal var messageTextView: TextView = itemView.messageTextView
        internal var messageImageView: ImageView = itemView.messageImageView
        internal var messengerTextView: TextView = itemView.messengerTextView
        internal var messengerImageView: CircleImageView = itemView.messengerImageView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fabric.with(this, Crashlytics())

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        username = ANONYMOUS

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth?.currentUser

        if (firebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            username = firebaseUser?.displayName.toString()
            if (firebaseUser?.photoUrl != null) {
                photoUrl = firebaseUser?.photoUrl.toString()
            }
            if (username.isEmpty()) {
                username = if (TextUtils.isEmpty(firebaseUser?.email)) {
                    firebaseUser?.phoneNumber.toString()
                } else {
                    firebaseUser?.email.toString()
                }
            }
        }

        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()

        linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager?.stackFromEnd = true

        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        val parser = SnapshotParser{
            val message = it.value as? Message ?: Message()
            message.id = it.key
            message
        }

        val messagesRef = firebaseDatabaseReference?.child(MESSAGES_CHILD) as? Query
        val options = messagesRef?.let {
            FirebaseRecyclerOptions.Builder<Message>()
                    .setQuery(it, parser)
                    .build()
        }

        options?.let {
            firebaseAdapter = object : FirebaseRecyclerAdapter<Message, MessageViewHolder>(it) {
                override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageViewHolder {
                    val inflater = LayoutInflater.from(viewGroup.context)
                    return MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
                }

                override fun onBindViewHolder(viewHolder: MessageViewHolder,
                        position: Int, message: Message) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    if (message.text != null) {
                        viewHolder.messageTextView.text = message.text
                        viewHolder.messageTextView.visibility = TextView.VISIBLE
                        viewHolder.messageImageView.visibility = ImageView.GONE
                    } else {
                        val imageUrl = message.imageUrl.toString()
                        if (imageUrl.startsWith("gs://")) {
                            val storageReference = FirebaseStorage.getInstance()
                                    .getReferenceFromUrl(imageUrl)
                            storageReference.downloadUrl.addOnCompleteListener {
                                if (it.isSuccessful) {
                                    val downloadUrl = it.result.toString()
                                    Glide.with(viewHolder.messageImageView.context)
                                            .load(downloadUrl)
                                            .into(viewHolder.messageImageView)
                                } else {
                                    Log.w(TAG, "Getting download url was not successful.",
                                            it.exception)
                                }
                            }
                        } else {
                            Glide.with(viewHolder.messageImageView.context)
                                    .load(message.imageUrl)
                                    .into(viewHolder.messageImageView)
                        }
                        viewHolder.messageImageView.visibility = ImageView.VISIBLE
                        viewHolder.messageTextView.visibility = TextView.GONE
                    }


                    viewHolder.messengerTextView.text = message.name
                    if (message.photoUrl == null) {
                        viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(this@MainActivity,
                                R.drawable.ic_account_circle_black_36dp))
                    } else {
                        Glide.with(this@MainActivity)
                                .load(message.photoUrl)
                                .into(viewHolder.messengerImageView)
                    }
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance().update(getMessageIndexable(message))

                    // log a view action on it
                    FirebaseUserActions.getInstance().end(getMessageViewAction(message))
                }
            }
        }

        firebaseAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val friendlyMessageCount = firebaseAdapter?.itemCount
                val lastVisiblePosition = linearLayoutManager?.findLastCompletelyVisibleItemPosition()
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (friendlyMessageCount != null) {
                    if (lastVisiblePosition == -1 || positionStart >= friendlyMessageCount - 1
                            && lastVisiblePosition == positionStart - 1) {
                        messageRecyclerView.scrollToPosition(positionStart)
                    }
                }
            }
        })

        messageRecyclerView.layoutManager = linearLayoutManager
        messageRecyclerView.adapter = firebaseAdapter

        // Initialize and request AdMob ad.
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Initialize Firebase Measurement.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Initialize Firebase Remote Config.
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        // Define Firebase Remote Config Settings.
        val firebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build()

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap["friendly_msg_length"] = 10L

        // Apply config settings and default values.
        firebaseRemoteConfig?.setConfigSettings(firebaseRemoteConfigSettings)
        firebaseRemoteConfig?.setDefaults(defaultConfigMap)

        // Fetch remote config.
        fetchConfig()

        messageEditText.filters = arrayOf<InputFilter>(
                InputFilter.LengthFilter(sharedPreferences?.getInt(FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT) ?: 0
                ))
        messageEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                sendButton.isEnabled = charSequence.toString().trim { it <= ' ' }.isNotEmpty()
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        addMessageImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE)
        }

        sendButton.setOnClickListener {
            val message = Message(messageEditText?.text.toString(), username,
                    photoUrl, null)
            firebaseDatabaseReference?.child(MESSAGES_CHILD)?.push()?.setValue(message)
            messageEditText?.setText("")
            firebaseAnalytics?.logEvent(MESSAGE_SENT_EVENT, null)
        }
    }

    private fun getMessageViewAction(message: Message): Action {
        return Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(message.name, MESSAGE_URL + message.id)
                .setMetadata(Action.Metadata.Builder().setUpload(false))
                .build()
    }

    private fun getMessageIndexable(message: Message): Indexable {
        val sender = Indexables.personBuilder()
        sender.setIsSelf(username == message.name)
                .setName(message.name)
                .setUrl(MESSAGE_URL + (message.id + "/sender"))

        val recipient = Indexables.personBuilder()
                .setName(username)
                .setUrl(MESSAGE_URL + (message.id + "/recipient"))

        return Indexables.messageBuilder()
                .setName(message.text)
                .setUrl(MESSAGE_URL + message.id)
                .setSender(sender)
                .setRecipient(recipient)
                .build()
    }

    public override fun onPause() {
        if (adView != null) {
            adView.pause()
        }
        firebaseAdapter?.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        if (adView != null) {
            adView.resume()
        }
        firebaseAdapter?.startListening()
    }

    public override fun onDestroy() {
        if (adView != null) {
            adView.destroy()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.invite_menu -> {
                sendInvitation()
            }
            R.id.crash_menu -> {
                Log.w("Crashlytics", "Crash button clicked")
                causeCrash()
            }
            R.id.sign_out_menu -> {
                firebaseAuth?.signOut()
                Auth.GoogleSignInApi.signOut(googleApiClient)
                firebaseUser = null
                username = ANONYMOUS
                photoUrl = ""
                startActivity(Intent(this, SignInActivity::class.java))
                finish()
            }
            R.id.fresh_config_menu -> {
                fetchConfig()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun causeCrash() {
        throw NullPointerException("Fake null pointer exception")
    }

    private fun sendInvitation() {
        val intent = AppInviteInvitation.IntentBuilder(
                getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build()
        startActivityForResult(intent, REQUEST_INVITE)
    }

    // Fetch the config to determine the allowed length of messages.
    private fun fetchConfig() {
        var cacheExpiration: Long = 3600 // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        //сравнение с true, т.к. иначе пишет что ожидал Boolean, а не Boolean
        if (firebaseRemoteConfig?.info?.configSettings?.isDeveloperModeEnabled == true) {
            cacheExpiration = 0
        }
        firebaseRemoteConfig?.fetch(cacheExpiration)?.addOnSuccessListener {
            // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
            firebaseRemoteConfig?.activateFetched()
            applyRetrievedLengthLimit()
        }?.addOnFailureListener {
            // There has been an error fetching the config
            Log.w(TAG, "Error fetching config", it)
            applyRetrievedLengthLimit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val uri = data.data
                    Log.d(TAG, "Uri: $uri")

                    val tempMessage = Message("", username, photoUrl,
                            LOADING_IMAGE_URL)
                    firebaseDatabaseReference?.child(MESSAGES_CHILD)?.push()?.setValue(tempMessage) { databaseError,
                            databaseReference ->
                        if (databaseError == null) {
                            val key = databaseReference.key
                            val storageReference = FirebaseStorage.getInstance()
                                    .getReference(firebaseUser?.uid ?: "")
                                    .child(key)
                                    .child(uri.lastPathSegment)

                            putImageInStorage(storageReference, uri, key)
                        } else {
                            Log.w(TAG, "Unable to write message to database.",
                                    databaseError.toException())
                        }
                    }
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            val payload = Bundle()
            if (resultCode == Activity.RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent")

                // Check how many invitations were sent and log.
                val ids = data?.let {
                    AppInviteInvitation.getInvitationIds(resultCode, it)
                }
                Log.d(TAG, "Invitations sent: ${(ids?.size ?: "")}")
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent")
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SHARE, payload)

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.")
            }
        }
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri?, key: String) {
        uri?.let {
            storageReference.putFile(it).addOnCompleteListener(this@MainActivity
            ) {
                if (it.isSuccessful) {
                    val message = Message("", username, photoUrl,
                            it.result.downloadUrl.toString())
                    firebaseDatabaseReference?.child(MESSAGES_CHILD)?.child(key)?.setValue(message)
                } else {
                    Log.w(TAG, "Image upload task was not successful.",
                            it.exception)
                }
            }
        }
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private fun applyRetrievedLengthLimit() {
        val friendlyMsgLength = firebaseRemoteConfig?.getLong("friendly_msg_length")
        messageEditText?.filters = friendlyMsgLength?.toInt()?.let { InputFilter.LengthFilter(it) }?.let {
            arrayOf<InputFilter>(
                    it)
        }
        Log.d(TAG, "FML is: $friendlyMsgLength")
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:$connectionResult")
    }

    companion object {

        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        private const val REQUEST_INVITE = 1
        private const val REQUEST_IMAGE = 2
        const val DEFAULT_MSG_LENGTH_LIMIT = 10
        const val ANONYMOUS = "anonymous"
        private const val MESSAGE_SENT_EVENT = "message_sent"
        private const val MESSAGE_URL = "http://friendlychat.firebase.google.com/message/"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }
}
