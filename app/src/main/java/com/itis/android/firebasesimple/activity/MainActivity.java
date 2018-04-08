/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.crashlytics.android.Crashlytics;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.model.Message;
import com.itis.android.firebasesimple.utils.Preferences;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        MessageViewHolder(View v) {
            super(v);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            messageImageView = itemView.findViewById(R.id.messageImageView);
            messengerTextView = itemView.findViewById(R.id.messengerTextView);
            messengerImageView = itemView.findViewById(R.id.messengerImageView);
        }
    }

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    private static final int REQUEST_IMAGE = 2;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

    private Button sendButton;
    private RecyclerView messageRecyclerView;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar progressBar;
    private EditText messageEditText;
    private ImageView addMessageImageView;
    private AdView adView;

    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private GoogleApiClient googleApiClient;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private DatabaseReference firebaseDatabaseReference;
    private FirebaseRecyclerAdapter<Message, MessageViewHolder> firebaseAdapter;

    private String username;
    private String photoUrl;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Fabric.with(this, new Crashlytics());

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = ANONYMOUS;

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            username = firebaseUser.getDisplayName();
            if (firebaseUser.getPhotoUrl() != null) {
                photoUrl = firebaseUser.getPhotoUrl().toString();
            }
            if(firebaseUser.getPhoneNumber() != null) {
                username = firebaseUser.getPhoneNumber();
            } else {
                if (firebaseUser.getEmail() != null) {
                    username = firebaseUser.getEmail();
                }
            }
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        progressBar = findViewById(R.id.progressBar);
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        SnapshotParser<Message> parser = dataSnapshot -> {
            Message message = dataSnapshot.getValue(Message.class);
            if (message != null) {
                message.setId(dataSnapshot.getKey());
            }
            return message;
        };
        DatabaseReference messagesRef = firebaseDatabaseReference.child(MESSAGES_CHILD);
        FirebaseRecyclerOptions<Message> options =
                new FirebaseRecyclerOptions.Builder<Message>()
                        .setQuery(messagesRef, parser)
                        .build();

        firebaseAdapter = new FirebaseRecyclerAdapter<Message, MessageViewHolder>(options) {

            @Override
            public MessageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
                LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
                return new MessageViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false));
            }

            @Override
            protected void onBindViewHolder(final MessageViewHolder viewHolder,
                                            int position,
                                            Message message) {

                progressBar.setVisibility(ProgressBar.INVISIBLE);
                if (message.getText() != null) {
                    viewHolder.messageTextView.setText(message.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
                    String imageUrl = message.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance()
                                .getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                task -> {
                                    if (task.isSuccessful()) {
                                        String downloadUrl = task.getResult().toString();
                                        Glide.with(viewHolder.messageImageView.getContext())
                                                .load(downloadUrl)
                                                .into(viewHolder.messageImageView);
                                    } else {
                                        Log.w(TAG, "Getting download url was not successful.",
                                                task.getException());
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(message.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }


                viewHolder.messengerTextView.setText(message.getName());
                if (message.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(MainActivity.this)
                            .load(message.getPhotoUrl())
                            .into(viewHolder.messengerImageView);
                }

                if (message.getText() != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance().update(getMessageIndexable(message));
                }

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(message));
            }
        };

        firebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = firebaseAdapter.getItemCount();
                int lastVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    messageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        messageRecyclerView.setLayoutManager(linearLayoutManager);
        messageRecyclerView.setAdapter(firebaseAdapter);

        // Initialize and request AdMob ad.
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Initialize Firebase Measurement.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Remote Config.
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put("friendly_msg_length", 10L);

        // Apply config settings and default values.
        firebaseRemoteConfig.setConfigSettings(firebaseRemoteConfigSettings);
        firebaseRemoteConfig.setDefaults(defaultConfigMap);

        // Fetch remote config.
        fetchConfig();

        messageEditText = findViewById(R.id.messageEditText);
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(sharedPreferences
                .getInt(Preferences.FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    sendButton.setEnabled(true);
                } else {
                    sendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        addMessageImageView = findViewById(R.id.addMessageImageView);
        addMessageImageView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE);
        });

        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(view -> {
            Message message = new Message(messageEditText.getText().toString(), username,
                    photoUrl, null);
            firebaseDatabaseReference.child(MESSAGES_CHILD).push().setValue(message);
            messageEditText.setText("");
            firebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
        });
    }

    private Action getMessageViewAction(Message message) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(message.getName(), MESSAGE_URL.concat(message.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(Message message) {
        PersonBuilder sender = Indexables.personBuilder();
        if (message.getName() != null) {
            sender.setIsSelf(username.equals(message.getName()))
                    .setName(message.getName())
                    .setUrl(MESSAGE_URL.concat(message.getId() + "/sender"));
        }

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(username)
                .setUrl(MESSAGE_URL.concat(message.getId() + "/recipient"));

        return Indexables.messageBuilder()
                .setName(message.getText())
                .setUrl(MESSAGE_URL.concat(message.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        firebaseAdapter.stopListening();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        firebaseAdapter.startListening();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.invite_menu:
                sendInvitation();
                return true;
            case R.id.crash_menu:
                Log.w("Crashlytics", "Crash button clicked");
                causeCrash();
                return true;
            case R.id.sign_out_menu:
                firebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(googleApiClient);
                firebaseUser = null;
                username = ANONYMOUS;
                photoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;
            case R.id.fresh_config_menu:
                fetchConfig();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void causeCrash() {
        throw new NullPointerException("Fake null pointer exception");
    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder(
                getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    // Fetch the config to determine the allowed length of messages.
    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        // If developer mode is enabled reduce cacheExpiration to 0 so that each fetch goes to the
        // server. This should not be used in release builds.
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        firebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(aVoid -> {
                    // Make the fetched config available via FirebaseRemoteConfig get<type> calls.
                    firebaseRemoteConfig.activateFetched();
                    applyRetrievedLengthLimit();
                })
                .addOnFailureListener(e -> {
                    // There has been an error fetching the config
                    Log.w(TAG, "Error fetching config", e);
                    applyRetrievedLengthLimit();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    final Uri uri = data.getData();
                    Log.d(TAG, "Uri: " + uri.toString());

                    Message tempMessage = new Message(null, username, photoUrl,
                            LOADING_IMAGE_URL);
                    firebaseDatabaseReference.child(MESSAGES_CHILD).push()
                            .setValue(tempMessage, (databaseError, databaseReference) -> {
                                if (databaseError == null) {
                                    String key = databaseReference.getKey();
                                    StorageReference storageReference =
                                            FirebaseStorage.getInstance()
                                                    .getReference(firebaseUser.getUid())
                                                    .child(key)
                                                    .child(uri.getLastPathSegment());

                                    putImageInStorage(storageReference, uri, key);
                                } else {
                                    Log.w(TAG, "Unable to write message to database.",
                                            databaseError.toException());
                                }
                            });
                }
            }
        } else if (requestCode == REQUEST_INVITE) {
            if (resultCode == RESULT_OK) {
                // Use Firebase Measurement to log that invitation was sent.
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_sent");

                // Check how many invitations were sent and log.
                String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                Log.d(TAG, "Invitations sent: " + ids.length);
            } else {
                // Use Firebase Measurement to log that invitation was not sent
                Bundle payload = new Bundle();
                payload.putString(FirebaseAnalytics.Param.VALUE, "inv_not_sent");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, payload);

                // Sending failed or it was canceled, show failure message to the user
                Log.d(TAG, "Failed to send invitation.");
            }
        }
    }

    private void putImageInStorage(StorageReference storageReference, Uri uri, final String key) {
        storageReference.putFile(uri).addOnCompleteListener(MainActivity.this,
                task -> {
                    if (task.isSuccessful()) {
                        Message message =
                                new Message(null, username, photoUrl,
                                        task.getResult().getDownloadUrl().toString());
                        firebaseDatabaseReference.child(MESSAGES_CHILD).child(key)
                                .setValue(message);
                    } else {
                        Log.w(TAG, "Image upload task was not successful.",
                                task.getException());
                    }
                });
    }

    /**
     * Apply retrieved length limit to edit text field. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = firebaseRemoteConfig.getLong("friendly_msg_length");
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
        Log.d(TAG, "FML is: " + friendly_msg_length);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }
}
