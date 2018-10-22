package com.zippy.zippykiosk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.DialogInterface;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.analytics.HitBuilders;
import com.zippy.zippykiosk.rest.Business;
import com.zippy.zippykiosk.rest.CheckinReport;
import com.zippy.zippykiosk.rest.CheckinResult;
import com.zippy.zippykiosk.rest.ClaimRewardResponse;
import com.zippy.zippykiosk.rest.RetrofitUtils;
import com.zippy.zippykiosk.rest.Reward;
import com.zippy.zippykiosk.rest.User;
import com.zippy.zippykiosk.rest.UserUpdateEmailBody;
import com.zippy.zippykiosk.rest.ZippyApi;
import com.zippy.zippykiosk.rest.ZippyError;

import org.parceler.Parcels;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


/**
 * Created by KB on 4/04/15.
 * Copyright 2015 Zippy.com.au.
 */
public class RewardsActivity extends BaseActivity {
    private ListRecyclerAdapter mAdapter;
    private TextView mPointsEarnedBigTextView;
    private TextView mPointsEarnedTextView;
    private TextView mTotalPointsTextView;
    private TextView mTitle;
    private TextView mListTitle;
    private TextView mPointsTitle;
    private Button mSpendButton;
    private Button mClaimHistoryButton;
    private Button mResendButton;
    private Button mEmailButton;
    private TextView mActivationMessageTextView;
    private TextView mEnterEmailMessageTextView;
    private View mActivationCardView;

    private long mPointsTotal;
    private boolean mIsClaimable;
    private int mUserId;
    private int mUserStatus;
    private String mUserEmailAddress;
    private boolean mIsGuest;
    private boolean mIsVerified;
    private boolean mForceEmail;
    private boolean mExtraAccountFields; // If prompting for email, also include extra name and suburb fields

    private Business mBusiness;

    private List<RewardItem> mRewards;
    private MaterialDialog mClaimedRewardDialog;
    private final Handler mHandler = new Handler();
    private MaterialDialog mEmailDialog;
    private ProgressDialog mProgressDialog;
    private CheckinResult mCheckinResult;
    private RewardItem mNewUnlockedReward;

    private Boolean mAnimationDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("onCreate savedInstanceState=" + savedInstanceState);
        setContentView(R.layout.activity_rewards);

        mCheckinResult = Parcels.unwrap(getIntent().getParcelableExtra("CheckinResult"));
        mPointsTotal = mCheckinResult.pointsBalance;
        mIsClaimable = mCheckinResult.isClaimable;
        mUserId = mCheckinResult.userId;
        mUserEmailAddress = mCheckinResult.userEmail;
        mUserStatus = mCheckinResult.userStatus;
        mIsGuest = mCheckinResult.isGuest;
        mIsVerified = mCheckinResult.isVerified;
        mForceEmail = AppPrefs.getSharedPreferences(this).getBoolean(AppPrefs.PREF_ACCOUNT_FORCE_EMAIL, false);
        mExtraAccountFields = AppPrefs.getSharedPreferences(this).getString(AppPrefs.PREF_ACCOUNT_FIELDS, "0").equals("1");
        mBusiness = Business.load(this);
        if(mBusiness==null) {
            finish();
        }
        if(!Business.rewardsEqual(mBusiness.rewards, mCheckinResult.rewards)) {
            mBusiness.rewards = mCheckinResult.rewards.clone();
            Business.save(this,mBusiness);
        }
        mRewards = DB.getRewardsList(mCheckinResult.rewards);

        mTotalPointsTextView = (TextView)findViewById(R.id.totalPoints);
        mPointsEarnedBigTextView = (TextView)findViewById(R.id.points_earned_big);

        mPointsEarnedTextView = (TextView)findViewById(R.id.points_earned);

        mTotalPointsTextView.setText(String.valueOf(mPointsTotal));


        mTitle = (TextView)findViewById(R.id.rewardsTitle);
        mListTitle = (TextView)findViewById(R.id.listTitle);
        mSpendButton = (Button) findViewById(R.id.spend_button);

        if(mBusiness.config.valueConversionRate==null) {
            ((TextView) findViewById(R.id.points_title)).setText(R.string.your_points_balance);
            mSpendButton.setVisibility(View.GONE);
            findViewById(R.id.points_balance_title).setVisibility(View.GONE);
            findViewById(R.id.points_earned_layout).setVisibility(View.VISIBLE);
        }else {
            // Arrange Layout for value based points
            ((TextView) findViewById(R.id.points_title)).setText(R.string.to_earn_points_today);
            mSpendButton.setVisibility(View.VISIBLE);

            findViewById(R.id.points_balance_title).setVisibility(View.VISIBLE);
            findViewById(R.id.points_earned_layout).setVisibility(View.GONE);
            mSpendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);

