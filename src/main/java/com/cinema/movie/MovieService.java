package com.cinema.movie;

import java.util.ArrayList;
import java.util.List;

public class MovieService {

    private final MovieDAO movieDAO;

    public MovieService() {
        movieDAO = new MovieDAO();
    }

    public List<MovieResponse> getAllMovies() {

        List<Movie> movies = movieDAO.findAll();

        List<MovieResponse> movieResponses = new ArrayList<>();

        for (Movie movie : movies) {

            MovieResponse movieResponse = new MovieResponse(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDurationMinutes()
            );

            movieResponses.add(movieResponse);
        }

        return movieResponses;
    }
}
