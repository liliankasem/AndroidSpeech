package com.liliankasem.demo1_speechdictation_oxford;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.projectoxford.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.projectoxford.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.projectoxford.speechrecognition.RecognitionResult;
import com.microsoft.projectoxford.speechrecognition.RecognitionStatus;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionMode;
import com.microsoft.projectoxford.speechrecognition.SpeechRecognitionServiceFactory;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements ISpeechRecognitionServerEvents
{
    boolean m_isMicrophoneReco;
    SpeechRecognitionMode m_recoMode;
    MicrophoneRecognitionClient m_micClient = null;
    public enum FinalResponseStatus { NotReceived, OK, Timeout }
    FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getString(R.string.subscription_key).startsWith("Please")) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }

        hypothesisText = (EditText) findViewById(R.id.hypothesisText);
        recognisedText = (EditText) findViewById(R.id.recognisedText);
        wpmText = (TextView) findViewById(R.id.num_wordsmin);


        //Setting up Project Oxford Speech Dictation
        m_recoMode = SpeechRecognitionMode.LongDictation;
        m_isMicrophoneReco = true;
        initializeRecoClient();

        if (m_isMicrophoneReco) {
            m_micClient.startMicAndRecognition();
        }

        //Timer for calculating words per minute
        startTimer();
    }

    void initializeRecoClient()
    {
        String language = "en-gb";
        String subscriptionKey = this.getString(R.string.subscription_key);

        if (m_isMicrophoneReco && null == m_micClient) {
            m_micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(this,
                    m_recoMode,
                    language,
                    this,
                    subscriptionKey);
        }
    }

    public void onPartialResponseReceived(final String response)
    {
        hypothesisText.setText(response + " ");
    }

    public void onFinalResponseReceived(final RecognitionResult response)
    {
        boolean isFinalDicationMessage = m_recoMode == SpeechRecognitionMode.LongDictation &&
                (response.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                        response.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);

        if (response.Results.length > 0) {
            dictationtext += " " + response.Results[0].DisplayText;
            hypothesisText.setText("");
            recognisedText.setText(dictationtext + " ");
        }

        if (m_isMicrophoneReco && ((m_recoMode == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage)) {
           // m_micClient.endMicAndRecognition();
            m_micClient.startMicAndRecognition();
        }
        if ((m_recoMode == SpeechRecognitionMode.ShortPhrase) || isFinalDicationMessage) {
            this.isReceivedResponse = FinalResponseStatus.OK;
        }
    }

    /* Called on WithIntent clients (only in ShortPhrase mode) after the final reco result has
        been parsed into a structured JSON intent. */
    public void onIntentReceived(final String payload) {}
    /* Called when the Server Detects Error */
    public void onError(final int errorCode, final String response)
    {
        recognisedText.setText("\n********* Error Detected *********\n" + errorCode + " " + response + "\n");
    }
    public void onAudioEvent(boolean recording)
    {
        if (!recording) {
           // m_micClient.endMicAndRecognition();
            m_micClient.startMicAndRecognition();
        }
    }








    /** Other  code **/
    static EditText hypothesisText;
    static EditText recognisedText;
    static TextView wpmText;
    static int textLength;
    static double wpm;
    static double elapsedTime;
    String dictationtext = "";

    protected static void startTimer() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                elapsedTime += 0.5; //increase every sec
                mHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 500);
    };
    public static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            //textLength += hypothesisText.getText().length();
            textLength = recognisedText.getText().length();

            //textLength = recognisedText.getText().length() + hypothesisText.getText().length();
            wpm = (double) (textLength / elapsedTime) * 30;
            wpmText.setText(String.format("%.2f", wpm));
        }
    };

}