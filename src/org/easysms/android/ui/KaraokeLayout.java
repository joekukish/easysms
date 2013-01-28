package org.easysms.android.ui;

import java.util.StringTokenizer;

import org.easysms.android.R;
import org.easysms.android.util.TextToSpeechManager;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class KaraokeLayout extends ViewGroup {

	private int timesKaraoke = 0;
	private Handler handler;

	public void playKaraoke(final KaraokeLayout fl) {
		timesKaraoke++;

		if (timesKaraoke <= 1) {

			// Do something long
			Runnable runnable = new Runnable() {
				@Override
				public void run() {

					for (int i = 1; i < fl.getChildCount(); ++i) {
						final Button btn = (Button) fl.getChildAt(i);
						// wholesentenceplayed += btn.getText();
						btn.setFocusableInTouchMode(true);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								// progress.setProgress(value);
								btn.requestFocus();
								// drop all pending entries in the playback
								// queue.

								// plays the audio.
								TextToSpeechManager.getInstance().say(
										(String) btn.getText());
							}
						});

					}
					timesKaraoke = 0;

				}
			};
			new Thread(runnable).start();

			String wholesentenceplayed = "";
			for (int i = 1; i < fl.getChildCount(); ++i) {
				final Button btn = (Button) fl.getChildAt(i);
				wholesentenceplayed += btn.getText();
			}
		}
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		private static int NO_SPACING = -1;
		private int horizontalSpacing = NO_SPACING;
		private boolean newLine = false;
		private int verticalSpacing = NO_SPACING;
		private int x;
		private int y;

		public LayoutParams(Context context, AttributeSet attributeSet) {
			super(context, attributeSet);
			this.readStyleParameters(context, attributeSet);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(ViewGroup.LayoutParams layoutParams) {
			super(layoutParams);
		}

		public boolean horizontalSpacingSpecified() {
			return horizontalSpacing != NO_SPACING;
		}

		private void readStyleParameters(Context context,
				AttributeSet attributeSet) {
			TypedArray a = context.obtainStyledAttributes(attributeSet,
					R.styleable.FlowLayout_LayoutParams);
			try {
				horizontalSpacing = a
						.getDimensionPixelSize(
								R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing,
								NO_SPACING);
				verticalSpacing = a
						.getDimensionPixelSize(
								R.styleable.FlowLayout_LayoutParams_layout_verticalSpacing,
								NO_SPACING);
				newLine = a.getBoolean(
						R.styleable.FlowLayout_LayoutParams_layout_newLine,
						false);
			} finally {
				a.recycle();
			}
		}

		public void setPosition(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public boolean verticalSpacingSpecified() {
			return verticalSpacing != NO_SPACING;
		}
	}

	public static final int HORIZONTAL = 0;

	public static final int VERTICAL = 1;
	private boolean debugDraw = false;
	private int horizontalSpacing = 0;
	private int orientation = 0;
	private int verticalSpacing = 0;

	public KaraokeLayout(Context context) {
		super(context);
		this.readStyleParameters(context, null);

		addPlayButton();
	}

	public KaraokeLayout(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		this.readStyleParameters(context, attributeSet);

		addPlayButton();
	}

	public KaraokeLayout(Context context, AttributeSet attributeSet,
			int defStyle) {
		super(context, attributeSet, defStyle);

		this.readStyleParameters(context, attributeSet);

		addPlayButton();
	}

	private ImageView mPlayButton;

	private void addPlayButton() {
		// creates the button used to play the text
		mPlayButton = new ImageView(this.getContext());
		mPlayButton.setBackgroundResource(R.drawable.playsmsclick);
		mPlayButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// playKaraoke(instructionsBubble);
			}
		});

		// adds the button.
		addView(mPlayButton);
	}

	public void addText(String text) {
		// parse the sentence into words and put it into an array of words
		StringTokenizer st = new StringTokenizer(text);

		String[] tabWords = new String[100];
		int nbWords = 0;
		while (st.hasMoreElements()) {
			tabWords[nbWords] = (String) st.nextElement();
			nbWords++;
		}

		// create a button for each words and append it to the
		// bubble composition
		for (int i = 0; i < nbWords; ++i) {
			final Button btn = new Button(getContext());
			btn.setText(tabWords[i]);

			// btn.setTextSize( (int)
			// TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
			// 10, getResources().getDisplayMetrics()));
			btn.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));
			btn.setTextColor(getResources().getColor(android.R.color.black));

			// before being clicked the button is grey
			btn.setBackgroundResource(R.drawable.button);

			final String toSay = tabWords[i];

			// play each button
			btn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// plays the audio.
					TextToSpeechManager.getInstance().say(toSay);
				}
			});

			// on long click, delete the button
			btn.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					KaraokeLayout.this.removeView(btn);
					return true;
				}
			});

			// adds the button to the container.
			addView(btn, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

		}
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	private Paint createPaint(int color) {
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(color);
		paint.setStrokeWidth(2.0f);
		return paint;
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		boolean more = super.drawChild(canvas, child, drawingTime);
		this.drawDebugInfo(canvas, child);
		return more;
	}

	private void drawDebugInfo(Canvas canvas, View child) {
		if (!debugDraw) {
			return;
		}

		Paint childPaint = this.createPaint(0xffffff00);
		Paint layoutPaint = this.createPaint(0xff00ff00);
		Paint newLinePaint = this.createPaint(0xffff0000);

		LayoutParams lp = (LayoutParams) child.getLayoutParams();

		if (lp.horizontalSpacing > 0) {
			float x = child.getRight();
			float y = child.getTop() + child.getHeight() / 2.0f;
			canvas.drawLine(x, y, x + lp.horizontalSpacing, y, childPaint);
			canvas.drawLine(x + lp.horizontalSpacing - 4.0f, y - 4.0f, x
					+ lp.horizontalSpacing, y, childPaint);
			canvas.drawLine(x + lp.horizontalSpacing - 4.0f, y + 4.0f, x
					+ lp.horizontalSpacing, y, childPaint);
		} else if (this.horizontalSpacing > 0) {
			float x = child.getRight();
			float y = child.getTop() + child.getHeight() / 2.0f;
			canvas.drawLine(x, y, x + this.horizontalSpacing, y, layoutPaint);
			canvas.drawLine(x + this.horizontalSpacing - 4.0f, y - 4.0f, x
					+ this.horizontalSpacing, y, layoutPaint);
			canvas.drawLine(x + this.horizontalSpacing - 4.0f, y + 4.0f, x
					+ this.horizontalSpacing, y, layoutPaint);
		}

		if (lp.verticalSpacing > 0) {
			float x = child.getLeft() + child.getWidth() / 2.0f;
			float y = child.getBottom();
			canvas.drawLine(x, y, x, y + lp.verticalSpacing, childPaint);
			canvas.drawLine(x - 4.0f, y + lp.verticalSpacing - 4.0f, x, y
					+ lp.verticalSpacing, childPaint);
			canvas.drawLine(x + 4.0f, y + lp.verticalSpacing - 4.0f, x, y
					+ lp.verticalSpacing, childPaint);
		} else if (this.verticalSpacing > 0) {
			float x = child.getLeft() + child.getWidth() / 2.0f;
			float y = child.getBottom();
			canvas.drawLine(x, y, x, y + this.verticalSpacing, layoutPaint);
			canvas.drawLine(x - 4.0f, y + this.verticalSpacing - 4.0f, x, y
					+ this.verticalSpacing, layoutPaint);
			canvas.drawLine(x + 4.0f, y + this.verticalSpacing - 4.0f, x, y
					+ this.verticalSpacing, layoutPaint);
		}

		if (lp.newLine) {
			if (orientation == HORIZONTAL) {
				float x = child.getLeft();
				float y = child.getTop() + child.getHeight() / 2.0f;
				canvas.drawLine(x, y - 6.0f, x, y + 6.0f, newLinePaint);
			} else {
				float x = child.getLeft() + child.getWidth() / 2.0f;
				float y = child.getTop();
				canvas.drawLine(x - 6.0f, y, x + 6.0f, y, newLinePaint);
			}
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
		return new LayoutParams(getContext(), attributeSet);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}

	private int getHorizontalSpacing(LayoutParams lp) {
		int hSpacing;
		if (lp.horizontalSpacingSpecified()) {
			hSpacing = lp.horizontalSpacing;
		} else {
			hSpacing = this.horizontalSpacing;
		}
		return hSpacing;
	}

	private int getVerticalSpacing(LayoutParams lp) {
		int vSpacing;
		if (lp.verticalSpacingSpecified()) {
			vSpacing = lp.verticalSpacing;
		} else {
			vSpacing = this.verticalSpacing;
		}
		return vSpacing;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), lp.y
					+ child.getMeasuredHeight());
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int sizeWidth = MeasureSpec.getSize(widthMeasureSpec)
				- this.getPaddingRight() - this.getPaddingLeft();
		int sizeHeight = MeasureSpec.getSize(heightMeasureSpec)
				- this.getPaddingRight() - this.getPaddingLeft();

		int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
		int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

		int size;
		int mode;

		if (orientation == HORIZONTAL) {
			size = sizeWidth;
			mode = modeWidth;
		} else {
			size = sizeHeight;
			mode = modeHeight;
		}

		int lineThicknessWithSpacing = 0;
		int lineThickness = 0;
		int lineLengthWithSpacing = 0;
		int lineLength;

		int prevLinePosition = 0;

		int controlMaxLength = 0;
		int controlMaxThickness = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() == GONE) {
				continue;
			}

			child.measure(MeasureSpec.makeMeasureSpec(sizeWidth,
					modeWidth == MeasureSpec.EXACTLY ? MeasureSpec.AT_MOST
							: modeWidth), MeasureSpec.makeMeasureSpec(
					sizeHeight,
					modeHeight == MeasureSpec.EXACTLY ? MeasureSpec.AT_MOST
							: modeHeight));

			LayoutParams lp = (LayoutParams) child.getLayoutParams();

			int hSpacing = this.getHorizontalSpacing(lp);
			int vSpacing = this.getVerticalSpacing(lp);

			int childWidth = child.getMeasuredWidth();
			int childHeight = child.getMeasuredHeight();

			int childLength;
			int childThickness;
			int spacingLength;
			int spacingThickness;

			if (orientation == HORIZONTAL) {
				childLength = childWidth;
				childThickness = childHeight;
				spacingLength = hSpacing;
				spacingThickness = vSpacing;
			} else {
				childLength = childHeight;
				childThickness = childWidth;
				spacingLength = vSpacing;
				spacingThickness = hSpacing;
			}

			lineLength = lineLengthWithSpacing + childLength;
			lineLengthWithSpacing = lineLength + spacingLength;

			boolean newLine = lp.newLine
					|| (mode != MeasureSpec.UNSPECIFIED && lineLength > size);
			if (newLine) {
				prevLinePosition = prevLinePosition + lineThicknessWithSpacing;

				lineThickness = childThickness;
				lineLength = childLength;
				lineThicknessWithSpacing = childThickness + spacingThickness;
				lineLengthWithSpacing = lineLength + spacingLength;
			}

			lineThicknessWithSpacing = Math.max(lineThicknessWithSpacing,
					childThickness + spacingThickness);
			lineThickness = Math.max(lineThickness, childThickness);

			int posX;
			int posY;
			if (orientation == HORIZONTAL) {
				posX = getPaddingLeft() + lineLength - childLength;
				posY = getPaddingTop() + prevLinePosition;
			} else {
				posX = getPaddingLeft() + prevLinePosition;
				posY = getPaddingTop() + lineLength - childHeight;
			}
			lp.setPosition(posX, posY);

			controlMaxLength = Math.max(controlMaxLength, lineLength);
			controlMaxThickness = prevLinePosition + lineThickness;
		}

		if (orientation == HORIZONTAL) {
			this.setMeasuredDimension(
					resolveSize(controlMaxLength, widthMeasureSpec),
					resolveSize(controlMaxThickness, heightMeasureSpec));
		} else {
			this.setMeasuredDimension(
					resolveSize(controlMaxThickness, widthMeasureSpec),
					resolveSize(controlMaxLength, heightMeasureSpec));
		}
	}

	private void readStyleParameters(Context context, AttributeSet attributeSet) {
		TypedArray a = context.obtainStyledAttributes(attributeSet,
				R.styleable.FlowLayout);
		try {
			horizontalSpacing = a.getDimensionPixelSize(
					R.styleable.FlowLayout_horizontalSpacing, 0);
			verticalSpacing = a.getDimensionPixelSize(
					R.styleable.FlowLayout_verticalSpacing, 0);
			orientation = a.getInteger(R.styleable.FlowLayout_orientation,
					HORIZONTAL);
			debugDraw = a.getBoolean(R.styleable.FlowLayout_debugDraw, false);
		} finally {
			a.recycle();
		}
	}
}