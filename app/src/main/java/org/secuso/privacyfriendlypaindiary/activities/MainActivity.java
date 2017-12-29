/*
 This file is part of Privacy Friendly App Example.

 Privacy Friendly App Example is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly App Example is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly App Example. If not, see <http://www.gnu.org/licenses/>.
 */

package org.secuso.privacyfriendlypaindiary.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.secuso.privacyfriendlypaindiary.R;
import org.secuso.privacyfriendlypaindiary.database.DBService;
import org.secuso.privacyfriendlypaindiary.database.DBServiceInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.BodyRegion;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.Condition;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.PainQuality;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.Time;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.DiaryEntryInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.PainDescriptionInterface;
import org.secuso.privacyfriendlypaindiary.helpers.EventDecorator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Christopher Beckmann, Karola Marky, Susanne Felsen
 * @version 20171227
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int COLOR_MIDDLEBLUE = Color.parseColor("#8aa5ce");

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    private MaterialCalendarView calendar;
    private EventDecorator decorator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendar = (MaterialCalendarView) findViewById(R.id.calendar_view);
//        calendar.setSelectedDate(CalendarDay.today());
        decorator = new EventDecorator(COLOR_MIDDLEBLUE);
        calendar.addDecorator(decorator);
        calendar.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                getDiaryEntryDates(date.getMonth(), date.getYear());
            }
        });
        calendar.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                //checks whether there is already an entry on this date, creates one if there is not
                if(!decorator.shouldDecorate(date)) {
                    createDiaryEntry(date.getDate());
                } else {
//                    editDiaryEntry(date.getDate());
                    viewDiaryEntry(date.getDate());
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        calendar.state().edit()
                .setMaximumDate(CalendarDay.today())
                .commit();
        getDiaryEntryDates(CalendarDay.today().getMonth(), CalendarDay.today().getYear());

    }

    private void getDiaryEntryDates(int month, int year) {
        DBServiceInterface service = DBService.getInstance(this);

        Calendar c = Calendar.getInstance();
        if (month > 0) {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month - 1);
        } else {
            c.set(Calendar.YEAR, year - 1);
            c.set(Calendar.MONTH, 11);
        }
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH) - 6);
        Date startDate = c.getTime();
        if (month < 11) {
            c.set(Calendar.MONTH, month + 1);
        } else {
            c.set(Calendar.YEAR, year + 1);
            c.set(Calendar.MONTH, 0);
        }
        c.set(Calendar.DAY_OF_MONTH, 7);
        Date endDate = c.getTime();

        Set<Date> dates = service.getDiaryEntryDatesByTimeSpan(startDate, endDate);
        Set<CalendarDay> calendarDates = new HashSet<>();
        for (Date date : dates) {
            calendarDates.add(CalendarDay.from(date));
        }
        decorator.setDates(calendarDates);
        calendar.invalidateDecorators();
    }

    /**
     * This method connects the Activity to the menu item
     *
     * @return ID of the menu item it belongs to
     */
    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_main;
    }

    private void createDiaryEntry(Date date) {
        Intent intent = new Intent(MainActivity.this, DiaryEntryActivity.class);
        intent.putExtra("DATE_OF_ENTRY", dateFormat.format(date));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void viewDiaryEntry(Date date) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setView(initDiaryEntrySummary(date));
        alertDialog.setPositiveButton("OK", null);
        AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private View initDiaryEntrySummary(Date date) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.diaryentry_summary, null);

        DBServiceInterface service = DBService.getInstance(this);
        DiaryEntryInterface diaryEntry = service.getDiaryEntryByDate(date);
        PainDescriptionInterface painDescription = diaryEntry.getPainDescription();

        ((TextView) view.findViewById(R.id.date)).setText(dateFormat.format(date));
        if(diaryEntry.getNotes() != null) {
            ((TextView) view.findViewById(R.id.notes_value)).setText(diaryEntry.getNotes());
        }
        if(diaryEntry.getCondition() != null) {
            ((ImageView) view.findViewById(R.id.condition_icon)).setImageResource(getResourceIDForCondition(diaryEntry.getCondition()));
        }
        if(painDescription != null) {
            ((TextView) view.findViewById(R.id.painlevel_value)).setText(Integer.toString(painDescription.getPainLevel()));
            BodyRegion bodyRegion = painDescription.getBodyRegion();
            if(bodyRegion != null) {
                int resourceID = getResourceIDForBodyRegion(bodyRegion);
                if(resourceID != 0) {
                    ((ImageView) view.findViewById(R.id.bodyregion)).setImageResource(resourceID);
                    view.findViewById(R.id.bodyregion).setVisibility(View.VISIBLE);
                }
            }
            String painQualities = convertPainQualityEnumSetToString(painDescription.getPainQualities());
            if(painQualities != null) {
                ((TextView) view.findViewById(R.id.painquality_value)).setText(painQualities);
            }
            String timesOfPain = convertTimeEnumSetToString(painDescription.getTimesOfPain());
            if(timesOfPain != null) {
                ((TextView) view.findViewById(R.id.timeofpain_value)).setText(timesOfPain);
            }
        }
        //TODO: medication

        return view;
    }

    private int getResourceIDForCondition(Condition condition) {
        switch(condition) {
            case VERY_BAD:
                return R.drawable.ic_sentiment_very_dissatisfied;
            case BAD:
                return R.drawable.ic_sentiment_dissatisfied_black;
            case OKAY:
                return R.drawable.ic_sentiment_neutral_black;
            case GOOD:
                return R.drawable.ic_sentiment_satisfied_black;
            case VERY_GOOD:
                return R.drawable.ic_sentiment_very_satisfied_black;
            default:
                return R.drawable.ic_sentiment_neutral_black;
        }
    }

    private int getResourceIDForBodyRegion(BodyRegion bodyRegion) {
        switch(bodyRegion) {
            case ABDOMEN_RIGHT:
                return R.drawable.schmerztagebuch_person_bauch_rechts;
            case ABDOMEN_LEFT:
                return R.drawable.schmerztagebuch_person_bauch_links;
            case GROIN_LEFT:
                return R.drawable.schmerztagebuch_person_leiste_links;
            case GROIN_RIGHT:
                return R.drawable.schmerztagebuch_person_leiste_rechts;
            case THIGH_LEFT:
                return R.drawable.schmerztagebuch_person_oberschenkel_links;
            case THIGH_RIGHT:
                return R.drawable.schmerztagebuch_person_oberarm_rechts;
            case KNEE_LEFT:
                return R.drawable.schmerztagebuch_person_knie_links;
            case KNEE_RIGHT:
                return R.drawable.schmerztagebuch_person_knie_rechts;
            case LOWER_LEG_LEFT:
                return R.drawable.schmerztagebuch_person_unterschenkel_links;
            case LOWER_LEG_RIGHT:
                return R.drawable.schmerztagebuch_person_unterschenkel_rechts;
            case FOOT_LEFT:
                return R.drawable.schmerztagebuch_person_fuss_links;
            case FOOT_RIGHT:
                return R.drawable.schmerztagebuch_person_fuss_rechts;
            case CHEST_LEFT:
                return R.drawable.schmerztagebuch_person_brust_links;
            case CHEST_RIGHT:
                return R.drawable.schmerztagebuch_person_brust_rechts;
            case NECK:
                return R.drawable.schmerztagebuch_person_hals;
            case HEAD:
                return R.drawable.schmerztagebuch_person_kopf;
            case UPPER_ARM_LEFT:
                return R.drawable.schmerztagebuch_person_oberarm_links;
            case UPPER_ARM_RIGHT:
                return R.drawable.schmerztagebuch_person_oberarm_rechts;
            case LOWER_ARM_LEFT:
                return R.drawable.schmerztagebuch_person_unterarm_links;
            case LOWER_ARM_RIGHT:
                return R.drawable.schmerztagebuch_person_unterarm_rechts;
            case HAND_LEFT:
                return R.drawable.schmerztagebuch_person_hand_links;
            case HAND_RIGHT:
                return R.drawable.schmerztagebuch_person_hand_rechts;
            default:
                return 0;
        }
    }

    private String convertPainQualityEnumSetToString(EnumSet<PainQuality> painQualities) {
        String painQualitiesAsString = "";
        for(PainQuality quality : painQualities) {
            painQualitiesAsString += getString(quality.getResourceID()) + ", ";
        }
        if(!painQualitiesAsString.isEmpty()) {
            painQualitiesAsString = painQualitiesAsString.substring(0, painQualitiesAsString.length() - 2);
        } else {
            painQualitiesAsString = null;
        }
        return painQualitiesAsString;
    }

    private String convertTimeEnumSetToString(EnumSet<Time> times) {
        String timesAsString = "";
        for(Time time : times) {
            timesAsString += getString(time.getResourceID()) + ", ";
        }
        if(!timesAsString.isEmpty()) {
            timesAsString = timesAsString.substring(0, timesAsString.length() - 2);
        } else {
            timesAsString = null;
        }
        return timesAsString;
    }

    private void editDiaryEntry(Date date) {
        Intent intent = new Intent(MainActivity.this, DiaryEntryActivity.class);
        intent.putExtra("DATE_OF_ENTRY", dateFormat.format(date));
        intent.putExtra("EDIT", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}