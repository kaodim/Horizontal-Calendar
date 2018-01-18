# Kaodim Horizontal Calendar

A material horizontal calendar view for Android based on `RecyclerView`.

![demo](/art/WeekNavigation.gif)

## Installation

Add the library to your **build.gradle**:

```gradle

dependencies {
      compile 'compile 'com.github.kaodim:Horizontal-Calendar:1.3.2'
    }
```

## Prerequisites

The minimum API level supported by this library is **API 14 (ICE_CREAM_SANDWICH)**.

## Usage

- Add `HorizontalCalendarView` to your layout file, for example:

```xml
<android.support.design.widget.AppBarLayout>
		............ 
		
        <devs.mulham.horizontalcalendar.HorizontalCalendarView
            android:id="@+id/calendarView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/colorPrimary"
            app:textColorSelected="#FFFF"/>
            
</android.support.design.widget.AppBarLayout>
```

- In your Activity or Fragment, define your **start** and **end** dates to set the range of the calendar:
- Kaodim Horizontal Calendar by default has 24 months before and after current date

```java
/* end after 1 month from now */
Calendar endDate = Calendar.getInstance();
endDate.add(Calendar.MONTH, 24);

/* start before 1 month from now */
Calendar startDate = Calendar.getInstance();
startDate.add(Calendar.MONTH, -24);
```

- Then setup `HorizontalCalendar` in your **Activity** through its Builder: 

```java
HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(this, R.id.calendarView)
                .range(startDate, endDate)
                .datesNumberOnScreen(5)
                .build();
```

- Or if you are using a **Fragment**:

```java
HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(rootView, R.id.calendarView)
	...................
```

- To listen to date change events you need to set a listener:

```java
horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar date, int position) {
                //do something
            }
        });
```

- You can also listen to **scroll** , **long press** and **click** events by overriding each respective method within **HorizontalCalendarListener**:

```java
horizontalCalendar.setCalendarListener(new HorizontalCalendarListener() {
            @Override
            public void onDateSelected(Calendar date, int position) {

            }

	    @Override
            public void onDateClicked(Calendar date, int position) {

            }

            @Override
            public void onCalendarScroll(HorizontalCalendarView calendarView, 
            int dx, int dy) {
                
            }

            @Override
            public boolean onDateLongClicked(Calendar date, int position) {
                return true;
            }
        });
```

## Customization

- You can customize it directly inside your **layout**:

```xml
<devs.mulham.horizontalcalendar.HorizontalCalendarView
            android:id="@+id/calendarView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:textColorNormal="#bababa"
            app:textColorSelected="#FFFF"
            app:selectorColor="#c62828"  //default to colorAccent
            app:selectedDateBackground="@drawable/myDrawable"/>
```

- Or you can do it programmatically in your **Activity** or **Fragment** using `HorizontalCalendar.Builder`:

```java
HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(this, R.id.calendarView)
                .range(Calendar startDate, Calendar endDate)
                .datesNumberOnScreen(int number)   // Number of Dates cells shown on screen (default to 5).
                .configure()    // starts configuration.
                    .formatTopText(String dateFormat)       // default to "MMM".
                    .formatMiddleText(String dateFormat)    // default to "dd".
                    .formatBottomText(String dateFormat)    // default to "EEE".
                    .showTopText(boolean show)              // show or hide TopText (default to true).
                    .showBottomText(boolean show)           // show or hide BottomText (default to true).
                    .textColor(int normalColor, int selectedColor)    // default to (Color.LTGRAY, Color.WHITE).
                    .selectedDateBackground(Drawable background)      // set selected date cell background.
                    .selectorColor(int color)               // set selection indicator bar's color (default to colorAccent).
                .end()          // ends configuration.
                .defaultSelectedDate(Date date)    // Date to be seleceted at start (default to current day `new Date()`).
                .build();
```

#### More Customizations

```java
builder.configure()
           .textSize(float topTextSize, float middleTextSize, float bottomTextSize)
           .sizeTopText(float size)
           .sizeMiddleText(float size)
           .sizeBottomText(float size)
           .colorTextTop(int normalColor, int selectedColor, int todayColor)
           .colorTextMiddle(int normalColor, int selectedColor, int todayColor)
           .colorTextBottom(int normalColor, int selectedColor, int todayColor)
       .end()
```

## Reconfiguration
HorizontalCalendar configurations can be changed after initialization:
 
- Change calendar dates range:
```java
horizontalCalendar.setRange(Calendar startDate, Calendar endDate);
```
 
- Change default(not selected) items style:
```java
horizontalCalendar.getDefaultStyle()
        .setColorTopText(int color)
        .setColorMiddleText(int color)
        .setColorBottomText(int color)
        .setBackground(Drawable background);      
```

- Change selected item style:
```java
horizontalCalendar.getSelectedItemStyle()
        .setColorTopText(int color)
        ..............
```

- Change formats, text sizes and selector color:
```java
horizontalCalendar.getConfig()
        .setSelectorColor(int color)
        .setFormatTopText(String format)
        .setSizeTopText(float size)
        ..............
```

#### Important
**Make sure to call `horizontalCalendar.refresh();` when you finish your changes**

## Features

- Disable specific dates with `HorizontalCalendarPredicate`, a unique style for disabled dates can be specified as well with `CalendarItemStyle`:
```java
builder.disableDates(new HorizontalCalendarPredicate() {
                           @Override
                           public boolean test(Calendar date) {
                               return false;    // return true if this date should be disabled, false otherwise.
                           }
       
                           @Override
                           public CalendarItemStyle style() {
                               return null;     // create and return a new Style for disabled dates, or null if no styling needed.
                           }
                       })
```

All the methods are implemented in **HorizontalCalendarListener.java**:

- Select a specific **Date** programmatically with the option whether to play the animation or not:
```java
horizontalCalendar.selectDate(Calendar date, boolean immediate); // set immediate to false to ignore animation.
```

- Scroll to today:
```
horizontalCalendar.goToday();
```

- Scroll to next day by giving current date as input:
```
horizontalCalendar.goNextDay(Calendar currentDate);
```

- Scroll to previous day by giving current date as input:
```
horizontalCalendar.goPreviousDay(Calendar currentDate);
```
- Check if a date is contained in the Calendar:
```java
horizontalCalendar.contains(Calendar date);
```

- Get number of **days** between two dates:
```java
Utils.daysBetween(Calendar startInclusive, Calendar endExclusive);
```
