package com.nuclavis.rospark.customcalendar

import android.view.View
import java.util.Calendar

/**
 * Interface definition for a callback to be invoked when a date view is clicked
 *
 * @author Naishadh Parmar
 * @version 1.0
 * @since 2017-07-14
 */
interface OnDateSelectedListener {
    /**
     * Called when a date is selected
     * @param view The date view that was clicked (the tag on this view will be as given in the map linking date to the tag)
     * @param selectedDate Calendar representation of the selected date
     * @param desc Description of the date (as given in the map linking date to its description)
     */
    fun onDateSelected(view: View?, selectedDate: Calendar?, desc: Any?)
}