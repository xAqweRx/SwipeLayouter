package com.xaqwerx.swipelayout;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class SwipeLayout extends RelativeLayout implements View.OnTouchListener {

	private static final String STATE_BUNDLE_KEY = "STATE_BUNDLE_KEY";
	private static final String STATE_POSITIONS_KEY = "STATE_POSITIONS_KEY";
	private static final String STATE_CLOSEST_KEY = "STATE_CLOSEST_KEY";


	/**
	 * BUTTON or SWIPE mode ( by ordinal value )
	 */
	private Integer mMode;

	/**
	 * styling for button mode
	 */
	private Direction mDirection = Direction.DOWN;
	private Integer mButtonTextMore;
	private Integer mButtonTextLess;
	private Integer mButtonStyle;

	/**
	 * styling for swipe mode
	 */
	private Integer mActionButtonSrc;
	private Integer mActionButtonHeight;

	private final Integer DEFAULT_SRC = R.drawable.line;
	private final Integer DEFAULT_SIZE_DP = 20;

	/**
	 * value for calculating closest position to swipe to
	 */
	private Float prevYVal;

	/**
	 * values for checking click event. If in BUTTON mode -> this helps with events;
	 */
	private Float downYVal;
	private Float downXVal;
	private boolean isClick;

	/**
	 * values of container min and max values
	 */
	private Float minHeight;
	private Float maxHeight;

	/**
	 * views of action container and content container
	 */
	private View actionContainer;
	private RelativeLayout contentContainer;

	/**
	 * positions where to swipe
	 */
	private ArrayList<StopPosition> positions = new ArrayList<>();
	private StopPosition currentClosestPosition;

	/**
	 * event interceptors helpers
	 */
	boolean canBeSwipeProcessed = false;
	boolean canBeIntercepted = false;

	public SwipeLayout(Context context) {
		super(context);
		init(null);

	}

	public SwipeLayout(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	public SwipeLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SwipeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(attrs);
	}

	private void init(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = this.getContext().getTheme().obtainStyledAttributes(
					attrs,
					R.styleable.SwipeLayout,
					0, 0);

			try {
				mMode = a.getInteger(R.styleable.SwipeLayout_mode, 0);
				if (isSwipe()) {
					mActionButtonSrc = a.getResourceId(R.styleable.SwipeLayout_swActionButtonSrc, DEFAULT_SRC);
					mActionButtonHeight = a.getDimensionPixelSize(R.styleable.SwipeLayout_swActionButtonHeight, getPixelSize(DEFAULT_SIZE_DP));
				}
				else {
					mButtonTextMore = a.getResourceId(R.styleable.SwipeLayout_swButtonTextMore, R.string.show_more);
					mButtonTextLess = a.getResourceId(R.styleable.SwipeLayout_swButtonTextLess, R.string.show_less);
					mButtonStyle = a.getResourceId(R.styleable.SwipeLayout_swButtonStyle, R.style.swButtonStyle);
				}

			} finally {
				a.recycle();
			}
		}


		/* scroll container */
		contentContainer = new RelativeLayout(this.getContext()){
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				return super.onTouchEvent(event);
			}
		};
		contentContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		contentContainer.setOnTouchListener(this);
		contentContainer.setId(View.generateViewId());
		this.addView(contentContainer);

		this.post(new Runnable() {
			@Override
			public void run() {

				calculateMaxHeight();

				int childCount = SwipeLayout.this.getChildCount();
				int stepsDone = 0;
				int index = 0;
				do {
					View child = SwipeLayout.this.getChildAt(index);
					if (child.getId() != actionContainer.getId() && child.getId() != contentContainer.getId()) {
						SwipeLayout.this.removeView(child);
						contentContainer.addView(child);
					}
					else {
						index++;
					}
					stepsDone++;
				} while (stepsDone < childCount);
			}
		});

		addActionContainer();
		this.setOnTouchListener(this);
		this.requestDisallowInterceptTouchEvent(true);
		if (this.getId() == NO_ID) {
			this.setId(View.generateViewId());
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle(new Bundle());
		bundle.putParcelable(STATE_BUNDLE_KEY, super.onSaveInstanceState());
		bundle.putSerializable(STATE_POSITIONS_KEY, positions);
		bundle.putSerializable(STATE_CLOSEST_KEY, currentClosestPosition);
		return bundle;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			this.positions = (ArrayList<StopPosition>) bundle.getSerializable(STATE_POSITIONS_KEY);
			super.onRestoreInstanceState(bundle.getParcelable(STATE_BUNDLE_KEY));
			return;
		}

		super.onRestoreInstanceState(state);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (isFixed())
			return false;

		int action = event.getAction();
		Log.d(SwipeLayout.class.getName(), " Action is  => " + event.getAction() + " view is " + v.getId());

		switch (action) {
			// finger down event
			case (MotionEvent.ACTION_DOWN):

				if (v.getId() == actionContainer.getId() && isSwipe()) {
					canBeSwipeProcessed = true;
					canBeIntercepted = true;
				}

				downYVal = event.getY();
				downXVal = event.getX();

				isClick = true;
				return true;
			// finger up event
			case (MotionEvent.ACTION_UP):
				if (positions.size() > 0 && ((isClick && isButton()) || (isSwipe() && canBeSwipeProcessed))) {
					float value = (v.getId() == this.getId() ? 0 : v.getTop()) + event.getY();
					StopPosition closestPosition = calculateClosest(value);
					closestPosition = calculateNextDirection(closestPosition);
					currentClosestPosition = closestPosition;
					animate(this.getHeight(), closestPosition.getHeight(value));
				}

				canBeSwipeProcessed = false;
				canBeIntercepted = false;
				prevYVal = null;

				return true;

			// when swiping is going on
			case (MotionEvent.ACTION_MOVE):
				if (
						(downYVal != null && Math.abs(downYVal - event.getY()) > 50) ||
								(downXVal != null && Math.abs(downXVal - event.getX()) > 50)
						) {
					isClick = false;
				}

				if (v.getId() == this.getId() && canBeSwipeProcessed && isSwipe()) {
					ViewGroup.LayoutParams params = this.getLayoutParams();

					if (params.height == ViewGroup.LayoutParams.MATCH_PARENT || params.height == ViewGroup.LayoutParams.WRAP_CONTENT)
						params.height = this.getMeasuredHeight();

					prevYVal = prevYVal == null ? event.getY() : prevYVal;

					params.height += event.getY() - prevYVal;

					if (params.height < minHeight) {
						params.height = minHeight.intValue();
						contentContainer.setVisibility(GONE);
					}
					else if (params.height > maxHeight) {
						params.height = maxHeight.intValue();
					}
					else {
						if (contentContainer.getVisibility() == GONE)
							contentContainer.setVisibility(VISIBLE);
					}

					prevYVal = event.getY();
					this.setLayoutParams(params);
				}
				return true;
		}
		return false;
	}

	/**
	 * intercepting events of swiping, if down was on Image
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		boolean res = (ev.getAction() == MotionEvent.ACTION_MOVE) && canBeIntercepted;
		Log.d(SwipeLayout.class.getName(), "IS intercepted " + res + " canBeIntercepted " + canBeIntercepted + " MotionEvent => " + ev.getAction() );
		return !isButton() && res;
	}

	/*-------------------------*/
	/*     PUBLIC METHODS      */
	/*-------------------------*/

	/**
	 * Data method, for adding "stop" position
	 * @param position {@link StopPosition} wher layout will
	 */
	public void addPosition(StopPosition position) {
		positions.add(position);
	}

	public void addPositions(ArrayList<StopPosition> position) {
		positions.addAll(position);
	}

	public void clearPositions() {
		positions.clear();
	}

	public LayoutMode getMode() {
		return LayoutMode.values()[mMode];
	}

	public void setMode(LayoutMode mMode) {
		setMode(mMode, null);
	}

	public void setMode(LayoutMode mMode, Direction direction) {
		this.mMode = mMode.ordinal();

		if (direction != null)
			this.mDirection = direction;

		removeActionContainer();
		addActionContainer();
	}

	public View getActionContainer() {
		return actionContainer;
	}

	public RelativeLayout getContentContainer() {
		return contentContainer;
	}

	public Direction getDirection() {
		return mDirection;
	}

	public void setDirection(Direction mDirection) {
		this.mDirection = mDirection;
		removeActionContainer();
		addActionContainer();
	}

	public void swipeTo(final StopPosition position) {
		this.post(new Runnable() {
			@Override
			public void run() {
				position.calculateDistance(SwipeLayout.this, 0, calculateMaxHeight(), minHeight);
				animate(SwipeLayout.this.getMeasuredHeight(), position.getHeight(0));
			}
		});
	}

	/*-------------------------*/
	/*     IS METHODS          */
	/*-------------------------*/

	public boolean isSwipe() {
		return mMode == LayoutMode.SWIPE.ordinal();
	}

	public boolean isButton() {
		return mMode == LayoutMode.BUTTON.ordinal();
	}

	public boolean isFixed() {
		return mMode == LayoutMode.FIXED.ordinal();
	}

	public Boolean isDown() {
		if (isButton()) {
			return SwipeLayout.this.mDirection == Direction.DOWN;
		}
		else {
			return null;
		}
	}

	public Boolean isUp() {
		if (isButton()) {
			return SwipeLayout.this.mDirection == Direction.UP;
		}
		else {
			return null;
		}
	}

	/*-------------------------*/
	/*     PRIVATE METHODS     */
	/*-------------------------*/

	private void animate(float fromVal, float toVal) {
		ValueAnimator animation = ValueAnimator.ofFloat(fromVal, toVal);
		animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator updatedAnimation) {
				float animatedValue = (float) updatedAnimation.getAnimatedValue();
				ViewGroup.LayoutParams params = SwipeLayout.this.getLayoutParams();
				if (animatedValue < minHeight) {
					params.height = minHeight.intValue();
				}
				else if (animatedValue > maxHeight) {
					params.height = maxHeight.intValue();
				}
				else {
					params.height = (int) animatedValue;
				}
				SwipeLayout.this.setLayoutParams(params);

			}
		});
		animation.addListener(new AnimatorListener() {
			@Override
			public void onAnimationStart(Animator animation) {

			}

			@Override
			public void onAnimationEnd(Animator animation) {
				checkButtonText();
			}

			@Override
			public void onAnimationCancel(Animator animation) {

			}

			@Override
			public void onAnimationRepeat(Animator animation) {

			}
		});
		animation.start();
	}

	private StopPosition calculateClosest(float YVal) {
		StopPosition closestPosition = null;
		for (StopPosition position : positions) {
			if (closestPosition == null) {
				closestPosition = position;
				closestPosition.calculateDistance(this, YVal, maxHeight, minHeight);
			}

			position.calculateDistance(this, YVal, maxHeight, minHeight);
			if (Math.abs(closestPosition.getDistance(YVal)) > Math.abs(position.getDistance(YVal))) {
				closestPosition = position;
			}
		}

		return closestPosition;
	}

	private float calculateMaxHeight() {
		if (maxHeight == null) {
			ViewParent parent = SwipeLayout.this.getParent();
			if (parent instanceof View) {
				View parentView = (View) parent;
				maxHeight = (float) parentView.getMeasuredHeight();
				if (isButton()) {
					maxHeight = maxHeight + minHeight;
				}
			}
			else {
				maxHeight = (float) SwipeLayout.this.getRootView().getHeight();
			}
		}
		return maxHeight;
	}

	private StopPosition getNextDownPosition(StopPosition currentPosition) {
		StopPosition nextPosition = null;
		int nextDistanceVal = Integer.MAX_VALUE;
		for (StopPosition position : this.positions) {
			int distance = position.getDistanceFromTop() - currentPosition.getDistanceFromTop();
			if (distance <= 0)
				continue;

			if (nextPosition == null) {
				nextPosition = position;
				nextDistanceVal = nextPosition.getDistanceFromTop() - currentPosition.getDistanceFromTop();
				continue;
			}

			if (distance < nextDistanceVal) {
				nextPosition = position;
				nextDistanceVal = distance;
			}
		}
		return nextPosition;
	}

	private StopPosition getNextUpPosition(StopPosition currentPosition) {
		StopPosition prevPosition = null;
		int prevDistanceVal = -1 * Integer.MAX_VALUE;
		for (StopPosition position : this.positions) {
			int distance = position.getDistanceFromTop() - currentPosition.getDistanceFromTop();
			if (distance >= 0)
				continue;

			if (prevPosition == null) {
				prevPosition = position;
				prevDistanceVal = prevPosition.getDistanceFromTop() - currentPosition.getDistanceFromTop();
				continue;
			}

			if (Math.abs(distance) < Math.abs(prevDistanceVal)) {
				prevPosition = position;
				prevDistanceVal = distance;
			}
		}
		return prevPosition;
	}

	private void removeActionContainer() {
		this.removeView(actionContainer);

		RelativeLayout.LayoutParams layoutParams = (LayoutParams) this.contentContainer.getLayoutParams();
		layoutParams.removeRule(ABOVE);

		actionContainer = null;
	}

	private void addActionContainer() {
		LayoutParams params;
		initDefaultStylingIfEmpty();

		if (!isFixed()) {
			if (isSwipe()) {
				actionContainer = new AppCompatImageView(this.getContext()) {
					@Override
					public boolean onTouchEvent(MotionEvent event) {
						return true;
					}
				};
				((AppCompatImageView) actionContainer).setImageDrawable(ContextCompat.getDrawable(getContext(), mActionButtonSrc));
				params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mActionButtonHeight);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				minHeight = Float.valueOf(mActionButtonHeight);

			}
			else {
				actionContainer = new AppCompatTextView(new ContextThemeWrapper(this.getContext(), mButtonStyle), null, 0) {
					@Override
					public boolean onTouchEvent(MotionEvent event) {
						return true;
					}
				};

				params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

				int measureSpecParams = MeasureSpec.getSize(MeasureSpec.UNSPECIFIED);
				actionContainer.measure(measureSpecParams, measureSpecParams);
				minHeight = (float) actionContainer.getMeasuredHeight();

				checkButtonText();
			}

			actionContainer.setLayoutParams(params);
			actionContainer.setId(View.generateViewId());
			actionContainer.setOnTouchListener(this);
			this.addView(actionContainer);

			params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
			params.addRule(RelativeLayout.ABOVE, actionContainer.getId());
			contentContainer.setLayoutParams(params);
		}
	}

	private int getPixelSize(int dp) {
		return (int) (dp * this.getContext().getResources().getDisplayMetrics().density);
	}

	private void initDefaultStylingIfEmpty() {
		if (isSwipe()) {
			mActionButtonSrc = mActionButtonSrc == null ? R.drawable.line : mActionButtonSrc;
			mActionButtonHeight = mActionButtonHeight == null ? getPixelSize(DEFAULT_SIZE_DP) : mActionButtonHeight;
		}
		else {
			mButtonTextMore = mButtonTextMore == null ? R.string.show_more : mButtonTextMore;
			mButtonTextLess = mButtonTextLess == null ? R.string.show_less : mButtonTextLess;
			mButtonStyle = mButtonStyle == null ? R.style.swButtonStyle : mButtonStyle;
		}
	}

	private StopPosition calculateNextDirection(StopPosition closestPosition){
		if (isButton()) {
			StopPosition nextDownPosition = getNextDownPosition(closestPosition);
			StopPosition nextUpPosition = getNextUpPosition(closestPosition);

			if (mDirection == Direction.DOWN) {
				StopPosition afterNextPosition = nextDownPosition == null ? null : getNextDownPosition(closestPosition);
				closestPosition = nextDownPosition == null ? closestPosition : nextDownPosition;
				if (afterNextPosition == null) {
					((TextView) actionContainer).setText(mButtonTextLess);
					mDirection = Direction.UP;
				}
			}
			else {
				StopPosition afterPrevPosition = nextUpPosition == null ? null : getNextUpPosition(closestPosition);
				closestPosition = nextUpPosition == null ? closestPosition : nextUpPosition;

				if (afterPrevPosition == null) {
					((TextView) actionContainer).setText(mButtonTextMore);
					mDirection = Direction.DOWN;
				}
			}
			return closestPosition;
		} else
			return closestPosition;
	}

	private void checkButtonText() {
		if (isButton()) {
			this.post(new Runnable() {
				@Override
				public void run() {
					StopPosition position = calculateClosest(SwipeLayout.this.getMeasuredHeight());
					currentClosestPosition = position;
					if (isDown() && getNextDownPosition(position) == null) {
						SwipeLayout.this.mDirection = Direction.UP;
					}
					else if (isUp() && getNextUpPosition(position) == null) {
						SwipeLayout.this.mDirection = Direction.DOWN;
					}

					((TextView) actionContainer).setText(SwipeLayout.this.mDirection == Direction.DOWN ? mButtonTextMore : mButtonTextLess);
				}
			});
		}
	}

	/*-------------------------*/
	/*     INNER CLASSES       */
	/*-------------------------*/
	public enum LayoutMode {
		SWIPE,
		BUTTON,
		FIXED
	}

	public enum SwipeToPosition {
		TO_BOTTOM, // to bottom of parent
		TO_POSITION, // px from top
		TO_END_OF, // id of layout
		TO_TOP_OF, // id of layout
		TO_TOP // collapse view
	}

	private enum Direction {
		UP,
		DOWN
	}

	public static class StopPosition implements Serializable {

		private static final long serialVersionUID = -2160313128788711565L;
		private final SwipeToPosition swipeToPosition;
		private final Integer id;
		private final Integer height;

		Integer distanceFromTop;

		private HashMap<Float, Float> cacheDistance = new HashMap<>();
		private HashMap<Float, Float> cacheHeight = new HashMap<>();

		public StopPosition(SwipeToPosition swipeToPosition, Integer value) {
			this.swipeToPosition = swipeToPosition;
			if (this.swipeToPosition == SwipeToPosition.TO_POSITION) {
				this.height = value;
				this.id = View.NO_ID;
			}
			else if (this.swipeToPosition == SwipeToPosition.TO_END_OF || this.swipeToPosition == SwipeToPosition.TO_TOP_OF) {
				this.id = value;
				this.height = 0;
			}
			else {
				throw new RuntimeException("Please use correct Constructor for this type of swipe");
			}
		}

		public StopPosition(SwipeToPosition swipeToPosition) {
			this.swipeToPosition = swipeToPosition;

			if (this.swipeToPosition != SwipeToPosition.TO_TOP && this.swipeToPosition != SwipeToPosition.TO_BOTTOM)
				throw new RuntimeException("Please use correct Constructor for this type of swipe");

			this.id = View.NO_ID;
			this.height = 0;
		}

		float  calculateDistance(View globalView, float toYValue, float maxHeight, float minHeight) {
			if (cacheDistance.get(toYValue) != null)
				return cacheDistance.get(toYValue);

			Float distance = 0f;

			switch (swipeToPosition) {
				case TO_BOTTOM:
					distance = maxHeight - toYValue;
					cacheHeight.put(toYValue, maxHeight);
					distanceFromTop = (int) (maxHeight);
					break;
				case TO_TOP:
					distance = minHeight - toYValue;
					cacheHeight.put(toYValue, minHeight);
					distanceFromTop = 0;
					break;
				case TO_POSITION:
					distance = this.height - toYValue;
					cacheHeight.put(toYValue, Float.valueOf(this.height));
					distanceFromTop = this.height;
					break;
				case TO_END_OF:
					View viewToEnd = globalView.findViewById(id);

					int measureSpecParams = View.MeasureSpec.getSize(View.MeasureSpec.UNSPECIFIED);
					viewToEnd.measure(measureSpecParams, measureSpecParams);
					int measuredHeight = viewToEnd.getMeasuredHeight();

					float topEndVal = calculateTop(globalView, viewToEnd);
					distance = topEndVal  + measuredHeight - toYValue;
					cacheHeight.put(toYValue, topEndVal  + minHeight + measuredHeight);

					distanceFromTop = (int) (topEndVal + minHeight + measuredHeight);
					break;
				case TO_TOP_OF:
					View viewToTop = globalView.findViewById(id);
					float topVal = calculateTop(globalView, viewToTop);
					distance = topVal - toYValue;
					cacheHeight.put(toYValue, topVal + minHeight);

					distanceFromTop = (int) (topVal + minHeight);
					break;
			}


			cacheDistance.put(toYValue, distance);
			return distance;
		}

		Float getDistance(float toYValue) {
			return cacheDistance.get(toYValue);
		}

		Float getHeight(float toYValue) {
			return cacheHeight.get(toYValue);
		}

		Integer getDistanceFromTop() {
			return distanceFromTop;
		}

		Float calculateTop(View viewGlobal, View viewParent) {
			Float add = 0f;
			if (viewParent != viewGlobal) {
				add = calculateTop(viewGlobal, (View) viewParent.getParent());
			}
			return viewParent.getY() + add;
		}
	}

}
