package com.vdesmet.lib.calendar.factory;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.ViewGroup;

/**
 * Factory to retrieve styles and backgrounds to use in the CalendarViews.
 */
public class DayStyleFactory {
    public static final int DAY_STYLE_TILED = 0;
    public static final int DAY_STYLE_FLAT = 1;

    public static final int DEFAULT_STYLE = DAY_STYLE_TILED;

    public static boolean isValidStyle(final int dayStyle) {
        return (dayStyle == DAY_STYLE_TILED ||
                dayStyle == DAY_STYLE_FLAT);
    }

    public static ViewGroup getDayLayoutForStyle(final LayoutInflater inflater, final ViewGroup parent,
                                                 final int dayStyle) {

        switch(dayStyle) {
            case DAY_STYLE_TILED:
                // Inflate the layout, and add the background resource
                final ViewGroup dayLayout = (ViewGroup) inflater.inflate(com.vdesmet.lib.calendar.R.layout.lib_calendar_day, parent, false);
                dayLayout.setBackgroundResource(com.vdesmet.lib.calendar.R.drawable.lib_calendar_background);
                return dayLayout;

            case DAY_STYLE_FLAT:
                // Inflate te layout, and return it
                return (ViewGroup) inflater.inflate(com.vdesmet.lib.calendar.R.layout.lib_calendar_day, parent, false);

            default:
                // Invalid style, throw exception
                throw new IllegalArgumentException("Day Style is invalid, cannot inflate day layout.");
        }
    }

    public static int getDayDisabledBackgroundColor(final int dayStyle, final Resources resources) {
        switch(dayStyle) {
            case DAY_STYLE_TILED:
                return resources.getColor(com.vdesmet.lib.calendar.R.color.lib_calendar_day_background_disabled);

            case DAY_STYLE_FLAT:
                // No custom background color in the flat style
                return 0;

            default:
                // Invalid style, throw exception
                throw new IllegalArgumentException("Day Style is invalid, cannot inflate day layout.");
        }
    }

    public static int getBackgroundResourceForStyle(final int dayStyle) {
        switch(dayStyle) {
            case DAY_STYLE_TILED:
                return com.vdesmet.lib.calendar.R.color.lib_calendar_background;
            case DAY_STYLE_FLAT:
                return com.vdesmet.lib.calendar.R.color.lib_calendar_background_flat;

            default:
                // Invalid style, throw exception
                throw new IllegalArgumentException("Day Style is invalid, cannot inflate day layout.");
        }
    }
}
