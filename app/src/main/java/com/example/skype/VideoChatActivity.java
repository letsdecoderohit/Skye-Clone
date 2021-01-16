package com.example.skype;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import kotlin.jvm.internal.PropertyReference0Impl;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity
        implements Session.SessionListener ,
        PublisherKit.PublisherListener

{


    private static String API_Key = "47016284";
    private static String SESSION_ID = "2_MX40NzAxNjI4NH5-MTYwNzAxNDMzMDM5M35hck9zQ05KT0pSSEpNMVpxYlIvR1NSRzd-fg";
    private  static  String TOKEN = "T1==cGFydG5lcl9pZD00NzAxNjI4NCZzaWc9OWMwZmQ4NTllZTZiMzM1MTU3MTFmZDQ4MzIxZTlkYzA0M2I3OTIzNzpzZXNzaW9uX2lkPTJfTVg0ME56QXhOakk0Tkg1LU1UWXdOekF4TkRNek1ETTVNMzVoY2s5elEwNUtUMHBTU0VwTk1WcHhZbEl2UjFOU1J6ZC1mZyZjcmVhdGVfdGltZT0xNjA3MDE0Mzg1Jm5vbmNlPTAuOTk0MTQ3NTAxNjQwNzA4OSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNjA5NjA2Mzg1JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private  static final  String LOG_TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_Video_APP_PERM = 124;

    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;

    private FrameLayout mPublisherViewController;
    private FrameLayout mSubscriberViewController;

    private ImageView closeVideoChatBtn;
    private DatabaseReference userRef;
    private String userID = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        closeVideoChatBtn = findViewById(R.id.closed_video_chat_btn);

        closeVideoChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.child(userID).hasChild("Ringing")){

                            userRef.child(userID).child("Ringing")
                                    .removeValue();

                            if(mPublisher != null)
                            {
                                mPublisher.destroy();
                            }

                            if(mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }

                        if(dataSnapshot.child(userID).hasChild("Calling")){

                            userRef.child(userID).child("Calling").removeValue();

                            if(mPublisher != null)
                            {
                                mPublisher.destroy();
                            }

                            if(mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }else{

                            if(mPublisher != null)
                            {
                                mPublisher.destroy();
                            }

                            if(mSubscriber != null)
                            {
                                mSubscriber.destroy();
                            }

                            startActivity(new Intent(VideoChatActivity.this, RegistrationActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        requestPermissions();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode , permissions , grantResults, VideoChatActivity.this);
    }

    @AfterPermissionGranted(RC_Video_APP_PERM)
    private void requestPermissions(){

        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

        if(EasyPermissions.hasPermissions(this,perms)){

            mPublisherViewController = findViewById(R.id.publsiher_container);
            mSubscriberViewController = findViewById(R.id.subscriber_container);

            //initialize and connect to the sessions
            mSession = new com.opentok.android.Session.Builder(this,API_Key,SESSION_ID).build();
            mSession.setSessionListener(VideoChatActivity.this);
            mSession.connect(TOKEN);
        }else{
            EasyPermissions.requestPermissions(this,"Hey this app needs the Mic and Camera Permission, please ALlow.",RC_Video_APP_PERM, perms);
        }
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }


    //2. Publishing a stream to the session
    @Override
    public void onConnected(Session session) {

        Log.i(LOG_TAG,"Session Connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(VideoChatActivity.this);

        mPublisherViewController.addView(mPublisher.getView());

        if(mPublisher.getView() instanceof GLSurfaceView){
            ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);

        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {

        Log.i(LOG_TAG,"Stream Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {

        //3. Subscribing to stream
        Log.i(LOG_TAG,"Stream Received");

        if(mSubscriber == null){
            mSubscriber = new Subscriber.Builder(this,stream).build();
            mSession.subscribe(mSubscriber);
            mSubscriberViewController.addView(mSubscriber.getView());
        }


    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        Log.i(LOG_TAG,"Stream Dropped");

        if(mSubscriber != null){

            mSubscriber = null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

        Log.i(LOG_TAG,"Stream Error");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}