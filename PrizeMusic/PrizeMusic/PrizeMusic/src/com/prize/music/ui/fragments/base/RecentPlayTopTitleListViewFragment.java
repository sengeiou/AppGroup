package com.prize.music.ui.fragments.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.prize.music.Constants;
import com.prize.music.IfragToActivityLister;
import com.prize.music.R;
import com.prize.music.activities.SearchBrowserActivity;
import com.prize.music.bean.PopBean;
import com.prize.music.helpers.RefreshableFragment;
import com.prize.music.helpers.utils.LogUtils;
import com.prize.music.helpers.utils.MusicUtils;
import com.prize.music.helpers.utils.ToastUtils;
import com.prize.music.history.HistoryDao;
import com.prize.music.service.ApolloService;
import com.prize.music.ui.adapters.MainPopAdapter;
import com.prize.music.ui.adapters.base.ListViewAdapter;
import com.prize.music.ui.fragments.MeFragment;
import com.prize.music.ui.fragments.MusicLibraryFragment;

public abstract class RecentPlayTopTitleListViewFragment extends
		RefreshableFragment implements OnItemClickListener, OnTouchListener,
		IfragToActivityLister, OnItemLongClickListener, LoaderCallbacks<Cursor> {

	private IfragToActivityLister mIfragToActivity;

	protected String TAG = "RecentPlayTopTitleListViewFragment";
	// Adapter
	protected ListViewAdapter mAdapter;

	// ListView
	protected ListView mListView;

	// Cursor
	protected Cursor mCursor;

	// Selected position
	protected int mSelectedPosition;

	// Used to set ringtone
	protected long mSelectedId;

	// Options
	protected final int PLAY_SELECTION = 0;

	protected final int USE_AS_RINGTONE = 1;

	protected final int ADD_TO_PLAYLIST = 2;

	protected final int SEARCH = 3;

	protected int mFragmentGroupId = 0;

	protected String mCurrentId, mSortOrder = null, mWhere = null,
			mType = null, mTitleColumn = null;

	protected String[] mProjection = null;

	protected Uri mUri = null;

	//
	private TextView action_back;

	private TextView search_Tv;
	DialogFragment df = null;

	// protected ViewPager viewPager;

	// Bundle

	private Handler handler;

	public RecentPlayTopTitleListViewFragment() {
	}

	@Override
	public void onAttach(Activity activity) {

		try {
			mIfragToActivity = (IfragToActivityLister) activity;
		} catch (Exception e) {
			throw new ClassCastException(activity.toString()
					+ "must implement  IfragToActivity");
		}
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		try {
			if (mIfragToActivity != null) {
				mIfragToActivity = null;
			}
		} catch (Exception e) {
			return;
		}
		super.onDetach();
	}

	/*
	 * To be overrode in child classes to setup fragment data
	 */

	public abstract void setupFragmentData();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupFragmentData();
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.setAdapter(mAdapter);

	}

	private OnClickListener listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int key = v.getId();
			switch (key) {
			case R.id.action_back:
				getActivity().getSupportFragmentManager().popBackStack();
				break;
			case R.id.action_search:
				// getActivity().onSearchRequested();
				Intent searchIntent = new Intent(getActivity(),
						SearchBrowserActivity.class);
				startActivity(searchIntent);
				break;

			default:
				break;
			}

		}
	};
	private RelativeLayout select_Rlyt;
	private RelativeLayout action_back_Rlyt;
	private TextView action_cancel;
	private TextView action_sure;
	protected boolean isSelectMode = false;

	public void onViewCreated(View view, Bundle savedInstanceState) {

		view.setOnTouchListener(this);
		super.onViewCreated(view, savedInstanceState);
	};

	/**
	 * 解决fragment事件穿透问题
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View root = inflater.inflate(R.layout.toptitle_listview, container,
				false);
		mListView = (ListView) root.findViewById(android.R.id.list);
		action_back = (TextView) root.findViewById(R.id.action_back);
		search_Tv = (TextView) root.findViewById(R.id.action_search);

		select_Rlyt = (RelativeLayout) root.findViewById(R.id.select_Rlyt);
		action_back_Rlyt = (RelativeLayout) root
				.findViewById(R.id.action_back_Rlyt);
		action_cancel = (TextView) root.findViewById(R.id.action_cancel);
		action_sure = (TextView) root.findViewById(R.id.action_sure);
		search_Tv.setOnClickListener(listener);
		action_back.setOnClickListener(listener);
		if (getArguments() != null) {
			String title = getArguments().getString("flag");
			if (!TextUtils.isEmpty(title)) {
				action_back.setText(title);
			}

		}

		// findViewById();

		setListener();
		return root;
	}

	private void setListener() {
		handler = new Handler();
		action_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateViews();
				mIfragToActivity.processAction(Constants.ACTION_CANCE);

			}
		});
		action_sure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mAdapter.isSelectAll()) {
					mAdapter.selectAllItem(false);
					action_sure.setText(getString(R.string.all_select));
				} else {
					action_sure.setText(getString(R.string.no_select));
					mAdapter.selectAllItem(true);
				}

				mIfragToActivity.countNum(mAdapter.getSelectedAudioIds().length);

			}
		});
		action_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getFragmentManager().popBackStack();

			}
		});

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, final int position,
			long id) {
		if (!isSelectMode) {
			MusicUtils.removeAllTracks();
			mAdapter.play(v);
			parent.post(new Runnable() {

				@Override
				public void run() {
					MusicUtils.playAll(getActivity(), mCursor, position - 1);

				}
			});
		} else {
			mAdapter.toggleCheckedState(position - 1);
			int selectCount = mAdapter.getSelectedAudioIds().length;
			mIfragToActivity.countNum(selectCount);
			if (selectCount == mListView.getCount()-1) {
				mAdapter.setIsSelectAll(true);
				action_sure.setText(getString(R.string.no_select));
			} else {
				mAdapter.setIsSelectAll(false);
				action_sure.setText(getString(R.string.all_select));
			}
		}

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		if (!isSelectMode) {
			// Animation animation = AnimationUtils.loadAnimation(getActivity(),
			// R.anim.in_from_right);
			select_Rlyt.setVisibility(View.VISIBLE);
			// select_Rlyt.startAnimation(animation);
			action_back_Rlyt.setVisibility(View.GONE);
			getActivity().findViewById(R.id.bottomactionbar_new).setVisibility(
					View.GONE);
			LinearLayout mLinearLayout = (LinearLayout) getActivity()
					.findViewById(R.id.main_bottom_layout);
			if (mLinearLayout != null) {
				mLinearLayout.setVisibility(View.VISIBLE);
				// mLinearLayout.startAnimation(animation);
			}
			mAdapter.setSelectMode(true);
			mAdapter.toggleCheckedState(position - 1);
			mIfragToActivity.countNum(1);
		}
		isSelectMode = true;
		return true;
	}

	@Override
	public void onResume() {

		super.onResume();

		getView().setFocusableInTouchMode(true);
		getView().requestFocus();
		getView().setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

				if (event.getAction() == KeyEvent.ACTION_UP
						&& keyCode == KeyEvent.KEYCODE_BACK) {
					if (window != null && window.isShowing()) {
						window.dismiss();
					}
					if (select_Rlyt.getVisibility() == View.VISIBLE) {
						updateViews();
						return true;
					}

				}

				return false;
			}

		});
	}

	private void updateViews() {
		// Animation animation = AnimationUtils.loadAnimation(getActivity(),
		// R.anim.out_to_right);
		select_Rlyt.setVisibility(View.GONE);
		// select_Rlyt.startAnimation(animation);
		isSelectMode = false;
		mAdapter.setSelectMode(false);
		mAdapter.notifyDataSetChanged();
		mAdapter.selectAllItem(false);
		action_sure.setText(getString(R.string.all_select));
		action_back_Rlyt.setVisibility(View.VISIBLE);
	}

	/**
	 * 更新数据在需要的时候
	 */
	private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mListView != null) {
				mAdapter.notifyDataSetChanged();
			}
		}
	};

	@Override
	public void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ApolloService.META_CHANGED);
		filter.addAction(ApolloService.PLAYSTATE_CHANGED);
		getActivity().registerReceiver(mMediaStatusReceiver, filter);
	}

	@Override
	public void onStop() {
		getActivity().unregisterReceiver(mMediaStatusReceiver);
		super.onStop();
	}

	@Override
	public void countNum(int count) {

	}

	@Override
	public void processAction(String action) {
		//prize-public-bug:13800 null pointer-exception-20160328-pengcancan-start
		if (mAdapter==null) {
			return;
		}
		//prize-public-bug:13800 null pointer-exception-20160328-pengcancan-end
		final long[] selectIds = mAdapter.getSelectedAudioIds();
		if (selectIds == null || selectIds.length <= 0) {
			if (getActivity() == null) {
				return;
			}
			ToastUtils.showOnceToast(getActivity(),
					getString(R.string.pl_select_item));
			return;
		}
		if (Constants.ACTION_BELL == action) {
			MusicUtils.setRingtone(getActivity(),
					mAdapter.getSelectedAudioIds()[0]);
			updateViews();
			// 通知取消刷MainActivity新界面
			mIfragToActivity.processAction(Constants.ACTION_CANCE);
		} else if (Constants.ACTION_SORT.equals(action)) {
			/*
			 * handler.post(new Runnable() {
			 * 
			 * @Override public void run() {
			 * MusicUtils.addToaddToFavorites(getActivity(), selectIds);
			 * 
			 * } }); updateViews(); // 通知取消刷MainActivity新界面
			 * mIfragToActivity.processAction(Constants.ACTION_CANCE);
			 */
			new AsyncLoader_GuessInfo(action, selectIds).execute();
		} else if (Constants.ACTION_DELETE.equals(action)) {
			df = com.prize.music.ui.fragments.base.PromptDialogFragment
					.newInstance("从列表中删除"
							+ mAdapter.getSelectedAudioIds().length + "首歌",
							mDeletePromptListener);
			df.show(getActivity().getSupportFragmentManager(), "deleteDialog");

		} else if (Constants.ACTION_ADD.equals(action)) {
			// initAddPopu();''
			initAddPopu();
		}
	}

	private void deleteMusicFromList(long[] selectIds) {
		int len = selectIds.length;
		for (int i = 0; i < len; i++) {
			HistoryDao.getInstance(getActivity()).deleteByAudioId(selectIds[i]);
		}
		// refresh();
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

	private View.OnClickListener mDeletePromptListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			// deleteMusicFromList(mAdapter.getSelectedAudioIds());
			new AsyncLoader_GuessInfo(Constants.ACTION_DELETE, null).execute();
			df.dismissAllowingStateLoss();
			updateViews();
			// 通知取消刷MainActivity新界面
			mIfragToActivity.processAction(Constants.ACTION_CANCE);

		}
	};
	private PopupWindow rightPopupWindow;
	private ArrayList<PopBean> areaDatas = new ArrayList<PopBean>();

	/**
	 * @Description:[popwindow的handler]
	 * @return
	 */
	private Handler getPopWindowHandler() {
		return new Handler() {
			@Override
			public void handleMessage(Message msg) {
				Bundle data = msg.getData();
				if (msg.what == 1) {
					long playlistid = data.getLong("selIndex");
					boolean isExisted = MusicUtils.addTrackToPlaylist(
							getActivity(), playlistid,
							mAdapter.getSelectedAudioIds());
					if (isExisted) {
						ToastUtils.showOnceToast(getActivity(),
								getString(R.string.Song_has_been));
					} else {
						ToastUtils.showOnceToast(getActivity(),
								getString(R.string.addSuccessful));

					}
				}
				window.dismiss();
				updateViews();
				// 通知取消刷MainActivity新界面
				mIfragToActivity.processAction(Constants.ACTION_CANCE);
			}
		};
	}

	protected TextView main_mEdit_add;

	protected TextView popu_cancle;

	private PopupWindow window;

	private void initAddPopu() {

		if (areaDatas != null) {
			areaDatas.clear();

		}
		if (getActivity() == null) {
			return;
		}
		List<Map<String, Object>> mListss = MusicUtils
				.getTableList(getActivity());
		// 初始化弹出菜单
		int len = mListss.size();
		// if (len <= 0) {
		for (int i = 0; i < len; i++) {
			if (!mListss.get(i).get("name")
					.equals(getActivity().getString(R.string.create_list))) {
				long id = (Long) mListss.get(i).get("id");
				String name = (String) mListss.get(i).get("name");
				PopBean mPopBean = new PopBean(id + "", name);
				areaDatas.add(mPopBean);
			}
		}
		// 初始化弹出菜单
		View popupView = LayoutInflater.from(getActivity()).inflate(
				R.layout.popupwindow_add_list, null);

		LinearLayout popu_add_linearlayout = (LinearLayout) popupView
				.findViewById(R.id.popu_add_linearlayout);
		popu_cancle = (TextView) popupView.findViewById(R.id.popu_cancle);
		if (window == null) {
			window = new PopupWindow(popupView,
					WindowManager.LayoutParams.MATCH_PARENT,
					WindowManager.LayoutParams.WRAP_CONTENT);
		}

		// 设置菜单背景，不设置背景菜单不会显示
		window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		// 菜单外点击菜单自动消失
		window.setOutsideTouchable(true);
		// 初始化菜单上的按键，并设置监听
		ListView li = (ListView) popupView.findViewById(R.id.popul_list);
		MainPopAdapter pAdapter = new MainPopAdapter(getActivity(),
				getPopWindowHandler(), areaDatas);
		if (pAdapter != null && pAdapter.getCount() > 5) {
			LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) popu_add_linearlayout
					.getLayoutParams(); // 取控件mGrid当前的布局参数
			linearParams.height = 600;// 当控件的高强制设成600象素
			popu_add_linearlayout.setLayoutParams(linearParams);
		}

		li.setAdapter(pAdapter);
		window.setAnimationStyle(R.style.mypopwindow_anim_style);
		window.showAtLocation(action_cancel, Gravity.BOTTOM, 0, 0);
		popu_cancle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				window.dismiss();

			}
		});
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), mUri, mProjection, mWhere, null,
				mSortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Check for database errors
		if (data == null) {
			return;
		}
		if (mCursor != null)
			mCursor.close();
		mAdapter.changeCursor(data);
		mListView.invalidateViews();
		mCursor = data;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (mAdapter != null)
			mAdapter.changeCursor(null);
	}

	class AsyncLoader_GuessInfo extends AsyncTask<Void, Void, Boolean> {
		String action = null;
		ProgressDialog dialog = null;
		long[] ids = null;

		public AsyncLoader_GuessInfo(String action, long[] ids) {
			this.action = action;
			this.ids = ids;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			dialog = new ProgressDialog(getActivity(),
					ProgressDialog.THEME_HOLO_LIGHT);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setCancelable(true);
			if (action.equals(Constants.ACTION_ADD)) {
				dialog.setMessage(getActivity().getString(R.string.adding));
			} else if (action.equals(Constants.ACTION_SORT)) {
				dialog.setMessage(getActivity().getString(
						R.string.collectioning));
				if (mAdapter.getSelectedAudioIds().length > 100) {
					dialog.show();
				}
			} else if (action.equals(Constants.ACTION_DELETE)) {
				dialog.setMessage(getActivity().getString(R.string.deleteing));
				if (mAdapter.getSelectedAudioIds().length > 30) {
					dialog.show();
				}
			}
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			boolean isExisted = false;
			if (action.equals(Constants.ACTION_DELETE)) {
				deleteMusicFromList(mAdapter.getSelectedAudioIds());
				return true;
			}

			if (action.equals(Constants.ACTION_SORT)) {
				// MusicUtils.addToaddToFavorites(getActivity(), ids);
				long new_id = MusicUtils.getFavoritesId(getActivity());
				isExisted = MusicUtils.addTrackToPlaylist(getActivity(),
						new_id, ids);
				if (dialog.isShowing()) {
					SystemClock.sleep(500);
				}
				return isExisted;
			}
			return false;
		}

		@Override
		// 处理界面
		protected void onPostExecute(Boolean result) {
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			// updateCoorentViews();
			updateViews();
			mIfragToActivity.processAction(Constants.ACTION_CANCE);
			if (action.equals(Constants.ACTION_ADD)) {
				ToastUtils.showOnceToast(getActivity(),
						getString(R.string.addSuccessful));
			} else if (action.equals(Constants.ACTION_SORT)) {
				if (result) {
					ToastUtils.showOnceToast(getActivity(), getActivity()
							.getString(R.string.Song_has_been));
				} else {
					ToastUtils.showOnceToast(getActivity(), getActivity()
							.getString(R.string.collectionSuccessful));
				}

			} else if (action.equals(Constants.ACTION_DELETE)) {
				if (result) {
					ToastUtils.showOnceToast(getActivity(), getActivity()
							.getString(R.string.deleteSuccessful));
					refresh();
				} else {
					ToastUtils.showOnceToast(getActivity(), getActivity()
							.getString(R.string.deleteFail));
				}
			}
		}

	}
}