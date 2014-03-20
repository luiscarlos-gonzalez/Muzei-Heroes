package com.kentverger.muzeiheroes;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Query;

public interface MarvelAPI {
	@GET("/v1/public/characters")
	MarvelResponse getCharacters(@Query("ts") String timestamp, @Query("hash") String hash, @Query("apikey") String apikey, @Query("limit") String limit, @Query("offset") String offset);
	
	static class MarvelResponse{
		Data data;
	}
	
	static class Data{
		List<Results> results;
	}
	
	static class Results{
		int id;
		String name;
		String description;
		Thumbnail thumbnail;
		List<Urls> urls;
		
	}
	
	static class Thumbnail{
		String path;
		String extension;
	}
	
	static class Urls{
		String type;
		String url;
	}
}
