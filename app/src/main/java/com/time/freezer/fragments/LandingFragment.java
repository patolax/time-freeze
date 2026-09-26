package com.time.freezer.fragments;

import static android.app.Activity.RESULT_OK;
import static com.time.freezer.fragments.Constants.SCAN_DIRECTION_HORIZONTAL;
import static com.time.freezer.fragments.Constants.SCAN_SHAPE_CURVE;
import static com.time.freezer.fragments.Constants.SCAN_SHAPE_LINE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.Purchase;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.time.freezer.MainActivity;
import com.time.freezer.R;
import com.time.freezer.base.utils.AppRatingDialog;
import com.time.freezer.base.utils.HandleBottomSheet;
import com.time.freezer.base.utils.SharedPreferencesManager;
import com.time.freezer.base.view.colorpicker.ColorPickerDialog;
import com.time.freezer.base.view.colorpicker.ColorPickerSwatch;
import com.time.freezer.base.view.colorpicker.ColorUtils;
import com.time.freezer.base.view.toggle.Toggle;
import com.time.freezer.base.view.toggle.ToggleButtonLayout;
import com.time.freezer.billing.BillingClientLifecycle;
import com.time.freezer.billing.BillingConstants;
import com.time.freezer.databinding.FragmentLandingBinding;
import com.time.freezer.filters.FilterItem;
import com.time.freezer.filters.FilterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class LandingFragment extends Fragment implements OnFilterClickListener, HandleBottomSheet {

    com.google.android.material.button.MaterialButton btnStart;
    com.google.android.material.button.MaterialButton btnGallery;
    com.google.android.material.button.MaterialButton btnTry;

    CircleImageView imgColor;
    ToggleButtonLayout toggleDirection;
    ToggleButtonLayout toggleShape;
    ToggleButtonLayout toggleSpeed;
    TextView txtShapeSelection;
    TextView txtDirectionSelection;
    TextView txtSeedSelection;
    ToggleButtonLayout toggleFilter;
    LinearLayout bottom_sheet;
    TextView txtFilterSelection;
    TextView txtColorSelection;
    NestedScrollView nestedScrollView;
    RecyclerView recyclerView;
    View layoutTitleBar;
    TextView txtFilterPanelLabel;
    TextView btnShowTrailer;
    MaterialButton btnBuy;
    FilterAdapter customAdapter;
    ActionListner listener;
    int selectedColor = R.color.selectedColor;
    ColorPickerDialog colorcalendar = null;
    MainActivity activity;

    ToggleButtonLayout toggleSaveImage;

    TextView txtSaveImageSelection;
    GradientDrawable bgShape;
    ScanSettings settings;
    BottomSheetBehavior sheetBehavior;
    FilterItem selectedFilterItem;
    BillingClientLifecycle billingClientLifecycle;
    Purchase updrade;
    boolean premiumUser;
    private final ExecutorService mFilterLoadExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    GridLayoutManager layoutManager;
    int PickImageRequestCode = 1000;
    private FirebaseAnalytics mFirebaseAnalytics;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private FragmentLandingBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLandingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        toggleFilter = binding.toggleFilter;
        btnStart = binding.btnStart;
        toggleSaveImage = binding.toggleSaveImage;
        txtSaveImageSelection = binding.txtSaveImageSelection;
        btnGallery = binding.btnGallery;
        btnTry = binding.filiterPanel.btnTry;
        btnBuy = binding.filiterPanel.btnBuy;
        btnShowTrailer = binding.filiterPanel.btnShowTrailer;
        txtFilterPanelLabel = binding.filiterPanel.txtFilterPanelLabel;
        toggleShape = binding.toggleShape;
        toggleDirection = binding.toggleDirection;
        toggleSpeed = binding.toggleSpeed;
        txtColorSelection = binding.txtColorSelection;
        txtDirectionSelection = binding.txtDirectionSelection;
        txtFilterSelection = binding.txtFilterSelection;
        txtSeedSelection = binding.txtSeedSelection;
        txtShapeSelection = binding.txtShapeSelection;
        recyclerView = binding.filiterPanel.filterRecyclerView;
        layoutTitleBar = binding.filiterPanel.layoutTitleBar;
        imgColor = binding.imagColorSelector;
        nestedScrollView = binding.filiterPanel.nestedScrollView;

        activity = (MainActivity) getActivity();
        settings = new ScanSettings();
        selectedFilterItem = settings.getFilter();

        btnGallery.setOnClickListener(v -> onClickGallery());
        btnBuy.setOnClickListener(v -> onClickRecord(false));
        btnTry.setOnClickListener(v -> onTry());
        btnStart.setOnClickListener(v -> onClickRecord(false));

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = getResources().getDisplayMetrics().density;
        float dpWidth = outMetrics.widthPixels / density;
        int columns = Math.round(dpWidth / 80);
        layoutManager = new GridLayoutManager(getActivity(), columns);
        recyclerView.setLayoutManager(layoutManager);

        loadFilters();

        toggleDirection.setOnToggleSelectedListener((toggle, selected) -> {
            int id = toggle.getId();
            txtDirectionSelection.setText(getString(Constants.convertDirectionToText(id)));
            settings.setDirection(Constants.convertDirectionToConstant(id));
            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_DIRECTION, settings.getDirection());
        });
        toggleSpeed.setOnToggleSelectedListener((toggle, selected) -> {
            int id = toggle.getId();
            txtSeedSelection.setText(getString(Constants.convertSpeedToText(id)));
            settings.setSpeed(Constants.convertSpeedToConstant(id));
            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_SPEED, settings.getSpeed());
        });
        toggleShape.setOnToggleSelectedListener((toggle, selected) -> {
            int id = toggle.getId();
            txtShapeSelection.setText(getString(Constants.convertShapeToText(id)));
            settings.setShape(Constants.convertShapeToConstant(id));
            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_SHAPE, settings.getShape());
        });
	toggleSaveImage.setOnToggleSelectedListener(new ToggleButtonLayout.OnToggledListener() {
        @Override
        public void onToggled(Toggle toggle, boolean selected) {
            int id = toggle.getId();
            toggleSaveImage.setToggled(id, selected);
            txtSaveImageSelection.setText(getString(Constants.convertSaveImageToText(selected)));
            settings.setSaveImage(selected ? Constants.SCAN_SAVE_IMAGE : Constants.SCAN_DONT_SAVE_IMAGE);
            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_SAVE_IMAGE, settings.getSaveImage());
        }
        });

        toggleFilter.setOnToggleSelectedListener(new ToggleButtonLayout.OnToggledListener() {
            @Override
            public void onToggled(Toggle toggle, boolean selected) {
                toggleFilter.setToggled(toggle.getId(), true);
                onOpenFilter();
            }

        });

        layoutTitleBar.setOnClickListener(v -> {
            if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        btnShowTrailer.setOnClickListener(v -> {
            logEvent("Trailer", selectedFilterItem.getName());
            PreviewDialog previewDialog = PreviewDialog.newInstance(
                    selectedFilterItem.getTitle(), selectedFilterItem.getPreview().replace("https://youtu.be/", ""));
            previewDialog.show(getChildFragmentManager(), "previewDialog");
        });

        bottom_sheet = binding.filiterPanel.bottomSheet;
        sheetBehavior = BottomSheetBehavior.from(bottom_sheet);
        sheetBehavior.setHideable(true);
        sheetBehavior.setDraggable(false);
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                    }
                    break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {
                //  bottom_sheet.animate().y(v <= 0 ?
                //          view.getY() + sheetBehavior.getPeekHeight() - bottom_sheet.getHeight() :
                //          view.getHeight() - bottom_sheet.getHeight()).setDuration(0).start();
            }
        });
        setColorButton();
        setSelectedSettings();
        setupBilling();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ActionListner) {
            listener = (ActionListner) context;
        }
    }

    private void setupBilling() {
        billingClientLifecycle = BillingClientLifecycle.getInstance(activity.getApplication());
        billingClientLifecycle.purchases.observe(getViewLifecycleOwner(), new Observer<List<Purchase>>() {
            @Override
            public void onChanged(List<Purchase> skus) {
                if (skus != null && !skus.isEmpty()) {
                    Purchase temp = skus.get(0);
                    if (temp.getProducts().contains(BillingConstants.Upgrade_SKU)) {
                        updrade = temp;
                        premiumUser = true;
                        btnBuy.setVisibility(View.GONE);
                    } else {
                        updrade = null;
                        premiumUser = false;
                    }
                    if (customAdapter != null) {
                        customAdapter.setPremiumUser(premiumUser);
                    }
                    onSelectedFilterChange();
                }
            }
        });
    }

    public void setColorButton() {
        if (imgColor != null) {
            imgColor.setOnClickListener(v -> getColorPickerDialog().show(
                    getChildFragmentManager(), "dash"));
            bgShape = (GradientDrawable) imgColor.getDrawable();
            selectedColor = ColorUtils.getColor(activity, R.color.selectedColor);
            setColorCircle(selectedColor);
        }
    }

    private void setColorCircle(int color) {
        selectedColor = color;
        if (imgColor != null && bgShape != null) {
            bgShape.setColor(color);
            imgColor.setImageDrawable(bgShape);
        }
        txtColorSelection.setText(String.format("(%d,%d,%d)", Color.red(color), Color.green(color), Color.blue(color)));
    }

    @Override
    public void hideBottomSheetFromOutSide(MotionEvent event) {
        if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            Rect outRect = new Rect();
            bottom_sheet.getGlobalVisibleRect(outRect);
            if (!outRect.contains((int) event.getRawX(), (int) event.getRawY()))
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFilterLoadExecutor.shutdownNow();
    }

    private void loadFilters() {
        mFilterLoadExecutor.execute(() -> {
            List<FilterItem> list = new ArrayList<>();
            try {
                list = new FilterManager().load(activity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<FilterItem> finalList = list;
            mMainHandler.post(() -> setFilters(finalList));
        });
    }

    private void setFilters(List<FilterItem> list) {
        customAdapter = new FilterAdapter(activity, list, this, premiumUser);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setNestedScrollingEnabled(false);
        String filterName = SharedPreferencesManager.getString(activity, SharedPreferencesManager.SCAN_FILTER_NAME, settings.getFilter().getName());
        int index = 0;
        int temp = 0;
        for (FilterItem item : list) {
            if (item.getName().equals(filterName)) {
                selectedFilterItem = item;
                index = temp;
                break;
            }
            temp++;
        }
        try {
            if (selectedFilterItem != null) {
                onSelectedFilterChange();
                settings.setFilter(selectedFilterItem);
                //btnFilter.setToggled(R.id.toggle_open, true);
                customAdapter.setSelected(selectedFilterItem);
                final int scrollIndex = index;
                recyclerView.post(() -> {
                    View child = recyclerView.getChildAt(scrollIndex);
                    float y = recyclerView.getY() + (child == null ? 0 : child.getY());
                    y = y < 50 ? 0 : y;
                    nestedScrollView.smoothScrollTo(0, (int) y);
                });
            }
        } catch (Exception ex) {
        }
        showRating();
    }

    private void showRating() {
        final AppRatingDialog appRatingDialog = new AppRatingDialog.Builder(activity)
                .setTriggerCount(2)
                .setRepeatCount(5)
                .setLayoutBackgroundColor(R.color.backgroundColor)
                .setIconDrawable(true, ContextCompat.getDrawable(activity, R.drawable.logo))
                .setRateButtonBackground(R.color.colorPrimaryDark)
                .build();

        appRatingDialog.show();
    }

    private String getFilterName(String name) {
        if (name.equalsIgnoreCase("no")) {
            return "None";
        }
        return name;
    }

    private void setSelectedSettings() {
        settings.readFromPreferences(activity);
        toggleShape.setToggled(Constants.convertShapeToToggleId(settings.getShape()), true);
        txtShapeSelection.setText(getString(Constants.convertShapeToText(Constants.convertShapeToToggleId(settings.getShape()))));
        toggleDirection.setToggled(Constants.convertDirectionToToggleId(settings.getDirection()), true);
        txtDirectionSelection.setText(getString(Constants.convertDirectionToText(Constants.convertDirectionToToggleId(settings.getDirection()))));
        toggleSpeed.setToggled(Constants.convertSpeedToToggleId(settings.getSpeed()), true);
        txtSeedSelection.setText(getString(Constants.convertSpeedToText(Constants.convertSpeedToToggleId(settings.getSpeed()))));
        setColorCircle(settings.getScannerColor());
        boolean saveImageToGallery = settings.isSaveImage();
        toggleSaveImage.setToggled(R.id.toggle_image_save, saveImageToGallery);
        txtSaveImageSelection.setText(getString(Constants.convertSaveImageToText(saveImageToGallery)));
    }

    private ColorPickerDialog getColorPickerDialog() {
        if (colorcalendar == null) {
            colorcalendar = ColorPickerDialog.newInstance(
                    R.string.app_name,
                    ColorUtils.colorChoice(activity), selectedColor, 5, ColorPickerDialog.SIZE_SMALL);
            colorcalendar
                    .setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener() {
                        @Override
                        public void onColorSelected(int color) {
                            setColorCircle(color);
                            settings.setScannerColor(color);
                            SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_COLOR, selectedColor);
                        }
                    });
        }
        return colorcalendar;
    }

    public void onClickRecord(boolean reward) {
        if (checkPermissons()) return;
        if (listener != null) {
             if (settings.getFilter().isPremium() && !premiumUser && !reward) {
                showPermissonsAlert(settings.getFilter().getTitle());
                return;
            }
            if (settings.isBackgroundFilter()
                    && (settings.getImagePath() == null || settings.getImagePath() == "")) {
                showImageRequiredAlert(settings.getFilter().getTitle());
                return;
            }

            logEvent("SelectedFilter",settings.getFilter().getTitle());
            logEvent(settings.getDirection() == SCAN_DIRECTION_HORIZONTAL ? "Horizontal": "Vertical", "click");
            logEvent(settings.getShape() == SCAN_SHAPE_LINE ? "Line": (settings.getShape() == SCAN_SHAPE_CURVE ?  "Curve": "ZigZag"), "click");
 			if(settings.isSaveImage()){
                logEvent("SaveImage", "click");
            }
            listener.loadCamera(settings);
        }
    }

    private boolean checkPermissons() {

        if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        )) {
            showPermissonsAlert();
            return true;
        }
        return false;
    }

    private void showPermissonsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Permissions");
        builder.setMessage("The required permissions (camera, audio, or storage) not granted. please go to settings and set permissions to start.");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialoginterface, int i) {
                dialoginterface.cancel();
            }
        });
        builder.show();
    }

    public void onClickGallery() {
        if (checkPermissons()) return;
        logEvent("loadGallery", "click");
        listener.loadGallery();
    }

    public void onTry() {
        onClickRecord(false);
    }

    public void onBuy() {
        logEvent("onBuy", "click");

        billingClientLifecycle.launchBillingFlow(activity, BillingConstants.Upgrade_SKU);
    }

    public void onOpenFilter() {
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            closeFilterPanel();
        }
    }

    private void closeFilterPanel() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void showPermissonsAlert(String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Premium Filter");
        builder.setMessage("Upgrade is required to use premium filters. " + "\n" +
                "Upgrade or watch a reward ad to temporarily unlock the filter.");
        builder.setPositiveButton("Cancel", (dialoginterface, i) -> dialoginterface.cancel());
        builder.setNeutralButton("Watch Reward Ad", (dialoginterface, i) -> {
            activity.showAd();
            dialoginterface.cancel();
        });
        builder.setNegativeButton("Upgrade", (dialoginterface, i) -> {
            onBuy();
            dialoginterface.cancel();
        });
        builder.show();
    }

    private void showImageRequiredAlert(String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Image Required");
        builder.setMessage(name + " filter requires an image. Tap on the filter to select an image.");
        builder.setNegativeButton("Cancel", (dialoginterface, i) -> dialoginterface.cancel());
        builder.setPositiveButton("Ok", (dialoginterface, i) -> {
            onOpenFilter();
            dialoginterface.cancel();
        });
        builder.show();
    }

    @Override
    public void onFilterClick(FilterItem item) {
        if (item == null)
            return;
        settings.setFilter(item);
        selectedFilterItem = item;
        SharedPreferencesManager.setString(activity, SharedPreferencesManager.SCAN_FILTER_NAME, item.getName());
        SharedPreferencesManager.setInt(activity, SharedPreferencesManager.SCAN_FILTER_FULLIMAGE, item.isFullImageFilter() ? 1 : 0);
        onSelectedFilterChange();
        if (settings.isBackgroundFilter()) {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            someActivityResultLauncher.launch(pickPhoto);
        }
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        if (data != null) {
                            Uri returnUri = data.getData();
                            SharedPreferencesManager.setString(activity, SharedPreferencesManager.SCAN_FILTER_IMAGE, returnUri.toString());
                            settings.setImagePath(returnUri.toString());
                        }
                    }
                }
            });

    private void onSelectedFilterChange() {
        if (selectedFilterItem == null) return;
        String filterName = getFilterName(selectedFilterItem.getTitle());
        logEvent("FilterChange", filterName);
        txtFilterSelection.setText(filterName);
        txtFilterPanelLabel.setText(filterName + " Filter");
        if (selectedFilterItem.isPremium() && !premiumUser) {
            btnBuy.setVisibility(View.VISIBLE);
            btnTry.setVisibility(View.GONE);
        } else {
            btnBuy.setVisibility(View.GONE);
            btnTry.setVisibility(View.VISIBLE);
        }
    }

    private void logEvent(String eventId, String data){
        if(mFirebaseAnalytics == null) return;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, data);
        bundle.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, data);
        mFirebaseAnalytics.logEvent(eventId, bundle);
    }
}