                    final CheckinAmountDialog checkinDialog = CheckinAmountDialog.create(RewardsActivity.this, mBusiness.uri, mCheckinResult.qrCode, mBusiness.config.accessPin);

                    checkinDialog.timeoutCallback(new CheckinAmountDialog.TimeoutCallback() {
                        @Override
                        public void onTimerStart(@NonNull CheckinAmountDialog dialog) {
                            mHandler.removeCallbacksAndMessages(null);
                            mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                        }

                        @Override
                        public void onTimerStop(@NonNull CheckinAmountDialog dialog) {
                            mHandler.removeCallbacksAndMessages(null);
                        }
                    });
                    checkinDialog.checkinCallback(new CheckinAmountDialog.CheckinCallback() {
                        @Override
                        public void success(@NonNull CheckinAmountDialog dialog, @NonNull CheckinResult result) {
                            mCheckinResult = result;

                            mPointsTotal = mCheckinResult.pointsBalance;
                            mIsGuest = mCheckinResult.isGuest;
                            mIsVerified = mCheckinResult.isVerified;
                            mIsClaimable = mCheckinResult.isClaimable;
                            if (mCheckinResult.pointsEarned > 0) {
                                mNewUnlockedReward = newUnlockedReward(mCheckinResult.pointsEarned, mPointsTotal);
                                mAdapter.notifyDataSetChanged();
                                mTotalPointsTextView.setText(String.valueOf(mPointsTotal));
                                //mSpendButton.setVisibility(View.GONE);

                                //showPointsEarned(mCheckinResult.pointsEarned, false);
                                //findViewById(R.id.points_earned_layout).setVisibility(View.VISIBLE);
                            }
                            updateUIState();
                        }

                        @Override
                        public void dismiss(@NonNull CheckinAmountDialog dialog) {

                        }
                    });
                    checkinDialog.show();
                }
            });

        }
        mClaimHistoryButton = (Button)findViewById(R.id.claim_history_button);
        mClaimHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClaimedReward(mCheckinResult.lastClaimedReward, mCheckinResult.lastClaimedRewardDate, R.string.you_spent_points);
            }
        });
        if(mCheckinResult.lastClaimedReward==null) {
            mClaimHistoryButton.setVisibility(View.GONE);
        }


        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        mActivationCardView = findViewById(R.id.card_view_message);
        mActivationMessageTextView = (TextView) mActivationCardView.findViewById(R.id.activation_message);
        mEnterEmailMessageTextView = (TextView) mActivationCardView.findViewById(R.id.enter_email_message);
        mResendButton = (Button)mActivationCardView.findViewById(R.id.button_resend_email);
        mResendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resendVerificationEmail();

            }
        });
        mEmailButton = (Button)mActivationCardView.findViewById(R.id.button_change_email);
        mEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userEmail();

            }
        });

        mNewUnlockedReward = newUnlockedReward(mCheckinResult.pointsEarned, mPointsTotal);

        RecyclerView rewardsListView = (RecyclerView) findViewById(R.id.lst_rewards);
        rewardsListView.setHasFixedSize(true);
        mAdapter = new ListRecyclerAdapter(mRewards);
        rewardsListView.setAdapter(mAdapter);
        rewardsListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        rewardsListView.setLayoutManager(new LinearLayoutManager(this));


        showPointsEarned(mCheckinResult.pointsEarned, savedInstanceState == null); // animate if first open

        updateUIState();

        if(mIsGuest && (mCheckinResult.checkinsUserTotal>1 || mForceEmail)) {
            // Prompt for email
            userEmail();
        }else {
            animateSpendButton();
        }

    }


    // Animate spend button if visible and if haven't animated previously
    // This is to draw attention to it
    private void animateSpendButton() {
        if(!mAnimationDone && mSpendButton.getVisibility() == View.VISIBLE) {
            mAnimationDone = true;
            Animation zoomAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_bounce);
            mSpendButton.startAnimation(zoomAnimation);
        }
    }

    private void scaleAnimation(final View startView, final View finishView) {

        finishView.setVisibility(View.INVISIBLE);

        startView.setVisibility(View.VISIBLE);
        startView.setScaleX(8f);
        startView.setScaleY(8f);
        startView.postDelayed(new Runnable() {
            public void run() {
                int toPos[] = new int[2];

                //findViewById(R.id.scroll_view).getLocationInWindow(toPos);
                int topOffset = 0;//toPos[1]; // Adjust for statusbar offset.
                finishView.getLocationInWindow(toPos);
                startView.animate()
                        .x(toPos[0])
                        .y(toPos[1] - topOffset)
                        .scaleY(1f)
                        .scaleX(1f)
                        .setDuration(500)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                finishView.setVisibility(View.VISIBLE);
                                startView.setVisibility(View.GONE);
                                startView.clearAnimation();
                            }
                        });
            }
        }, 500);
    }
    private void showPointsEarned(long pointsEarned, boolean animate) {

        if(pointsEarned>0) {
            ((TextView)findViewById(R.id.points_message)).setText(mBusiness.name);
            mPointsEarnedBigTextView.setText(String.valueOf(pointsEarned));
            mPointsEarnedTextView.setText(String.valueOf(pointsEarned));
            ((TextView)findViewById(R.id.points_at_label)).setText(pointsEarned > 1 ? R.string.points_at : R.string.point_at);

            if (animate) {
                scaleAnimation(mPointsEarnedBigTextView, mPointsEarnedTextView);
            } else {
                mPointsEarnedBigTextView.setVisibility(View.GONE);
            }
        }else {
            mPointsEarnedBigTextView.setVisibility(View.GONE);
            StringBuilder sb = new StringBuilder();

            if(mBusiness.config!=null && mBusiness.config.checkinInterval !=null && mBusiness.config.checkinMaxInterval != null) {

                // Format checkin rules for readable string such as '1 check-in every 30 minutes or 3 check-ins per day'
                Duration checkinInterval = Duration.parse(mBusiness.config.checkinInterval);
                Duration maxCheckinInterval = Duration.parse(mBusiness.config.checkinMaxInterval);
                if(checkinInterval!=null) {
                    if (checkinInterval.days > 0) {
                        sb.append("1 check-in every ");
                        if (checkinInterval.days == 1) {
                            sb.append("day");
                        }else {
                            sb.append(checkinInterval.days).append(" days");
                        }
                    }else if(checkinInterval.hours > 0) {
                        sb.append("1 check-in every ");
                        if (checkinInterval.days == 1) {
                            sb.append("hour");
                        }else {
                            sb.append(checkinInterval.hours).append(" hrs");
                        }
                    }else if(checkinInterval.mins > 0) {
                        sb.append("1 check-in every ");
                        if (checkinInterval.mins == 1) {
                            sb.append("minute");
                        }else {
                            sb.append(checkinInterval.mins).append(" minutes");
                        }
                    }
                }
                if(maxCheckinInterval!=null && mBusiness.config.checkinMax>0 ) {

                    if (maxCheckinInterval.days > 0) {
                        if(sb.length()>0) {
                            sb.append(" or ");
                        }
                        sb.append(mBusiness.config.checkinMax).append(" check-in");
                        if(mBusiness.config.checkinMax>1) {
                            sb.append("s");
                        }
                        if (maxCheckinInterval.days == 1) {
                            sb.append(" per day");
                        }else {
                            sb.append(" per ").append(maxCheckinInterval.days).append(" days");
                        }
                    }else if(maxCheckinInterval.hours > 0) {
                        if(sb.length()>0) {
                            sb.append(" or ");
                        }
                        sb.append(mBusiness.config.checkinMax).append(" check-in");
                        if(mBusiness.config.checkinMax>1) {
                            sb.append("s");
                        }
                        if (maxCheckinInterval.days == 1) {
                            sb.append(" per hour");
                        }else {
                            sb.append(" per ").append(maxCheckinInterval.hours).append(" hrs");
                        }
                    }else if(maxCheckinInterval.mins > 0) {
                        if(sb.length()>0) {
                            sb.append(" or ");
                        }
                        sb.append(mBusiness.config.checkinMax).append(" check-in");
                        if(mBusiness.config.checkinMax>1) {
                            sb.append("s");
                        }
                        if (maxCheckinInterval.mins == 1) {
                            sb.append(" per minute");
                        }else {
                            sb.append(" per ").append(maxCheckinInterval.mins).append(" minutes");
                        }
                    }
                }
            }
            if(mBusiness.config!=null && mBusiness.config.valueConversionRate==null) {
                if (sb.length() > 0) {
                    ((TextView) findViewById(R.id.points_message)).setText("This business only allows:\n" + sb.toString());
                    //((TextView) findViewById(R.id.points_message)).setText("Unfortunately you’ve not earned any points for this check-in. This business only allows:\n" + sb.toString());
                } else {
                    ((TextView) findViewById(R.id.points_message)).setText("This business has limits on how often you can checkin.");
                }

                //((TextView) findViewById(R.id.points_message)).setText("Unfortunately you’ve not earned any points for this check-in.\nThis business has limits on how often you can checkin.");
            }
        }
        findViewById(R.id.you_earned_label).setVisibility(pointsEarned>0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.points_at_label).setVisibility(pointsEarned>0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.points_earned).setVisibility(pointsEarned>0 ? View.VISIBLE : View.GONE);
    }

    private void updateUIState() {
        String title;
        if(mIsClaimable && isUnlockedRewards() && !(mForceEmail && mIsGuest)) {
            title = getResources().getString(R.string.choose_your_rewards);
        }else {
            title = String.format(Locale.US,"%s Rewards", mBusiness.name);
        }
        mTitle.setText(title);
        mListTitle.setText(title);

        if(mIsGuest) {
            if (mIsClaimable && !mForceEmail) {
                mEnterEmailMessageTextView.setVisibility(View.VISIBLE);
                mActivationMessageTextView.setVisibility(View.GONE);
            }else {
                mActivationMessageTextView.setText(Html.fromHtml(getString(mForceEmail ? R.string.forceemail_disabled_rewards_message : R.string.noemail_disabled_rewards_message)));
                mEnterEmailMessageTextView.setVisibility(View.GONE);
                mActivationMessageTextView.setVisibility(View.VISIBLE);
            }
            mResendButton.setVisibility(View.GONE);
            mEmailButton.setText(R.string.btn_enter_email);
            mActivationCardView.setVisibility(View.VISIBLE);
        }else if(!mIsVerified) {
            if (mIsClaimable) {
                mActivationMessageTextView.setText(Html.fromHtml(String.format(getString(R.string.email_changed_message), "<b>" + mUserEmailAddress + "</b>")));
            }else {
                mActivationMessageTextView.setText(Html.fromHtml(String.format(getString(R.string.disabled_rewards_message), "<b>" + mUserEmailAddress + "</b>")));
            }
            mActivationMessageTextView.setVisibility(View.VISIBLE);
            mEnterEmailMessageTextView.setVisibility(View.GONE);
            mResendButton.setVisibility(View.VISIBLE);
            mEmailButton.setText(R.string.btn_change_email);
            mActivationCardView.setVisibility(View.VISIBLE);
        }else {
            mActivationCardView.setVisibility(View.GONE);
        }

    }

    @Nullable
    private RewardItem newUnlockedReward(long pointsEarned, long pointsTotal) {
        if(pointsEarned==0 || pointsTotal==0) return null;
        long prevPointsTotal = pointsTotal - pointsEarned;

        RewardItem newUnlockedReward=null;
        for(RewardItem item: mRewards) {
            if (prevPointsTotal < item.points && pointsTotal>=item.points) {
                newUnlockedReward = item;
            }
        }
        return newUnlockedReward;
    }

    private boolean isUnlockedRewards() {
        for(RewardItem item: mRewards) {
            if (mPointsTotal >= item.points) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        Log.i("onResume");
        super.onResume();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);

    }

    @Override
    protected void onDestroy() {
        Log.i("onDestroy");
        mHandler.removeCallbacksAndMessages(null);
        if(mProgressDialog!=null) {
            mProgressDialog.dismiss();
        }
        if(mEmailDialog!=null) {
            mEmailDialog.dismiss();
        }
        super.onDestroy();
    }



    private final Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            finish();
        }
    };


    public class ListRecyclerAdapter extends RecyclerView.Adapter<ListRecyclerAdapter.ListRowHolder> {
        private List<RewardItem> mItemList;

        public ListRecyclerAdapter(List<RewardItem> itemList) {
            this.mItemList = itemList;
        }

        @Override
        public void onBindViewHolder(ListRowHolder listRowHolder, int i) {
            RewardItem listItem = mItemList.get(i);
            listRowHolder.title.setText(listItem.title);
            listRowHolder.points.setText(String.valueOf(listItem.points));

        }

        @Override
        public int getItemViewType(int position) {
            // returns 1 if reward is enabled, 0 if disabled
            RewardItem rewardItem = mItemList.get(position);
            if(mIsClaimable && mPointsTotal >= rewardItem.points && !(mForceEmail && mIsGuest)) {
                if(mNewUnlockedReward!=null && rewardItem.id == mNewUnlockedReward.id) {
                    return (2);
                }else {
                    return(1);
                }
            }else {
                return(0);
            }
        }

        @Override
        public int getItemCount() {
            return (null != mItemList ? mItemList.size() : 0);
        }


        @Override
        public ListRowHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reward, parent, false);

            if(viewType==0) {
                // Item disabled. Not enough points or user not able to claim (eg not verified and claimed one reward)
                v.setEnabled(false);
                v.findViewById(R.id.message).setVisibility(View.GONE);
                ((TextView)v.findViewById(R.id.points)).setTextColor(getResources().getColor(R.color.body_text_disabled));
                ((TextView)v.findViewById(R.id.title)).setTextColor(getResources().getColor(R.color.body_text_disabled));
                ((ImageView)v.findViewById(R.id.imageLock)).setImageResource(R.drawable.ic_lock_outline_black_36dp);
                v.findViewById(R.id.imageLock).setAlpha(0.2f);
                v.findViewById(R.id.imageLock).setVisibility(View.VISIBLE);
            }else {
                v.setEnabled(true);
                v.findViewById(R.id.message).setVisibility(viewType == 2 ? View.VISIBLE : View.GONE);
                ((TextView)v.findViewById(R.id.points)).setTextColor(getResources().getColor(R.color.theme_primary));
                ((TextView)v.findViewById(R.id.title)).setTextColor(getResources().getColor(R.color.body_text_1));
                ((ImageView)v.findViewById(R.id.imageLock)).setImageResource(R.drawable.ic_lock_open_green_36dp);
                v.findViewById(R.id.imageLock).setVisibility(View.VISIBLE);
            }
            return new ListRowHolder(v);
        }


        public final class ListRowHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView points;
            long lastClickTime = System.currentTimeMillis();

            public ListRowHolder(View view) {
                super(view);
                this.title = (TextView) view.findViewById(R.id.title);
                this.points = (TextView) view.findViewById(R.id.points);

                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mHandler.removeCallbacksAndMessages(null);
                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                        long currentTime = System.currentTimeMillis();
                        if(currentTime - lastClickTime < 500) {
                            return;
                        }
                        lastClickTime = currentTime;

                        final RewardItem rewardItem = mItemList.get(getAdapterPosition());
                        MaterialDialog dialog1 = new MaterialDialog.Builder(RewardsActivity.this)
                                .title("Are you sure you would like to spend " + rewardItem.points + " points on this reward now? You can only claim it within staffed hours, please make sure a staff member is near by.")
                                .content(rewardItem.title)
                                .positiveText(android.R.string.ok)
                                .negativeText("Cancel")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                        ClaimReward(rewardItem, mCheckinResult.qrCode);
                                    }
                                })
                                .build();

                        //Here's the magic.. Set the dialog to not focusable (makes navigation ignore us adding the window)
                        dialog1.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                        dialog1.show(); //Show the dialog!

                        //Set the dialog to immersive
                        dialog1.getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());

                        //Clear the not focusable flag from the window
                        dialog1.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

                    }
                });
            }
        }

        private void ClaimReward(final RewardItem rewardItem, final String qrCodeSlug) {

            mHandler.removeCallbacksAndMessages(null);

            mProgressDialog =  ProgressDialog.create("Processing. Please wait...");
            mProgressDialog.show(RewardsActivity.this);

            ZippyApi.getInstance().getApiService().claimReward(rewardItem.id,"</qrcodes/" + qrCodeSlug +"/resolve/user>", new Callback<ClaimRewardResponse>() {
                @Override
                public void success(ClaimRewardResponse claimRewardResponse, Response response) {
                    mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                    int pointsSpent = -claimRewardResponse.point.amount;


                    //mIsClaimable = claimRewardResponse.info.reward.claimable;
                    if(claimRewardResponse.getPointsForBusiness(mBusiness.id)!=mPointsTotal-pointsSpent) {
                        Log.e("After claiming a reward, mismatch in points balance");
                    }
                    if(!claimRewardResponse.reward.name.equals(rewardItem.title)) {
                        Log.e("After claiming a reward, reward title changed");
                    }
                    if(pointsSpent != claimRewardResponse.reward.points) {
                        Log.e("Points spent is not the same as reward points");
                    }

                    // Update the users balance after claiming a reward
                    mPointsTotal = mPointsTotal-pointsSpent;
                    mNewUnlockedReward = null;
                    mCheckinResult.lastClaimedReward = claimRewardResponse.reward;
                    mCheckinResult.lastClaimedRewardDate = new Date();
                    if(mCheckinResult.lastClaimedReward==null) {
                        mClaimHistoryButton.setVisibility(View.GONE);
                    }

                    mAdapter.notifyDataSetChanged();
                    mTotalPointsTextView.setText(String.valueOf(mPointsTotal));
                    updateUIState();

                    showClaimedReward(mCheckinResult.lastClaimedReward, mCheckinResult.lastClaimedRewardDate, R.string.you_have_spent_points);

                }

                @Override
                public void failure(RetrofitError error) {
                    KioskApp.tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(RetrofitUtils.getErrorDescription("Claim Reward Failure.", error))
                            .setFatal(false)
                            .build());
                    CrashlyticsCore.getInstance().logException(new Exception("Claim Reward Failure.", error));

                    //String serverResponseBody = ZippyApi.getResponseBody(error);
                    //if(serverResponseBody!=null) Log.e(serverResponseBody);
                    mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                    mProgressDialog.showError("Error",RetrofitUtils.getUserErrorDescription(error));
                }
            });
        }
    }

    private void resendVerificationEmail() {
        mHandler.removeCallbacksAndMessages(null);
        mProgressDialog = ProgressDialog.create("Processing...");
        mProgressDialog.show(RewardsActivity.this);

        ZippyApi.getInstance().getApiService().userResendVerificationEmail("</users/" + mUserEmailAddress + ">", new Callback<Response>() {

            @Override
            public void success(Response ignore, Response response) {
                updateUIState();
                mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                mProgressDialog.showSuccess("Success! We’ve sent you an email",
                        Html.fromHtml("To activate your account, please click the link in the email we sent to <b>" + mUserEmailAddress + "</b>"));
                updateUIState();
                mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                mProgressDialog = null;
            }

            @Override
            public void failure(RetrofitError error) {
                mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                mProgressDialog.showError("Error", error.getLocalizedMessage());
                mProgressDialog = null;
                CrashlyticsCore.getInstance().logException(new Exception("User Request Verification Email.", error));
            }
        });
    }


    private void userEmail() {
        Log.i("userEmail()");
        mEmailDialog = new MaterialDialog.Builder(RewardsActivity.this)
                .title(mIsGuest ? R.string.enter_email_title : R.string.change_email_title)
                .customView(R.layout.email_layout, true)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .autoDismiss(false)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction dialogAction) {
                        dialog.dismiss();
                    }
                })
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction dialogAction) {
                        final View view = dialog.getCustomView();
                        assert view != null;
                        final EditText emailEditText = (EditText) view.findViewById(R.id.email);
                        final EditText nameEditText = (EditText) view.findViewById(R.id.name);
                        final EditText postcodeEditText = (EditText) view.findViewById(R.id.postcode);
                        final TextView errorTextView = (TextView) view.findViewById(R.id.error);
                        final TextView messageTextView = (TextView) view.findViewById(R.id.message);
                        final View progressBar = view.findViewById(R.id.progressBar);

                        final String email = emailEditText.getText().toString();
                        final String customername = mExtraAccountFields ? nameEditText.getText().toString() : null;
                        final String postcode = mExtraAccountFields ? postcodeEditText.getText().toString() : null;
                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(false);
                        emailEditText.setEnabled(false);
                        nameEditText.setEnabled(false);
                        postcodeEditText.setEnabled(false);
                        progressBar.setVisibility(View.VISIBLE);
                        errorTextView.setVisibility(View.GONE);

                        if (messageTextView.getVisibility() == View.GONE) {
                            mHandler.removeCallbacksAndMessages(null);

                            if (mIsGuest) {

                                ZippyApi.getInstance().promoteUser(mUserId, mUserStatus, email, customername, postcode, new Callback<Response>() {

                                    @Override
                                    public void success(Response ignore, Response response) {
                                        mIsGuest = false;
                                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                        dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                                        emailEditText.setVisibility(View.GONE);
                                        nameEditText.setVisibility(View.GONE);
                                        postcodeEditText.setVisibility(View.GONE);
                                        progressBar.setVisibility(View.GONE);
                                        dialog.setTitle(R.string.email_changed_title);
                                        messageTextView.setText(Html.fromHtml(String.format(getString(R.string.email_changed_message), "<b>" + email + "</b>")));
                                        messageTextView.setVisibility(View.VISIBLE);
                                        mUserStatus = User.setPromotedStatus(mUserStatus);
                                        mUserEmailAddress = email;
                                        updateUIState();
                                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        ZippyError zippyError = ZippyError.createZippyError(error);
                                        if (zippyError != null && zippyError.httpStatus == 409 && zippyError.isErrorCode(ZippyError.CONFLICT)) {
                                            // We get a 409 error if promote fails because account already exists with matching email
                                            CrashlyticsCore.getInstance().logException(new Exception("Promote User Failed.", error));
                                            dialog.setTitle("Account already exists");
                                            messageTextView.setText("Would you like to link to account " + email + " ?");

                                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                            emailEditText.setVisibility(View.GONE);
                                            nameEditText.setVisibility(View.GONE);
                                            postcodeEditText.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.GONE);
                                            errorTextView.setVisibility(View.GONE);
                                            messageTextView.setVisibility(View.VISIBLE);

                                        } else {
                                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                            emailEditText.setEnabled(true);
                                            nameEditText.setEnabled(true);
                                            postcodeEditText.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                            errorTextView.setVisibility(View.VISIBLE);
                                            if (zippyError != null && zippyError.httpStatus == 422 && zippyError.isErrorCode(ZippyError.UNPROCESSABLE_ENTITY_INVALID)) {
                                                errorTextView.setText("The email address is not valid");
                                            } else {
                                                errorTextView.setText(error.getLocalizedMessage());
                                            }
                                            CrashlyticsCore.getInstance().logException(new Exception("User Promote Failed.", error));
                                        }
                                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                    }
                                });
                            } else {
                                // Just update the user's email. We shouldn't be changing anything else.
                                UserUpdateEmailBody userUpdateEmailBody = new UserUpdateEmailBody(email);
                                ZippyApi.getInstance().getApiService().userUpdateEmail(mUserId, userUpdateEmailBody, new Callback<Response>() {

                                    @Override
                                    public void success(Response ignore, Response response) {
                                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                        dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                                        emailEditText.setVisibility(View.GONE);
                                        progressBar.setVisibility(View.GONE);
                                        //dialog.setTitle("Email Address Updated");
                                        dialog.setTitle(R.string.email_changed_title);

                                        messageTextView.setText(Html.fromHtml(String.format(getString(R.string.email_changed_message), "<b>" + email + "</b>")));
                                        messageTextView.setVisibility(View.VISIBLE);
                                        mUserEmailAddress = email;
                                        updateUIState();
                                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        ZippyError zippyError = ZippyError.createZippyError(error);
                                        if (zippyError != null && zippyError.httpStatus == 409 && zippyError.isErrorCode(ZippyError.CONFLICT)) {
                                            // If email conflict changing users email, and if user has not been validated then we need to prompt and try merging

                                            CrashlyticsCore.getInstance().logException(new Exception("User Change Email Failed.", error));
                                            dialog.setTitle("Account already exists");
                                            messageTextView.setText("Would you like to link to account " + email + " ?");

                                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                            emailEditText.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.GONE);
                                            errorTextView.setVisibility(View.GONE);
                                            messageTextView.setVisibility(View.VISIBLE);

                                        } else {
                                            dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                            dialog.getActionButton(DialogAction.NEGATIVE).setEnabled(true);
                                            emailEditText.setEnabled(true);
                                            progressBar.setVisibility(View.GONE);
                                            errorTextView.setVisibility(View.VISIBLE);

                                            if (zippyError != null && zippyError.httpStatus == 422 && zippyError.isErrorCode(ZippyError.UNPROCESSABLE_ENTITY_INVALID)) {
                                                errorTextView.setText("The email address is not valid");
                                            } else {
                                                errorTextView.setText(error.getLocalizedMessage());
                                            }
                                            CrashlyticsCore.getInstance().logException(new Exception("User Change Email Failed.", error));
                                        }
                                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                    }
                                });
                            }
                        } else if (dialog.getActionButton(DialogAction.NEGATIVE).getVisibility() == View.VISIBLE) {
                            mHandler.removeCallbacksAndMessages(null);

                            // Use email address rather than userID if possible, in case previous merge successful
                            // but subsequent failure to get checkin report and new userID. We can't use guest email in userUri.
                            String mergeUri = "</users/" + (mIsGuest ? mUserId : mUserEmailAddress) + ">";
                            final String targetUri = "</users/" + email + ">";
                            ZippyApi.getInstance().getApiService().userMerge(mergeUri, targetUri, new Callback<Response>() {

                                @Override
                                public void success(Response ignore, Response response) {
                                    // Merge successful. We now need to get latest checkin status as user may have more points from merged target account
                                    String businessUri = "</business/" + mBusiness.id + ">";
                                    ZippyApi.getInstance().getApiService().checkinReport(businessUri, targetUri, new Callback<CheckinReport>() {
                                        @Override
                                        public void success(CheckinReport checkinReport, Response response) {
                                            dialog.dismiss();
                                            if (BuildConfig.DEBUG) {
                                                if (!email.equalsIgnoreCase(checkinReport.user.email))
                                                    Log.e("Email address should be the same");
                                                if (checkinReport.report.user.guest) {
                                                    Log.e("User should not be a guest after merge");
                                                }
                                            }
                                            mUserId = checkinReport.user.id;
                                            mUserStatus = checkinReport.user.status;
                                            mUserEmailAddress = checkinReport.user.email;
                                            mPointsTotal = checkinReport.report.point.balance;
                                            mIsGuest = checkinReport.report.user.guest;
                                            mIsVerified = checkinReport.report.user.verified;
                                            mIsClaimable = checkinReport.report.reward.claimable;

                                            // TODO: Don't need checkin totals? Only used when activity created
                                            //mCheckinsBusinessTotal = checkinReport.report.checkin.businessTotal;
                                            //mCheckinsUserTotal = checkinReport.report.checkin.userTotal;

                                            mNewUnlockedReward = null;
                                            mAdapter.notifyDataSetChanged();
                                            mTotalPointsTextView.setText(String.valueOf(mPointsTotal));
                                            updateUIState();
                                            mEmailDialog = null;
                                            mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                        }

                                        @Override
                                        public void failure(RetrofitError error) {
                                            // TODO: Bad state. User merged, but we fail to get updated checkinReport
                                            // Best we can do is probably just show current points etc that user had before merge, rather than report an error because
                                            // we can't undo the merge.
                                            // Alternative could be to remain on dialog showing error message with a Retry option.
                                            dialog.dismiss();
                                            Log.e("Potentially bad state. User merged, but failed to get checkin report", error);

                                            mUserEmailAddress = email;
                                            mIsGuest = false; // If we merged then we should no longer be a guest
                                            updateUIState();
                                            mEmailDialog = null;
                                            mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                        }
                                    });

                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    dialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                                    dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                                    progressBar.setVisibility(View.GONE);
                                    dialog.setTitle("Error");
                                    messageTextView.setText("Could not link to account " + email + "\n" + error.getLocalizedMessage());
                                    CrashlyticsCore.getInstance().logException(new Exception("User Merge Failed.", error));
                                    mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                                }
                            });
                        } else {
                            dialog.dismiss();
                            mEmailDialog = null;
                        }
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mEmailDialog = null;
                        mHandler.removeCallbacksAndMessages(null);
                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                        animateSpendButton();
                    }
                })
                .build();
        View view = mEmailDialog.getCustomView();
        assert view != null;
        mEmailDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);

        ((EditText) view.findViewById(R.id.email)).addTextChangedListener(emailTextWatcher);
        if(mExtraAccountFields && mIsGuest) {
            ((EditText) view.findViewById(R.id.name)).addTextChangedListener(emailTextWatcher);
            ((EditText) view.findViewById(R.id.postcode)).addTextChangedListener(emailTextWatcher);
        }else {
            view.findViewById(R.id.name).setVisibility(View.GONE);
            view.findViewById(R.id.postcode).setVisibility(View.GONE);
        }
        //Here's the magic.. Set the dialog to not focusable (makes navigation ignore us adding the window)
        //emailDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        mEmailDialog.show(); //Show the dialog!

        //Set the dialog to immersive
        //emailDialog.getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());

        //Clear the not focusable flag from the window
        //emailDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }

    private TextWatcher emailTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Reset inactivity timeout after every key press, so they have sufficient time to enter email
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);

            View view = mEmailDialog.getCustomView();

            String email = ((EditText)view.findViewById(R.id.email)).getText().toString();
            Boolean validationPass = !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
            if(validationPass && mExtraAccountFields && mIsGuest) {
                String name = ((EditText)view.findViewById(R.id.name)).getText().toString();
                validationPass = !name.isEmpty();
            }
            if(validationPass && mExtraAccountFields && mIsGuest) {
                String postcode = ((EditText)view.findViewById(R.id.postcode)).getText().toString();
                validationPass = postcode.length()==4;
            }
            mEmailDialog.getActionButton(DialogAction.POSITIVE).setEnabled(validationPass);
        }
    };

    private final Runnable mClaimedRewardTimer = new Runnable() {
        @Override
        public void run() {
            View view = mClaimedRewardDialog.getCustomView();
            updateClaimedRewardTime(view);
            mHandler.postDelayed(mClaimedRewardTimer, 1000);
        }
    };

    private void updateClaimedRewardTime(View v) {
        StringBuilder timeStampStringBuilder = new StringBuilder(DateUtils.formatDateTime(getBaseContext(), DB.timestamp, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));
        if(System.currentTimeMillis() - DB.timestamp < (long)7*24*60*60*1000) {
            // Time ago limited to max 7 days, then reverts to the date.
            timeStampStringBuilder
                    .append(" (")
                    .append(DateUtils.getRelativeTimeSpanString(DB.timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString())
                    .append(")");
        }
        ((TextView) v.findViewById(R.id.timestamp)).setText(timeStampStringBuilder.toString());
    }

    private void showClaimedReward(@Nullable Reward reward,@Nullable Date claimedDate, @StringRes int res_id) {
        if(reward==null || claimedDate==null) {
            return;
        }
        int pointsSpent = mCheckinResult.lastClaimedReward.points;
        long timeClaimedInMillis = claimedDate.getTime();

        mClaimedRewardDialog = new MaterialDialog.Builder(RewardsActivity.this)
                .customView(R.layout.claimed_reward, false)
                .cancelable(false)
                .positiveText(R.string.done)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        mHandler.removeCallbacksAndMessages(null);
                        mHandler.postDelayed(mTimeout, Constants.INACTIVITY_TIMEOUT);
                        dialog.dismiss();
                    }
                })
                .showListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mHandler.postDelayed(mClaimedRewardTimer, 1000);
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mHandler.removeCallbacks(mClaimedRewardTimer);
                        mClaimedRewardDialog = null;
                    }
                })
                .build();
        View view = mClaimedRewardDialog.getCustomView();
        assert view != null;

        String msg = String.format(getString(res_id), pointsSpent, pointsSpent>1 ? "s" : "");
        ((TextView) view.findViewById(R.id.title)).setText(msg);
        ((TextView) view.findViewById(R.id.reward)).setText(reward.name);
        DB.timestamp = timeClaimedInMillis;
        updateClaimedRewardTime(view);

        //Here's the magic.. Set the dialog to not focusable (makes navigation ignore us adding the window)
        mClaimedRewardDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        mClaimedRewardDialog.show(); //Show the dialog!

        //Set the dialog to immersive
        mClaimedRewardDialog.getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());

        //Clear the not focusable flag from the window
        mClaimedRewardDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
