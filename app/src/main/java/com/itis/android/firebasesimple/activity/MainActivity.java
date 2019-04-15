package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.adapter.MessageAdapter;
import com.itis.android.firebasesimple.model.Message;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

import static com.itis.android.firebasesimple.utils.ConstantsKt.ANONYMOUS;
import static com.itis.android.firebasesimple.utils.ConstantsKt.DEFAULT_MSG_LENGTH_LIMIT;
import static com.itis.android.firebasesimple.utils.ConstantsKt.FRIENDLY_MSG_LENGTH;
import static com.itis.android.firebasesimple.utils.ConstantsKt.LOADING_IMAGE_URL;
import static com.itis.android.firebasesimple.utils.ConstantsKt.MESSAGE_SENT_EVENT;
import static com.itis.android.firebasesimple.utils.ConstantsKt.REQUEST_IMAGE;
import static com.itis.android.firebasesimple.utils.ConstantsKt.REQUEST_INVITE;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";

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

    private FirebaseFirestore firestore;
    private MessageAdapter firebaseAdapter;

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
            if (username.isEmpty()) {
                username = firebaseUser.getEmail();
            }
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        progressBar = findViewById(R.id.progressBar);
        CollectionReference messagesRef = initChatAdapter();
        // Initialize and request AdMob ad.
        initAd();
        // Initialize Firebase Measurement.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // Initialize Firebase Remote Config.
        initRemoteConfig();

        initListeners(messagesRef);
    }

    private void initRemoteConfig() {
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(5)
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
    }

    private void initAd() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    @NotNull
    private CollectionReference initChatAdapter() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        firestore = FirebaseFirestore.getInstance();
        CollectionReference messagesRef = firestore.collection(MESSAGES_CHILD);
        Query query = messagesRef.limit(50);
        firebaseAdapter = new MessageAdapter(query);

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
        return messagesRef;
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

                    firestore.collection(MESSAGES_CHILD)
                            .add(tempMessage)
                            .addOnSuccessListener(documentReference -> {
                                String key = documentReference.getId();
                                StorageReference storageReference =
                                        FirebaseStorage.getInstance()
                                                .getReference(firebaseUser.getUid())
                                                .child(key)
                                                .child("images/" + uri.getLastPathSegment());

                                putImageInStorage(storageReference, uri, key);
                            })
                            .addOnFailureListener(error -> {
                                Log.e("Error:", error.getMessage());
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
        storageReference.putFile(uri)
                .addOnSuccessListener(success -> {
                    success.getStorage().getDownloadUrl().addOnSuccessListener(res -> {
                        Message message =
                                new Message(null, username, photoUrl,
                                        res.toString());
                        firestore.collection(MESSAGES_CHILD)
                                .document(key)
                                .set(message);
                    });
                })
                .addOnFailureListener(error->{
                    Log.w(TAG, "Image upload task was not successful.",
                            error);
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

    private void initListeners(CollectionReference messagesRef) {
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
            messagesRef.add(message);
            messageEditText.setText("");
            firebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
        });

        messageEditText = findViewById(R.id.messageEditText);
        messageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(sharedPreferences
                .getInt(FRIENDLY_MSG_LENGTH, DEFAULT_MSG_LENGTH_LIMIT))});
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

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }
}
