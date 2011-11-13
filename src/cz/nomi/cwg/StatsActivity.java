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
	private DatabaseAdapter db;

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		setContentView(R.layout.stats);

		db = new DatabaseAdapter(this);
		db.open();
	
		TextView countOwnCwgSum = (TextView) findViewById(R.id.stats_own_cwg_sum);
		TextView countOwnCwgSumWithCatalog = (TextView) findViewById(R.id.stats_own_cwg_sum_with_catalog);
		TextView countOwnCwgSumWithoutCatalog = (TextView) findViewById(R.id.stats_own_cwg_sum_without_catalog);
		TextView countOwnCwgSumDuplicity = (TextView) findViewById(R.id.stats_own_cwg_sum_duplicity);

		TextView countOwnCwg = (TextView) findViewById(R.id.stats_own_cwg);
		TextView countOwnCwgWithCatalog = (TextView) findViewById(R.id.stats_own_cwg_with_catalog);
		TextView countOwnCwgWithoutCatalog = (TextView) findViewById(R.id.stats_own_cwg_without_catalog);
		TextView countOwnCwgDuplicity = (TextView) findViewById(R.id.stats_own_cwg_duplicity);

		TextView countCwgCatalog = (TextView) findViewById(R.id.stats_cwg_catalog);
		
		countOwnCwgSum.setText(Integer.toString(db.countOwnCwgSum()));
		countOwnCwgSumWithCatalog.setText(Integer.toString(db.countOwnCwgSumWithCatalog()));
		countOwnCwgSumWithoutCatalog.setText(Integer.toString(db.countOwnCwgSumWithoutCatalog()));
		countOwnCwgSumDuplicity.setText(Integer.toString(db.countOwnCwgSumDuplicity()));

		countOwnCwg.setText(Integer.toString(db.countOwnCwg()));
		countOwnCwgWithCatalog.setText(Integer.toString(db.countOwnCwgWithCatalog()));
		countOwnCwgWithoutCatalog.setText(Integer.toString(db.countOwnCwgWithoutCatalog()));
		countOwnCwgDuplicity.setText(Integer.toString(db.countOwnCwgDuplicity()));
		countCwgCatalog.setText(Integer.toString(db.countCwgCatalog()));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		db.close();
	}
}
