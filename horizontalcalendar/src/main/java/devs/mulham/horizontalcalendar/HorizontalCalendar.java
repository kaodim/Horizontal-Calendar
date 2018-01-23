package devs.mulham.horizontalcalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import devs.mulham.horizontalcalendar.adapter.DaysAdapter;
import devs.mulham.horizontalcalendar.adapter.HorizontalCalendarBaseAdapter;
import devs.mulham.horizontalcalendar.model.CalendarItemStyle;
import devs.mulham.horizontalcalendar.model.HorizontalCalendarConfig;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarPredicate;
import devs.mulham.horizontalcalendar.utils.Utils;


/**
 * See {@link HorizontalCalendarView HorizontalCalendarView}
 *
 * @author Mulham-Raee
 * @see HorizontalCalendarListener
 * @since v1.0.0
 */
public final class HorizontalCalendar {

    //region private Fields
    HorizontalCalendarView calendarView;
    private HorizontalCalendarBaseAdapter mCalendarAdapter;

    //Start & End Dates
    Calendar startDate;
    Calendar endDate;

    //Number of Dates to Show on Screen
    private final int numberOfDatesOnScreen;

    //Store today's position in adpater
    public int positionOfToday;
    private int tempCount = 0;

    private boolean swipe = false;
    private boolean isDragRight, isDragLeft;

    public String weekDay;

    //store position of last selected adapter position
    private int lastSelectedPosition;

    //store position of last selected adapter position
    private int longClickedPosition;


    //Interface events
    HorizontalCalendarListener calendarListener;

    private final int calendarId;
    /* Format, Colors & Font Sizes*/
    private final CalendarItemStyle defaultStyle;
    private final CalendarItemStyle selectedItemStyle;
    private final CalendarItemStyle todayItemStyle;
    private final HorizontalCalendarConfig config;
    //endregion

    /**
     * Private Constructor to insure HorizontalCalendar can't be initiated the default way
     */
    HorizontalCalendar(Builder builder, HorizontalCalendarConfig config, CalendarItemStyle defaultStyle, CalendarItemStyle selectedItemStyle,CalendarItemStyle todayItemStyle) {
        this.numberOfDatesOnScreen = builder.numberOfDatesOnScreen;
        this.calendarId = builder.viewId;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.config = config;
        this.defaultStyle = defaultStyle;
        this.selectedItemStyle = selectedItemStyle;
        this.todayItemStyle = todayItemStyle;
    }

    /* Init Calendar View */
    void init(View rootView, final Calendar defaultSelectedDate, HorizontalCalendarPredicate disablePredicate) {
        calendarView = rootView.findViewById(calendarId);
        calendarView.setHasFixedSize(true);
        calendarView.setHorizontalScrollBarEnabled(false);


        //set snaphelper to null to disable dragging
//        HorizontalSnapHelper snapHelper = new HorizontalSnapHelper();
//        snapHelper.attachToHorizontalCalendar(this);

//        SnapHelper snapHelperTop = new GravitySnapHelper(Gravity.START);
//        snapHelperTop.attachToRecyclerView(calendarView);


        if (disablePredicate == null) {
            disablePredicate = defaultDisablePredicate;
        } else {
            disablePredicate = new HorizontalCalendarPredicate.Or(disablePredicate, defaultDisablePredicate);
        }

        mCalendarAdapter = new DaysAdapter(this, startDate, endDate, disablePredicate);
        calendarView.setAdapter(mCalendarAdapter);
        calendarView.setLayoutManager(new HorizontalLayoutManager(calendarView.getContext(), false));
        calendarView.getLayoutManager().setScrollEnabled(true);
        calendarView.getLayoutManager().canScrollHorizontally();

        //uncomment for listen to scroll event
        calendarView.addOnScrollListener(new HorizontalCalendarScrollListener());


        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        weekDay = dayFormat.format(defaultSelectedDate.getTime());

        longClickedPosition = -1;
        Log.d("Today: ","" + weekDay);

        post(new Runnable() {
            @Override
            public void run() {
                positionOfToday = positionOfDate(defaultSelectedDate);
                lastSelectedPosition = positionOfToday;
                weekDayNoAnimation(positionOfDateNoshift(defaultSelectedDate));
            }
        });

        calendarView.applyConfigFromLayout(this);

    }

