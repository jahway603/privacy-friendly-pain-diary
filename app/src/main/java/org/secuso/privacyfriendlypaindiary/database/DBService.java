package org.secuso.privacyfriendlypaindiary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.secuso.privacyfriendlypaindiary.database.entities.enums.BodyRegion;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.Condition;
import org.secuso.privacyfriendlypaindiary.database.entities.enums.Gender;
import org.secuso.privacyfriendlypaindiary.database.entities.impl.DiaryEntry;
import org.secuso.privacyfriendlypaindiary.database.entities.impl.Drug;
import org.secuso.privacyfriendlypaindiary.database.entities.impl.DrugIntake;
import org.secuso.privacyfriendlypaindiary.database.entities.impl.PainDescription;
import org.secuso.privacyfriendlypaindiary.database.entities.impl.User;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.DiaryEntryInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.DrugIntakeInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.DrugInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.PainDescriptionInterface;
import org.secuso.privacyfriendlypaindiary.database.entities.interfaces.UserInterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Karola Marky, Susanne Felsen
 * @version 20171121
 *          Structure based on http://www.androidhive.info/2011/11/android-sqlite-database-tutorial/ (last access: 18.11.17)
 *          <p>
 *          This class defines the structure of our database.
 */
public class DBService extends SQLiteOpenHelper implements DBServiceInterface {

    private static DBService instance = null;

    private static final String TAG = DBService.class.getSimpleName();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "paindiary";

    public static final String DATE_PATTERN = "dd.MM.yyyy";

    private DBService(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static DBService getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new DBService(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAll(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAll(db);
        onCreate(db);
    }

    private void createAll(SQLiteDatabase db) {
        db.execSQL(User.TABLE_CREATE);
        db.execSQL(Drug.TABLE_CREATE);
        db.execSQL(PainDescription.TABLE_CREATE);
        db.execSQL(DiaryEntry.TABLE_CREATE);
        db.execSQL(DrugIntake.TABLE_CREATE);
        Log.i(TAG, "Created database.");
    }

    private void dropAll(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + User.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Drug.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PainDescription.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DiaryEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DrugIntake.TABLE_NAME);
    }

    @Override
    public long storeUser(UserInterface user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = getUserContentValues(user);

        long id = db.insert(User.TABLE_NAME, null, values);
        Log.d(TAG, "Created user.");

        return id;
    }

    @Override
    public void updateUser(UserInterface user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = getUserContentValues(user);
        db.update(User.TABLE_NAME, values, User.COLUMN_ID + " = ?",
                new String[]{String.valueOf(user.getObjectID())});
    }

    private ContentValues getUserContentValues(UserInterface user) {
        ContentValues values = new ContentValues();
        values.put(User.COLUMN_FIRST_NAME, user.getFirstName());
        values.put(User.COLUMN_LAST_NAME, user.getLastName());
        values.put(User.COLUMN_GENDER, user.getGender().getValue());
        Date dateOfBirth = user.getDateOfBirth();
        if (dateOfBirth != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            values.put(User.COLUMN_DATE_OF_BIRTH, dateFormat.format(dateOfBirth));
        }
        return values;
    }

    @Override
    public void deleteUser(UserInterface user) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d(TAG, "Deleted user with name '" + user.getFirstName() + " " + user.getLastName() + "'.");
        db.delete(User.TABLE_NAME, User.COLUMN_ID + " = ?",
                new String[]{Long.toString(user.getObjectID())});
    }

    @Override
    public UserInterface getUserByID(long id) {
//        Log.d("DATABASE", Integer.toString(id));
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(User.TABLE_NAME, null, User.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        UserInterface user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = instantiateUserFromCursor(cursor);
        }
        cursor.close();

        return user;
    }

