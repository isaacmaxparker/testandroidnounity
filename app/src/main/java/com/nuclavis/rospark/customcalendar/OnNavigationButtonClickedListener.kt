package com.nuclavis.rospark.customcalendar

import java.util.Calendar

/**
 * Interface definition for a callback to be invoked when a month navigation button is clicked
 *
 * @author Naishadh Parmar
 * @version 1.0
 * @since 2017-07-14
 */
interface OnNavigationButtonClickedListener {
    /**
     * Called when a month navigation button is called
     * @param whichButton Either `CustomCalendar.PREVIOUS` or `CustomCalendar.NEXT`
     * @param newMonth Calendar representation of the month that will be displayed next (including the day of month that will be selected)
     * @return For the new month, an array such that the first element is a map linking date to its description (This description will be accessible from the `desc` parameter of the onDateSelected method of OnDateSelectedListener) and the second element is a map linking date to the tag to be set on its date view (This tag will be accessible from the `view` parameter of the onDateSelected method of the OnDateSelectedListener)
     */
    fun onNavigationButtonClicked(whichButton: Int, newMonth: Calendar?): Array<Map<Int?, Any?>?>?
}