package com.cinema.movie;

import java.util.ArrayList;
import java.util.List;

public class MovieService {

    private final MovieDAO movieDAO;

    public MovieService() {
        movieDAO = new MovieDAO();
    }

    public List<MovieDTO> getAllMovies() {

        List<Movie> movies = movieDAO.findAll();

        List<MovieDTO> movieDTOs = new ArrayList<>();

        for (Movie movie : movies) {

            MovieDTO movieDTO = new MovieDTO(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDurationMinutes()
            );

            movieDTOs.add(movieDTO);
        }

        return movieDTOs;
    }
}
