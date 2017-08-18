package vital.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.vitaliisavenchuk.swipelayout.SwipeLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

	SwipeLayout layout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		layout = (SwipeLayout) findViewById(R.id.swipe);
		findViewById(R.id.next_style).setOnClickListener(this);

		layout.addPosition(new SwipeLayout.StopPosition(SwipeLayout.SwipeToPosition.TO_END_OF, R.id.first));
		layout.addPosition(new SwipeLayout.StopPosition(SwipeLayout.SwipeToPosition.TO_BOTTOM));
		layout.addPosition(new SwipeLayout.StopPosition(SwipeLayout.SwipeToPosition.TO_TOP));

	}

	@Override
	public void onClick(View v) {
		switch (layout.getMode()) {
			case SWIPE:
				layout.setMode(SwipeLayout.LayoutMode.BUTTON);
				break;
			case BUTTON:
				layout.setMode(SwipeLayout.LayoutMode.FIXED);
				break;
			case FIXED:
				layout.setMode(SwipeLayout.LayoutMode.SWIPE);
				break;
		}
	}
}
