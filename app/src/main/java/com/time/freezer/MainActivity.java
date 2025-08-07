package com.time.freezer;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.time.freezer.base.utils.HandleBottomSheet;
import com.time.freezer.base.utils.SharedPreferencesManager;
import com.time.freezer.billing.BillingClientLifecycle;
import com.time.freezer.filters.FilterItem;
import com.time.freezer.fragments.ActionListner;
import com.time.freezer.fragments.Constants;
import com.time.freezer.fragments.GalleryFragment;
import com.time.freezer.fragments.LandingFragment;
import com.time.freezer.fragments.ScanSettings;
import com.time.freezer.fragments.VideoFragment;
import com.time.freezer.base.utils.ShaderUtils;

import java.util.List;

import com.google.android.gms.ads.rewarded.RewardedAd;

//Interstitial ad
//ca-app-pub-1025251637342589~9196124975

// ca-app-pub-1025251637342589/9108780474


//reward
//  ca-app-pub-1025251637342589~9196124975

// ca-app-pub-1025251637342589/6406364007


public class MainActivity extends FragmentActivity implements ActionListner {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String TAG_CAMERA_FRAGMENT = "tag_camera_frag";
    private static final int REQUEST_PERMISSIONS_CODE = 100;
    private boolean mPermissionsSatisfied = false;
    private BillingClientLifecycle billingClientLifecycle;

    private RewardedAd rewardedAd;
    private boolean rewardedWon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.graphics.Point size = new android.graphics.Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);

        billingClientLifecycle = BillingClientLifecycle.getInstance(getApplication());
        billingClientLifecycle.onCreate(this);

        Fragment fragment = new LandingFragment();
        switchToFragment(fragment, false);
        SharedPreferencesManager.setString(this, SharedPreferencesManager.SCAN_FILTER_IMAGE, "");
        setupPermissions();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                setupRewardAd();
            }
        });
    }

    private void setupRewardAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-1025251637342589/6406364007",
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d("rewarded", loadAdError.toString());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d("rewarded", "Ad was loaded.");
                    }
                });
    }


    public void showAd() {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        /** Called when ad showed the full screen content. */
                        @Override
                        public void onAdShowedFullScreenContent() {
                            Log.d(TAG, "onAdShowedFullScreenContent");
                        }

                        /** Called when the ad failed to show full screen content. */
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            Log.d(TAG, "onAdFailedToShowFullScreenContent: " + adError.getMessage());
                            // Don't forget to set the ad reference to null so you
                            // don't show the ad a second time.
                            rewardedAd = null;
                            setupRewardAd();
                            Toast.makeText(
                                            MainActivity.this, "Error loading the ad!", Toast.LENGTH_SHORT)
                                    .show();
                        }

                        /** Called when full screen content is dismissed. */
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Don't forget to set the ad reference to null so you
                            // don't show the ad a second time.
                            rewardedAd = null;
                            rewardedWon = false;
                            openFilterOnReward();
                            setupRewardAd();
                        }
                    });

            rewardedAd.show(MainActivity.this, new OnUserEarnedRewardListener() {
                @Override
                public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                    // Handle the reward.
                    Log.d("rewardAd", "The user earned the reward.");
                    int rewardAmount = rewardItem.getAmount();
                    if (rewardAmount > 0) {
                        rewardedWon = true;
                    }
                }
            });
        } else {
            Toast.makeText(
                            MainActivity.this, "Ad is not ready yet, please try in a moment!", Toast.LENGTH_SHORT)
                    .show();
            Log.d("rewardAd", "The rewarded ad wasn't ready yet.");
        }
    }

    private void openFilterOnReward() {
        List<Fragment> fragments = MainActivity.this.getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment != null && fragment.isVisible() && (fragment instanceof LandingFragment))
                ((LandingFragment) fragment).onClickRecord(true);
        }
    }

    private void switchToFragment(Fragment newFrag, boolean transitionAnimation) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction();
        if (transitionAnimation) {
            fragmentTransaction.setCustomAnimations(R.anim.fragment_enter,
                    R.anim.fragment_exit, R.anim.fragment_leftenter,
                    R.anim.fragment_leftexit);
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.replace(R.id.fragment_container, newFrag).
                addToBackStack(newFrag.getClass().getName());
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        int fragments = getSupportFragmentManager().getBackStackEntryCount();
        if (fragments == 1) {
            finish();
        } else if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }


    private void setupPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions( //Method of Fragment
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                        },
                        REQUEST_PERMISSIONS_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (hasAllPermissionsSatisfied(permissions, grantResults)) {

            }
        }
    }

    private boolean hasAllPermissionsSatisfied(@NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean result = true;
        for (int i : grantResults) {
            result = result && (i == PackageManager.PERMISSION_GRANTED);
        }
        return result;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
            List<Fragment> fragments = fragmentManager.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    if (fragment != null && fragment.isVisible() && fragment instanceof HandleBottomSheet)
                        ((HandleBottomSheet) fragment).hideBottomSheetFromOutSide(event);
                }
            }
            Log.d("myApp", "dispatchTouchEvent");
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        shutdownCameraAndRestart();
        ShaderUtils.goFullscreen(this.getWindow());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void shutdownCameraAndRestart() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        Fragment currentFragment = manager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof VideoFragment) {
            VideoFragment mVideoFragment = (VideoFragment) currentFragment;
            ft.remove(mVideoFragment);
            ft.commitAllowingStateLoss();
            manager.popBackStackImmediate();
            mVideoFragment = null;
            ScanSettings settings = new ScanSettings();
            settings.readFromPreferences(this);
            loadCamera(settings);
        }
    }

    @Override
    public void loadCamera(ScanSettings settings) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putParcelable(Constants.FRAGMENT_INPUT_KEY, settings);
        fragment.setArguments(args);
        switchToFragment(fragment, true);
    }

    @Override
    public void loadGallery() {
        GalleryFragment fragment = GalleryFragment.newInstance(3);
        switchToFragment(fragment, true);
    }
}
