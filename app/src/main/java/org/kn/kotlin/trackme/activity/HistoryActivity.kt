package org.kn.kotlin.trackme.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import org.kn.kotlin.trackme.R
import org.kn.kotlin.trackme.adapter.HistoryAdapter
import org.kn.kotlin.trackme.utils.RealmController
import io.realm.Realm

abstract class HistoryActivity : AppCompatActivity() {

    internal var mRecyclerView: RecyclerView? = null

    lateinit var realm: Realm

    private var mListAdapter: HistoryAdapter? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        mRecyclerView = findViewById(R.id.card_list) as RecyclerView
        this.realm = RealmController.with(this).realm

        // Determine the number of columns to display, based on screen width.
        val rows = resources.getInteger(R.integer.map_grid_cols)
        val layoutManager = GridLayoutManager(this, rows, GridLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = layoutManager

        // Delay attaching Adapter to RecyclerView until we can ensure that we have correct
        // Google Play service version (in onResume).
    }

    protected abstract fun createMapListAdapter(): HistoryAdapter

    override fun onLowMemory() {
        super.onLowMemory()

        if (mListAdapter != null) {
            for (m in mListAdapter!!.mapViews) {
                m.onLowMemory()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (mListAdapter != null) {
            for (m in mListAdapter!!.mapViews) {
                m.onPause()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mListAdapter = createMapListAdapter()
        val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (resultCode == ConnectionResult.SUCCESS) {
            mRecyclerView!!.adapter = mListAdapter
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 1).show()
        }

        if (mListAdapter != null) {
            for (m in mListAdapter!!.mapViews) {
                m.onResume()
            }
        }
    }

    override fun onDestroy() {
        if (mListAdapter != null) {
            for (m in mListAdapter!!.mapViews) {
                m.onDestroy()
            }
        }

        super.onDestroy()
    }

    /**
     * Show a full mapView when a mapView card is selected. This method is attached to each CardView
     * displayed within this activity's RecyclerView.
     *
     * @param view The view (CardView) that was clicked.
     */
    abstract fun showNewRecord(view: View)
}