    private UserInterface instantiateUserFromCursor(Cursor cursor) {
        long objectID = cursor.getLong(cursor.getColumnIndex(User.COLUMN_ID));
        int indexFirstName = cursor.getColumnIndex(User.COLUMN_FIRST_NAME);
        String firstName = null;
        if (!cursor.isNull(indexFirstName)) {
            firstName = cursor.getString(indexFirstName);
        }
        int indexLastName = cursor.getColumnIndex(User.COLUMN_LAST_NAME);
        String lastName = null;
        if (!cursor.isNull(indexLastName)) {
            lastName = cursor.getString(indexLastName);
        }
        int indexGender = cursor.getColumnIndex(User.COLUMN_GENDER);
        Gender gender = null;
        if (!cursor.isNull(indexGender)) {
            gender = Gender.valueOf(cursor.getInt(indexGender));
        }
        int indexDate = cursor.getColumnIndex(User.COLUMN_DATE_OF_BIRTH);
        Date dateOfBirth = null;
        if (!cursor.isNull(indexDate)) {
            String date = cursor.getString(indexDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            try {
                dateOfBirth = dateFormat.parse(date);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing user date of birth." + e);
            }
        }
        UserInterface user = new User(firstName, lastName, gender, dateOfBirth);
        user.setObjectID(objectID);

        return user;
    }

    @Override
    public long storeDiaryEntryAndAssociatedObjects(DiaryEntryInterface diaryEntry) {
        SQLiteDatabase db = this.getWritableDatabase();

        PainDescriptionInterface painDescription = diaryEntry.getPainDescription();
        long painDescriptionID = storePainDescription(painDescription);

        ContentValues values = new ContentValues();
        values.put(PainDescription.TABLE_NAME + "_id", painDescriptionID); //foreign key
        Date date = diaryEntry.getDate();
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            values.put(DiaryEntry.COLUMN_DATE, dateFormat.format(date));
        }
        values.put(DiaryEntry.COLUMN_CONDITION, diaryEntry.getCondition().getValue());
        values.put(DiaryEntry.COLUMN_NOTES, diaryEntry.getNotes());
        long diaryEntryID = db.insert(DiaryEntry.TABLE_NAME, null, values);

        for (DrugIntakeInterface intake : diaryEntry.getDrugIntakes()) {
            intake.getDiaryEntry().setObjectID(diaryEntryID); //maybe get diary entry from database instead
            storeDrugIntakeAndAssociatedDrug(intake);
        }
        return diaryEntryID;
    }

