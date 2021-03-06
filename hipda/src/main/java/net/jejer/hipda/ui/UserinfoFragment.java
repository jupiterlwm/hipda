package net.jejer.hipda.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import net.jejer.hipda.R;
import net.jejer.hipda.async.HiStringRequest;
import net.jejer.hipda.async.PostSmsAsyncTask;
import net.jejer.hipda.async.SimpleListLoader;
import net.jejer.hipda.async.VolleyHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.bean.SimpleListBean;
import net.jejer.hipda.bean.SimpleListItemBean;
import net.jejer.hipda.bean.UserInfoBean;
import net.jejer.hipda.glide.GlideHelper;
import net.jejer.hipda.utils.HiParser;
import net.jejer.hipda.utils.HiUtils;

import java.util.ArrayList;
import java.util.List;

public class UserinfoFragment extends Fragment {
    private final String LOG_TAG = getClass().getSimpleName();

    public static final String ARG_USERNAME = "USERNAME";
    public static final String ARG_UID = "UID";

    private String mUid;
    private String mUsername;

    private ImageView mAvatarView;
    private TextView mDetailView;

    private ListView mThreadListView;
    private SimpleListAdapter mSimpleListAdapter;
    private Button mButton;
    private LoaderManager.LoaderCallbacks<SimpleListBean> mCallbacks;

    private boolean isShowThreads;
    private boolean isThreadsLoaded;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(LOG_TAG, "onCreate");
        setHasOptionsMenu(true);

        if (getArguments().containsKey(ARG_USERNAME)) {
            mUsername = getArguments().getString(ARG_USERNAME);
        }

        if (getArguments().containsKey(ARG_UID)) {
            mUid = getArguments().getString(ARG_UID);
        }

        List<SimpleListItemBean> a = new ArrayList<SimpleListItemBean>();
        mSimpleListAdapter = new SimpleListAdapter(getActivity(), R.layout.item_simple_list, a, SimpleListLoader.TYPE_SEARCH_USER_THREADS);
        mCallbacks = new SearchThreadByUidLoaderCallbacks();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_userinfo, container, false);
        view.setClickable(false);

        mAvatarView = (ImageView) view.findViewById(R.id.userinfo_avatar);

        TextView usernameTv = (TextView) view.findViewById(R.id.userinfo_username);
        usernameTv.setText(mUsername);

        mDetailView = (TextView) view.findViewById(R.id.userinfo_detail);
        mDetailView.setText("正在获取信息...");

        //to avoid click through this view
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        mThreadListView = (ListView) view.findViewById(R.id.lv_search_threads);
        mButton = (Button) view.findViewById(R.id.btn_search_threads);
        mButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View view) {
                isShowThreads = !isShowThreads;
                if (isShowThreads) {
                    mButton.setText("显示信息");
                    mDetailView.setVisibility(View.GONE);
                    mThreadListView.setVisibility(View.VISIBLE);
                    if (!isThreadsLoaded) {
                        mButton.setEnabled(false);
                        getLoaderManager().restartLoader(0, null, mCallbacks).forceLoad();
                    }
                } else {
                    mButton.setText("搜索帖子");
                    mThreadListView.setVisibility(View.GONE);
                    mDetailView.setVisibility(View.VISIBLE);
                }
            }
        });

        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        StringRequest sReq = new HiStringRequest(getActivity(), HiUtils.UserInfoUrl + mUid,
                new OnDetailLoadComplete(),
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mDetailView.setText("获取信息失败, 请重试.");
                    }
                });
        VolleyHelper.getInstance().add(sReq);

        mThreadListView.setAdapter(mSimpleListAdapter);
        mThreadListView.setOnItemClickListener(new OnItemClickCallback());

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(LOG_TAG, "onCreateOptionsMenu");

        menu.clear();
        inflater.inflate(R.menu.menu_userinfo, menu);

        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle(mUsername);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Implemented in activity
                return false;
            case R.id.action_send_sms:
                showSendSmsDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    class OnDetailLoadComplete implements Response.Listener<String> {
        @Override
        public void onResponse(String response) {
            UserInfoBean info = HiParser.parseUserInfo(response);
            if (info != null) {
                if (HiSettingsHelper.getInstance().isShowThreadListAvatar()) {
                    mAvatarView.setVisibility(View.VISIBLE);
                    GlideHelper.loadAvatar(getActivity(), mAvatarView, info.getmAvatarUrl());
                } else {
                    mAvatarView.setVisibility(View.GONE);
                }
                mDetailView.setText(info.getmDetail());
            } else {
                mDetailView.setText("解析信息失败, 请重试.");
            }
        }
    }

    private void showSendSmsDialog() {
        final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewlayout = inflater.inflate(R.layout.dialog_userinfo_sms, null);

        final EditText smsTextView = (EditText) viewlayout.findViewById(R.id.et_userinfo_sms);

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(getActivity());
        popDialog.setTitle("发送短消息给 " + mUsername);
        popDialog.setView(viewlayout);
        // Add the buttons
        popDialog.setPositiveButton("发送",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new PostSmsAsyncTask(getActivity(), mUid, null).execute(smsTextView.getText().toString());
                    }
                });
        popDialog.setNegativeButton("取消", null);
        popDialog.create().show();
    }

    public class SearchThreadByUidLoaderCallbacks implements LoaderManager.LoaderCallbacks<SimpleListBean> {

        @Override
        public Loader<SimpleListBean> onCreateLoader(int arg0, Bundle arg1) {
            Toast.makeText(getActivity(), "加载中...", Toast.LENGTH_SHORT).show();
            return new SimpleListLoader(UserinfoFragment.this.getActivity(), SimpleListLoader.TYPE_SEARCH_USER_THREADS, 1, mUid);
        }

        @Override
        public void onLoadFinished(Loader<SimpleListBean> loader,
                                   SimpleListBean list) {
            Log.v(LOG_TAG, "onLoadFinished enter");

            if (mButton != null)
                mButton.setEnabled(true);

            if (list == null || list.getCount() == 0) {
                Log.v(LOG_TAG, "onLoadFinished list == null || list.getCount == 0");
                Toast.makeText(getActivity(), "帖子加载失败", Toast.LENGTH_LONG).show();
                return;
            }

            Log.v(LOG_TAG, "mThreadListAdapter.addAll(arg1.threads) called, added " + list.getCount());
            mSimpleListAdapter.addAll(list.getAll());
            isThreadsLoaded = true;
        }

        @Override
        public void onLoaderReset(Loader<SimpleListBean> arg0) {
            Log.v(LOG_TAG, "onLoaderReset");
        }

    }

    public class OnItemClickCallback implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> listView, View itemView, int position,
                                long row) {

            setHasOptionsMenu(false);
            SimpleListItemBean item = mSimpleListAdapter.getItem(position);

            Bundle bun = new Bundle();
            Fragment fragment = null;
            bun.putString(ThreadDetailFragment.ARG_TID_KEY, item.getId());
            bun.putString(ThreadDetailFragment.ARG_TITLE_KEY, item.getTitle());
            fragment = new ThreadDetailFragment();
            fragment.setArguments(bun);
            if (HiSettingsHelper.getInstance().getIsLandscape()) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.thread_detail_container_in_main, fragment, ThreadDetailFragment.class.getName())
                        .addToBackStack(ThreadDetailFragment.class.getName())
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .add(R.id.main_frame_container, fragment, ThreadDetailFragment.class.getName())
                        .addToBackStack(ThreadDetailFragment.class.getName())
                        .commit();
            }
        }
    }

}
