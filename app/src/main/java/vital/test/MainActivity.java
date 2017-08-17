package vital.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.vitaliisavenchuk.swipelayout.SwipeLayout;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SwipeLayout layout = (SwipeLayout) findViewById(R.id.swipe);

		layout.addPosition(new SwipeLayout.SwipePosition(SwipeLayout.SwipeToPosition.TO_END_OF, R.id.first));
		layout.addPosition(new SwipeLayout.SwipePosition(SwipeLayout.SwipeToPosition.TO_BOTOM));
		layout.addPosition(new SwipeLayout.SwipePosition(SwipeLayout.SwipeToPosition.TO_TOP));

	}
}