    @Override
    public void updateDiaryEntryAndAssociatedObjects(DiaryEntryInterface diaryEntry) {
        SQLiteDatabase db = this.getWritableDatabase();
        PainDescriptionInterface painDescription = diaryEntry.getPainDescription();
        updatePainDescription(painDescription);

        ContentValues values = new ContentValues();
        values.put(PainDescription.TABLE_NAME + "_id", painDescription.getObjectID()); //foreign key
        Date date = diaryEntry.getDate();
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            values.put(DiaryEntry.COLUMN_DATE, dateFormat.format(date));
        }
        values.put(DiaryEntry.COLUMN_CONDITION, diaryEntry.getCondition().getValue());
        values.put(DiaryEntry.COLUMN_NOTES, diaryEntry.getNotes());
        db.update(DiaryEntry.TABLE_NAME, values, DiaryEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(diaryEntry.getObjectID())});

        Set<DrugIntakeInterface> oldIntakes = getDrugIntakesForDiaryEntry(diaryEntry.getObjectID());
        Set<DrugIntakeInterface> newIntakes = diaryEntry.getDrugIntakes();
        Set<Long> newIntakeIDs = new HashSet<>();
        for(DrugIntakeInterface intake : newIntakes) {
            if(!intake.isPersistent()) {
                storeDrugIntakeAndAssociatedDrug(intake);
            } else {
                newIntakeIDs.add(intake.getObjectID());
                updateDrugIntake(intake);
            }
        }
        //all drug intake objects that are no longer associated with the diary entry object are deleted
        for(DrugIntakeInterface intake : oldIntakes) {
            if(!newIntakeIDs.contains(intake.getObjectID())) {
                deleteDrugIntake(intake);
            }
        }
    }

    @Override
    public void deleteDiaryEntryAndAssociatedObjects(DiaryEntryInterface diaryEntry) {
        deletePainDescription(diaryEntry.getPainDescription());
        for(DrugIntakeInterface intake : diaryEntry.getDrugIntakes()) {
            deleteDrugIntake(intake);
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DiaryEntry.TABLE_NAME, DiaryEntry.COLUMN_ID + " = ?",
                new String[]{Long.toString(diaryEntry.getObjectID())});
    }

    @Override
    public DiaryEntryInterface getDiaryEntryByID(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(DiaryEntry.TABLE_NAME, null, DiaryEntry.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        DiaryEntryInterface diaryEntry = null;
        if (cursor != null && cursor.moveToFirst()) {
            diaryEntry = instantiateDiaryEntryFromCursor(cursor);
        }
        cursor.close();

        return diaryEntry;
    }

    /**
     * Fetches the list of diary entries for the given date from the database.
     *
     * @param date
     * @return list of diary entries for the given date --> list should only contain one element (one entry per day)
     */
    public List<DiaryEntryInterface> getDiaryEntriesByDate(Date date) {
        List<DiaryEntryInterface> diaryEntries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);

        Cursor cursor = db.query(DiaryEntry.TABLE_NAME, null, DiaryEntry.COLUMN_DATE + "=?",
                new String[]{dateFormat.format(date)}, null, null, null, null);

        DiaryEntryInterface diaryEntry = null;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                diaryEntry = instantiateDiaryEntryFromCursor(cursor);
                diaryEntries.add(diaryEntry);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return diaryEntries;
    }

    /**
     * Instantiates a diary entry object from the given cursor.
     *
     * @param cursor
     * @return diary entry object with associated pain description and drug intakes.
     */
    private DiaryEntryInterface instantiateDiaryEntryFromCursor(Cursor cursor) {
        long objectID = cursor.getLong(cursor.getColumnIndex(DiaryEntry.COLUMN_ID));
        int indexDate = cursor.getColumnIndex(DiaryEntry.COLUMN_DATE);
        Date dateOfEntry = null;
        if (!cursor.isNull(indexDate)) {
            String date = cursor.getString(indexDate);
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            try {
                dateOfEntry = dateFormat.parse(date);
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing diary entry date." + e);
            }
        }
        DiaryEntryInterface diaryEntry = new DiaryEntry(dateOfEntry);
        diaryEntry.setObjectID(objectID);

        int indexNotes = cursor.getColumnIndex(DiaryEntry.COLUMN_NOTES);
        if (!cursor.isNull(indexNotes)) {
            diaryEntry.setNotes(cursor.getString(indexNotes));
        }
        diaryEntry.setCondition(Condition.valueOf(cursor.getInt(cursor.getColumnIndex(DiaryEntry.COLUMN_CONDITION))));

        long painDescriptionID = cursor.getLong(cursor.getColumnIndex(PainDescription.TABLE_NAME + "_id"));
        PainDescriptionInterface painDescription = getPainDescriptionByID(painDescriptionID);
        diaryEntry.setPainDescription(painDescription);

        Set<DrugIntakeInterface> intakes = getDrugIntakesForDiaryEntry(objectID);
        for(DrugIntakeInterface intake : intakes) {
            diaryEntry.addDrugIntake(intake);
        }

        return diaryEntry;
    }

    private long storePainDescription(PainDescriptionInterface painDescription) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(PainDescription.COLUMN_PAIN_LEVEL, painDescription.getPainLevel());
        values.put(PainDescription.COLUMN_BODY_REGION, painDescription.getBodyRegion().getValue());
        //TODO
        return db.insert(PainDescription.TABLE_NAME, null, values);
    }

    private void updatePainDescription(PainDescriptionInterface painDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PainDescription.COLUMN_PAIN_LEVEL, painDescription.getPainLevel());
        values.put(PainDescription.COLUMN_BODY_REGION, painDescription.getBodyRegion().getValue());
        //TODO
        db.update(PainDescription.TABLE_NAME, values, Drug.COLUMN_ID + " = ?",
                new String[]{String.valueOf(painDescription.getObjectID())});
    }

    private void deletePainDescription(PainDescriptionInterface painDescription) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(PainDescription.TABLE_NAME, PainDescription.COLUMN_ID + " = ?",
                new String[]{Long.toString(painDescription.getObjectID())});
    }

    private PainDescriptionInterface getPainDescriptionByID(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(PainDescription.TABLE_NAME, null, PainDescription.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        PainDescriptionInterface painDescription = null;
        if (cursor != null && cursor.moveToFirst()) {
            painDescription = instantiatePainDescriptionFromCursor(cursor);
        }
        cursor.close();

        return painDescription;
    }

    private PainDescriptionInterface instantiatePainDescriptionFromCursor(Cursor cursor) {
        long objectID = cursor.getLong(cursor.getColumnIndex(PainDescription.COLUMN_ID));
        int painLevel = cursor.getInt(cursor.getColumnIndex(PainDescription.COLUMN_PAIN_LEVEL));
        BodyRegion bodyRegion = BodyRegion.valueOf(cursor.getInt(cursor.getColumnIndex(PainDescription.COLUMN_BODY_REGION)));
        PainDescriptionInterface painDescription = new PainDescription(painLevel, bodyRegion);
        painDescription.setObjectID(objectID);
        //TODO
        return painDescription;
    }

    /**
     * Stores the given drug intake and the associated drug (if not yet persistent).
     *
     * @param intake intake to be stored; associated diary entry object has to be persistent (objectID must be set)
     * @return
     */
    private long storeDrugIntakeAndAssociatedDrug(DrugIntakeInterface intake) {
        SQLiteDatabase db = this.getWritableDatabase();
        DrugInterface drug = intake.getDrug();
        long drugID;
        if (!drug.isPersistent()) {
            drugID = storeDrug(drug);
        } else {
            drugID = drug.getObjectID();
        }
        ContentValues values = getDrugIntakeContentValues(intake);
        values.put(Drug.TABLE_NAME + "_id", drugID);
        values.put(DiaryEntry.TABLE_NAME + "_id", intake.getDiaryEntry().getObjectID());
        return db.insert(DrugIntake.TABLE_NAME, null, values);
    }

    /**
     * Updates the given drug intake. Associated drug can not be updated (call {@link DBService#deleteDrugIntake(DrugIntakeInterface)}
     * and {@link DBService#storeDrugIntakeAndAssociatedDrug(DrugIntakeInterface)} instead.)
     *
     * @param intake drug intake to update; must be persistent (see {@link DrugIntakeInterface#isPersistent()})
     */
    private void updateDrugIntake(DrugIntakeInterface intake) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = getDrugIntakeContentValues(intake);
        db.update(DrugIntake.TABLE_NAME, values, User.COLUMN_ID + " = ?",
                new String[]{String.valueOf(intake.getObjectID())});
    }

    /**
     * Returns a ContentValues object containing all non-foreign key columns and values.
     *
     * @param intake
     * @return
     */
    private ContentValues getDrugIntakeContentValues(DrugIntakeInterface intake) {
        ContentValues values = new ContentValues();
        values.put(DrugIntake.COLUMN_MORNING, intake.getQuantityMorning());
        values.put(DrugIntake.COLUMN_NOON, intake.getQuantityNoon());
        values.put(DrugIntake.COLUMN_EVENING, intake.getQuantityEvening());
        values.put(DrugIntake.COLUMN_NIGHT, intake.getQuantityNight());
        return values;
    }

    /**
     * Deletes the given drug intake object. The associated drug is not deleted from the database.
     *
     * @param intake drug intake to delete
     */
    private void deleteDrugIntake(DrugIntakeInterface intake) {
        SQLiteDatabase db = this.getWritableDatabase();
        DrugInterface drug = intake.getDrug();
        db.delete(DrugIntake.TABLE_NAME, DrugIntake.COLUMN_ID + " = ?",
                new String[]{Long.toString(intake.getObjectID())});
    }

    public Set<DrugIntakeInterface> getDrugIntakesForDiaryEntry(long diaryEntryID) {
        Set<DrugIntakeInterface> intakes = new HashSet<>();

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT  * FROM " + DrugIntake.TABLE_NAME + " WHERE " + DiaryEntry.TABLE_NAME + "_id = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(diaryEntryID)});

        DrugIntakeInterface intake = null;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                intake = instantiateDrugIntakeFromCursor(cursor);
                intakes.add(intake);
            } while (cursor.moveToNext());
        }

        return intakes;
    }

    /**
     * Instantiates a drug intake object from the given cursor and attaches the associated drug object. Diary entry field is not set!
     *
     * @param cursor
     * @return
     */
    private DrugIntakeInterface instantiateDrugIntakeFromCursor(Cursor cursor) {
        long objectID = cursor.getLong(cursor.getColumnIndex(DrugIntake.COLUMN_ID));
        int quantityMorning = cursor.getInt(cursor.getColumnIndex(DrugIntake.COLUMN_MORNING));
        int quantityNoon = cursor.getInt(cursor.getColumnIndex(DrugIntake.COLUMN_NOON));
        int quantityEvening = cursor.getInt(cursor.getColumnIndex(DrugIntake.COLUMN_EVENING));
        int quantityNight = cursor.getInt(cursor.getColumnIndex(DrugIntake.COLUMN_NIGHT));
        long drugID = cursor.getLong(cursor.getColumnIndex(Drug.TABLE_NAME + "_id"));
        DrugInterface drug = getDrugByID(drugID);
        DrugIntakeInterface intake = new DrugIntake(drug, quantityMorning, quantityNoon, quantityEvening, quantityNight);
        intake.setObjectID(objectID);
        return intake;
    }

    @Override
    public long storeDrug(DrugInterface drug) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Drug.COLUMN_NAME, drug.getName());
        values.put(Drug.COLUMN_DOSE, drug.getDose());
        values.put(Drug.COLUMN_CURRENTLY_TAKEN, drug.isCurrentlyTaken());

        long id = db.insert(Drug.TABLE_NAME, null, values);
        Log.d(TAG, "Created drug.");

        return id;
    }

    @Override
    public void updateDrug(DrugInterface drug) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Drug.COLUMN_NAME, drug.getName());
        values.put(Drug.COLUMN_DOSE, drug.getDose());
        values.put(Drug.COLUMN_CURRENTLY_TAKEN, drug.isCurrentlyTaken());

        db.update(Drug.TABLE_NAME, values, Drug.COLUMN_ID + " = ?",
                new String[]{String.valueOf(drug.getObjectID())});
    }

    @Override
    public void deleteDrug(DrugInterface drug) {
        SQLiteDatabase database = this.getWritableDatabase();
        String selectQuery = "SELECT COUNT(*) FROM " + DrugIntake.TABLE_NAME + " WHERE " + Drug.TABLE_NAME + "_id = ?";
        Cursor cursor = database.rawQuery(selectQuery, new String[]{Long.toString(drug.getObjectID())});

        int count = -1;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        if (count == 0) {
            database.delete(Drug.TABLE_NAME, Drug.COLUMN_ID + " = ?",
                    new String[]{Long.toString(drug.getObjectID())});
        }
    }

    @Override
    public DrugInterface getDrugByID(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(Drug.TABLE_NAME, null, Drug.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null, null);
        DrugInterface drug = null;
        if (cursor != null && cursor.moveToFirst()) {
            drug = instantiateDrugFromCursor(cursor);
        }
        cursor.close();

        return drug;
    }

    @Override
    public List<DrugInterface> getAllDrugs() {
        List<DrugInterface> drugs = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + Drug.TABLE_NAME;

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        DrugInterface drug = null;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                drug = instantiateDrugFromCursor(cursor);
                drugs.add(drug);
            } while (cursor.moveToNext());
        }

        return drugs;
    }

    @Override
    public List<DrugInterface> getAllCurrentlyTakenDrugs() {
        List<DrugInterface> drugs = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + Drug.TABLE_NAME + "WHERE " + Drug.COLUMN_CURRENTLY_TAKEN + " = 1";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);

        DrugInterface drug = null;

        if (cursor != null && cursor.moveToFirst()) {
            do {
                drug = instantiateDrugFromCursor(cursor);
                drugs.add(drug);
            } while (cursor.moveToNext());
        }

        return drugs;
    }

    private DrugInterface instantiateDrugFromCursor(Cursor cursor) {
        long objectID = cursor.getLong(cursor.getColumnIndex(Drug.COLUMN_ID));
        String name = cursor.getString(cursor.getColumnIndex(Drug.COLUMN_NAME));
        int indexDose = cursor.getColumnIndex(Drug.COLUMN_DOSE);
        String dose = null;
        if (!cursor.isNull(indexDose)) {
            dose = cursor.getString(indexDose);
        }
        int currentlyTakenInt = cursor.getInt(cursor.getColumnIndex(Drug.COLUMN_CURRENTLY_TAKEN));
        boolean currentlyTaken = true;
        DrugInterface drug = new Drug(name, dose);
        drug.setObjectID(objectID);
        if (currentlyTakenInt == 0) currentlyTaken = false;
        drug.setCurrentlyTaken(currentlyTaken);

        return drug;
    }

}
