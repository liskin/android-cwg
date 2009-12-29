/*
	This file is part of Android-CWG.

	Android-CWG is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	Android-CWG is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with Android-CWG.  If not, see <http://www.gnu.org/licenses/>.
*/

package cz.nomi.cwg;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class StatsActivity extends Activity {
	DatabaseAdapter db;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		setContentView(R.layout.stats);

		db = new DatabaseAdapter(this);
		db.open();

		TextView cwgCount = (TextView) findViewById(R.id.stats_cwg_count);
		TextView cwgUnique = (TextView) findViewById(R.id.stats_cwg_unique);
		TextView cwgDuplicity = (TextView) findViewById(R.id.stats_cwg_duplicity);

		cwgCount.setText(Integer.toString(db.countCwgSumCount()));
		cwgUnique.setText(Integer.toString(db.countCwg()));
		cwgDuplicity.setText(Integer.toString(db.countCwgDuplicity()));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		db.close();
	}
}
