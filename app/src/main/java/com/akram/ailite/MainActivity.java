package com.akram.ailite;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private static final String APP_URL = "https://akram-ai-lite.xrakram2020.workers.dev/";
    private static final String INTERSTITIAL_AD_UNIT = "ca-app-pub-4048890094526498/3875121541";
    private static final String REWARDED_AD_UNIT = "ca-app-pub-4048890094526498/7155891002";

    private WebView webView;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean interstitialShownOnLaunch = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitialAd();
            loadRewardedAd();
        });

        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new AdBridge(), "AndroidAdBridge");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!interstitialShownOnLaunch && interstitialAd != null) {
                    interstitialShownOnLaunch = true;
                    interstitialAd.show(MainActivity.this);
                }
            }
        });

        webView.loadUrl(APP_URL);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                if (!interstitialShownOnLaunch && webView != null && webView.getProgress() == 100) {
                    interstitialShownOnLaunch = true;
                    interstitialAd.show(MainActivity.this);
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
            }
        });
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARDED_AD_UNIT, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }
        });
    }

    public class AdBridge {
        @JavascriptInterface
        public void showRewardedForImages() {
            runOnUiThread(() -> {
                if (rewardedAd != null) {
                    final boolean[] earnedReward = {false};
                    rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            rewardedAd = null;
                            loadRewardedAd();
                            if (earnedReward[0]) {
                                notifyWebRewardEarned();
                            }
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            rewardedAd = null;
                            loadRewardedAd();
                        }
                    });
                    rewardedAd.show(MainActivity.this, rewardItem -> earnedReward[0] = true);
                } else {
                    loadRewardedAd();
                }
            });
        }
    }

    private void notifyWebRewardEarned() {
        if (webView != null) {
            webView.post(() -> webView.evaluateJavascript(
                    "if (typeof window.onAdWatchedForImages === 'function') { window.onAdWatchedForImages(); }",
                    null
            ));
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
