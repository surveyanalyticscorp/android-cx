package com.qpcx.retailapp.view.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import androidx.fragment.app.Fragment;
import androidx.core.view.GravityCompat;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.qpcx.retailapp.R;
import com.qpcx.retailapp.domain.api.ProductCategoryLoaderTask;
import com.qpcx.retailapp.view.activities.ECartHomeActivity;
import com.qpcx.retailapp.view.activities.ShoppingCartActivity;
import com.questionpro.cxlib.QuestionProCX;
//import com.questionpro.cxlib.model.Type;
import com.twotoasters.jazzylistview.recyclerview.JazzyRecyclerViewScrollListener;

@SuppressLint("ResourceAsColor")
public class HomeFragment extends Fragment {
	CollapsingToolbarLayout collapsingToolbar;
	RecyclerView recyclerView;
	int mutedColor = R.attr.colorPrimary;
	//CategoryListAdapter simpleRecyclerAdapter;

	/** The double back to exit pressed once. */
	private boolean doubleBackToExitPressedOnce;

	/** The m handler. */
	private Handler mHandler = new Handler();

	/** The m runnable. */
	private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			doubleBackToExitPressedOnce = false;
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		/*TouchPoint touchPoint = new TouchPoint.Builder(8092755,"datta.kunde@questionpro.com")
				.firstName("QuestionPro")
				.lastName("Qp")
				.segmentCode("S1")
				.showAsDialog(true)
				.build();
		QuestionProCX.getInstance().init((AppCompatActivity)getActivity(), touchPoint);*/

		View view = inflater.inflate(R.layout.frag_product_category, container, false);
		view.findViewById(R.id.search_item).setVisibility(View.GONE);
		view.findViewById(R.id.search_item).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						/*TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US)
								.showAsDialog(true)
								.build();
						QuestionProCX.getInstance().init(getActivity(), touchPoint);*/
						//QuestionProCX.getInstance().launchFeedbackSurvey(12844942);
					}
				});

		view.findViewById(R.id.launch_survey).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*TouchPoint touchPoint = new TouchPoint.Builder(Type.CUSTOMER_EXPERIENCE, DataCenter.US)
						.showAsDialog(false)
								.build();
				QuestionProCX.init(getActivity(), touchPoint);
				QuestionProCX.launchFeedbackSurvey(11543913);*/

				Intent intent=new Intent(getActivity(), ShoppingCartActivity.class);
				startActivity(intent);
			}
		});

		final Toolbar toolbar = (Toolbar) view.findViewById(R.id.anim_toolbar);
		((ECartHomeActivity) getActivity()).setSupportActionBar(toolbar);
		((ECartHomeActivity) getActivity()).getSupportActionBar()
				.setDisplayHomeAsUpEnabled(true);

		toolbar.setNavigationIcon(R.drawable.ic_drawer);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((ECartHomeActivity) getActivity()).getmDrawerLayout()
						.openDrawer(GravityCompat.START);
			}
		});

		collapsingToolbar = (CollapsingToolbarLayout) view
				.findViewById(R.id.collapsing_toolbar);

		collapsingToolbar.setTitle("Categories");

		ImageView header = (ImageView) view.findViewById(R.id.header);

		Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
				R.drawable.header);

		Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
			@SuppressWarnings("ResourceType")
			@Override
			public void onGenerated(Palette palette) {

				mutedColor = palette.getMutedColor(R.color.primary_500);
				collapsingToolbar.setContentScrimColor(mutedColor);
				collapsingToolbar.setStatusBarScrimColor(R.color.black_trans80);
			}
		});

		recyclerView = (RecyclerView) view.findViewById(R.id.scrollableview);

		recyclerView.setHasFixedSize(true);
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(
				getActivity());
		recyclerView.setLayoutManager(linearLayoutManager);

		JazzyRecyclerViewScrollListener jazzyScrollListener = new JazzyRecyclerViewScrollListener();
		recyclerView.setOnScrollListener(jazzyScrollListener);

		jazzyScrollListener.setTransitionEffect(4);
		
		
		new ProductCategoryLoaderTask(recyclerView,getActivity()).execute();
		
//
//		if (simpleRecyclerAdapter == null) {
//			simpleRecyclerAdapter = new CategoryListAdapter(getActivity());
//			recyclerView.setAdapter(simpleRecyclerAdapter);
//
//			simpleRecyclerAdapter
//					.SetOnItemClickListener(new OnItemClickListener() {
//
//						@Override
//						public void onItemClick(View view, int position) {
//
//							if (position == 0) {
//								GlobaDataHolder.getGlobaDataHolder()
//										.getAllElectronics();
//							} else if (position == 1) {
//								GlobaDataHolder.getGlobaDataHolder()
//										.getAllFurnitures();
//							}
//							Utils.switchFragmentWithAnimation(
//									R.id.frag_container,
//									new ProductOverviewFragment(),
//									((ECartHomeActivity) getActivity()), null,
//									AnimationType.SLIDE_LEFT);
//
//						}
//					});
//		}

		view.setFocusableInTouchMode(true);
		view.requestFocus();
		view.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

				if (event.getAction() == KeyEvent.ACTION_UP
						&& keyCode == KeyEvent.KEYCODE_BACK) {

					if (doubleBackToExitPressedOnce) {
						// super.onBackPressed();

						if (mHandler != null) {
							mHandler.removeCallbacks(mRunnable);
						}

						getActivity().finish();

						return true;
					}

					doubleBackToExitPressedOnce = true;
					Toast.makeText(getActivity(),
							"Please click BACK again to exit",
							Toast.LENGTH_SHORT).show();

					mHandler.postDelayed(mRunnable, 2000);

				}
				return true;
			}
		});

		return view;

	}

}
