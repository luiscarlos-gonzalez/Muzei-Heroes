package com.kentverger.muzeiheroes;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Random;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.kentverger.muzeiheroes.MarvelAPI.MarvelResponse;
import com.kentverger.muzeiheroes.MarvelAPI.Results;

import android.net.Uri;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class MuzeiHeroesArtSource extends RemoteMuzeiArtSource {
	
    private static final String TAG = "MuzeiHeroes";
    private static final String SOURCE_NAME = "MuzeiHeroesArtSource";
    
    private static final int ROTATE_TIME_MILLIS = 24 * 60 * 60 * 1000;

	public MuzeiHeroesArtSource() {
		super(SOURCE_NAME);
	}
	
    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

	@Override
	protected void onTryUpdate(int arg0) throws RetryException {
		String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
		
		RestAdapter restAdapter = new RestAdapter.Builder()
		.setEndpoint("http://gateway.marvel.com:80")
        .setErrorHandler(new ErrorHandler() {
            @Override
            public Throwable handleError(RetrofitError retrofitError) {
                int statusCode = retrofitError.getResponse().getStatus();
                if (retrofitError.isNetworkError()
                        || (500 <= statusCode && statusCode < 600)) {
                	Log.d(TAG, "Some error code: "+statusCode);
                    return new RetryException();
                }
                scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                return retrofitError;
            }
        })
        .build();
		
        MarvelAPI service = restAdapter.create(MarvelAPI.class);
        MarvelResponse response = null;
        try{
        	Date date = new Date();
        	Timestamp ts = new Timestamp(date.getTime());
        	MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	String dataToHash = ts.toString()+"e0f58eefb6fd81198f96f1c38f4682d49db8cce6"+"6b0b003c1e223740941e322c52292001";
        	byte[] hash = md.digest(dataToHash.getBytes());
        	BigInteger bigInt = new BigInteger(1,hash);
        	String hashtext = bigInt.toString(16);
        	response = service.getCharacters(ts.toString(), hashtext, "6b0b003c1e223740941e322c52292001");
        }catch(RetrofitError e){
        	Log.d(TAG, e.getResponse().getStatus()+" "+ e.getResponse().getReason()+" "+e.getResponse().getUrl());
        }
        
        if (response == null || response.data.results == null) {
            throw new RetryException();
        }

        if (response.data.results.size() == 0) {
            Log.w(TAG, "No photos returned from API.");
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
            return;
        }
        
        Random random = new Random();
        Results character;
        String token;
        while (true) {
        	character = response.data.results.get(random.nextInt(response.data.results.size()));
            token = Integer.toString(character.id);
            if (response.data.results.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }
        
        Log.d(TAG,character.thumbnail.path+"."+character.thumbnail.extension);

        publishArtwork(new Artwork.Builder()
                .title(character.name)
                .byline(character.description)
                .imageUri(Uri.parse(character.thumbnail.path+"."+character.thumbnail.extension))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(character.thumbnail.path+"."+character.thumbnail.extension)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);

	}


}
