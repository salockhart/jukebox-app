package ca.dal.group5.jukefit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import ca.dal.group5.jukefit.API.APISpec;
import ca.dal.group5.jukefit.API.MockAPI;
import ca.dal.group5.jukefit.API.RequestHandler;
import ca.dal.group5.jukefit.Model.Group;
import ca.dal.group5.jukefit.Model.Member;
import ca.dal.group5.jukefit.Preferences.PreferencesService;

import static ca.dal.group5.jukefit.R.id.determinateBar;

public class PlaylistAndWorkoutActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean isSensorPresent = false;

    ListView leaderboardListView;
    TextView stepsDifferenceTextView;
    TextView stepsTakenTextView;
    TextView speedTextView;
    ProgressBar stepsProgress;

    APISpec ServerAPI;
    PreferencesService prefs;

    String groupCode = "<na>";
    String groupName = "<na>";
    int stepsTaken = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_and_workout);

        groupCode = getIntent().getStringExtra("GROUP_CODE");
        groupName = getIntent().getStringExtra("GROUP_NAME");

        ServerAPI = new MockAPI();
        prefs = new PreferencesService(this);

        leaderboardListView = (ListView) findViewById(R.id.playerProgress);
        leaderboardListView.setItemsCanFocus(false);
        leaderboardListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        stepsDifferenceTextView = (TextView) findViewById(R.id.remainingSteps);

        stepsTakenTextView = (TextView) findViewById(R.id.stepCount);
        stepsProgress = (ProgressBar) findViewById(determinateBar);
        speedTextView = (TextView) findViewById(R.id.speed);

        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSensorPresent = true;
        } else {
            isSensorPresent = false;
        }

        this.setTitle(groupName + " - " + groupCode);
        beginSyncTask();
        playSong("http://www.bensound.com/royalty-free-music?download=cute");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorPresent) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSensorPresent) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        stepsTakenTextView.setText(String.valueOf(event.values[0]).substring(0, String.valueOf(event.values[0]).length() - 2));
        stepsTaken = Integer.parseInt(stepsTakenTextView.getText().toString()) % 10000;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    //Toast.makeText(AddSongsActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    void beginSyncTask() {
        final Handler h = new Handler();
        final int delay = 5000; //milliseconds

        h.postDelayed(new Runnable() {
            public void run() {
                final Runnable runnable = this;
                ServerAPI.updateScore(groupCode, prefs.getDeviceID(), stepsTaken, new RequestHandler<Member>() {
                    @Override
                    public void callback(Member result) {
                        ServerAPI.groupInformation(groupCode, new RequestHandler<Group>() {
                            @Override
                            public void callback(Group result) {
                                setLeaderboard(result);
                                setStepsProgress(stepsTaken);
                                setStepsDifference(result, stepsTaken);
                                h.postDelayed(runnable, delay);
                            }
                        });
                    }
                });
            }
        }, 0);
    }

    void setLeaderboard(Group group) {
        ArrayList<String> memberInformation = new ArrayList<String>();
        for (Member member : group.getSortedMembers()) {
            memberInformation.add(member.getName() + "                                       " + member.getScore());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.playerlistitem, R.id.playerName, memberInformation);
        leaderboardListView.setAdapter(adapter);
    }

    void setStepsProgress(int currentSteps) {
        stepsProgress.setProgress(currentSteps / 100);
        stepsTakenTextView.setText(currentSteps + "");
    }

    void setStepsDifference(Group group, int currentSteps) {
        ArrayList<Integer> scores = new ArrayList<Integer>();
        for (Member member : group.getSortedMembers()) {
            scores.add(member.getScore());
        }

        if (currentSteps >= (scores.get(0))) {
            int diff = currentSteps - scores.get(1);
            stepsDifferenceTextView.setText("You lead by " + diff + " steps");
            stepsDifferenceTextView.setTextColor(Color.GREEN);
        } else {
            int diff = scores.get(0) - currentSteps;
            stepsDifferenceTextView.setText("You trail by " + diff + " steps");
            stepsDifferenceTextView.setTextColor(Color.RED);
        }
    }

    void playSong(String url) {
        Log.d("playSong", "top");
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d("playSong", "prepared");
//              TODO: if we join a group late, we need to skip forward
//              TODO: we will want to try and prepare nextSong before currentSong is over
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("playSong", "complete");
//              TODO: when one is complete, play the next
            }
        });
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
