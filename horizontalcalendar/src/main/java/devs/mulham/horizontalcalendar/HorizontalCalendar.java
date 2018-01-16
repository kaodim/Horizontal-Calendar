package devs.mulham.horizontalcalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import devs.mulham.horizontalcalendar.adapter.DaysAdapter;
import devs.mulham.horizontalcalendar.adapter.HorizontalCalendarBaseAdapter;
import devs.mulham.horizontalcalendar.model.CalendarItemStyle;
import devs.mulham.horizontalcalendar.model.HorizontalCalendarConfig;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarPredicate;
import devs.mulham.horizontalcalendar.utils.HorizontalSnapHelper;
import devs.mulham.horizontalcalendar.utils.Utils;
import devs.mulham.horizontalcalendar.helpers.OnSwipeTouchListener;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;


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

    private boolean executed = false;

    public String weekDay;

    //store position of last selected adapter position
    private int lastSelectedPosition;

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


        HorizontalSnapHelper snapHelper = new HorizontalSnapHelper();
        snapHelper.attachToHorizontalCalendar(this);

        if (disablePredicate == null) {
            disablePredicate = defaultDisablePredicate;
        } else {
            disablePredicate = new HorizontalCalendarPredicate.Or(disablePredicate, defaultDisablePredicate);
        }

        mCalendarAdapter = new DaysAdapter(this, startDate, endDate, disablePredicate);
        calendarView.setAdapter(mCalendarAdapter);
        calendarView.setLayoutManager(new HorizontalLayoutManager(calendarView.getContext(), false));
//        calendarView.getLayoutManager().setScrollEnabled(false);
//        calendarView.getLayoutManager().canScrollHorizontally();

        //uncomment for listen to scroll event
        calendarView.addOnScrollListener(new HorizontalCalendarScrollListener());

//        calenderViewOnSwipeListener();

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        weekDay = dayFormat.format(defaultSelectedDate.getTime());
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

//
//    private void calenderViewOnSwipeListener(){
//        calendarView.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
//            public void onSwipeRight() {
//                scrollToNextPreviousWeekdayPosition(lastSelectedPosition+7);
//                Toast.makeText(getContext(), "right", Toast.LENGTH_SHORT).show();
//            }
//            public void onSwipeLeft() {
//                scrollToNextPreviousWeekdayPosition(lastSelectedPosition-7);
//                Toast.makeText(getContext(), "left", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    /**
     * Select today date and center the Horizontal Calendar to this date
     *
     * @param immediate pass true to make the calendar scroll as fast as possible to reach the date of today
     *                  ,or false to play default scroll animation speed.
     */
    public void goToday(boolean immediate) {
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
     *
     * @param date      The date to select
     *
     */
    public void goNextDay(Calendar date) {
        int datePosition = positionOfDate(date);
        if(getShiftCellsForWeekDay(datePosition)==0){ //if saturday
            moveToWeekFirstOrLastDay(datePosition +1);
        }else{
            date.add(date.DATE,1);
            datePosition = positionOfDate(date);
            scrollToPositionWithNoAnimation(datePosition);
        }
    }

    /**
     * Scroll to previous day from last selected postion
     *
     * @param date      The date to select
     *
     */
    public void goPreviousDay(Calendar date) {
        int datePosition = positionOfDate(date);
        if(getShiftCellsForWeekDay(datePosition)== 6){ //if Sunday
            moveToWeekFirstOrLastDay(datePosition -1);
        }else{
            date.add(date.DATE,-1);
            datePosition = positionOfDate(date);
            scrollToPositionWithNoAnimation(datePosition);
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
     *
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

                //when scroll to left on weekly bar
                if(position > calendarView.getPositionOfCenterItem())
                    relativePosition = position + getShiftCellsTodayWeekDay();
                    //when scroll to right on weekly bar
                else if(position < calendarView.getPositionOfCenterItem())
                    // -6 as cell position start with 0
                    relativePosition = position + getShiftCellsTodayWeekDay() - 6;


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
    public void scrollToNextPreviousWeekdayPosition(final int position) {
        if (position != -1) {
            if(lastSelectedPosition != -1){

                //call onDateSelected listener to update view
                if (calendarListener != null) {
                    calendarListener.onDateClicked(getDateAt(position), position);
                }

                int relativePosition = position;

                //when scroll to left on weekly bar
                if(position > calendarView.getPositionOfCenterItem()) {
                    relativePosition = position + getShiftCellsForWeekDay(position);
                    //when scroll to right on weekly bar
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


//                executed = false;
            }
//            executed = false;
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

                //when scroll to left on weekly bar
                if(position > calendarView.getPositionOfCenterItem()) {
                    relativePosition = position + getShiftCellsForWeekDay(position);
                    //when scroll to right on weekly bar
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
//            int relativeCenterPosition = Utils.calculateRelativeCenterPosition(position, calendarView.getPositionOfCenterItem(), getShiftCellsCenter());
//            if (relativeCenterPosition == position) {
//                return;
//            }

            if(lastSelectedPosition != -1){

                if (calendarListener != null) {
                    calendarListener.onDateClicked(getDateAt(position), position);

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
    private class HorizontalCalendarScrollListener extends RecyclerView.OnScrollListener {

        int lastSelectedItem = lastSelectedPosition;
        int firstVisibleItem, visibleItemCount, totalItemCount;

//        final Runnable selectedItemRefresher = new SelectedItemRefresher();

        HorizontalCalendarScrollListener() {
        }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_IDLE:
                executed = false;
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


    @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //On Scroll, agenda is refresh to update background colors
//            post(selectedItemRefresher);
//
//            visibleItemCount = recyclerView.getChildCount();
//            totalItemCount = calendarView.getLayoutManager().getItemCount();
//            firstVisibleItem = calendarView.getLayoutManager().findFirstVisibleItemPosition();
            //Todo: Change the code so that onScrolled executed once
            if(!executed && dx > 0){
                executed = true;
                Log.d("X: " ,String.valueOf(dx));
                Log.d("XPos: " ,String.valueOf(lastSelectedPosition));
                scrollToNextPreviousWeekdayPosition(lastSelectedPosition+7);
            }else if(!executed && dx < 0){
                    executed = true;
                    Log.d("-X: " ,String.valueOf(dx));
                    Log.d("XPos: " ,String.valueOf(lastSelectedPosition));
                    scrollToNextPreviousWeekdayPosition(lastSelectedPosition-7);
            }


            tempCount++;
            Log.d("Count",String.valueOf(tempCount));
            Log.d("Excecute",String.valueOf(executed));
        }


//        private class SelectedItemRefresher implements Runnable {
//
//            SelectedItemRefresher() {
//            }


//            @Override
//            public void run() {
//                final int positionOfCenterItem = calendarView.getPositionOfCenterItem();
//                if ((lastSelectedItem == -1) || (lastSelectedItem != positionOfCenterItem)) {
//                    //On Scroll, agenda is refresh to update background colors
//                    refreshItemsSelector(positionOfCenterItem);
//                    if (lastSelectedItem != -1) {
//                        refreshItemsSelector(lastSelectedItem);
//                    }
//                    lastSelectedItem = positionOfCenterItem;
//                }
//            }
//        }
    }
}