    public HorizontalCalendarListener getCalendarListener() {
        return calendarListener;
    }

    public void setCalendarListener(HorizontalCalendarListener calendarListener) {
        this.calendarListener = calendarListener;
    }


    /**
     * Select today date and center the Horizontal Calendar to this date
     *
     *
     */
    public void goToday() {
        scrollToTodayPositionWithNoAnimation(positionOfToday);
    }

    /**
     * Select the date and center the Horizontal Calendar to this date
     *
     * @param date      The date to select
     * @param immediate pass true to make the calendar scroll as fast as possible to reach the target date
     *                  ,or false to play default scroll animation speed.
     */
    public void selectDate(Calendar date, boolean immediate) {

        int datePosition = positionOfDate(date);
        if (immediate) {
            centerToPositionWithNoAnimation(datePosition);
            if (calendarListener != null) {
                calendarListener.onDateSelected(date, datePosition);
            }
        } else {
            calendarView.setSmoothScrollSpeed(HorizontalLayoutManager.SPEED_NORMAL);
            centerCalendarToPosition(datePosition);
        }
    }


    /**
     * Set the long clicked position to be able to use when determine the position during
     * drag listener
     *
     * @param position date to select
     *
     */
    public void setLongClickedPosition(int position) {
        longClickedPosition = position;
    }


    /**
     * Check if given date is before last selected date
     *
     * @param date      The date to select
     *
     */
    public Boolean dayBeforeLastSelectedPosition(Calendar date) {
        return date.before(getDateAt(lastSelectedPosition));
    }

    /**
     * Check if given date is before last selected date
     *
     * @param date      The date to select
     *
     */
    public Boolean dayAfterLastSelectedPosition(Calendar date) {
        return date.after(getDateAt(lastSelectedPosition));
    }

    /**
     * Scroll to next day from last selected postion
     * If next day is first day of next week, move to next week
     * @param date      The date to select
     *
     */
    public void goNextDay(Calendar date) {
        int datePosition = positionOfDate(date);
        //check if the given date is Saturday
        if(getShiftCellsForWeekDay(datePosition)==0){ //if saturday
            moveToWeekFirstOrLastDay(datePosition +1);
        }else{
//            date.add(date.DATE,1);
//            datePosition = positionOfDate(date);
            scrollToPositionWithNoAnimation(datePosition + 1);
        }
    }

    /**
     * Scroll to previous day from last selected postion
     * If next day is last day of last week, move to previous week
     * @param date      The date to select
     *
     */
    public void goPreviousDay(Calendar date) {
        int datePosition = positionOfDate(date);
        //check if the given date is Sunday
        if(getShiftCellsForWeekDay(datePosition)== 6){ //if Sunday
            moveToWeekFirstOrLastDay(datePosition -1);
        }else{
//            date.add(date.DATE,-1);
//            datePosition = positionOfDate(date);
            scrollToPositionWithNoAnimation(datePosition -1);
        }
    }

    /**
     * Smooth scroll Horizontal Calendar to center this position and select the new centered day.
     *
     * @param position The position to center the calendar to!
     */
    public void centerCalendarToPosition(final int position) {
        if (position != -1) {
            int relativeCenterPosition = Utils.calculateRelativeCenterPosition(position, calendarView.getPositionOfCenterItem(), getShiftCellsCenter());
            if (relativeCenterPosition == position) {
                return;
            }

            calendarView.smoothScrollToPosition(relativeCenterPosition);
        }
    }

