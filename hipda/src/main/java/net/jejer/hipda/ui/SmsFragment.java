package net.jejer.hipda.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import net.jejer.hipda.R;
import net.jejer.hipda.async.PostSmsAsyncTask;
import net.jejer.hipda.async.SimpleListLoader;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.bean.SimpleListBean;
import net.jejer.hipda.utils.Constants;

public class SmsFragment extends Fragment implements PostSmsAsyncTask.PostListener {
    private final String LOG_TAG = getClass().getSimpleName();

    public static final String ARG_ID = "ID";
    public static final String ARG_UID = "UID";

    private String mId;
    private String mUid;
    private SmsAdapter mAdapter;
    private SmsListLoaderCallbacks mLoaderCallbacks;
    private ListView mListView;

    private HiProgressDialog postProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(LOG_TAG, "onCreate");
        setHasOptionsMenu(true);

        if (getArguments().containsKey(ARG_ID)) {
            mId = getArguments().getString(ARG_ID);
        }
        if (getArguments().containsKey(ARG_UID)) {
            mUid = getArguments().getString(ARG_UID);
        }

        mAdapter = new SmsAdapter(getActivity(), R.layout.item_sms_list);
        mLoaderCallbacks = new SmsListLoaderCallbacks();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_sms, container, false);
        mListView = (ListView) view.findViewById(R.id.lv_sms);

        //to avoid click through this view
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        ImageButton postIb = (ImageButton) view.findViewById(R.id.ib_send_sms);
        final EditText etSms = (EditText) view.findViewById(R.id.et_sms);
        etSms.setTextSize(HiSettingsHelper.getPostTextSize());
        postIb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String replyText = etSms.getText().toString();
                if (replyText.length() > 0) {
                    new PostSmsAsyncTask(getActivity(), mUid, SmsFragment.this).execute(replyText);
                    // Close SoftKeyboard
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etSms.getWindowToken(), 0);
                    etSms.setText("");
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.v(LOG_TAG, "onActivityCreated");

        // destroyLoader called here to avoid onLoadFinished called when onResume
        getLoaderManager().destroyLoader(0);
        mListView.setAdapter(mAdapter);
        getLoaderManager().restartLoader(0, null, mLoaderCallbacks).forceLoad();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(LOG_TAG, "onCreateOptionsMenu");

        menu.clear();
        //inflater.inflate(R.menu.menu_simple_thread_list, menu);

        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setTitle("与" + mId + "的短消息");

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrePost() {
        postProgressDialog = HiProgressDialog.show(getActivity(), "正在发送...");
    }

    @Override
    public void onPostDone(int status, String message) {
        if (status == Constants.STATUS_SUCCESS) {
            //new sms has some delay, so this is a dirty hack
            new CountDownTimer(1000, 1000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    try {
                        getLoaderManager().restartLoader(0, null, mLoaderCallbacks).forceLoad();
                    } catch (Exception ignored) {

                    }
                }
            }.start();
            postProgressDialog.dismiss(message);
        } else {
            postProgressDialog.dismiss(message, 3000);
        }

    }

    public class SmsListLoaderCallbacks implements LoaderManager.LoaderCallbacks<SimpleListBean> {

        @Override
        public Loader<SimpleListBean> onCreateLoader(int arg0, Bundle arg1) {
            return new SimpleListLoader(SmsFragment.this.getActivity(), SimpleListLoader.TYPE_SMSDETAIL, 1, mUid);
        }

        @Override
        public void onLoadFinished(Loader<SimpleListBean> loader,
                                   SimpleListBean list) {

            Log.v(LOG_TAG, "onLoadFinished enter");

            if (list == null || list.getCount() == 0) {
                Toast.makeText(SmsFragment.this.getActivity(),
                        "没有短消息", Toast.LENGTH_LONG).show();
                return;
            }

            mAdapter.clear();
            mAdapter.addAll(list.getAll());
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(mAdapter.getCount());
        }

        @Override
        public void onLoaderReset(Loader<SimpleListBean> arg0) {
            Log.v(LOG_TAG, "onLoaderReset");
        }
    }

}
