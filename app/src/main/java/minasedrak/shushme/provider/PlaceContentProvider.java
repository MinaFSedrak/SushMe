package minasedrak.shushme.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by MinaSedrak on 7/26/2017.
 */

public class PlaceContentProvider extends ContentProvider {


    public static final int PLACES = 100;
    public static final int PLACES_WITH_ID = 101;

    public static final UriMatcher mUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {

        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(PlaceContract.AUTHORITY, PlaceContract.PATH_PLACES, PLACES);
        uriMatcher.addURI(PlaceContract.AUTHORITY, PlaceContract.PATH_PLACES + "/#", PLACES_WITH_ID);

        return uriMatcher;
    }

    private PlaceDbHelper mPlaceDbHelper;


    @Override
    public boolean onCreate() {
        Context context = getContext();
        mPlaceDbHelper = new PlaceDbHelper(context);

        return true;
    }


    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mPlaceDbHelper.getReadableDatabase();

        int matching = mUriMatcher.match(uri);
        Cursor returningCursor;

        switch (matching){
            case PLACES:
                returningCursor = db.query(PlaceContract.PlaceEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri "+ uri);
        }

        returningCursor.setNotificationUri(getContext().getContentResolver(), uri);

        return returningCursor;
    }


    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {

        final SQLiteDatabase db = mPlaceDbHelper.getWritableDatabase();

        int matching = mUriMatcher.match(uri);
        Uri returningUri;

        switch (matching){

            case PLACES:
                long id = db.insert(PlaceContract.PlaceEntry.TABLE_NAME, null, values);
                if( id > 0){
                    returningUri = ContentUris.withAppendedId(PlaceContract.PlaceEntry.CONTENT_URI, id);
                }else {
                    throw new SQLiteException("Failed to insert data to " + uri);
                }
                break;

            default:
                throw new UnsupportedOperationException("Unknown Uri " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returningUri;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mPlaceDbHelper.getWritableDatabase();

        int matching = mUriMatcher.match(uri);
        int deletedPlaces;

        switch (matching){
            case PLACES_WITH_ID:
                String place_id = uri.getPathSegments().get(1);

                deletedPlaces = db.delete(PlaceContract.PlaceEntry.TABLE_NAME, "_id=?", new String[]{place_id});
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        if( deletedPlaces != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return deletedPlaces;
    }


    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mPlaceDbHelper.getWritableDatabase();

        int matching = mUriMatcher.match(uri);
        int updatedPlaces;

        switch (matching){
            case PLACES_WITH_ID:
                String place_id = uri.getPathSegments().get(1);

                updatedPlaces = db.update(PlaceContract.PlaceEntry.TABLE_NAME, values,"_id=?", new String[]{place_id});
                break;

            default:
                throw new UnsupportedOperationException("Unknown uri " + uri);
        }

        if( updatedPlaces != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return updatedPlaces;
    }

}