    /**
     * Scroll Horizontal Calendar to center this position and select the new centered day.
     *
     * @param position The position to center the calendar to!
     */
    void centerToPositionWithNoAnimation(final int position) {
        if (position != -1) {
            int relativeCenterPosition = Utils.calculateRelativeCenterPosition(position, calendarView.getPositionOfCenterItem(), getShiftCellsCenter());
            if (relativeCenterPosition == position) {
                return;
            }

            final int oldSelectedItem = calendarView.getPositionOfCenterItem();
            calendarView.scrollToPosition(relativeCenterPosition);
            calendarView.post(new Runnable() {
                @Override
                public void run() {
                    final int newSelectedItem = calendarView.getPositionOfCenterItem();
                    //refresh to update background colors
                    refreshItemsSelector(newSelectedItem, oldSelectedItem);
                }
            });
        }
    }


    /**
     * Scroll Horizontal Calendar to weekday position and select the new day for weekday.
     * Used to setup the visible week for the first time
     * @param position The position to center the calendar to!
     */
    void weekDayNoAnimation(final int position) {
        if (position != -1) {
            int relativeCenterPosition = Utils.calculateRelativeCenterPosition(position, calendarView.getPositionOfCenterItem(), getShiftCellsCenter());
            if (relativeCenterPosition == position) {
                return;
            }

            final int oldSelectedItem = calendarView.getPositionOfCenterItem();
            calendarView.scrollToPosition(relativeCenterPosition);
            calendarView.post(new Runnable() {
                @Override
                public void run() {
                    final int newSelectedItem = calendarView.getPositionOfCenterItem();
                    //refresh to update background colors
                    refreshItemsSelector(newSelectedItem, oldSelectedItem);
                }
            });
        }
    }

