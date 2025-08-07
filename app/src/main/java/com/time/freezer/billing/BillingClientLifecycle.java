package com.time.freezer.billing;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.firebase.crashlytics.internal.model.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BillingClientLifecycle implements DefaultLifecycleObserver, PurchasesUpdatedListener,
        BillingClientStateListener, AcknowledgePurchaseResponseListener {

    private static final String TAG = "BillingLifecycle";

    public SingleLiveEvent<List<Purchase>> purchaseUpdateEvent = new SingleLiveEvent<>();

    public MutableLiveData<List<Purchase>> purchases = new MutableLiveData<>();

    // Change SkuDetails to ProductDetails
    public MutableLiveData<Map<String, ProductDetails>> skusWithProductDetails = new MutableLiveData<>();

    private static volatile BillingClientLifecycle INSTANCE;

    private Application app;
    private BillingClient billingClient;

    private BillingClientLifecycle(Application app) {
        this.app = app;
    }

    public static BillingClientLifecycle getInstance(Application app) {
        if (INSTANCE == null) {
            synchronized (BillingClientLifecycle.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BillingClientLifecycle(app);
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        Log.d(TAG, "ON_CREATE");
        billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection() // Add this line to enable reconnection
                .build();
        startConnection();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query products and purchases here.
                    Log.d(TAG, "Billing setup finished successfully.");
                    queryProductDetails();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to reconnect to the service if the connection is lost.
                Log.d(TAG, "Billing service disconnected. Reconnecting...");
                startConnection();
            }
        });
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner)  {
        Log.d(TAG, "ON_DESTROY");
        if (billingClient.isReady()) {
            Log.d(TAG, "BillingClient can only be used once -- closing connection");
            billingClient.endConnection();
        }
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onBillingSetupFinished: " + responseCode + " " + debugMessage);
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            // New method to query product details
            queryProductDetails();
            queryPurchases();
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "onBillingServiceDisconnected");
    }

    /**
     * Replaced onSkuDetailsResponse with onProductDetailsResponse
     */
    public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                         QueryProductDetailsResult queryProductDetailsResult) {
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                Log.i(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);

                    Map<String, ProductDetails> newProductsDetailList = new HashMap<>();
                    for (var productDetails : queryProductDetailsResult.getProductDetailsList()) {
                        newProductsDetailList.put(productDetails.getProductId(), productDetails);
                    }
                    skusWithProductDetails.postValue(newProductsDetailList);
                    Log.i(TAG, "onProductDetailsResponse: count " + newProductsDetailList.size());
                break;
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ERROR:
                Log.e(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
                break;
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            default:
                Log.wtf(TAG, "onProductDetailsResponse: " + responseCode + " " + debugMessage);
        }
    }

    public void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, purchasesList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        processPurchases(purchasesList);
                    } else {
                        Log.e(TAG, "queryPurchasesAsync: " + billingResult.getDebugMessage());
                    }
                }
        );
    }

    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult == null) {
            Log.wtf(TAG, "onPurchasesUpdated: null BillingResult");
            return;
        }
        int responseCode = billingResult.getResponseCode();
        String debugMessage = billingResult.getDebugMessage();
        Log.d(TAG, "onPurchasesUpdated: " + responseCode + " " + debugMessage);
        switch (responseCode) {
            case BillingClient.BillingResponseCode.OK:
                if (purchases == null) {
                    Log.d(TAG, "onPurchasesUpdated: null purchase list");
                    processPurchases(null);
                } else {
                    processPurchases(purchases);
                }
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                Log.i(TAG, "onPurchasesUpdated: User canceled the purchase");
                break;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                Log.i(TAG, "onPurchasesUpdated: The user already owns this item");
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                Log.e(TAG, "onPurchasesUpdated: Developer error means that Google Play " +
                        "does not recognize the configuration. If you are just getting started, " +
                        "make sure you have configured the application correctly in the " +
                        "Google Play Console. The SKU product ID must match and the APK you " +
                        "are using must be signed with release keys."
                );
                break;
        }
    }

    private void processPurchases(List<Purchase> purchasesList) {
        if (purchasesList != null) {
            Log.d(TAG, "processPurchases: " + purchasesList.size() + " purchase(s)");
        } else {
            Log.d(TAG, "processPurchases: with no purchases");
        }
        if (isUnchangedPurchaseList(purchasesList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed");
            return;
        }
        purchaseUpdateEvent.postValue(purchasesList);
        purchases.postValue(purchasesList);
        if (purchasesList != null) {
            logAcknowledgementStatus(purchasesList);
        }
    }

    private void logAcknowledgementStatus(List<Purchase> purchasesList) {
        for (Purchase purchase : purchasesList) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged()) {
                    AcknowledgePurchaseParams acknowledgePurchaseParams =
                            AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.getPurchaseToken())
                                    .build();
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
                }
            }
        }
    }

    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        return false;
    }

    /**
     * Replaced querySkuDetails with the new queryProductDetails
     */
    public void queryProductDetails() {
        Log.d(TAG, "queryProductDetails");

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.Upgrade_SKU)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        QueryProductDetailsParams queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build();

        billingClient.queryProductDetailsAsync(
                queryProductDetailsParams,
                this::onProductDetailsResponse
        );
    }

    /**
     * Launching the billing flow. Updated to use ProductDetailsParams.
     */
    public int launchBillingFlow(Activity activity, String productId) {
        Map<String, ProductDetails> detailsMap = this.skusWithProductDetails.getValue();
        if (detailsMap != null && detailsMap.containsKey(productId)) {
            ProductDetails productDetails = detailsMap.get(productId);

            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                    new ArrayList<>();
            productDetailsParamsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build());

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build();
            Log.i(TAG, "launchBillingFlow: product ID: " + productDetails.getProductId());
            if (!billingClient.isReady()) {
                Log.e(TAG, "launchBillingFlow: BillingClient is not ready");
                return BillingClient.BillingResponseCode.ERROR;
            }
            BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
            int responseCode = billingResult.getResponseCode();
            String debugMessage = billingResult.getDebugMessage();
            Log.d(TAG, "launchBillingFlow: BillingResponse " + responseCode + " " + debugMessage);
            return responseCode;
        }
        return -1;
    }

    @Override
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
        Log.d(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getResponseCode());
    }
}