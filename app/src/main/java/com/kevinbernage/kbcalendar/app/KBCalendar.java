/*
 * Copyright (C) 2014 Kevin Bernage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kevinbernage.kbcalendar.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.widget.AdapterView;

import org.lucasr.twowayview.TwoWayView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class KBCalendar extends View {

    private TwoWayView mListView;
    private KBCalendarAdapter mCalendarAdapter;
    private ArrayList<Date> mListDays;

    //Start & End Dates
    private SimpleDateFormat dateFormat;
    private Date dateStartCalendar;
    private Date dateEndCalendar;

    //Interface events
    private IKBAgendaEvent iAgendaDateSelect;

    //RootView
    private View rootViewParent;

    //Number of Row Show on Screen
    private int numberOfRowOnScreen;
    //Position in arraylist of the center item
    public static int positionOfCenterItem;

    /* Format, Font Size & Colors*/
    private int daySize;
    private int dayNumberSize;
    private String formatDay;
    private String formatDayNumber;
    private String formatDate;
    private String hexColorDay;
    private String hexColorDayNumber;
    private String hexBackgroundColor;


    public KBCalendar(Context context) {
        super(context);
        init();
    }
    public KBCalendar(Context context, IKBAgendaEvent iAgendaDateSelect) {
        super(context);
        this.iAgendaDateSelect = iAgendaDateSelect;
        init();
    }
    public KBCalendar(Context context, IKBAgendaEvent iAgendaDateSelect, View rootViewParent) {
        super(context);
        this.iAgendaDateSelect = iAgendaDateSelect;
        this.rootViewParent = rootViewParent;
        init();
    }

    private void init(){

        /* Defaults variables */
        numberOfRowOnScreen = 5;
        formatDate = "dd/MM/yyyy";
        formatDay = "EEE";
        formatDayNumber = "dd/MM";
        daySize = 13;
        dayNumberSize = 17;
        hexColorDay = String.format("#%06X", (0xFFFFFF & Color.BLACK));
        hexColorDayNumber = String.format("#%06X", (0xFFFFFF & Color.BLACK));
        hexBackgroundColor = String.format("#%06X", (0xFFFFFF & Color.GRAY));

        dateFormat = new SimpleDateFormat(formatDate);
        try {
            dateStartCalendar = dateFormat.parse("26/06/2013");
            dateEndCalendar = dateFormat.parse("30/06/2014");

        } catch (ParseException e) {
            e.printStackTrace();
        }
        mListDays = new ArrayList<Date>();
    }



    /************************************************/
    /************************************************/
    /***************  PUBLIC METHODS ****************/
    /************************************************/
    /************************************************/

    /* Init KBCalendar View ; This method have to be call imperatively */
    public void loadKBCalendar(){
        View rootView;
        if(rootViewParent != null)
            rootView = rootViewParent;
        else
            rootView = ((Activity)getContext()).getWindow().getDecorView().findViewById(android.R.id.content);

        mListView = (TwoWayView) rootView.findViewById(R.id.list);
        mListView.setHorizontalScrollBarEnabled(false);


        //ArrayList of dates is set with all the dates between
        //start and end date
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTime(dateStartCalendar);
        calendar.add(Calendar.DATE, -5);
        Date dateStartBefore = calendar.getTime();
        calendar.setTime(dateEndCalendar);
        calendar.add(Calendar.DATE, 5);
        Date dateEndAfter = calendar.getTime();

        for (Date date = dateStartBefore; !date.equals(dateEndAfter);) {
            calendar.setTime(date);
            calendar.add(Calendar.DATE, 1);
            date = calendar.getTime();
            mListDays.add(date);
        }

        mCalendarAdapter = new KBCalendarAdapter(this, getContext(), mListDays, numberOfRowOnScreen);
        mListView.setAdapter(mCalendarAdapter);
        mListView.setOnScrollListener(new TwoWayView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(TwoWayView view, int scrollState) {
                if(scrollState == 0){
                    //On scroll end, the dateSelect event is call
                    //and agenda is center to the good item
                    int position = getPositionOfCenterItem();
                    centerToPosition(position);
                    if(iAgendaDateSelect != null)
                        iAgendaDateSelect.onDateSelect(mListDays.get(position));
                }
            }

            @Override
            public void onScroll(TwoWayView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //On Scroll, agenda is refresh to update background colors
                int shiftCells = numberOfRowOnScreen / 2;
                positionOfCenterItem = firstVisibleItem + shiftCells;
                mCalendarAdapter.notifyDataSetChanged();
                iAgendaDateSelect.onListScroll(getCurrentDate());
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View child, int position,
                                    long id) {
                //On item Click, the dateSelect event is call
                //and agenda is center to the good item
                centerToPosition(position);
                if(iAgendaDateSelect != null)
                    iAgendaDateSelect.onDateSelect(mListDays.get(position));
            }
        });
        new InitTask().execute();
    }


    /* Center the listView to today */
    public void goToday(){ centerToDate(new Date());}

    /* Center the listView to the date*/
    public void centerToDate(Date dateCenter){
        centerToPosition(positionOfDate(dateCenter));
    }

    /* Set the start date of the calendar */
    public void setDateStartCalendar(Date dateStartCalendar) {
        this.dateStartCalendar = dateStartCalendar;
    }
    public Date getDateStartCalendar() { return dateStartCalendar; }
    /* Set the date end of the calendar */
    public void setDateEndCalendar(Date dateEndCalendar) {this.dateEndCalendar = dateEndCalendar;}
    public Date getDateEndCalendar() { return dateEndCalendar; }
    /* Set the number of row that appears on screen */
    public void setNumberOfRowOnScreen(int numberOfRowOnScreen) {
        this.numberOfRowOnScreen = numberOfRowOnScreen;
    }
    /* Return the current selected date */
    public Date getCurrentDate(){ return mListDays.get(getPositionOfCenterItem()); }

    /* Getters and setters for Format, Font Size and Colors*/
    public int getDaySize() { return daySize; }
    public void setDaySize(int daySize) { this.daySize = daySize; }
    public int getDayNumberSize() { return dayNumberSize; }
    public void setDayNumberSize(int dayNumberSize) { this.dayNumberSize = dayNumberSize; }
    public String getFormatDay() { return formatDay; }
    public void setFormatDay(String formatDay) { this.formatDay = formatDay; }
    public String getFormatDayNumber() { return formatDayNumber; }
    public void setFormatDayNumber(String formatDayNumber) { this.formatDayNumber = formatDayNumber; }
    public String getHexColorDay() { return hexColorDay; }
    public void setHexColorDay(String hexColorDay) { this.hexColorDay = hexColorDay; }
    public String getHexColorDayNumber() { return hexColorDayNumber; }
    public void setHexColorDayNumber(String hexColorDayNumber) { this.hexColorDayNumber = hexColorDayNumber; }

    /* Getter and setter for background color of agenda. An Alpha is set to this color depends on position of item*/
    public String getHexBackgroundColor() { return hexBackgroundColor; }
    public void setHexBackgroundColor(String hexBackgroundColor) { this.hexBackgroundColor = hexBackgroundColor; }





    /************************************************/
    /************************************************/
    /**************  PRIVATE METHODS ****************/
    /************************************************/
    /************************************************/


    /* Return position of date in listView ; Return -1 if date not exist */
    private int positionOfDate(Date date){
        int position = -1;
        for(int i=0;i<mListDays.size();i++){if(isDatesDaysEquals(date, mListDays.get(i))){ position = i; }}
        return position;
    }
    /* Return true if dates are equals */
    private boolean isDatesDaysEquals(Date date1, Date date2){
        if(dateFormat.format(date1).equals(dateFormat.format(date2))) return true; else return false;
    }
    /* Center KBAdapter to an item with its position */
    private void centerToPosition(int position){
        CenterToPositionTask centerTask = new CenterToPositionTask();
        centerTask.position = position;
        centerTask.execute();
    }
    /* Return position of selected date on center of screen*/
    private int getPositionOfCenterItem(){
        return mListView.getFirstVisiblePosition() + numberOfRowOnScreen / 2;
    }

    private class CenterToPositionTask extends AsyncTask<Void, Void, String[]> {
        int position;
        protected void onPreExecute() {

            if(position != -1){
                mListView.post(new Runnable() {
                    @Override
                    public void run() {
                        //Selection item is the firstItemVisible
                        //So the selected item is set to the position - the number of row /2
                        int shiftCells = numberOfRowOnScreen / 2;
                        if(position - shiftCells > 0){
                            mListView.setSelection(position - shiftCells);
                        }else{
                            mListView.setSelection(position);
                        }
                    }
                });
            }
        }

        @Override
        protected String[] doInBackground(Void... params) {
            try {Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            //Refresh the adapter to draw background cells color
            int shiftCells = numberOfRowOnScreen / 2;
            positionOfCenterItem = position + shiftCells;
            mCalendarAdapter.notifyDataSetChanged();
        }
    }

    private class InitTask extends AsyncTask<Void, Void, String[]> {
        protected void onPreExecute() {
        }

        @Override
        protected String[] doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);

            goToday();
        }
    }


    public interface IKBAgendaEvent {

        public void onDateSelect(Date date);
        public void onListScroll(Date date);

    }
}
