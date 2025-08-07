package com.time.freezer.base.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import java.util.ArrayList;
import java.util.Locale;

import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class VoiceCommandManager {
    private SpeechRecognizer speechRecognizer;
    private BehaviorSubject<Boolean> onListening;
    private BehaviorSubject<String> onCommand;
    Intent speechRecognizerIntent;
    boolean isListening = false;

    public void setup(Context ctx) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx);
        onListening = BehaviorSubject.create();
        onCommand = BehaviorSubject.create();

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.time.freezer");
        //speechRecognizerIntent.putExtra(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, RecognizerIntent.EXTRA_PREFER_OFFLINE);
        //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000);
        //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 15000);
        //speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                //onListening.onNext(true);
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                Log.d("voice", "onEndOfSpeech ");
                //onListening.onNext(false);
                startListening();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                Log.d("voice", "onError " + error);
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    //onListening.onNext(false);
                    startListening();
                }
            }

            @Override
            public void onResults(Bundle bundle) {
                isListening = false;
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                onCommand.onNext(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    public void stopListening() {
        Log.d("voice", "stopListening ");
        speechRecognizer.stopListening();
    }

    public void startListening() {
        Log.d("voice", "startListening ");
        if (!isListening) {
            isListening = true;
            speechRecognizer.startListening(speechRecognizerIntent);
        }
    }

    public void shoutDown() {
        speechRecognizer.destroy();
    }

    public BehaviorSubject<Boolean> getOnListening() {
        return onListening;
    }

    public BehaviorSubject<String> getOnCommand() {
        return onCommand;
    }

    public static RecordingStatus convertVoiceCommands(String command) {
        if (command == null || command.equals("")) return null;
        command = command.toLowerCase();
        if (command.contains("pause")) {
            return RecordingStatus.Pause;
        } else if (command.contains("restart")) {
            return RecordingStatus.Restart;
        } else if (command.contains("start")) {
            return RecordingStatus.Start;
        } else if (command.contains("stop")) {
            return RecordingStatus.Stop;
        }else{
            return  null;
        }
    }
}
