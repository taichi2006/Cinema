package com.cinema.Model.Service;

import com.cinema.Model.DAO.MovieDAO;
import com.cinema.Model.DTO.MovieDTO;
import com.cinema.Model.Entity.Movie;

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