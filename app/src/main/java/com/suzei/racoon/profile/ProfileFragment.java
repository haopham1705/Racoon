package com.suzei.racoon.profile;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;
import com.suzei.racoon.R;
import com.suzei.racoon.model.Users;
import com.suzei.racoon.util.NumberPickerUtil;
import com.suzei.racoon.util.TakePick;

import butterknife.BindColor;
import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class ProfileFragment extends Fragment implements ProfileContract.ProfileView {

    private static final int CHANGE_INPUT_BIO = 2;
    private static final int CHANGE_INPUT_NAME = 4;

    private ProfilePresenter profilePresenter;
    private DatabaseReference mUserRef;
    private StorageReference mUserStorage;
    private Unbinder unbinder;
    private TakePick takePick;
    private ContextThemeWrapper themeWrapper;

    private String currentUserId;

    @BindString(R.string.add_bio) String stringAddBio;
    @BindColor(R.color.colorPrimary) int colorPrimary;
    @BindColor(android.R.color.black) int colorBlack;
    @BindDrawable(R.drawable.gender_male) Drawable drawableMale;
    @BindDrawable(R.drawable.gender_female) Drawable drawableFemale;
    @BindDrawable(R.drawable.gender_unknown) Drawable drawableUnknown;
    @BindView(R.id.profile_back) ImageButton backView;
    @BindView(R.id.profile_age) TextView ageView;
    @BindView(R.id.profile_description) TextView descView;
    @BindView(R.id.profile_gender) TextView genderView;
    @BindView(R.id.profile_image) RoundedImageView picView;
    @BindView(R.id.profile_name) TextView nameView;
    @BindView(R.id.profile_action_buttons_layout) LinearLayout actionButtonsLayout;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile, container, false);
        initObjects(view);
        setUpPresenter();
        setUpTakePick();
        return view;
    }

    private void initObjects(View view) {
        currentUserId = FirebaseAuth.getInstance().getUid();
        mUserRef = FirebaseDatabase.getInstance().getReference().child("users").child(currentUserId);
        mUserStorage = FirebaseStorage.getInstance().getReference().child("users").child("pictures")
                .child(currentUserId);
        unbinder = ButterKnife.bind(this, view);
        themeWrapper = new ContextThemeWrapper(getContext(), R.style.AlertDialogTheme);


        backView.setVisibility(View.GONE);
        actionButtonsLayout.setVisibility(View.GONE);
    }

    private void setUpPresenter() {
        profilePresenter = new ProfilePresenter(this);
        profilePresenter.showUserDetails(currentUserId);
    }

    private void setUpTakePick() {
        takePick = new TakePick(getActivity(), this, new TakePick.ImageListener() {

            @Override
            public void onEmojiPick(String image) {
                mUserRef.child("image").setValue(image);
            }

            @Override
            public void onCameraGalleryPick(byte[] imageByte) {
                UploadTask uploadTask = mUserStorage.putBytes(imageByte);
                takePick.uploadThumbnailToStorage(mUserStorage, uploadTask, image_url ->
                        mUserRef.child("image").setValue(image_url));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("onActivityResult");
        takePick.onActivityResult(requestCode, resultCode, data);
    }

    @OnClick(R.id.profile_image)
    public void onImageClick() {
        Timber.i("Take Pick");
        takePick.showPicker();
    }

    @Override
    public void showProgress() {
        //Show loading animation
    }

    @Override
    public void hideProgress() {
        //hide loading animation
    }

    @Override
    public void onLoadSuccess(Users users) {
        nameView.setText(users.getName());
        ageView.setText(String.valueOf(users.getAge()));
        Picasso.get().load(users.getImage()).into(picView);
        genderView.setText(users.getGender());

        switch (users.getGender()) {
            case "Male":
                genderView.setCompoundDrawablesWithIntrinsicBounds(drawableMale, null,
                        null, null);
                break;
            case "Female":
                genderView.setCompoundDrawablesWithIntrinsicBounds(drawableFemale, null,
                        null, null);
                break;
            case "Unknown":
                genderView.setCompoundDrawablesWithIntrinsicBounds(drawableUnknown, null,
                        null, null);
                break;
        }
    }

    @Override
    public void onLoadingFailure() {
        //Show toast message
    }

    private void showGenderPicker(int gender) {
        CharSequence[] genderSeq = {"Male", "Female", "Unknown"};

        AlertDialog.Builder builder = new AlertDialog.Builder(themeWrapper);
        builder.setTitle("Choose gender:");
        builder.setSingleChoiceItems(genderSeq, gender, null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setPositiveButton("Ok", (dialog, which) -> {
            ListView listView = ((AlertDialog) dialog).getListView();
            Object checkedItem = listView.getAdapter().getItem(listView.getCheckedItemPosition());
            mUserRef.child("gender").setValue(checkedItem);
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showNumberPicker() {
        int age = Integer.parseInt(ageView.getText().toString());
        NumberPicker numberPicker = new NumberPicker(getContext());
        NumberPickerUtil.setNumberPickerTextColor(numberPicker, colorBlack);
        numberPicker.setMaxValue(115);
        numberPicker.setMinValue(13);
        numberPicker.setValue(age);

        AlertDialog.Builder builder = new AlertDialog.Builder(themeWrapper);
        builder.setMessage("Choose an age:");
        builder.setView(numberPicker);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            int selectedAge = numberPicker.getValue();
            mUserRef.child("age").setValue(selectedAge);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showInputDialog(String title, String defaultText, int type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(themeWrapper);

        builder.setTitle(title);

        EditText editText = new EditText(getContext());
        editText.setText(defaultText);
        editText.setTextColor(colorBlack);
        builder.setView(editText);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String inputtedText = editText.getText().toString().trim();

            switch (type) {
                case CHANGE_INPUT_BIO:
                    mUserRef.child("bio").setValue(inputtedText);
                    break;

                case CHANGE_INPUT_NAME:
                    mUserRef.child("name").setValue(inputtedText);
                    break;

                default:
                    throw new IllegalArgumentException("Invalid dialog type= " + type);
            }

            dialog.dismiss();

        });

        builder.show();
    }
}
