package io.github.b_lam.resplash.Activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.b_lam.resplash.Data.Data.Photo;
import io.github.b_lam.resplash.Data.Data.PhotoDetails;
import io.github.b_lam.resplash.Data.Service.PhotoService;
import io.github.b_lam.resplash.Dialogs.InfoDialog;
import io.github.b_lam.resplash.Dialogs.StatsDialog;
import io.github.b_lam.resplash.Network.ImageDownloader;
import io.github.b_lam.resplash.R;
import io.github.b_lam.resplash.Resplash;
import retrofit2.Call;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    final String TAG = "DetailActivity";
    private boolean like;
    private Photo mPhoto;
    private PhotoDetails mPhotoDetails;
    private PhotoService mService;
    private SharedPreferences sharedPreferences;
    private Drawable colorIcon;
    final static int TYPE_DOWNLOAD = 1;
    final static int TYPE_WALLPAPER = 2;

    @BindView((R.id.toolbar_detail)) Toolbar toolbar;
    @BindView(R.id.imgFull) ImageView imgFull;
    @BindView(R.id.imgProfile) ImageView imgProfile;
    @BindView(R.id.btnLike) ImageButton btnLike;
    @BindView(R.id.tvUser) TextView tvUser;
    @BindView(R.id.tvLocation) TextView tvLocation;
    @BindView(R.id.tvDate) TextView tvDate;
    @BindView(R.id.tvLikes) TextView tvLikes;
    @BindView(R.id.tvColor) TextView tvColor;
    @BindView(R.id.tvDownloads) TextView tvDownloads;
    @BindView(R.id.progress_download) ProgressBar progressBar;
    @BindView(R.id.fab_menu) FloatingActionMenu floatingActionMenu;
    @BindView(R.id.fab_download) FloatingActionButton fabDownload;
    @BindView(R.id.fab_wallpaper) FloatingActionButton fabWallpaper;
    @BindView(R.id.fab_stats) FloatingActionButton fabStats;
    @BindView(R.id.fab_info) FloatingActionButton fabInfo;
    @BindView(R.id.detail_content) LinearLayout content;
    @BindView(R.id.detail_progress) ProgressBar loadProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ButterKnife.bind(this);

        Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material, getTheme());
        upArrow.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPhoto = new Gson().fromJson(getIntent().getStringExtra("Photo"), Photo.class);
        this.mService = PhotoService.getService();

        loadPreferences();

        floatingActionMenu.setClosedOnTouchOutside(true);
        createCustomAnimation();

        fabDownload.setOnClickListener(onClickListener);
        fabInfo.setOnClickListener(onClickListener);
        fabStats.setOnClickListener(onClickListener);
        fabWallpaper.setOnClickListener(onClickListener);

        Glide.with(DetailActivity.this)
                .load(mPhoto.urls.regular)
                .priority(Priority.HIGH)
                .placeholder(R.drawable.placeholder)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(imgFull);

        Glide.with(DetailActivity.this)
                .load(mPhoto.user.profile_image.large)
                .priority(Priority.HIGH)
                .diskCacheStrategy(DiskCacheStrategy.RESULT)
                .into(imgProfile);

        colorIcon = getResources().getDrawable(R.drawable.ic_fiber_manual_record_white_18dp, getTheme());

        PhotoService.OnRequestPhotoDetailsListener mPhotoDetailsRequestListener = new PhotoService.OnRequestPhotoDetailsListener() {
            @Override
            public void onRequestPhotoDetailsSuccess(Call<PhotoDetails> call, Response<PhotoDetails> response) {
                Log.d(TAG, String.valueOf(response.code()));
                if (response.isSuccessful()) {
                    mPhotoDetails = response.body();
                    tvUser.setText("Photo by " + mPhotoDetails.user.name);
                    if (mPhotoDetails.location != null) {
                        if (mPhotoDetails.location.city != null && mPhotoDetails.location.country != null) {
                            tvLocation.setText(mPhotoDetails.location.city + ", " + mPhotoDetails.location.country);
                        }else if(mPhotoDetails.location.city != null){
                            tvLocation.setText(mPhotoDetails.location.city);
                        }else if(mPhotoDetails.location.country != null){
                            tvLocation.setText(mPhotoDetails.location.country);
                        }
                    } else {
                        tvLocation.setText("-----");
                    }
                    tvDate.setText(mPhotoDetails.created_at.split("T")[0]);
                    tvLikes.setText(NumberFormat.getInstance(Locale.CANADA).format(mPhotoDetails.likes) + " Likes");
                    colorIcon.setColorFilter(Color.parseColor(mPhotoDetails.color), PorterDuff.Mode.SRC_IN);
                    tvColor.setText(mPhotoDetails.color);
                    tvDownloads.setText(NumberFormat.getInstance(Locale.CANADA).format(mPhotoDetails.downloads) + " Downloads");
                    loadProgress.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    floatingActionMenu.setVisibility(View.VISIBLE);
                } else if (response.code() == 403) {
                    Toast.makeText(Resplash.getInstance().getApplicationContext(), "Can't make anymore requests.", Toast.LENGTH_LONG).show();
                } else {
                    mService.requestPhotoDetails(mPhoto, this);
                }
            }

            @Override
            public void onRequestPhotoDetailsFailed(Call<PhotoDetails> call, Throwable t) {
                Log.d(TAG, t.toString());
                mService.requestPhotoDetails(mPhoto, this);
            }
        };

        mService.requestPhotoDetails(mPhoto, mPhotoDetailsRequestListener);

        imgFull.setOnClickListener(imageOnClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.fab_download:
                    if (mPhoto != null) {
                        floatingActionMenu.close(true);
                        Toast.makeText(getApplicationContext(), "Download started", Toast.LENGTH_SHORT).show();
                        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(mPhoto.color), PorterDuff.Mode.MULTIPLY);
                        progressBar.setVisibility(View.VISIBLE);
                        switch (sharedPreferences.getString("download_quality", "Raw")) {
                            case "Raw":
                                downloadFromURL(mPhoto.urls.raw, TYPE_DOWNLOAD);
                                break;
                            case "Full":
                                downloadFromURL(mPhoto.urls.full, TYPE_DOWNLOAD);
                                break;
                            case "Regular":
                                downloadFromURL(mPhoto.urls.regular, TYPE_DOWNLOAD);
                                break;
                            case "Small":
                                downloadFromURL(mPhoto.urls.small, TYPE_DOWNLOAD);
                                break;
                            case "Thumb":
                                downloadFromURL(mPhoto.urls.thumb, TYPE_DOWNLOAD);
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid download quality");
                        }
                    }
                    break;
                case R.id.fab_wallpaper:
                    if (mPhoto != null) {
                        floatingActionMenu.close(true);
                        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(mPhoto.color), PorterDuff.Mode.MULTIPLY);
                        progressBar.setVisibility(View.VISIBLE);

                        downloadFromURL(mPhoto.urls.raw, TYPE_WALLPAPER);
                    }

                    break;
                case R.id.fab_info:
                    if (mPhoto != null) {
                        floatingActionMenu.close(true);
                        InfoDialog infoDialog = new InfoDialog();
                        infoDialog.setPhotoDetails(mPhotoDetails);
                        infoDialog.show(getFragmentManager(), null);
                    }
                    break;
                case R.id.fab_stats:
                    if (mPhoto != null) {
                        floatingActionMenu.close(true);
                        StatsDialog statsDialog = new StatsDialog();
                        statsDialog.setPhoto(mPhoto);
                        statsDialog.show(getFragmentManager(), null);
                    }
                    break;
            }
        }
    };

    public void goToUserProfile(View view){
        Intent intent = new Intent(this, UserActivity.class);
        intent.putExtra("username", mPhotoDetails.user.username);
        intent.putExtra("name", mPhotoDetails.user.name);
        startActivity(intent);
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            startActivity(intent);
//        } else {
//            if(imgProfile.getDrawable() != null) {
//                Resplash.getInstance().setDrawable(imgProfile.getDrawable());
//            }
//            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, imgProfile, "profileTransition");
//            ActivityCompat.startActivity(this, intent, options.toBundle());
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.action_share:
                shareTextUrl();
                return true;
            case R.id.action_view_on_unsplash:
                if(mPhotoDetails != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mPhotoDetails.links.html));
                    startActivity(i);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed(){
        supportFinishAfterTransition();
    }

    public View.OnClickListener imageOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(DetailActivity.this, PreviewActivity.class);
            i.putExtra("Link", mPhoto.urls.full);
            startActivity(i);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.cancel();
        }
    }

    public void updateHeartButton(boolean like){

        if(like){
            btnLike.setImageResource(R.drawable.ic_heart_red);
        }else{
            btnLike.setImageResource(R.drawable.ic_heart_outline_grey);
        }
    }

    public void likeImage(View view){
        if(!like){
            like = true;
        }else{
            like = false;
        }
        updateHeartButton(like);
    }

    private void shareTextUrl() {
        if(mPhoto != null) {
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("text/plain");
            share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            share.putExtra(Intent.EXTRA_SUBJECT, "Unsplash Image");
            share.putExtra(Intent.EXTRA_TEXT, mPhoto.links.html);

            startActivity(Intent.createChooser(share, "Share via"));
        }
    }

    public void downloadFromURL(String url, final int type){
        ImageDownloader imageDownloader = new ImageDownloader(new ImageDownloader.OnImageLoaderListener() {
            @Override
            public void onError(ImageDownloader.ImageError error) {
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.setProgress(0);
                Log.e(TAG, "Image download error");
                Toast.makeText(DetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgressChange(int percent) {
                Log.d(TAG, "Percent: " + percent);
                progressBar.setProgress(percent);
            }

            @Override
            public void onComplete(Bitmap result) {
                progressBar.setVisibility(View.INVISIBLE);
                progressBar.setProgress(0);
                if(type == TYPE_DOWNLOAD){
                    final Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                    final File myImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Resplash"
                            + File.separator + mPhoto.id + "_" + sharedPreferences.getString("download_quality", "Unknown") + "." + mFormat.name().toLowerCase());
                    ImageDownloader.writeToDisk(myImageFile, result, new ImageDownloader.OnBitmapSaveListener() {
                        @Override
                        public void onBitmapSaved() {
                            Toast.makeText(DetailActivity.this, "Image saved", Toast.LENGTH_LONG).show();
                            intent.setDataAndType(Uri.parse("file://" + myImageFile.getAbsolutePath()), "image/*");
                            sendNotification(intent);
                        }

                        @Override
                        public void onBitmapSaveError(ImageDownloader.ImageError error) {
                            Toast.makeText(DetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            error.printStackTrace();
                        }
                    }, mFormat, false);
                }else if(type == TYPE_WALLPAPER){
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(DetailActivity.this);
                    try {
                        wallpaperManager.setBitmap(result);
                        Toast.makeText(DetailActivity.this, "Wallpaper set", Toast.LENGTH_LONG).show();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        imageDownloader.download(url, true);
    }

    public void sendNotification(Intent intent) {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_resplash_notification)
                .setContentTitle("Download Wallpaper")
                .setContentText("Download Complete")
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(001, mBuilder.build());
    }

    private void loadPreferences(){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void createCustomAnimation() {
        AnimatorSet set = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(floatingActionMenu.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(floatingActionMenu.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(floatingActionMenu.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(floatingActionMenu.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(150);
        scaleInY.setDuration(150);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                floatingActionMenu.getMenuIconView().setImageResource(floatingActionMenu.isOpened()
                        ? R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp);
            }
        });

        set.play(scaleOutX).with(scaleOutY);
        set.play(scaleInX).with(scaleInY).after(scaleOutX);
        set.setInterpolator(new OvershootInterpolator(2));

        floatingActionMenu.setIconToggleAnimatorSet(set);
    }
}