    /**
     * Scroll Horizontal Calendar to  position and select the new day.
     *
     * @param position The position to center the calendar to!
     */
    public void scrollToPositionWithNoAnimation(final int position) {
        if (position != -1) {

            if(lastSelectedPosition != -1){

                if (calendarListener != null) {
                    calendarListener.onDateSelected(getDateAt(position), position);

                }
                final int oldSelectedItem = lastSelectedPosition;
                calendarView.scrollToPosition(position);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;
                    }
                });
            }
        }
    }

    /**
     * Scroll Horizontal Calendar to today position and select today date.
     *
     * @param position The position to center the calendar to!
     */
    public void scrollToTodayPositionWithNoAnimation(final int position) {
        if (position != -1) {
            if(lastSelectedPosition != -1){


                //call onDateSelected listener to update view
                if (calendarListener != null) {
                    calendarListener.onDateClicked(getDateAt(position), position);
                }

                int relativePosition = position;

                //when scroll to leftDirection on weekly bar
                if(position > calendarView.getPositionOfCenterItem())
                    relativePosition = position + getShiftCellsTodayWeekDay();
                    //when scroll to rightDirection on weekly bar
                else if(position < calendarView.getPositionOfCenterItem())
                    // -6 as cell position start with 0 from rightDirection
                    relativePosition = position + getShiftCellsTodayWeekDay() - 6;


                //update day adapter layout in weekbar
                final int oldSelectedItem = lastSelectedPosition;
//                calendarView.setSmoothScrollSpeed(HorizontalLayoutManager.SPEED_NORMAL);
                calendarView.scrollToPosition(relativePosition);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;
                    }
                });
            }
        }
    }

    /**
     * Scroll Horizontal Calendar to today position and select today date.
     *
     * @param position The position to center the calendar to!
     */
    public void scrollToNextPreviousWeekdayPosition(final int position) {
        if (position != -1) {
            if(lastSelectedPosition != -1){
                String date = DateFormat.format("EEEE - d MMM yyyy", getDateAt(position)).toString();

                Log.d("DateScroll: ", date);
                //call onDateSelected listener to update view
                if (calendarListener != null) {
                    calendarListener.onDateSelected(getDateAt(position), position);
                }

                int relativePosition = position;

                //when scroll to leftDirection on weekly bar
                if(position > calendarView.getPositionOfCenterItem()) {
                    relativePosition = position + getShiftCellsForWeekDay(position);
                    //when scroll to rightDirection on weekly bar
                }else if(position < calendarView.getPositionOfCenterItem()) {
                    // -6 as cell position start with 0
                    relativePosition = position + getShiftCellsForWeekDay(position) - 6;
                }

                Log.d("CenterPosition", String.valueOf(calendarView.getPositionOfCenterItem()));

                //update day adapter layout in weekbar
                final int oldSelectedItem = lastSelectedPosition;
                calendarView.scrollToPosition(relativePosition);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;

                    }
                });
            }
        }
    }


    /**
     * Set to current weekday when fling/drag is slow
     *
     * @param position The position of last selected position
     */
    public void scrollToWeekPosition(final int position) {
        if (position != -1) {
            if(lastSelectedPosition != -1){
                String date = DateFormat.format("EEEE - d MMM yyyy", getDateAt(position)).toString();

                //call onDateSelected listener to update view
//                if (calendarListener != null) {
//                    calendarListener.onDateSelected(getDateAt(position), position);
//                }

                int relativePosition = position;

                    if (position >= calendarView.getPositionOfCenterItem()) {
                        if (isDragRight){
                            relativePosition = position + getShiftCellsForWeekDay(position)-6;
                            Log.d("DateScroll: ", " right1");
                        }else if(isDragLeft){
                            relativePosition = position + getShiftCellsForWeekDay(position);

                            Log.d("DateScroll: ", " left1");
                        }
                    }else if(position <= calendarView.getPositionOfCenterItem()){
                        if (isDragRight){
                            relativePosition = position + getShiftCellsForWeekDay(position)-6;
                            Log.d("DateScroll: ", " right2");
                        }else if(isDragLeft){
                            relativePosition = position + getShiftCellsForWeekDay(position);

                            Log.d("DateScroll: ", " left2");
                        }
                    }

//                    //when scroll to leftDirection on weekly bar
//                    if(isDragRight) {
//                        if (position > calendarView.getPositionOfCenterItem()) {
//                            relativePosition = position + getShiftCellsForWeekDay(position);
//                            Log.d("DateScroll: ", " right1");
//                            //when scroll to rightDirection on weekly bar
//                        } else if (position < calendarView.getPositionOfCenterItem()) {
//                            Log.d("DateScroll: ", " right2");
////                            // -6 as cell position start with 0
//                            relativePosition = position + getShiftCellsForWeekDay(position) - 6;
//                        }
//                    }else if (isDragLeft){
//                        if (position > calendarView.getPositionOfCenterItem()) {
//                            relativePosition = position + getShiftCellsForWeekDay(position);
//
//                            Log.d("DateScroll: ", " left1");
//                            //when scroll to rightDirection on weekly bar
//                        } else if (position < calendarView.getPositionOfCenterItem()) {
//                            Log.d("DateScroll: ", " left2");
//                            // -6 as cell position start with 0
//                            relativePosition = position + getShiftCellsForWeekDay(position);
//                        }
//                    }


                Log.d("CenterPosition", String.valueOf(calendarView.getPositionOfCenterItem()));

                //update day adapter layout in weekbar
                final int oldSelectedItem = lastSelectedPosition;
                calendarView.scrollToPosition(relativePosition);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;

                    }
                });
            }
        }
    }



    /**
     * Scroll Horizontal Calendar to today position and select today date.
     *
     * @param position The position to center the calendar to!
     */
    public void moveToWeekFirstOrLastDay(final int position) {
        if (position != -1) {


            if(lastSelectedPosition != -1){
                //call onDateSelected listener to update view
                if (calendarListener != null) {
                    calendarListener.onDateSelected(getDateAt(position), position);
                }

                int relativePosition = position;

                //when scroll to leftDirection on weekly bar
                if(position > calendarView.getPositionOfCenterItem()) {
                    relativePosition = position + getShiftCellsForWeekDay(position);
                    //when scroll to rightDirection on weekly bar
                }else if(position < calendarView.getPositionOfCenterItem()) {
                    // -6 as cell position start with 0
                    relativePosition = position + getShiftCellsForWeekDay(position) - 6;
                }

                //update day adapter layout in weekbar
                final int oldSelectedItem = lastSelectedPosition;
                calendarView.scrollToPosition(relativePosition);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;
                    }
                });
            }
        }
    }





    /**
     * Scroll Horizontal Calendar to  position and select the new day when user clicked the date.
     *
     * @param position The position to center the calendar to!
     */
    public void scrollToPositionWhenClicked(final int position) {
        if (position != -1) {

            if(lastSelectedPosition != -1){

                if (calendarListener != null) {
                    calendarListener.onDateClicked(getDateAt(position), position);

                }

                //update the new and old date layout
                final int oldSelectedItem = lastSelectedPosition;
                calendarView.scrollToPosition(position);
                calendarView.post(new Runnable() {
                    @Override
                    public void run() {
                        final int newSelectedItem = position;
                        //refresh to update background colors
                        refreshItemsSelector(newSelectedItem, oldSelectedItem);
                        lastSelectedPosition = position;
                    }
                });
            }
        }
    }

    void refreshItemsSelector(int position1, int... positions) {
        mCalendarAdapter.notifyItemChanged(position1, "UPDATE_SELECTOR");
        if ((positions != null) && (positions.length > 0)) {
            for (int pos : positions) {
                mCalendarAdapter.notifyItemChanged(pos, "UPDATE_SELECTOR");
            }
        }
    }

    public boolean isItemDisabled(int position) {
        return mCalendarAdapter.isDisabled(position);
    }

    public void refresh(){
        mCalendarAdapter.notifyDataSetChanged();
    }

    public void show() {
        calendarView.setVisibility(View.VISIBLE);
    }

    public void hide() {
        calendarView.setVisibility(View.INVISIBLE);
    }

    public void post(Runnable runnable) {
        calendarView.post(runnable);
    }

    @TargetApi(21)
    public void setElevation(float elevation) {
        calendarView.setElevation(elevation);
    }

    /**
     * @return the current selected date
     */
    public Calendar getSelectedDate() {
        return mCalendarAdapter.getItem(calendarView.getPositionOfCenterItem());
    }

    /**
     * @return position of selected date in Horizontal Calendar
     */
    public int getSelectedDatePosition() {
        return lastSelectedPosition;
//        return calendarView.getPositionOfCenterItem();
    }

    /**
     * @param position The position of date
     * @return the date on this index
     * @throws IndexOutOfBoundsException if position is out of the calendar range
     */
    public Calendar getDateAt(int position) throws IndexOutOfBoundsException {
        return mCalendarAdapter.getItem(position);
    }

    /**
     * @param date The date to search for
     * @return true if the calendar contains this date or false otherwise
     */
    public boolean contains(Calendar date) {
        return positionOfDate(date) != -1;
    }

    public HorizontalCalendarView getCalendarView() {
        return calendarView;
    }

    public Context getContext() {
        return calendarView.getContext();
    }

    public void setRange(Calendar startDate, Calendar endDate){
        this.startDate = startDate;
        this.endDate = endDate;
       if (mCalendarAdapter instanceof DaysAdapter){
           ((DaysAdapter) mCalendarAdapter).update(startDate, endDate, false);
       }
    }

    public CalendarItemStyle getDefaultStyle() {
        return defaultStyle;
    }

    public CalendarItemStyle getSelectedItemStyle() {
        return selectedItemStyle;
    }

    public CalendarItemStyle getTodayItemStyle() {
        return todayItemStyle;
    }

    public HorizontalCalendarConfig getConfig() {
        return config;
    }

    public int getNumberOfDatesOnScreen() {
        return numberOfDatesOnScreen;
    }

    public int getShiftCellsCenter() {
        return numberOfDatesOnScreen / 2;
    }

    public int getShiftCellsTodayWeekDay() {
        return getWeekDayNumber(weekDay);
    }


    public int getShiftCellsForWeekDay(int position) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        String positionWeekDay = dayFormat.format(getDateAt(position).getTime());
        return getWeekDayNumber(positionWeekDay);
    }

    //return the int value of WeekDay, count starts from rightDirection
    public int getWeekDayNumber(String weekday){
        int shift = 0;
        switch(weekday){
            case "Sunday":
                shift = 6;
                break;
            case "Monday":
                shift = 5;
                break;
            case "Tuesday":
                shift = 4;
                break;
            case "Wednesday":
                shift = 3;
                break;
            case "Thursday":
                shift = 2;
                break;
            case "Friday":
                shift = 1;
                break;
            case "Saturday":
                shift = 0;
                break;
        }

        return shift;
    }

    /**
     * @return position of date in Calendar, or -1 if date does not exist
     */
    public int positionOfDate(Calendar date) {
        if (date.before(startDate) || date.after(endDate)) {
            return -1;
        }

        int position;
        if (Utils.isSameDate(date, startDate)) {
            position = 0;
        } else {
            position = Utils.daysBetween(startDate, date);
        }

        final int shiftCells = getShiftCellsCenter();
        return position + shiftCells;
    }

    /**
     * @return position of date in Calendar, or -1 if date does not exist
     */
    public int positionOfDateNoshift(Calendar date) {
        if (date.before(startDate) || date.after(endDate)) {
            return -1;
        }

        int position;
        if (Utils.isSameDate(date, startDate)) {
            position = 0;
        } else {
            position = Utils.daysBetween(startDate, date);
        }

        final int shiftCells = getShiftCellsTodayWeekDay();
        return position + shiftCells;
    }

    public static class Builder {

        final int viewId;
        final View rootView;

        // Start & End Dates
        Calendar startDate;
        Calendar endDate;
        Calendar defaultSelectedDate;

        // Number of Days to Show on Screen
        int numberOfDatesOnScreen;
        // Specified which dates should be disabled
        private HorizontalCalendarPredicate disablePredicate;

        private ConfigBuilder configBuilder;

        /**
         * @param rootView pass the rootView for the Fragment where HorizontalCalendar is attached
         * @param viewId   the id specified for HorizontalCalendarView in your layout
         */
        public Builder(View rootView, int viewId) {
            this.rootView = rootView;
            this.viewId = viewId;
        }

        /**
         * @param activity pass the activity where HorizontalCalendar is attached
         * @param viewId   the id specified for HorizontalCalendarView in your layout
         */
        public Builder(Activity activity, int viewId) {
            this.rootView = activity.getWindow().getDecorView();
            this.viewId = viewId;
        }

        public Builder range(Calendar startDate, Calendar endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        public Builder datesNumberOnScreen(int numberOfItemsOnScreen) {
            this.numberOfDatesOnScreen = numberOfItemsOnScreen;
            return this;
        }

        public Builder defaultSelectedDate(Calendar date) {
            defaultSelectedDate = date;
            return this;
        }

        public Builder disableDates(HorizontalCalendarPredicate predicate) {
            disablePredicate = predicate;
            return this;
        }

        public ConfigBuilder configure() {
            if (configBuilder == null) {
                configBuilder = new ConfigBuilder(this);
            }

            return configBuilder;
        }

        private void initDefaultValues() throws IllegalStateException {
            /* Defaults variables */
            if ((startDate == null) || (endDate == null)) {
                throw new IllegalStateException("HorizontalCalendar range was not specified, either startDate or endDate is null!");
            }
            if (numberOfDatesOnScreen <= 0) {
                numberOfDatesOnScreen = 5;
            }
            if (defaultSelectedDate == null) {
                defaultSelectedDate = Calendar.getInstance();
            }
        }

        /**
         * @return Instance of {@link HorizontalCalendar} initiated with builder settings
         */
        public HorizontalCalendar build() throws IllegalStateException {
            initDefaultValues();

            if (configBuilder == null) {
                configBuilder = new ConfigBuilder(this);
                configBuilder.end();
            }
            CalendarItemStyle defaultStyle = configBuilder.createDefaultStyle();
            CalendarItemStyle selectedItemStyle = configBuilder.createSelectedItemStyle();
            CalendarItemStyle todayItemStyle = configBuilder.createTodayItemStyle();
            HorizontalCalendarConfig config = configBuilder.createConfig();

            HorizontalCalendar horizontalCalendar = new HorizontalCalendar(this, config, defaultStyle, selectedItemStyle,todayItemStyle);
            horizontalCalendar.init(rootView, defaultSelectedDate, disablePredicate);
            return horizontalCalendar;
        }
    }

    private final HorizontalCalendarPredicate defaultDisablePredicate = new HorizontalCalendarPredicate() {

        @Override
        public boolean test(Calendar date) {
            return date.before(startDate) || date.after(endDate);
        }

        @Override
        public CalendarItemStyle style() {
            return new CalendarItemStyle(Color.GRAY, null);
        }
    };

//    //uncomment for listen to scroll event
    /**
     * Scroll listener on the current visible week
     */
    private class HorizontalCalendarScrollListener extends RecyclerView.OnScrollListener {

        int lastSelectedItem = lastSelectedPosition;
        boolean rightDirection, leftDirection,drag;
        int firstVisibleItem, visibleItemCount, totalItemCount;

//        final Runnable selectedItemRefresher = new SelectedItemRefresher();

        HorizontalCalendarScrollListener() {
        }

        /**
         * When onScrolled is finished this method is invoked to listen to the state
         */
        //update the date selected once the scrolling is ended
    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_IDLE:
                if(swipe && rightDirection){
                    swipe = false;
                    scrollToNextPreviousWeekdayPosition(lastSelectedPosition + 7);
                }
                else if (swipe && leftDirection){
                    swipe = false;
                    scrollToNextPreviousWeekdayPosition(lastSelectedPosition - 7);
                }else if(drag) {
                    drag = false;
                    scrollToWeekPosition(lastSelectedPosition);
//                    System.out.println("left" + leftDirection);
//                    System.out.println("right" + rightDirection);
                }
                System.out.println("The RecyclerView is not scrolling");
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                System.out.println("Scrolling now");
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:

                System.out.println("Scroll Settling");
                break;

        }

    }

        /**
         * Executed before onScrollStateChange
         * Scroll direction will be store if the dx is more than 0 in both directions
         */
    @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //On Scroll, agenda is refresh to update background colors
//            post(selectedItemRefresher);
        Log.d("X: " ,String.valueOf(dx));
            //variable swipe is set to true, hence execution happens only once
            if(!swipe && dx > 15){
                swipe = true;
                rightDirection = true;
                leftDirection = false;
                System.out.println("swipe right");
//
            }else if(!swipe && dx < -15){
                swipe = true;
                rightDirection = false;
                leftDirection = true;
                System.out.println("swipe left");

            }
            else if(!drag && (dx > -15 && dx < 0)){
                drag = true;
                isDragRight = false;
                isDragLeft = true;
                System.out.println("drag left");
            }
            else if(!drag && (dx < 15 && dx > 0)){
            drag = true;
            isDragRight = true;
            isDragLeft = false;
            System.out.println("drag right");
            }
            else{

            }

        }

    }
}